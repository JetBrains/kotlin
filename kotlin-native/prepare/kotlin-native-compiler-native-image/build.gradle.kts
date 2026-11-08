import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.nativeDistribution.nativeDistribution

plugins {
    id("common-configuration")
    kotlin("jvm")
    id("test-inputs-check")
}

description = "GraalVM native image of the Kotlin/Native compiler (opt-in, see README.md)"

val compilerJar = configurations.dependencyScope("compilerJar")
val compilerJarClasspath = configurations.resolvable("compilerJarClasspath") {
    extendsFrom(compilerJar.get())
    // The embeddable JAR is self-contained: it is the only JAR the JVM launcher (`run_konan`) puts on the classpath.
    isTransitive = false
}

dependencies {
    compilerJar(project(":kotlin-native:prepare:kotlin-native-compiler-embeddable", configuration = "runtimeElements"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(kotlinStdlib())
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

sourceSets {
    "main" {}
    "test" {
        kotlin {
            srcDir("tests/kotlin")
        }
    }
}

val graalLauncher = getNativeImageToolchainLauncherFor(JdkMajorVersion.JDK_25_0)

val currentOs = OperatingSystem.current()

val konancNativeImage = tasks.register<Exec>("konancNativeImage") {
    description = "Builds a GraalVM native image of the Kotlin/Native compiler"

    val launcher = graalLauncher
    val reachabilityMetadata = layout.projectDirectory.dir("resources")
    val classpathFiles = files(compilerJarClasspath, reachabilityMetadata)

    val nativeArgs = listOf(
        "-Os",
        "-H:+AddAllCharsets",
        "-H:+UnlockExperimentalVMOptions",
        "-H:+AllowJRTFileSystem",
        // The compiler allocates native memory through the Foreign Function & Memory API.
        "-H:+ForeignAPISupport",
        "--enable-native-access=ALL-UNNAMED",
        "--sun-misc-unsafe-memory-access=allow",
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.io=ALL-UNNAMED",
        "--add-opens", "java.base/java.nio=ALL-UNNAMED",
        "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED",
        // The Serial GC starts with a small young generation by default, so it collects several times even during a small compilation.
        // Not collecting before the first gigabyte makes an edit-and-rebuild cycle 10-15% faster. It can be overridden at run time with `-Xms`.
        "-R:MinHeapSize=1g",
    )

    inputs.files(compilerJarClasspath)
        .withNormalizer(ClasspathNormalizer::class)
        .withPropertyName("compilerClasspath")
    inputs.dir(reachabilityMetadata)
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("reachabilityMetadata")
    inputs.file(launcher.map { it.metadata.installationPath.file("release") })
        .withPathSensitivity(PathSensitivity.NONE)
        .withPropertyName("graalVmRelease")
    inputs.property("nativeArgs", nativeArgs)
    inputs.property("os", currentOs.name)

    val isWindows = currentOs.isWindows
    val mainClass = "org.jetbrains.kotlin.cli.utilities.MainKt"
    val outputFile = layout.buildDirectory.file("bin/konanc-native-image")
    // GraalVM appends the `.exe` extension to the `outputFile` on Windows by itself.
    val executableExtension = if (isWindows) ".exe" else ""
    outputs.file(layout.buildDirectory.file("bin/konanc-native-image$executableExtension"))
        .withPropertyName("executable")

    doFirst {
        val nativeImageExecutable = launcher.get().resolveNativeImageExecutable(isWindows)
        commandLine(
            nativeImageExecutable,
            *nativeArgs.toTypedArray(),
            "-cp", classpathFiles.joinToString(File.pathSeparator) { it.absolutePath },
            "-o", outputFile.get().asFile.absolutePath,
            mainClass,
        )
    }
}

open class NativeImageSmokeTestArgumentProvider @Inject constructor(
    objectFactory: ObjectFactory,
) : CommandLineArgumentProvider {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    val nativeImage: RegularFileProperty = objectFactory.fileProperty()

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val nativeDistributionRoot: DirectoryProperty = objectFactory.directoryProperty()

    override fun asArguments(): Iterable<String> = listOf(
        "-Dkotlin.native.image=${nativeImage.get().asFile.absolutePath}",
        "-Dkotlin.native.home=${nativeDistributionRoot.get().asFile.absolutePath}",
    )
}

projectTests {
    // Building the image requires a GraalVM toolchain and lots of memory, so the test runs only on explicit request.
    testTask(taskName = "nativeImageSmokeTest", skipInLocalBuild = false) {
        description = "Compiles and incrementally re-links a program with the native image of the compiler " +
                "via the opt-in in the `kotlinc-native` launcher"
        jvmArgumentProviders.add(objects.newInstance<NativeImageSmokeTestArgumentProvider>().apply {
            nativeImage.set(konancNativeImage.map { task -> task.outputs.files.singleFile }.let { layout.file(it) })
            nativeDistributionRoot.set(project.nativeDistribution.map { it.root })
        })
        dependsOn(konancNativeImage, ":kotlin-native:dist", ":kotlin-native:distInvalidateStaleCaches")
    }
}

// The default test task would build the native image as a side effect of a regular `test` run.
tasks.test {
    enabled = false
}
