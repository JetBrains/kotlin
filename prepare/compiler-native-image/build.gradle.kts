import org.gradle.api.file.DuplicatesStrategy
import org.gradle.crypto.checksum.Checksum
import org.gradle.internal.os.OperatingSystem
import java.util.regex.Pattern.quote

description = "Kotlin Compiler (Native Image)"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
    alias(libs.plugins.gradle.crypto.checksum)
}

val nativeImageClasspath = configurations.create("nativeImageClasspath") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val pluginsBuildClasspath = configurations.create("pluginsBuildClasspath") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val pluginsRuntime = configurations.create("pluginsRuntime") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    nativeImageClasspath(project(":kotlin-compiler-embeddable", configuration = "runtimeElements"))
    // Bundled plugins
    nativeImageClasspath(project(":kotlinx-serialization-compiler-plugin.embeddable"))
    nativeImageClasspath(project(":kotlin-allopen-compiler-plugin.embeddable"))
    nativeImageClasspath(project(":kotlin-noarg-compiler-plugin.embeddable"))
    nativeImageClasspath(project(":kotlin-sam-with-receiver-compiler-plugin.embeddable"))
    nativeImageClasspath(project(":kotlin-assignment-compiler-plugin.embeddable"))
    nativeImageClasspath(project(":kotlin-lombok-compiler-plugin.embeddable"))
    nativeImageClasspath(project(":kotlin-power-assert-compiler-plugin.embeddable"))
    nativeImageClasspath(project(":plugins:compose-compiler-plugin:compiler"))

    // Tests
    pluginsBuildClasspath(project(":kotlin-dataframe-compiler-plugin.embeddable"))

    pluginsRuntime(libs.kotlinx.serialization.core)
    pluginsRuntime(composeRuntime())
    pluginsRuntime(composeRuntimeDesktop())
    pluginsRuntime(composeRuntimeAnnotations())
    pluginsRuntime(composeRuntimeAnnotationsJs())
    pluginsRuntime(composeRuntimeAnnotationsJvm())
    pluginsRuntime(libs.androidx.collections)
    pluginsRuntime(libs.dataframe.core.dev)

    testFixturesApi(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":generators:test-generator")))

    testRuntimeOnly(libs.junit.jupiter.engine)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

val dynamicPluginsEnabled = kotlinBuildProperties
    .booleanProperty("kotlin.build.native-image.dynamic-plugins", false)
    .get()

val graalLauncher = getNativeImageToolchainLauncherFor(JdkMajorVersion.JDK_25_0)

projectTests {
    testData(project(":compiler").isolated, "testData/codegen")
    testData(project.isolated, "testData/projects/box")
    testData(project.isolated, "testData/projects/dynamicPlugins")

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
        if (dynamicPluginsEnabled) {
            include("**/NativeImageDynamicPluginBoxTestGenerated.class")
            include("**/NativeImageDynamicLegacyPluginBoxTestGenerated.class")
        }
        useNativeImageDist()
        usePlugins()
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
        usePlugins()
    }

    withJvmStdlibAndReflect()
    withTestJar()
    withMockJdkRuntime()
}

// Disable default test task to not interfere with compiler tests
tasks.test {
    enabled = false
}

val currentOs = OperatingSystem.current()

val kotlincNativeImageTask = tasks.register<Exec>("kotlincNativeImage") {
    description = "Build a native image of the kotlin-compiler-embeddable"

    val launcher = graalLauncher
    val resources = layout.projectDirectory.dir("resources")
    val preservedPackagesFile = layout.projectDirectory.file("preserved-packages.txt")
    val classpathFiles = files(nativeImageClasspath, resources)

    val basicNativeArgs = listOf(
        "-J-Xmx8g",
        "-Os",
        "-H:+AddAllCharsets",
        "-H:+UnlockExperimentalVMOptions",
        "-H:+AllowJRTFileSystem",
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.io=ALL-UNNAMED",
        "--add-opens", "java.base/java.nio=ALL-UNNAMED",
        "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED",
        "--add-opens", "java.desktop/javax.swing=ALL-UNNAMED",
    )

    val dynamicPluginsNativeArgs = if (dynamicPluginsEnabled) listOf(
        "-H:+RuntimeClassLoading",
        *providers.fileContents(preservedPackagesFile)
            .asText.get()
            .trim()
            .lineSequence()
            .map { "-H:Preserve=package=$it" }
            .toList().toTypedArray(),
    ) else emptyList()

    val nativeArgs = basicNativeArgs + dynamicPluginsNativeArgs

    inputs.files(nativeImageClasspath, resources, launcher.map { it.metadata.installationPath.asFile })
        .withNormalizer(ClasspathNormalizer::class)
        .withPropertyName("nativeImageClasspath")

    inputs.property("nativeArgs", nativeArgs)

    val isWindows = currentOs.isWindows
    val mainClass = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"
    val outputFile = layout.buildDirectory.file("bin/kotlinc-native-image")
    // Graal will automatically append .exe extension to the `outputFile`, but we need
    // to explicitly specify it as an output of the task
    val executableExtension = if (isWindows) ".exe" else ""
    val executableFile = layout.buildDirectory.file("bin/kotlinc-native-image$executableExtension")
    outputs.file(executableFile)

    doFirst {
        val nativeImageExecutable = launcher.get().resolveNativeImageExecutable(isWindows)
        val fullClasspath = classpathFiles.joinToString(File.pathSeparator) { it.absolutePath }
        commandLine(
            nativeImageExecutable,
            *nativeArgs.toTypedArray(),
            "-cp", fullClasspath,
            "-o", outputFile.get().asFile.absolutePath,
            mainClass,
        )
    }
}

val nativeImageDistSbomTask = configureSbom(
    target = "NativeImageDist",
    documentName = "Kotlin Compiler Native Image Distribution",
    gradleConfigurations = setOf(nativeImageClasspath.name),
)

val kotlincNativeImageDist = tasks.register<Copy>("kotlincNativeImageDist") {
    description = "Build the kotlin-compiler-embeddable native distribution"
    duplicatesStrategy = DuplicatesStrategy.FAIL
    rename(quote("-${version}"), "")
    rename(quote("-${bootstrapKotlinVersion}"), "")
    destinationDir = layout.buildDirectory.dir("dist").get().asFile
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

val nativeImageArchiveBaseName = run {
    val osName = when {
        currentOs.isWindows -> "windows"
        currentOs.isMacOsX -> "macos"
        else -> "linux"
    }
    val arch = when (val osArch = providers.systemProperty("os.arch").get()) {
        "aarch64", "arm64" -> "aarch64"
        "x86_64", "amd64" -> "x86_64"
        else -> error("Unsupported native-image host architecture: $osArch")
    }
    "kotlin-compiler-graalvm-native-image-$osName-$arch-${project.version}"
}
val nativeImageArchiveExtension = if (currentOs.isWindows) "zip" else "tar.gz"

fun AbstractArchiveTask.configureNativeImageArchive() {
    description = "Packs the native image distribution into the publishable release archive"
    from(kotlincNativeImageDist) {
        into(nativeImageArchiveBaseName)
    }
    archiveFileName.set("$nativeImageArchiveBaseName.$nativeImageArchiveExtension")
    destinationDirectory.set(layout.buildDirectory.map { it.dir("archives") })
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val kotlincNativeImageArchive = when {
    currentOs.isWindows -> tasks.register<Zip>("kotlincNativeImageArchive") {
        configureNativeImageArchive()
    }
    else -> tasks.register<Tar>("kotlincNativeImageArchive") {
        compression = Compression.GZIP
        configureNativeImageArchive()
    }
}

val kotlincNativeImageChecksum = tasks.register<Checksum>("kotlincNativeImageChecksum") {
    description = "Writes the SHA-256 checksum of the native image archive"
    inputFiles.setFrom(kotlincNativeImageArchive.map { it.archiveFile })
    outputDirectory.set(layout.buildDirectory.map { it.dir("checksum") })
    checksumAlgorithm.set(Checksum.Algorithm.SHA256)
}

tasks.register<Sync>("kotlincNativeImageArtifacts") {
    description = "Assembles artifacts for the native image distribution"
    duplicatesStrategy = DuplicatesStrategy.FAIL
    val archiveBaseName = nativeImageArchiveBaseName
    from(kotlincNativeImageArchive)
    from(kotlincNativeImageChecksum)
    from(nativeImageDistSbomTask) {
        rename { "$archiveBaseName.spdx.json" }
    }
    into(layout.buildDirectory.dir("artifacts"))
}

fun ProjectTestsExtension.nativeImageTestTask(name: String, body: Test.() -> Unit): TaskProvider<out Task> =
    testTask(taskName = name, skipInLocalBuild = false) {
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
fun Test.usePlugins() {
    withDist()
    addClasspathProperty(
        pluginsRuntime,
        "kotlin.native-image.plugins-runtime.classpath",
    )
    addClasspathProperty(
        pluginsBuildClasspath,
        "kotlin.native-image.plugins-build.classpath",
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
