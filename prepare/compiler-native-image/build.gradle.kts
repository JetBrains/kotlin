import org.gradle.api.file.DuplicatesStrategy
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.javaToolchains
import org.gradle.kotlin.dsl.register
import java.util.regex.Pattern.quote
import kotlin.io.path.exists

description = "Kotlin Compiler (Native Image)"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

val nativeImageClasspath by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val pluginsRuntime by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val bundledCompilerPluginProjects = listOf(
    ":kotlin-allopen-compiler-plugin.embeddable",
    ":kotlin-noarg-compiler-plugin.embeddable",
    ":kotlin-sam-with-receiver-compiler-plugin.embeddable",
    ":kotlin-assignment-compiler-plugin.embeddable",
    ":kotlin-lombok-compiler-plugin.embeddable",
    ":kotlin-power-assert-compiler-plugin.embeddable",
    ":kotlinx-serialization-compiler-plugin.embeddable",
    ":plugins:compose-compiler-plugin:compiler",
)

dependencies {
    nativeImageClasspath(project(":kotlin-compiler-embeddable", configuration = "runtimeElements"))

    bundledCompilerPluginProjects.forEach {
        nativeImageClasspath(project(it))
    }

    pluginsRuntime(libs.kotlinx.serialization.core)
    pluginsRuntime(composeRuntime())
    pluginsRuntime(composeRuntimeAnnotations())
    pluginsRuntime(libs.androidx.collections)

    testFixturesApi(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
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

val graalLauncher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(JdkMajorVersion.JDK_25_0.targetName))
    vendor.set(JvmVendorSpec.GRAAL_VM)
}

projectTests {
    testData(project(":compiler").isolated, "testData/codegen")
    testData(project.isolated, "testData/projects/box")

    testGenerator(
        "org.jetbrains.kotlin.compiler.nativeimage.GenerateNativeImageTestsKt",
        generateTestsInBuildDirectory = true,
    )

    nativeImageTestTask("nativeImageSmokeTest") {
        description = "Smoke test: compiles a hello-world with the native-image kotlinc " +
                "and verifies it succeeds."
        include("**/NativeImageSmokeTest.class")
        useNativeImageDist()
    }

    nativeImageTestTask("generateReachabilityMetadataSmoke") {
        description = "Quick reachability metadata regen: runs JVM kotlinc with the " +
                "reachability metadata collector agent on the smoke test."
        include("**/ReachabilityMetadataSmokeTest.class")
        useReachabilityMetadataResources()
        @OptIn(KotlinCompilerDistUsage::class)
        withDist()
    }

    nativeImageTestTask("nativeImageBoxTest") {
        description = "Runs native-image kotlinc against default kotlinc on box tests"
        include("**/NativeImageBoxTestGenerated.class")
        include("**/NativeImagePluginBoxTestGenerated.class")
        include("**/NativeImageLegacyPluginBoxTestGenerated.class")
        useNativeImageDist()
        usePluginsRuntime()
    }

    nativeImageTestTask("generateReachabilityMetadataBox") {
        description = "Runs JVM kotlinc with reachability metadata collector agent on box tests"
        include("**/NativeImageReachabilityMetadataTestGenerated.class")
        include("**/NativeImagePluginReachabilityMetadataTestGenerated.class")
        include("**/NativeImageLegacyPluginReachabilityMetadataTestGenerated.class")
        // We can't run in parallel because of the tracing agent
        systemProperty(
            "junit.jupiter.execution.parallel.enabled",
            "false",
        )
        useReachabilityMetadataResources()
        usePluginsRuntime()
    }

    withJvmStdlibAndReflect()
    withTestJar()
    withMockJdkRuntime()
}

val kotlincNativeImageTask = tasks.register<Exec>("kotlincNativeImage") {
    description = "Build a native image of the kotlin-compiler-embeddable"

    val launcher = graalLauncher
    val resources = layout.projectDirectory.dir("resources")
    val classpathFiles = files(nativeImageClasspath, resources)
    inputs.files(nativeImageClasspath, resources, launcher.map { it.metadata.installationPath.asFile })
        .withNormalizer(ClasspathNormalizer::class)
        .withPropertyName("nativeImageClasspath")

    val isWindows = OperatingSystem.current().isWindows
    val mainClass = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"
    val outputFile = layout.buildDirectory.file("bin/kotlinc-native-image")
    // Graal will automatically append .exe extension to the `outputFile`, but we need
    // to explicitly specify it as an output of the task
    val executableExtension = if (isWindows) ".exe" else ""
    val executableFile = layout.buildDirectory.file("bin/kotlinc-native-image$executableExtension")
    outputs.file(executableFile)

    doFirst {
        val javaHome = launcher.get().executablePath.asFile.toPath().parent.parent

        val nativeImageName = if (isWindows) "native-image.exe" else "native-image"
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

fun ProjectTestsExtension.nativeImageTestTask(name: String, body: Test.() -> Unit): TaskProvider<out Task> =
    testTask(taskName = name, jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = false) {
        javaLauncher.set(graalLauncher)
        body()
    }

fun Test.useNativeImageDist() {
    addClasspathProperty(
        kotlincNativeImageDist.map { layout.files(it.destinationDir) },
        "kotlin.native-image.dist.path",
    )
}

@OptIn(KotlinCompilerDistUsage::class)
fun Test.usePluginsRuntime() {
    withDist()
    addClasspathProperty(
        pluginsRuntime,
        "kotlin.native-image.plugins-runtime.classpath",
    )
}

fun Test.useReachabilityMetadataResources() {
    addClasspathProperty(
        nativeImageClasspath,
        "kotlin.compiler-embeddable.classpath",
    )
    addDirectoryProperty(
        layout.projectDirectory.dir("resources").asFile,
        "kotlin.native-image.resources.path",
    )
}
