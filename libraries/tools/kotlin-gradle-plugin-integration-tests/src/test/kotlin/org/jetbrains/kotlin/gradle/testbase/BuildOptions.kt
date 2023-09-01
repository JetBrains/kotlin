/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.configuration.WarningMode
import org.gradle.internal.logging.LoggingConfigurationBuildOptions.StacktraceOption
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties.COMPILE_INCREMENTAL_WITH_ARTIFACT_TRANSFORM
import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path
import java.util.*
import kotlin.io.path.absolutePathString

data class BuildOptions(
    val logLevel: LogLevel = LogLevel.INFO,
    val stacktraceMode: String? = StacktraceOption.FULL_STACKTRACE_LONG_OPTION,
    val kotlinVersion: String = TestVersions.Kotlin.CURRENT,
    val warningMode: WarningMode = WarningMode.Fail,
    val configurationCache: Boolean = false,
    val projectIsolation: Boolean = false,
    val configurationCacheProblems: BaseGradleIT.ConfigurationCacheProblems = BaseGradleIT.ConfigurationCacheProblems.FAIL,
    val parallel: Boolean = true,
    val incremental: Boolean? = null,
    val useGradleClasspathSnapshot: Boolean? = null,
    val useICClasspathSnapshot: Boolean? = null,
    val maxWorkers: Int = (Runtime.getRuntime().availableProcessors() / 4 - 1).coerceAtLeast(2),
    // On Windows OS enabling watch-fs prevents deleting temp directory, which fails the tests
    val fileSystemWatchEnabled: Boolean = !OS.WINDOWS.isCurrentOs,
    val buildCacheEnabled: Boolean = false,
    val kaptOptions: KaptOptions? = null,
    val androidVersion: String? = null,
    val jsOptions: JsOptions? = null,
    val buildReport: List<BuildReportType> = emptyList(),
    val usePreciseJavaTracking: Boolean? = null,
    val languageVersion: String? = null,
    val languageApiVersion: String? = null,
    val freeArgs: List<String> = emptyList(),
    val statisticsForceValidation: Boolean = true,
    val usePreciseOutputsBackup: Boolean? = null,
    val keepIncrementalCompilationCachesInMemory: Boolean? = null,
    val useDaemonFallbackStrategy: Boolean = false,
    val useParsableDiagnosticsFormatting: Boolean = true,
    val showDiagnosticsStacktrace: Boolean? = false, // false by default to not clutter the testdata + stacktraces change often
    val nativeOptions: NativeOptions = NativeOptions(),
    val compilerExecutionStrategy: KotlinCompilerExecutionStrategy? = null,
    val runViaBuildToolsApi: Boolean? = null,
    val konanDataDir: Path? = konanDir, // null can be used only if you are using custom 'kotlin.native.home' or 'org.jetbrains.kotlin.native.home' property instead of konanDir
) {
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
        val incrementalJsIr: Boolean? = null
    )

    data class NativeOptions(
        val cacheKind: NativeCacheKind? = NativeCacheKind.NONE,
        val cocoapodsGenerateWrapper: Boolean? = null,
        val cocoapodsPlatform: String? = null,
        val cocoapodsConfiguration: String? = null,
        val cocoapodsArchs: String? = null,
        val distributionType: String? = null,
        val distributionDownloadFromMaven: Boolean? = null,
        val reinstall: Boolean? = null,
        val restrictedDistribution: Boolean? = null,
        val useXcodeMessageStyle: Boolean? = null,
        val version: String? = null,
        val cacheOrchestration: String? = null,
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

        arguments.add("-Dorg.gradle.unsafe.configuration-cache=$configurationCache")
        arguments.add("-Dorg.gradle.unsafe.configuration-cache-problems=${configurationCacheProblems.name.lowercase(Locale.getDefault())}")

        if (gradleVersion >= GradleVersion.version("7.1")) {
            arguments.add("-Dorg.gradle.unsafe.isolated-projects=$projectIsolation")
        }
        if (parallel) {
            arguments.add("--parallel")
            arguments.add("--max-workers=$maxWorkers")
        } else {
            arguments.add("--no-parallel")
        }

        if (incremental != null) {
            arguments.add("-Pkotlin.incremental=$incremental")
        }

        useGradleClasspathSnapshot?.let { arguments.add("-P${COMPILE_INCREMENTAL_WITH_ARTIFACT_TRANSFORM.property}=$it") }
        useICClasspathSnapshot?.let { arguments.add("-Pkotlin.incremental.classpath.snapshot.enabled=$it") }

        if (fileSystemWatchEnabled) {
            arguments.add("--watch-fs")
        } else {
            arguments.add("--no-watch-fs")
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
        }

        if (androidVersion != null) {
            arguments.add("-Pandroid_tools_version=${androidVersion}")
        }
        arguments.add("-Ptest_fixes_version=${TestVersions.Kotlin.CURRENT}")

        if (buildReport.isNotEmpty()) {
            arguments.add("-Pkotlin.build.report.output=${buildReport.joinToString()}")
        }

        if (usePreciseJavaTracking != null) {
            arguments.add("-Pkotlin.incremental.usePreciseJavaTracking=$usePreciseJavaTracking")
        }

        if (statisticsForceValidation) {
            arguments.add("-Pkotlin_performance_profile_force_validation=true")
        }

        if (usePreciseOutputsBackup != null) {
            arguments.add("-Pkotlin.compiler.preciseCompilationResultsBackup=$usePreciseOutputsBackup")
        }
        if (languageApiVersion != null) {
            arguments.add("-Pkotlin.test.apiVersion=$languageApiVersion")
        }
        if (languageVersion != null) {
            arguments.add("-Pkotlin.test.languageVersion=$languageVersion")
        }

        if (keepIncrementalCompilationCachesInMemory != null) {
            arguments.add("-Pkotlin.compiler.keepIncrementalCompilationCachesInMemory=$keepIncrementalCompilationCachesInMemory")
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
        }

        if (showDiagnosticsStacktrace != null) {
            arguments.add("-Pkotlin.internal.diagnostics.showStacktrace=$showDiagnosticsStacktrace")
        }

        if (stacktraceMode != null) {
            arguments.add("--$stacktraceMode")
        }

        konanDataDir?.let {
            arguments.add("-Pkonan.data.dir=${konanDataDir.toAbsolutePath().normalize()}")
        }

        arguments.addAll(freeArgs)

        return arguments.toList()
    }

    private fun addNativeOptionsToArguments(
        arguments: MutableList<String>,
    ) {

        nativeOptions.cacheKind?.let {
            arguments.add("-Pkotlin.native.cacheKind=${nativeOptions.cacheKind.name.lowercase()}")
        }

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
        nativeOptions.cacheOrchestration?.let {
            arguments.add("-Pkotlin.native.cacheOrchestration=${it}")
        }

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