import org.gradle.api.file.DuplicatesStrategy
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.register
import java.util.regex.Pattern.quote
import kotlin.io.path.exists

description = "Kotlin Compiler (Native Image)"

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
}

val nativeImageClasspath by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    nativeImageClasspath(project(":kotlin-compiler-embeddable", configuration = "runtimeElements"))

    testFixturesApi(libs.junit.jupiter.api)
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":generators:test-generator")))

    testRuntimeOnly(libs.junit.jupiter.engine)
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

projectTests {
    testData(project(":compiler").isolated, "testData/codegen")

    testGenerator(
        "org.jetbrains.kotlin.compiler.nativeimage.GenerateNativeImageTestsKt",
        generateTestsInBuildDirectory = true,
    )

    testTask(
        taskName = "nativeImageBoxTest",
        jUnitMode = JUnitMode.JUnit5,
        skipInLocalBuild = false,
    ) {
        description = "Compares native-image kotlinc against default kotlinc on " +
                "compiler/testData/codegen/box."
        addClasspathProperty(
            kotlincNativeImageDist.map { layout.files(it.destinationDir) },
            "kotlin.native-image.dist.path"
        )
    }

    withJvmStdlibAndReflect()
    withTestJar()
    withMockJdkRuntime()
}

val kotlincNativeImageTask = tasks.register<Exec>("kotlincNativeImage") {
    description = "Build a native image of the kotlin-compiler-embeddable"

    val resources = layout.projectDirectory.dir("resources")
    val classpathFiles = files(nativeImageClasspath, resources)
    inputs.files(nativeImageClasspath, resources)
        .withNormalizer(ClasspathNormalizer::class)
        .withPropertyName("nativeImageClasspath")

    val isWindows = OperatingSystem.current().isWindows
    val mainClass = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"
    val executableExtension = if (isWindows) ".exe" else ""
    val outputFile = layout.buildDirectory.file("bin/kotlinc-native-image$executableExtension")
    outputs.file(outputFile)

    val javaLauncher = javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(JdkMajorVersion.JDK_25_0.targetName))
        vendor.set(JvmVendorSpec.GRAAL_VM)
    }

    doFirst {
        val javaHome = javaLauncher.get().executablePath.asFile.toPath().parent.parent

        val nativeImageName = if (isWindows) "native-image.cmd" else "native-image"
        val nativeImageBin = javaHome.resolve("lib/svm/bin/$nativeImageName")
        if (!nativeImageBin.exists()) {
            throw GradleException("native-image not found at ${nativeImageBin.toAbsolutePath()} (JAVA_HOME=${javaHome.toAbsolutePath()})")
        }
        val fullClasspath = classpathFiles.joinToString(File.pathSeparator) { it.absolutePath }
        commandLine(
            nativeImageBin,
            "--add-opens", "java.base/java.lang=ALL-UNNAMED",
            "--add-opens", "java.base/java.io=ALL-UNNAMED",
            "--add-opens", "java.base/java.nio=ALL-UNNAMED",
            "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED",
            "--add-opens", "java.desktop/javax.swing=ALL-UNNAMED",
            "-H:+AddAllCharsets",
            "-H:+UnlockExperimentalVMOptions",
            "-H:+AllowJRTFileSystem",
            "-cp", fullClasspath,
            "-o", outputFile.get().asFile.absolutePath,
            mainClass,
        )
    }
}

val kotlincNativeImageDist = tasks.register<Copy>("kotlincNativeImageDist") {
    description = "Build the kotlin-compiler-embeddable native distribution"
    duplicatesStrategy = DuplicatesStrategy.FAIL
    rename(quote("-${version}"), "")
    rename(quote("-${bootstrapKotlinVersion}"), "")
    destinationDir = layout.buildDirectory.dir("dist/kotlinc-native-image").get().asFile
    val wrapperScriptFiles = files("bin/kotlinc-native-image.sh", "bin/kotlinc-native-image.bat")
    into("bin") {
        from(kotlincNativeImageTask)
        from(wrapperScriptFiles) {
            filePermissions {
                unix("rwxr-xr-x")
            }
        }
    }
    val licenseFiles = files("$rootDir/license")
    into("license") {
        from(licenseFiles)
    }
    val librariesStripVersionFiles = files(nativeImageClasspath)
    into("lib") {
        from(librariesStripVersionFiles) {
            rename {
                it.replace(Regex("-\\d.*\\.jar\$"), ".jar")
            }
        }
        filePermissions {
            unix("rw-r--r--")
        }
    }
}
