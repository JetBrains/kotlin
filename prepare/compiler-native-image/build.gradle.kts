import TestCompilePaths.KOTLIN_COMPILER_EMBEDDABLE_CLASSPATH
import TestCompilePaths.KOTLIN_NATIVE_IMAGE_DIST_PATH
import TestCompilePaths.KOTLIN_NATIVE_IMAGE_PLUGINS_CLASSPATH
import TestCompilePaths.KOTLIN_NATIVE_IMAGE_PLUGINS_RUNTIME
import TestCompilePaths.KOTLIN_NATIVE_IMAGE_RESOURCES_PATH
import TestCompilePaths.KOTLIN_WEB_IMAGE_DIST_PATH
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.crypto.checksum.Checksum
import org.gradle.internal.os.OperatingSystem
import java.util.regex.Pattern.quote

description = "Kotlin Compiler (Native Image)"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
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

/**
 * Kotlin/Wasm libraries bundled into the web image distribution so that it is
 * self-contained and can compile Kotlin programs to WebAssembly out of the box.
 */
val webImageLibraries = configurations.create("webImageLibraries") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    nativeImageClasspath(project(":kotlin-compiler-embeddable", configuration = "runtimeElements"))
    // Bundled plugins
    nativeImageClasspath(project(":kotlin-scripting-compiler-embeddable"))
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

    webImageLibraries(project(":kotlin-stdlib", configuration = "wasmJsRuntimeElements"))
    webImageLibraries(project(":kotlin-test", configuration = "wasmJsRuntimeElements"))

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
    testData(project.isolated, "testData/projects/scripting")
    testData(project.isolated, "testData/projects/smokeWasm")

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

    nativeImageTestTask("webImageSmokeTest") {
        description = "Smoke test: compiles a hello-world to WebAssembly with the web-image " +
                "Kotlin/Wasm compiler and verifies that it succeeds."
        include("**/WebImageSmokeTest.class")
        useWebImageDist()
        withWasmRuntime()
    }

    nativeImageTestTask("nativeImageBoxTest") {
        description = "Runs native-image kotlinc against default kotlinc on box tests"
        include("**/NativeImageBoxTestGenerated.class")
        include("**/NativeImagePluginBoxTestGenerated.class")
        include("**/NativeImageLegacyPluginBoxTestGenerated.class")
        if (dynamicPluginsEnabled) {
            include("**/NativeImageDynamicPluginBoxTestGenerated.class")
            include("**/NativeImageDynamicLegacyPluginBoxTestGenerated.class")
            include("**/NativeImageScriptingTestGenerated.class")
        }
        useNativeImageDist()
        usePlugins()
        withJunit5ParallelExecution(4)
    }

    nativeImageTestTask("generateReachabilityMetadataBox") {
        description = "Runs JVM kotlinc with reachability metadata collector agent on box tests"
        include("**/ReachabilityMetadataBoxTestGenerated.class")
        include("**/ReachabilityMetadataPluginBoxTestGenerated.class")
        include("**/ReachabilityMetadataLegacyPluginBoxTestGenerated.class")
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
        "-Os",
        "-H:+AddAllCharsets",
        "-H:+UnlockExperimentalVMOptions",
        "-H:+AllowJRTFileSystem",
        "--enable-native-access=ALL-UNNAMED",
        "--sun-misc-unsafe-memory-access=allow",
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
            .filterNot { it.isBlank() || it.startsWith("#") }
            .map { "-H:Preserve=package=$it" }
            .toList().toTypedArray(),
    ) else emptyList()

    val nativeArgs = basicNativeArgs + dynamicPluginsNativeArgs

    inputs.files(nativeImageClasspath, resources, launcher.map { it.metadata.installationPath.asFile })
        .withNormalizer(ClasspathNormalizer::class)
        .withPropertyName("nativeImageClasspath")

    inputs.property("nativeArgs", nativeArgs)
    inputs.property("os", currentOs.name)

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

// === Web image (GraalVM Web Image: Java bytecode -> WebAssembly + JS wrapper) ===

/**
 * Directory containing the Binaryen toolchain (`wasm-as`), which is used by GraalVM Web Image
 * as the WebAssembly assembler. If not set, the toolchain is expected to be on `PATH`.
 */
val binaryenPath: Provider<String> = providers.gradleProperty("kotlin.build.web-image.binaryen.path")

val webImageMainClass = "org.jetbrains.kotlin.cli.js.KotlinWasmCompiler"
val webImageName = "kotlinc-wasm"

val kotlincWebImageTask = tasks.register<Exec>("kotlincWebImage") {
    description = "Build a GraalVM web image (WebAssembly) of the Kotlin/Wasm compiler"

    val launcher = graalLauncher
    val resources = layout.projectDirectory.dir("resources")
    val classpathFiles = files(nativeImageClasspath, resources)

    val webImageArgs = listOf(
        "--tool:svm-wasm",
        "-Os",
        "-H:+UnlockExperimentalVMOptions",
        "-H:+AddAllCharsets",
    )

    inputs.files(nativeImageClasspath, resources, launcher.map { it.metadata.installationPath.asFile })
        .withNormalizer(ClasspathNormalizer::class)
        .withPropertyName("webImageClasspath")

    inputs.property("webImageArgs", webImageArgs)

    val isWindows = currentOs.isWindows
    val outputDir = layout.buildDirectory.dir("web-image")
    // Web Image emits `<name>.js` together with the `<name>.js.wasm` module
    outputs.dir(outputDir)

    val binaryenDir = binaryenPath
    doFirst {
        val nativeImageExecutable = launcher.get().resolveNativeImageExecutable(isWindows)
        val fullClasspath = classpathFiles.joinToString(File.pathSeparator) { it.absolutePath }
        val outputBase = outputDir.get().asFile.also { it.mkdirs() }.resolve(webImageName)
        if (binaryenDir.isPresent) {
            environment("PATH", binaryenDir.get() + File.pathSeparator + System.getenv("PATH"))
        }
        commandLine(
            nativeImageExecutable,
            *webImageArgs.toTypedArray(),
            "-cp", fullClasspath,
            "-o", outputBase.absolutePath,
            webImageMainClass,
        )
    }
}

val webImageDistSbomTask = configureSbom(
    target = "WebImageDist",
    documentName = "Kotlin Compiler Web Image Distribution",
    gradleConfigurations = setOf(nativeImageClasspath.name),
)

val kotlincWebImageDist = tasks.register<Copy>("kotlincWebImageDist") {
    description = "Build the Kotlin/Wasm compiler web image distribution"
    duplicatesStrategy = DuplicatesStrategy.FAIL
    rename(quote("-${version}"), "")
    rename(quote("-${bootstrapKotlinVersion}"), "")
    destinationDir = layout.buildDirectory.dir("dist-web-image").get().asFile
    val wrapperScriptFiles = files("bin/kotlinc-wasm-web-image.sh", "bin/kotlinc-wasm-web-image.bat")
    into("bin") {
        from(kotlincWebImageTask)
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
    val wasmLibraries = files(webImageLibraries)
    into("lib") {
        from(wasmLibraries) {
            rename {
                it.replace(Regex("-\\d.*\\.klib\$"), ".klib")
            }
        }
        filePermissions {
            unix("rw-r--r--")
        }
    }
}

// The web image is a platform-independent WebAssembly module, so the archive is not
// qualified with the host OS and architecture, unlike the native image one
val webImageArchiveBaseName = "kotlin-compiler-graalvm-web-image-${project.version}"

val kotlincWebImageArchive = tasks.register<Zip>("kotlincWebImageArchive") {
    description = "Packs the web image distribution into the publishable release archive"
    from(kotlincWebImageDist) {
        into(webImageArchiveBaseName)
    }
    archiveFileName.set("$webImageArchiveBaseName.zip")
    destinationDirectory.set(layout.buildDirectory.map { it.dir("archives") })
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val kotlincWebImageChecksum = tasks.register<Checksum>("kotlincWebImageChecksum") {
    description = "Writes the SHA-256 checksum of the web image archive"
    inputFiles.setFrom(kotlincWebImageArchive.map { it.archiveFile })
    outputDirectory.set(layout.buildDirectory.map { it.dir("checksum-web-image") })
    checksumAlgorithm.set(Checksum.Algorithm.SHA256)
}

tasks.register<Sync>("kotlincWebImageArtifacts") {
    description = "Assembles artifacts for the web image distribution"
    duplicatesStrategy = DuplicatesStrategy.FAIL
    val archiveBaseName = webImageArchiveBaseName
    from(kotlincWebImageArchive)
    from(kotlincWebImageChecksum)
    from(webImageDistSbomTask) {
        rename { "$archiveBaseName.spdx.json" }
    }
    into(layout.buildDirectory.dir("artifacts-web-image"))
}

fun ProjectTestsExtension.nativeImageTestTask(name: String, body: Test.() -> Unit): TaskProvider<out Task> =
    testTask(taskName = name, skipInLocalBuild = false) {
        javaLauncher.set(graalLauncher)
        body()
    }

fun Test.useNativeImageDist() {
    addClasspathProperty(
        kotlincNativeImageDist.map { layout.files(it.destinationDir) },
        KOTLIN_NATIVE_IMAGE_DIST_PATH,
    )
}

fun Test.useWebImageDist() {
    addClasspathProperty(
        kotlincWebImageDist.map { layout.files(it.destinationDir) },
        KOTLIN_WEB_IMAGE_DIST_PATH,
    )
}

@OptIn(KotlinCompilerDistUsage::class)
fun Test.usePlugins() {
    withDist()
    addClasspathProperty(
        pluginsRuntime,
        KOTLIN_NATIVE_IMAGE_PLUGINS_RUNTIME,
    )
    addClasspathProperty(
        pluginsBuildClasspath,
        KOTLIN_NATIVE_IMAGE_PLUGINS_CLASSPATH,
    )
}

fun Test.useReachabilityMetadataResources() {
    addClasspathProperty(
        nativeImageClasspath,
        KOTLIN_COMPILER_EMBEDDABLE_CLASSPATH,
    )
    addDirectoryProperty(
        layout.projectDirectory.dir("resources").asFile,
        KOTLIN_NATIVE_IMAGE_RESOURCES_PATH,
    )
}
