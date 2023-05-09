/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.configuration.WarningMode
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties.COMPILE_INCREMENTAL_WITH_ARTIFACT_TRANSFORM
import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.dsl.NativeCacheKind
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.junit.jupiter.api.condition.OS
import java.util.*

data class BuildOptions(
    val logLevel: LogLevel = LogLevel.INFO,
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
    val verboseDiagnostics: Boolean = true,
    val nativeCacheKind: NativeCacheKind = NativeCacheKind.NONE
) {
    val safeAndroidVersion: String
        get() = androidVersion ?: error("AGP version is expected to be set")

    data class KaptOptions(
        val verbose: Boolean = false,
        val incrementalKapt: Boolean = false,
        val includeCompileClasspath: Boolean = false,
        val classLoadersCacheSize: Int? = null
    )

    data class JsOptions(
        val useIrBackend: Boolean? = null,
        val jsCompilerType: KotlinJsCompilerType? = null,
        val incrementalJs: Boolean? = null,
        val incrementalJsKlib: Boolean? = null,
        val incrementalJsIr: Boolean? = null,
        val compileNoWarn: Boolean = true
    )

    fun toArguments(
        gradleVersion: GradleVersion
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
            jsOptions.useIrBackend?.let { arguments.add("-Pkotlin.js.useIrBackend=$it") }
            jsOptions.jsCompilerType?.let { arguments.add("-Pkotlin.js.compiler=$it") }
            // because we have legacy compiler tests, we need nowarn for compiler testing
            if (jsOptions.compileNoWarn) {
                arguments.add("-Pkotlin.js.compiler.nowarn=true")
            }
        } else {
            arguments.add("-Pkotlin.js.compiler.nowarn=true")
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

        if (verboseDiagnostics) {
            arguments.add("-Pkotlin.internal.verboseDiagnostics=$verboseDiagnostics")
        }

        arguments.add("-Pkotlin.native.cacheKind=${nativeCacheKind.name.lowercase()}")

        arguments.addAll(freeArgs)

        return arguments.toList()
    }
}

fun BuildOptions.suppressDeprecationWarningsOn(
    @Suppress("UNUSED_PARAMETER") reason: String, // just to require specifying a reason for suppressing
    predicate: (BuildOptions) -> Boolean
) = if (predicate(this)) {
    copy(warningMode = WarningMode.Summary)
} else {
    this
}

fun BuildOptions.suppressDeprecationWarningsSinceGradleVersion(
    gradleVersion: String,
    currentGradleVersion: GradleVersion,
    reason: String
) = suppressDeprecationWarningsOn(reason) {
    currentGradleVersion >= GradleVersion.version(gradleVersion)
}
