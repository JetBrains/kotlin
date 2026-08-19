import org.gradle.api.file.DuplicatesStrategy
import org.gradle.crypto.checksum.Checksum
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
    id("test-inputs-check")
    alias(libs.plugins.gradle.crypto.checksum)
}

val nativeImageClasspath = configurations.create("nativeImageClasspath") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val pluginsRuntime = configurations.create("pluginsRuntime") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val bundledCompilerPlugins = mutableListOf<BundledCompilerPluginInfo>()

dependencies {
    nativeImageClasspath(project(":kotlin-compiler-embeddable", configuration = "runtimeElements"))

    bundledCompilerPlugin(
        pluginId = "org.jetbrains.kotlinx.serialization",
        registrarFqName = "org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationComponentRegistrar",
        commandLineProcessorFqName = "org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginOptions",
        jarPrefixes = listOf("kotlin-serialization-compiler-plugin"),
    ) {
        nativeImageClasspath(project(":kotlinx-serialization-compiler-plugin.embeddable"))
    }

    bundledCompilerPlugin(
        pluginId = "org.jetbrains.kotlin.allopen",
        registrarFqName = "org.jetbrains.kotlin.allopen.AllOpenComponentRegistrar",
        commandLineProcessorFqName = "org.jetbrains.kotlin.allopen.AllOpenCommandLineProcessor",
        jarPrefixes = listOf("allopen-compiler-plugin", "kotlin-allopen-compiler-plugin"),
    ) {
        nativeImageClasspath(project(":kotlin-allopen-compiler-plugin.embeddable"))
    }

    bundledCompilerPlugin(
        pluginId = "org.jetbrains.kotlin.noarg",
        registrarFqName = "org.jetbrains.kotlin.noarg.NoArgComponentRegistrar",
        commandLineProcessorFqName = "org.jetbrains.kotlin.noarg.NoArgCommandLineProcessor",
        jarPrefixes = listOf("noarg-compiler-plugin", "kotlin-noarg-compiler-plugin"),
    ) {
        nativeImageClasspath(project(":kotlin-noarg-compiler-plugin.embeddable"))
    }

    bundledCompilerPlugin(
        pluginId = "org.jetbrains.kotlin.samWithReceiver",
        registrarFqName = "org.jetbrains.kotlin.samWithReceiver.SamWithReceiverComponentRegistrar",
        commandLineProcessorFqName = "org.jetbrains.kotlin.samWithReceiver.SamWithReceiverCommandLineProcessor",
        jarPrefixes = listOf("sam-with-receiver-compiler-plugin", "kotlin-sam-with-receiver-compiler-plugin"),
    ) {
        nativeImageClasspath(project(":kotlin-sam-with-receiver-compiler-plugin.embeddable"))
    }

    bundledCompilerPlugin(
        pluginId = "org.jetbrains.kotlin.assignment",
        registrarFqName = "org.jetbrains.kotlin.assignment.plugin.AssignmentComponentRegistrar",
        commandLineProcessorFqName = "org.jetbrains.kotlin.assignment.plugin.AssignmentCommandLineProcessor",
        jarPrefixes = listOf("assignment-compiler-plugin", "kotlin-assignment-compiler-plugin"),
    ) {
        nativeImageClasspath(project(":kotlin-assignment-compiler-plugin.embeddable"))
    }

    bundledCompilerPlugin(
        pluginId = "org.jetbrains.kotlin.lombok",
        registrarFqName = "org.jetbrains.kotlin.lombok.LombokComponentRegistrar",
        commandLineProcessorFqName = "org.jetbrains.kotlin.lombok.LombokCommandLineProcessor",
        jarPrefixes = listOf("lombok-compiler-plugin", "kotlin-lombok-compiler-plugin"),
    ) {
        nativeImageClasspath(project(":kotlin-lombok-compiler-plugin.embeddable"))
    }

    bundledCompilerPlugin(
        pluginId = "org.jetbrains.kotlin.powerassert",
        registrarFqName = "org.jetbrains.kotlin.powerassert.PowerAssertCompilerPluginRegistrar",
        commandLineProcessorFqName = "org.jetbrains.kotlin.powerassert.PowerAssertCommandLineProcessor",
        jarPrefixes = listOf("power-assert-compiler-plugin", "kotlin-power-assert-compiler-plugin"),
    ) {
        nativeImageClasspath(project(":kotlin-power-assert-compiler-plugin.embeddable"))
    }

    bundledCompilerPlugin(
        pluginId = "androidx.compose.compiler.plugins.kotlin",
        registrarFqName = "androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar",
        commandLineProcessorFqName = "androidx.compose.compiler.plugins.kotlin.ComposeCommandLineProcessor",
        jarPrefixes = listOf("compose-compiler-plugin", "kotlin-compose-compiler-plugin"),
    ) {
        nativeImageClasspath(project(":plugins:compose-compiler-plugin:compiler"))
    }

    // Tests
    pluginsRuntime(libs.kotlinx.serialization.core)
    pluginsRuntime(composeRuntime())
    pluginsRuntime(composeRuntimeDesktop())
    pluginsRuntime(composeRuntimeAnnotations())
    pluginsRuntime(composeRuntimeAnnotationsJs())
    pluginsRuntime(composeRuntimeAnnotationsJvm())
    pluginsRuntime(libs.androidx.collections)

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

// Disable default test task to not interfere with compiler tests
tasks.test {
    enabled = false
}

val currentOs = OperatingSystem.current()

val generateBundledPluginsInfo = tasks.register("generateBundledPluginsInfo") {
    description = "Generates the bundled compiler plugins list consumed by BundledCompilerPlugins at runtime"

    val resources = layout.projectDirectory.dir("resources")
    val outputFile = resources.file("META-INF/org/jetbrains/kotlin/bundled-compiler-plugins.txt")
    val pluginLines = bundledCompilerPlugins.map { plugin ->
        listOf(
            plugin.pluginId,
            plugin.registrarFqName,
            plugin.commandLineProcessorFqName.orEmpty(),
            plugin.jarPrefixes.joinToString(","),
        ).joinToString(";")
    }
    inputs.property("pluginLines", pluginLines)
    inputs.dir(resources)
        .withNormalizer(ClasspathNormalizer::class)
        .withPropertyName("resourcesDir")
    outputs.file(outputFile)
    doLast {
        val bundledPluginsInfo = buildString {
            appendLine("# Generated by the 'generateBundledPluginsInfo' Gradle task. Do not edit by hand.")
            appendLine("# Format: pluginId;registrarFqName;commandLineProcessorFqName;jarPrefixes(comma-separated)")
            for (line in pluginLines) {
                appendLine(line)
            }
        }
        outputFile.asFile.parentFile.mkdirs()
        outputFile.asFile.writeText(bundledPluginsInfo)
    }
}

val kotlincNativeImageTask = tasks.register<Exec>("kotlincNativeImage") {
    description = "Build a native image of the kotlin-compiler-embeddable"

    val launcher = graalLauncher
    val resources = layout.projectDirectory.dir("resources")
    val classpathFiles = files(nativeImageClasspath, resources)
    inputs.files(nativeImageClasspath, resources, launcher.map { it.metadata.installationPath.asFile })
        .withNormalizer(ClasspathNormalizer::class)
        .withPropertyName("nativeImageClasspath")

    val isWindows = currentOs.isWindows
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
    val arch = when (val osArch = System.getProperty("os.arch")) {
        "aarch64", "arm64" -> "aarch64"
        "x86_64", "amd64" -> "x86_64"
        else -> error("Unsupported native-image host architecture: $osArch")
    }
    "kotlin-native-image-$osName-$arch-${project.version}"
}
val nativeImageArchiveExtension = if (currentOs.isWindows) "zip" else "tar.gz"

fun AbstractArchiveTask.configureNativeImageArchive() {
    description = "Packs the native image distribution into the publishable release archive"
    from(kotlincNativeImageDist) {
        into(nativeImageArchiveBaseName)
    }
    archiveFileName.set("$nativeImageArchiveBaseName.$nativeImageArchiveExtension")
    destinationDirectory.set(layout.buildDirectory.map { it.dir("archives") })
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

val kotlincNativeImageArtifacts = tasks.register<Sync>("kotlincNativeImageArtifacts") {
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

data class BundledCompilerPluginInfo(
    val pluginId: String,
    val registrarFqName: String,
    val commandLineProcessorFqName: String?,
    val jarPrefixes: List<String>,
)

fun DependencyHandlerScope.bundledCompilerPlugin(
    pluginId: String,
    registrarFqName: String,
    commandLineProcessorFqName: String?,
    jarPrefixes: List<String>,
    dependency: DependencyHandlerScope.() -> Unit
) {
    val pluginInfo = BundledCompilerPluginInfo(
        pluginId = pluginId,
        registrarFqName = registrarFqName,
        commandLineProcessorFqName = commandLineProcessorFqName,
        jarPrefixes = jarPrefixes,
    )
    bundledCompilerPlugins += pluginInfo
    dependency()
}

