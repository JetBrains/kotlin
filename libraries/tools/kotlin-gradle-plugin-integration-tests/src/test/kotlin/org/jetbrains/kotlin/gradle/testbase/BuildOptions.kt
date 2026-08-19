/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.JavaVersion
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.configuration.WarningMode
import org.gradle.internal.logging.LoggingConfigurationBuildOptions.StacktraceOption
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.gradle.targets.wasm.WasmCompilationMode
import org.jetbrains.kotlin.gradle.targets.wasm.WasmCompilationMode.Companion.toArgument
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import org.jetbrains.kotlin.gradle.testbase.BuildOptions.IsolatedProjectsMode
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.jupiter.api.condition.OS
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.invariantSeparatorsPathString

val DEFAULT_LOG_LEVEL = LogLevel.INFO

data class BuildOptions(
    val logLevel: LogLevel = DEFAULT_LOG_LEVEL,
    val stacktraceMode: String? = StacktraceOption.FULL_STACKTRACE_LONG_OPTION,
    val kotlinVersion: String = TestVersions.Kotlin.CURRENT,
    val warningMode: WarningMode = WarningMode.Fail,
    val ignoreWarningModeSeverityOverride: Boolean? = null, // Do not change ToolingDiagnostic severity when warningMode is defined as Fail
    val configurationCache: ConfigurationCacheValue = ConfigurationCacheValue.ENABLED,
    val isolatedProjects: IsolatedProjectsMode = IsolatedProjectsMode.ENABLED,
    val configurationCacheProblems: ConfigurationCacheProblems = ConfigurationCacheProblems.FAIL,
    val parallel: Boolean = true,
    val incremental: Boolean? = null,
    val maxWorkers: Int = (Runtime.getRuntime().availableProcessors() / 4 - 1).coerceAtLeast(2),
    /**
     * Enable File System Watching
     *
     * Disabled by default on Windows OS because  enabling watch-fs prevents deleting temp directory, which fails the tests.
     *
     * See https://docs.gradle.org/current/userguide/file_system_watching.html
     */
    val fileSystemWatchEnabled: Boolean = !OS.WINDOWS.isCurrentOs,
    val buildCacheEnabled: Boolean = false,
    val kaptOptions: KaptOptions? = null,
    val androidVersion: String? = null,
    /** Sets `android.newDsl=false` + `android.builtInKotlin=false` so AGP 9+ keeps the legacy DSL KGP integrates with. */
    val enableLegacyAgpDsl: Boolean = true,
    val jsOptions: JsOptions? = JsOptions(),
    val wasmOptions: WasmOptions? = WasmOptions(),
    val buildReport: List<BuildReportType> = emptyList(),
    val usePreciseJavaTracking: Boolean? = null,
    val useFirJvmRunner: Boolean? = null,
    val languageVersion: String? = null,
    val languageApiVersion: String? = null,
    val freeArgs: List<String> = emptyList(),
    val statisticsForceValidation: Boolean = true,
    val enableJvmIncrementalCompilationOfCommonSources: Boolean? = null,
    val enableJsUnsafeIncrementalCompilationForMultiplatform: Boolean? = null,
    val enableWasmUnsafeIncrementalCompilationForMultiplatform: Boolean? = null,
    val enableMonotonousIncrementalCompileSetExpansion: Boolean? = null,
    val useDaemonFallbackStrategy: Boolean = false,
    val useParsableDiagnosticsFormatting: Boolean = true,
    val showDiagnosticsStacktrace: Boolean? = false, // false by default to not clutter the testdata + stacktraces change often
    val nativeOptions: NativeOptions = NativeOptions(),
    val compilerExecutionStrategy: KotlinCompilerExecutionStrategy? = null,
    val runViaBuildToolsApi: Boolean? = null,
    val konanDataDir: Path? = konanDir, // null can be used only if you are using custom 'kotlin.native.home' or 'org.jetbrains.kotlin.native.home' property instead of konanDir
    val kotlinUserHome: Path? = testKitDir.resolve(".kotlin"),
    val compilerArgumentsLogLevel: String? = "info",
    /**
     * Enable file-leak-detector if not-null. A trace report will be written to the specified file.
     */
    val fileLeaksReportFile: Path? = null,
    val continueAfterFailure: Boolean = false,
    /**
     * Override the directory to store flag files indicating "daemon process is alive" controlled by Kotlin Daemon.
     *
     * @see [KGPDaemonsBaseTest]
     */
    val customKotlinDaemonRunFilesDirectory: File? = null,
    /**
     * Enable verbose VFS logging to view information about Virtual File System (VFS) changes at the beginning and end of a build.
     *
     * https://docs.gradle.org/current/userguide/file_system_watching.html#logging
     */
    val verboseVfsLogging: Boolean? = null,
    /**
     * Enable `--continuous` build.
     *
     * Note that `--continuous` *disables* `--no-daemon`.
     */
    val continuousBuild: Boolean? = null,
    val generateCompilerRefIndex: Boolean? = null,
    val jvmClasspathMetadata: Boolean? = null,
    val separateCompilation: Boolean? = null,
    val expandTypeAliasesInClasspathSnapshots: Boolean? = null,
    val fusReportDirectory: () -> Path? = { null }
) {
    enum class ConfigurationCacheValue {

        /** Explicitly/forcefully disable Configuration Cache */
        DISABLED,

        /** Explicitly/forcefully enable Configuration Cache */
        ENABLED,

        /** AUTO means unspecified by default but enabled on macOS */
        AUTO,

        /** Gradle, depending on its version, will decide whether to enable Configuration Cache */
        UNSPECIFIED;

        fun toBooleanFlag(gradleVersion: GradleVersion): Boolean? = when (this) {
            DISABLED -> false
            ENABLED -> true
            AUTO -> if (HostManager.hostIsMac) true else null
            UNSPECIFIED -> null
        }
    }

    enum class IsolatedProjectsMode {

        /** Always disable Isolated Projects */
        DISABLED,

        /** Always enable Isolated Projects */
        ENABLED;

        fun toBooleanFlag(gradleVersion: GradleVersion) = when (this) {
            DISABLED -> false
            ENABLED -> true
        }
    }

    val isK2ByDefault
        get() = KotlinVersion.DEFAULT >= KotlinVersion.KOTLIN_2_0

    fun copyEnsuringK1(): BuildOptions =
        copy(languageVersion = if (isK2ByDefault) "1.9" else null)

    fun copyEnsuringK2(): BuildOptions =
        copy(languageVersion = if (isK2ByDefault) null else "2.0")

    val safeAndroidVersion: String
        get() = androidVersion ?: error("AGP version is expected to be set")

    data class KaptOptions(
        val verbose: Boolean = false,
        val incrementalKapt: Boolean = false,
        val includeCompileClasspath: Boolean = false,
        val classLoadersCacheSize: Int? = null,
    )

    data class JsOptions(
        val incrementalJs: Boolean? = null,
        val incrementalJsKlib: Boolean? = null,
        val incrementalJsIr: Boolean? = null,
        val yarn: Boolean? = null,
    )

    @Suppress("EXPOSED_PARAMETER_TYPE")
    data class WasmOptions(
        val compilationMode: WasmCompilationMode? = null,
    )

    data class NativeOptions(
        val cocoapodsGenerateWrapper: Boolean? = null,
        val cocoapodsPlatform: String? = null,
        val cocoapodsConfiguration: String? = null,
        val cocoapodsArchs: String? = null,
        val distributionType: String? = null,
        val distributionDownloadFromMaven: Boolean? = true,
        val reinstall: Boolean? = null,
        val restrictedDistribution: Boolean? = null,
        val useXcodeMessageStyle: Boolean? = null,
        val version: String? = System.getProperty("kotlinNativeVersion"),
        val incremental: Boolean? = null,
        val enableKlibsCrossCompilation: Boolean? = null,
    )

    fun toArguments(
        gradleVersion: GradleVersion,
    ): List<String> {
        val arguments = mutableListOf<String>()
        when (logLevel) {
            LogLevel.DEBUG -> arguments.add("--debug")
            LogLevel.INFO -> arguments.add("--info")
            LogLevel.WARN -> arguments.add("--warn")
            LogLevel.QUIET -> arguments.add("--quiet")
            else -> Unit
        }
        arguments.add("-Pkotlin_version=$kotlinVersion")
        when (warningMode) {
            WarningMode.Fail -> arguments.add("--warning-mode=fail")
            WarningMode.All -> arguments.add("--warning-mode=all")
            WarningMode.Summary -> arguments.add("--warning-mode=summary")
            WarningMode.None -> arguments.add("--warning-mode=none")
        }

        val configurationCacheFlag = configurationCache.toBooleanFlag(gradleVersion)
        if (configurationCacheFlag != null) {
            arguments.add("-Dorg.gradle.unsafe.configuration-cache=$configurationCacheFlag")
            arguments.add("-Dorg.gradle.unsafe.configuration-cache-problems=${configurationCacheProblems.name.lowercase(Locale.getDefault())}")
            arguments.add("-Dorg.gradle.configuration-cache.parallel=true")
        }

        // If isolated projects _explicitly_ enabled, but the configuration cache is disabled, emit the error
        if (isolatedProjects == IsolatedProjectsMode.ENABLED && configurationCacheFlag != true) {
            throw IllegalArgumentException("Isolated projects can't be enabled, if the configuration cache is disabled!")
        }
        // Isolated projects can't be enabled, if the configuration cache is disabled
        val isolatedProjectsFlag = isolatedProjects.toBooleanFlag(gradleVersion) && configurationCacheFlag == true
        if (gradleVersion < GradleVersion.version(TestVersions.Gradle.G_9_7)) {
            arguments.add("-Dorg.gradle.unsafe.isolated-projects=$isolatedProjectsFlag")
        } else {
            if (isolatedProjectsFlag) {
                arguments.add("--isolated-projects")
            } else {
                arguments.add("--no-isolated-projects")
            }
        }

        if (parallel) {
            arguments.add("--parallel")
            arguments.add("--max-workers=$maxWorkers")
        } else {
            arguments.add("--no-parallel")
        }

        if (continueAfterFailure) {
            arguments.add("--continue")
        }

        if (incremental != null) {
            arguments.add("-Pkotlin.incremental=$incremental")
        }

        if (fileSystemWatchEnabled) {
            arguments.add("--watch-fs")
        } else {
            arguments.add("--no-watch-fs")
        }

        if (verboseVfsLogging != null) {
            arguments.add("-Dorg.gradle.vfs.verbose=$verboseVfsLogging")
        }

        if (continuousBuild == true) {
            arguments.add("--continuous")
        }

        arguments.add(if (buildCacheEnabled) "--build-cache" else "--no-build-cache")

        addNativeOptionsToArguments(arguments)

        if (kaptOptions != null) {
            arguments.add("-Pkapt.verbose=${kaptOptions.verbose}")
            arguments.add("-Pkapt.incremental.apt=${kaptOptions.incrementalKapt}")
            arguments.add("-Pkapt.include.compile.classpath=${kaptOptions.includeCompileClasspath}")
            kaptOptions.classLoadersCacheSize?.let { cacheSize ->
                arguments.add("-Pkapt.classloaders.cache.size=$cacheSize")
            }
        }

        if (jsOptions != null) {
            jsOptions.incrementalJs?.let { arguments.add("-Pkotlin.incremental.js=$it") }
            jsOptions.incrementalJsKlib?.let { arguments.add("-Pkotlin.incremental.js.klib=$it") }
            jsOptions.incrementalJsIr?.let { arguments.add("-Pkotlin.incremental.js.ir=$it") }
            jsOptions.yarn?.let { arguments.add("-Pkotlin.js.yarn=$it") }
        }

        wasmOptions?.compilationMode?.let { arguments.add("-Pkotlin.wasm.compilationMode=${it.toArgument()}") }

        if (androidVersion != null) {
            arguments.add("-Dandroid_tools_version=${androidVersion}")
            arguments.add("-Pandroid_tools_version=${androidVersion}")
        }
        if (enableLegacyAgpDsl) {
            arguments.add("-Pandroid.newDsl=false")
            arguments.add("-Pandroid.builtInKotlin=false")
        }
        arguments.add("-Ptest_fixes_version=${TestVersions.Kotlin.CURRENT}")

        if (buildReport.isNotEmpty()) {
            arguments.add("-Pkotlin.build.report.output=${buildReport.joinToString()}")
        }

        if (usePreciseJavaTracking != null) {
            arguments.add("-Pkotlin.incremental.usePreciseJavaTracking=$usePreciseJavaTracking")
        }

        if (useFirJvmRunner != null) {
            arguments.add("-Pkotlin.incremental.jvm.fir=$useFirJvmRunner")
        }

        if (statisticsForceValidation) {
            arguments.add("-Pkotlin_performance_profile_force_validation=true")
        }

        if (languageApiVersion != null) {
            arguments.add("-Pkotlin.test.apiVersion=$languageApiVersion")
        }
        if (languageVersion != null) {
            arguments.add("-Pkotlin.test.languageVersion=$languageVersion")
        }

        if (enableJvmIncrementalCompilationOfCommonSources != null) {
            arguments.add("-Pkotlin.jvm.enableIncrementalCompilationOfCommonSources=$enableJvmIncrementalCompilationOfCommonSources")
        }

        if (enableJsUnsafeIncrementalCompilationForMultiplatform != null) {
            arguments.add("-Pkotlin.internal.js.enableUnsafeOptimizationsForMultiplatform=$enableJsUnsafeIncrementalCompilationForMultiplatform")
        }

        if (enableWasmUnsafeIncrementalCompilationForMultiplatform != null) {
            arguments.add("-Pkotlin.internal.wasm.enableUnsafeOptimizationsForMultiplatform=$enableWasmUnsafeIncrementalCompilationForMultiplatform")
        }

        if (enableMonotonousIncrementalCompileSetExpansion != null) {
            arguments.add("-Pkotlin.internal.incremental.enableMonotonousCompileSetExpansion=$enableMonotonousIncrementalCompileSetExpansion")
        }

        if (jvmClasspathMetadata != null) {
            arguments.add("-Pkotlin.internal.jvm.enableKmpClasspathMetadataForIncrementalCompilation=$jvmClasspathMetadata")
        }

        if (separateCompilation != null) {
            arguments.add("-Pkotlin.kmp.separateCompilation=$separateCompilation")
        }

        if (expandTypeAliasesInClasspathSnapshots != null) {
            arguments.add(
                "-Pkotlin.internal.jvm.expandTypeAliasesInClasspathSnapshots=$expandTypeAliasesInClasspathSnapshots"
            )
        }

        arguments.add("-Pkotlin.daemon.useFallbackStrategy=$useDaemonFallbackStrategy")

        if (useParsableDiagnosticsFormatting) {
            arguments.add("-Pkotlin.internal.diagnostics.useParsableFormatting=$useParsableDiagnosticsFormatting")
        }

        if (compilerExecutionStrategy != null) {
            arguments.add("-Pkotlin.compiler.execution.strategy=${compilerExecutionStrategy.propertyValue}")
        }

        if (runViaBuildToolsApi != null) {
            arguments.add("-Pkotlin.compiler.runViaBuildToolsApi=$runViaBuildToolsApi")
            arguments.add("-Pkotlin.js.runViaBuildToolsApi=$runViaBuildToolsApi")
            arguments.add("-Pkotlin.wasm.runViaBuildToolsApi=$runViaBuildToolsApi")
            arguments.add("-Pkotlin.metadata.runViaBuildToolsApi=$runViaBuildToolsApi")
        }

        if (showDiagnosticsStacktrace != null) {
            arguments.add("-Pkotlin.internal.diagnostics.showStacktrace=$showDiagnosticsStacktrace")
        }

        if (ignoreWarningModeSeverityOverride != null) {
            arguments.add("-Pkotlin.internal.diagnostics.ignoreWarningMode=$ignoreWarningModeSeverityOverride")
        }

        if (stacktraceMode != null) {
            arguments.add("--$stacktraceMode")
        }

        konanDataDir?.let {
            arguments.add("-Pkonan.data.dir=${konanDataDir.normalize().absolute().invariantSeparatorsPathString}")
        }

        if (kotlinUserHome != null) {
            arguments.add("-Pkotlin.user.home=${kotlinUserHome.normalize().absolute().invariantSeparatorsPathString}")
        }

        if (compilerArgumentsLogLevel != null) {
            arguments.add("-Pkotlin.internal.compiler.arguments.log.level=$compilerArgumentsLogLevel")
        }


        if (generateCompilerRefIndex != null) {
            arguments.add("-Pkotlin.compiler.generateCompilerRefIndex=$generateCompilerRefIndex")
        }

        fusReportDirectory()?.let {
            arguments.add("-Pkotlin.session.logger.root.path=${it.absolutePathString()}")
        }

        arguments.addAll(freeArgs)

        return arguments.toList()
    }

    private fun addNativeOptionsToArguments(
        arguments: MutableList<String>,
    ) {
        nativeOptions.cocoapodsGenerateWrapper?.let {
            arguments.add("-Pkotlin.native.cocoapods.generate.wrapper=${it}")
        }
        nativeOptions.cocoapodsPlatform?.let {
            arguments.add("-Pkotlin.native.cocoapods.platform=${it}")
        }
        nativeOptions.cocoapodsArchs?.let {
            arguments.add("-Pkotlin.native.cocoapods.archs=${it}")
        }
        nativeOptions.cocoapodsConfiguration?.let {
            arguments.add("-Pkotlin.native.cocoapods.configuration=${it}")
        }
        nativeOptions.distributionDownloadFromMaven?.let {
            arguments.add("-Pkotlin.native.distribution.downloadFromMaven=${it}")
        }
        nativeOptions.distributionType?.let {
            arguments.add("-Pkotlin.native.distribution.type=${it}")
        }
        nativeOptions.reinstall?.let {
            arguments.add("-Pkotlin.native.reinstall=${it}")
        }
        nativeOptions.restrictedDistribution?.let {
            arguments.add("-Pkotlin.native.restrictedDistribution=${it}")
        }
        nativeOptions.useXcodeMessageStyle?.let {
            arguments.add("-Pkotlin.native.useXcodeMessageStyle=${it}")
        }
        nativeOptions.version?.let {
            arguments.add("-Pkotlin.native.version=${it}")
        }
        nativeOptions.incremental?.let {
            arguments.add("-Pkotlin.incremental.native=${it}")
        }
        nativeOptions.enableKlibsCrossCompilation?.let {
            arguments.add("-Pkotlin.native.enableKlibsCrossCompilation=${it}")
        }
    }

    enum class ConfigurationCacheProblems {
        FAIL, WARN
    }
}

fun BuildOptions.suppressDeprecationWarningsOn(
    @Suppress("UNUSED_PARAMETER") reason: String, // just to require specifying a reason for suppressing
    predicate: (BuildOptions) -> Boolean,
) = if (predicate(this)) {
    copy(warningMode = WarningMode.Summary)
} else {
    this
}

fun BuildOptions.suppressDeprecationWarningsSinceGradleVersion(
    gradleVersion: String,
    currentGradleVersion: GradleVersion,
    reason: String,
) = suppressDeprecationWarningsOn(reason) {
    currentGradleVersion >= GradleVersion.version(gradleVersion)
}

/**
 * This wrapper erases k/n version from passing parameters,
 * because we should use Kotlin Native bundled in KGP instead of which built from current branch.
 *
 * In this case we will use k/n version, which declared in KGP.
 *
 * The most common case is when we override local konan dir for some reason.
 */
fun BuildOptions.withBundledKotlinNative() = copy(
    nativeOptions = nativeOptions.copy(
        version = null
    )
)

fun BuildOptions.disableKlibsCrossCompilation() = copy(
    nativeOptions = nativeOptions.copy(enableKlibsCrossCompilation = false)
)


fun BuildOptions.enableIsolatedProjects() = copy(isolatedProjects = IsolatedProjectsMode.ENABLED)
fun BuildOptions.disableIsolatedProjects() = copy(isolatedProjects = IsolatedProjectsMode.DISABLED)

// KT-75899: Support Gradle Project Isolation in KGP JS & Wasm
fun BuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899() = disableIsolatedProjects()

fun BuildOptions.suppressWarningForOldKotlinVersion(
    currentGradleVersion: GradleVersion,
) = suppressDeprecationWarningsSinceGradleVersion(
    gradleVersion = TestVersions.Gradle.G_8_14,
    currentGradleVersion = currentGradleVersion,
    reason =
        """
        Old Kotlin versions produces deprecation warnings with latest Gradle release.
        """.trimIndent()
)

// Lint tasks produces deprecation warning since Gradle 8.14: https://issuetracker.google.com/issues/408334529
// On a non-first run if WarningMode was not changed, the Lint task does not produce a deprecation warning!
// Fixed in AGP 8.12-alpha06
fun BuildOptions.suppressAgpWarningSinceGradle814(
    currentGradleVersion: GradleVersion,
    currentAgpVersion: TestVersions.AgpCompatibilityMatrix,
    warningMode: WarningMode = WarningMode.Summary,
): BuildOptions {
    return when {
        warningMode == WarningMode.Summary &&
                currentAgpVersion < TestVersions.AgpCompatibilityMatrix.AGP_812 -> suppressDeprecationWarningsSinceGradleVersion(
            gradleVersion = TestVersions.Gradle.G_8_14,
            currentGradleVersion = currentGradleVersion,
            reason = "AGP produces deprecation warning on resolve: https://issuetracker.google.com/issues/408334529"
        )
        currentGradleVersion >= GradleVersion.version(TestVersions.Gradle.G_8_14) &&
                currentAgpVersion < TestVersions.AgpCompatibilityMatrix.AGP_812 -> copy(warningMode = warningMode)
        else -> this
    }
}

// https://issuetracker.google.com/issues/399393875, was fixed in AGP 8.11.0
fun BuildOptions.suppressAgpWarningIsProperty(
    currentGradleVersion: GradleVersion,
): BuildOptions {
    val currentAgpVersion = androidVersion?.let { TestVersions.AgpCompatibilityMatrix.fromVersion(it) }
    return if (currentAgpVersion != null && currentAgpVersion < TestVersions.AgpCompatibilityMatrix.AGP_811) {
        suppressDeprecationWarningsSinceGradleVersion(
            gradleVersion = TestVersions.Gradle.G_8_14,
            currentGradleVersion = currentGradleVersion,
            reason = "APG produces deprecation warning for is-property: https://issuetracker.google.com/issues/399393875"
        )
    } else this
}

fun BuildOptions.suppressDeprecatedJdkWarningWithGradle814(
    currentGradleVersion: GradleVersion,
    jdk: JdkVersions.ProvidedJdk
): BuildOptions = if (currentGradleVersion == GradleVersion.version(TestVersions.Gradle.G_8_14) &&
    jdk.version < JavaVersion.VERSION_17
) {
    // Gradle does not produce runtime warning in 'summary' mode, which still fails the build
    copy(warningMode = WarningMode.None)
} else this

fun rerunTask(taskName: String) = arrayOf(taskName, "--rerun")
