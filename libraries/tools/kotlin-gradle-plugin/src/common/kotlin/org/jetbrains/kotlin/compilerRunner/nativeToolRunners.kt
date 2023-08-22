/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.build.report.metrics.BuildMetricsReporter
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.dsl.NativeCacheOrchestration
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.useXcodeMessageStyle
import org.jetbrains.kotlin.gradle.plugin.mpp.nativeUseEmbeddableCompilerJar
import org.jetbrains.kotlin.gradle.report.GradleBuildMetricsReporter
import org.jetbrains.kotlin.gradle.targets.native.KonanPropertiesBuildService
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.konan.properties.resolvablePropertyString
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.DependencyDirectories
import java.io.File
import java.nio.file.Files
import java.util.*

private val Project.jvmArgs
    get() = PropertiesProvider(this).nativeJvmArgs?.split("\\s+".toRegex()).orEmpty()

internal val Project.konanHome: String
    get() = PropertiesProvider(this).konanDataDir?.let { NativeCompilerDownloader(project).compilerDirectory.absolutePath }
        ?: PropertiesProvider(this).nativeHome?.let { file(it).absolutePath }
        ?: NativeCompilerDownloader(project).compilerDirectory.absolutePath

internal val Project.disableKonanDaemon: Boolean
    get() = PropertiesProvider(this).nativeDisableCompilerDaemon == true

internal val Project.konanVersion: String
    get() = PropertiesProvider(this).nativeVersion
        ?: NativeCompilerDownloader.DEFAULT_KONAN_VERSION

internal val Project.konanDataDir: String?
    get() = PropertiesProvider(this).konanDataDir

internal fun Project.getKonanCacheKind(target: KonanTarget): NativeCacheKind {
    val commonCacheKind = PropertiesProvider(this).nativeCacheKind
    val targetCacheKind = PropertiesProvider(this).nativeCacheKindForTarget(target)
    return when {
        targetCacheKind != null -> targetCacheKind
        commonCacheKind != null -> commonCacheKind
        else -> KonanPropertiesBuildService.registerIfAbsent(this).get().defaultCacheKindForTarget(target)
    }
}

internal fun Project.getKonanCacheOrchestration(): NativeCacheOrchestration {
    return PropertiesProvider(this).nativeCacheOrchestration ?: NativeCacheOrchestration.Compiler
}

internal fun Project.isKonanIncrementalCompilationEnabled(): Boolean {
    return PropertiesProvider(this).incrementalNative ?: false
}

internal fun Project.getKonanParallelThreads(): Int {
    return PropertiesProvider(this).nativeParallelThreads ?: 4
}

private val Project.kotlinNativeCompilerJar: String
    get() = if (nativeUseEmbeddableCompilerJar)
        "$konanHome/konan/lib/kotlin-native-compiler-embeddable.jar"
    else
        "$konanHome/konan/lib/kotlin-native.jar"


internal abstract class KotlinNativeToolRunner(
    protected val toolName: String,
    private val settings: Settings,
    executionContext: GradleExecutionContext,
    metricsReporter: BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>
) : KotlinToolRunner(executionContext, metricsReporter) {

    class Settings(
        val konanVersion: String,
        val konanHome: String,
        val konanPropertiesFile: File,
        val useXcodeMessageStyle: Boolean,
        val jvmArgs: List<String>,
        val classpath: FileCollection,
        val konanDataDir: String?,
    ) {
        companion object {
            fun of(konanHome: String, konanDataDir: String?, project: Project) = Settings(
                konanVersion = project.konanVersion,
                konanHome = konanHome,
                konanPropertiesFile = project.file("${konanHome}/konan/konan.properties"),
                useXcodeMessageStyle = project.useXcodeMessageStyle,
                jvmArgs = project.jvmArgs,
                classpath = project.files(project.kotlinNativeCompilerJar, "${konanHome}/konan/lib/trove4j.jar"),
                konanDataDir = konanDataDir
            )
        }
    }

    final override val displayName get() = toolName

    final override val mainClass get() = "org.jetbrains.kotlin.cli.utilities.MainKt"
    final override val daemonEntryPoint
        get() = if (!settings.useXcodeMessageStyle) "daemonMain" else "daemonMainWithXcodeRenderer"

    // We need to unset some environment variables which are set by XCode and may potentially affect the tool executed.
    final override val execEnvironmentBlacklist: Set<String> by lazy {
        HashSet<String>().also { collector ->
            KotlinNativeToolRunner::class.java.getResourceAsStream("/env_blacklist")?.let { stream ->
                stream.reader().use { r -> r.forEachLine { collector.add(it) } }
            }
        }
    }

    final override val execSystemProperties by lazy {
        val messageRenderer = if (settings.useXcodeMessageStyle) MessageRenderer.XCODE_STYLE else MessageRenderer.GRADLE_STYLE
        mapOf(MessageRenderer.PROPERTY_KEY to messageRenderer.name)
    }

    final override val classpath get() = settings.classpath.files

    final override fun checkClasspath() =
        check(classpath.isNotEmpty()) {
            """
                Classpath of the tool is empty: $toolName
                Probably the '${PropertiesProvider.KOTLIN_NATIVE_HOME}' project property contains an incorrect path.
                Please change it to the compiler root directory and rerun the build.
            """.trimIndent()
        }

    data class IsolatedClassLoaderCacheKey(val classpath: Set<File>)

    // TODO: can't we use this for other implementations too?
    final override val isolatedClassLoaderCacheKey get() = IsolatedClassLoaderCacheKey(classpath)

    override fun transformArgs(args: List<String>) = listOf(toolName) + args

    final override fun getCustomJvmArgs() = settings.jvmArgs

    final override fun run(args: List<String>) {
        super.run(args + extractArgsFromSettings())
    }

    protected open fun extractArgsFromSettings(): List<String> {
        return settings.konanDataDir?.let { listOf("-Xkonan-data-dir=$it") } ?: emptyList()
    }
}

/** A common ancestor for all runners that run the cinterop tool. */
internal abstract class AbstractKotlinNativeCInteropRunner(
    toolName: String,
    private val settings: Settings,
    executionContext: GradleExecutionContext,
    metricsReporter: BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>
) : KotlinNativeToolRunner(toolName, settings, executionContext, metricsReporter) {

    override val mustRunViaExec get() = true

    override val execEnvironment by lazy {
        val result = mutableMapOf<String, String>()
        result.putAll(super.execEnvironment)
        result["LIBCLANG_DISABLE_CRASH_RECOVERY"] = "1"
        llvmExecutablesPath?.let {
            result["PATH"] = "$it;${System.getenv("PATH")}"
        }
        result
    }

    override fun extractArgsFromSettings(): List<String> {
        return settings.konanDataDir?.let { listOf("-Xkonan-data-dir", it) } ?: emptyList()
    }

    private val llvmExecutablesPath: String? by lazy {
        if (HostManager.host == KonanTarget.MINGW_X64) {
            // TODO: Read it from Platform properties when it is accessible.
            val konanProperties = Properties().apply {
                settings.konanPropertiesFile.inputStream().use(::load)
            }

            konanProperties.resolvablePropertyString("llvmHome.mingw_x64")?.let { toolchainDir ->
                DependencyDirectories.getDependenciesRoot(settings.konanDataDir)
                    .resolve("$toolchainDir/bin")
                    .absolutePath
            }
        } else
            null
    }
}

/** Kotlin/Native C-interop tool runner */
internal class KotlinNativeCInteropRunner
private constructor(
    private val settings: Settings,
    gradleExecutionContext: GradleExecutionContext,
    metricsReporter: BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>
) : AbstractKotlinNativeCInteropRunner("cinterop", settings, gradleExecutionContext, metricsReporter) {

    interface ExecutionContext {
        val runnerSettings: Settings
        val gradleExecutionContext: GradleExecutionContext
        val metricsReporter: BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>
        fun runWithContext(action: () -> Unit)
    }

    companion object {
        fun ExecutionContext.run(args: List<String>) {
            val runner = KotlinNativeCInteropRunner(runnerSettings, gradleExecutionContext, metricsReporter)
            runWithContext { runner.run(args) }
        }
    }
}

/** Kotlin/Native compiler runner */
internal class KotlinNativeCompilerRunner(
    private val settings: Settings,
    executionContext: GradleExecutionContext,
    metricsReporter: BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>
) : KotlinNativeToolRunner("konanc", settings.parent, executionContext, metricsReporter) {
    class Settings(
        val parent: KotlinNativeToolRunner.Settings,
        val disableKonanDaemon: Boolean,
    ) {
        companion object {
            fun of(konanHome: String, konanDataDir: String?, project: Project) = Settings(
                parent = KotlinNativeToolRunner.Settings.of(konanHome, konanDataDir, project),
                disableKonanDaemon = project.disableKonanDaemon,
            )
        }
    }

    private val useArgFile get() = settings.disableKonanDaemon

    override val mustRunViaExec get() = settings.disableKonanDaemon

    override fun transformArgs(args: List<String>): List<String> {
        if (!useArgFile) return super.transformArgs(args)

        val argFile = Files.createTempFile(/* prefix = */ "kotlinc-native-args", /* suffix = */ ".lst").toFile().apply { deleteOnExit() }
        argFile.printWriter().use { w ->
            args.forEach { arg ->
                val escapedArg = arg
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                w.println("\"$escapedArg\"")
            }
        }

        return listOf(toolName, "@${argFile.absolutePath}")
    }
}

/** Platform libraries generation tool. Runs the cinterop tool under the hood. */
internal class KotlinNativeLibraryGenerationRunner(
    private val settings: Settings,
    executionContext: GradleExecutionContext,
    metricsReporter: BuildMetricsReporter<GradleBuildTime, GradleBuildPerformanceMetric>
) :
    AbstractKotlinNativeCInteropRunner("generatePlatformLibraries", settings, executionContext, metricsReporter) {

    companion object {
        fun fromProject(project: Project) = KotlinNativeLibraryGenerationRunner(
            settings = Settings.of(project.konanHome, project.konanDataDir, project),
            executionContext = GradleExecutionContext.fromProject(project),
            metricsReporter = GradleBuildMetricsReporter()
        )
    }

    // The library generator works for a long time so enabling C2 can improve performance.
    override val disableC2: Boolean = false
}
