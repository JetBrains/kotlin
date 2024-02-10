/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.tooling.events.FailureResult
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.task.TaskFinishEvent
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.report.TaskExecutionResult
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrOutputGranularity
import org.jetbrains.kotlin.gradle.utils.addConfigurationMetrics
import org.jetbrains.kotlin.gradle.utils.runMetricMethodSafely
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.StatisticsValuesConsumer
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

internal sealed interface FusMetrics
internal object ExecutedTaskMetrics : FusMetrics {
    private fun getMetricToReport(task: String) = when (task.substringAfterLast(":")) {
        "dokkaHtml" -> BooleanMetrics.ENABLED_DOKKA_HTML
        "dokkaGfm" -> BooleanMetrics.ENABLED_DOKKA_GFM
        "dokkaJavadoc" -> BooleanMetrics.ENABLED_DOKKA_JAVADOC
        "dokkaJekyll" -> BooleanMetrics.ENABLED_DOKKA_JEKYLL
        "dokkaHtmlMultiModule" -> BooleanMetrics.ENABLED_DOKKA_HTML_MULTI_MODULE
        "dokkaGfmMultiModule" -> BooleanMetrics.ENABLED_DOKKA_GFM_MULTI_MODULE
        "dokkaJekyllMultiModule" -> BooleanMetrics.ENABLED_DOKKA_JEKYLL_MULTI_MODULE
        "dokkaHtmlCollector" -> BooleanMetrics.ENABLED_DOKKA_HTML_COLLECTOR
        "dokkaGfmCollector" -> BooleanMetrics.ENABLED_DOKKA_GFM_COLLECTOR
        "dokkaJavadocCollector" -> BooleanMetrics.ENABLED_DOKKA_JAVADOC_COLLECTOR
        "dokkaJekyllCollector" -> BooleanMetrics.ENABLED_DOKKA_JEKYLL_COLLECTOR
        else -> null
    }

    internal fun collectMetrics(event: FinishEvent?, metricConsumer: StatisticsValuesConsumer) {
        event?.descriptor?.name?.also {
            getMetricToReport(it)?.also { metricConsumer.report(it, true) }
        }
    }
}

internal object CompilerArgumentMetrics : FusMetrics {
    // compilerArgs arguments may have some attributes which are overrided by freeCompilerArguments.
    // Here we perform the work which is repeated in compiler in order to obtain correct values. This extra work could be avoided when
    // compiler would report metrics by itself via JMX
    internal fun collectMetrics(
        compilerArgs: CommonCompilerArguments?,
        argsArray: Array<String>,
        metricsConsumer: StatisticsValuesConsumer,
    ) {
        when (compilerArgs) {
            is K2JVMCompilerArguments -> {
                val args = K2JVMCompilerArguments()
                parseCommandLineArguments(argsArray.toList(), args)
                metricsConsumer.report(StringMetrics.JVM_DEFAULTS, args.jvmDefault)
                metricsConsumer.report(StringMetrics.USE_FIR, args.useK2.toString())

                val pluginPatterns = listOf(
                    Pair(BooleanMetrics.ENABLED_COMPILER_PLUGIN_ALL_OPEN, "kotlin-allopen-.*jar"),
                    Pair(BooleanMetrics.ENABLED_COMPILER_PLUGIN_NO_ARG, "kotlin-noarg-.*jar"),
                    Pair(BooleanMetrics.ENABLED_COMPILER_PLUGIN_SAM_WITH_RECEIVER, "kotlin-sam-with-receiver-.*jar"),
                    Pair(BooleanMetrics.ENABLED_COMPILER_PLUGIN_LOMBOK, "kotlin-lombok-.*jar"),
                    Pair(BooleanMetrics.ENABLED_COMPILER_PLUGIN_PARSELIZE, "kotlin-parcelize-compiler-.*jar"),
                    Pair(BooleanMetrics.ENABLED_COMPILER_PLUGIN_ATOMICFU, "atomicfu-.*jar")
                )
                val pluginJars = args.pluginClasspaths?.map { it.replace("\\", "/").split("/").last() }
                if (pluginJars != null) {
                    for (pluginPattern in pluginPatterns) {
                        if (pluginJars.any { it.matches(pluginPattern.second.toRegex()) }) {
                            metricsConsumer.report(pluginPattern.first, true)
                        }
                    }
                }
            }
            is K2JSCompilerArguments -> {
                val args = K2JSCompilerArguments()
                parseCommandLineArguments(argsArray.toList(), args)

                if (args.irProduceJs) {
                    metricsConsumer.report(BooleanMetrics.JS_SOURCE_MAP, args.sourceMap)
                    metricsConsumer.report(StringMetrics.JS_PROPERTY_LAZY_INITIALIZATION, args.irPropertyLazyInitialization.toString())
                }
            }
        }
    }

}

internal object NativeCompilerOptionMetrics : FusMetrics {
    fun collectMetrics(compilerOptions: KotlinNativeCompilerOptions, metricsConsumer: StatisticsValuesConsumer) {
        metricsConsumer.report(BooleanMetrics.KOTLIN_PROGRESSIVE_MODE, compilerOptions.progressiveMode.get())
        compilerOptions.apiVersion.orNull?.also { v ->
            metricsConsumer.report(StringMetrics.KOTLIN_API_VERSION, v.version)
        }
        compilerOptions.languageVersion.orNull?.also { v ->
            metricsConsumer.report(StringMetrics.KOTLIN_LANGUAGE_VERSION, v.version)
        }
    }
}

internal object KotlinTaskExecutionMetrics : FusMetrics {
    fun collectMetrics(taskExecutionResult: TaskExecutionResult, event: TaskFinishEvent, metricsConsumer: StatisticsValuesConsumer) {
        val totalTimeMs = event.result.endTime - event.result.startTime
        val buildMetrics = taskExecutionResult.buildMetrics
        metricsConsumer.report(NumericalMetrics.COMPILATION_DURATION, totalTimeMs)
        metricsConsumer.report(BooleanMetrics.KOTLIN_COMPILATION_FAILED, event.result is FailureResult)
        metricsConsumer.report(NumericalMetrics.COMPILATIONS_COUNT, 1)

        val metricsMap = buildMetrics.buildPerformanceMetrics.asMap()

        val linesOfCode = metricsMap[GradleBuildPerformanceMetric.ANALYZED_LINES_NUMBER]
        if (linesOfCode != null && linesOfCode > 0 && totalTimeMs > 0) {
            metricsConsumer.report(NumericalMetrics.COMPILED_LINES_OF_CODE, linesOfCode)
            metricsConsumer.report(NumericalMetrics.COMPILATION_LINES_PER_SECOND, linesOfCode * 1000 / totalTimeMs, null, linesOfCode)
            metricsMap[GradleBuildPerformanceMetric.ANALYSIS_LPS]?.also { value ->
                metricsConsumer.report(NumericalMetrics.ANALYSIS_LINES_PER_SECOND, value, null, linesOfCode)
            }
            metricsMap[GradleBuildPerformanceMetric.CODE_GENERATION_LPS]?.also { value ->
                metricsConsumer.report(NumericalMetrics.CODE_GENERATION_LINES_PER_SECOND, value, null, linesOfCode)
            }
        }
        metricsConsumer.report(
            NumericalMetrics.INCREMENTAL_COMPILATIONS_COUNT,
            if (taskExecutionResult.buildMetrics.buildAttributes.asMap().isEmpty()) 1 else 0
        )
    }
}

internal object BuildFinishMetrics : FusMetrics {
    fun collectMetrics(
        logger: Logger,
        buildFailed: Boolean,
        buildStartTime: Long?,
        projectEvaluatedTime: Long?,
        metricsConsumer: StatisticsValuesConsumer,
    ) {
        reportGlobalMetrics(logger, metricsConsumer)
        reportBuildFinished(logger, buildFailed, buildStartTime, projectEvaluatedTime, metricsConsumer)
    }

    private fun reportGlobalMetrics(logger: Logger, metricConsumer: StatisticsValuesConsumer) {
        runMetricMethodSafely(logger, "reportGlobalMetrics") {
            System.getProperty("os.name")?.also { metricConsumer.report(StringMetrics.OS_TYPE, System.getProperty("os.name")) }
            metricConsumer.report(NumericalMetrics.CPU_NUMBER_OF_CORES, Runtime.getRuntime().availableProcessors().toLong())
            metricConsumer.report(BooleanMetrics.EXECUTED_FROM_IDEA, System.getProperty("idea.active") != null)
            metricConsumer.report(NumericalMetrics.GRADLE_DAEMON_HEAP_SIZE, Runtime.getRuntime().maxMemory())
            metricConsumer.report(NumericalMetrics.GRADLE_BUILD_NUMBER_IN_CURRENT_DAEMON, DaemonReuseCounter.incrementAndGetOrdinal())
        }
    }

    private fun reportBuildFinished(
        logger: Logger,
        buildFailed: Boolean,
        buildStartedTime: Long?,
        projectEvaluatedTime: Long?,
        metricsContainer: StatisticsValuesConsumer,
    ) {
        runMetricMethodSafely(logger, "reportBuildFinish") {
            val finishTime = System.currentTimeMillis()
            if (buildStartedTime != null) {
                metricsContainer.report(NumericalMetrics.GRADLE_BUILD_DURATION, finishTime - buildStartedTime)
            }
            if (projectEvaluatedTime != null) {
                metricsContainer.report(NumericalMetrics.GRADLE_EXECUTION_DURATION, finishTime - projectEvaluatedTime)
            }
            metricsContainer.report(NumericalMetrics.BUILD_FINISH_TIME, finishTime)
            metricsContainer.report(BooleanMetrics.BUILD_FAILED, buildFailed)
        }
    }

}

internal object CompileKotlinTaskMetrics : FusMetrics {
    internal fun collectMetrics(
        name: String,
        compilerOptions: KotlinCommonCompilerOptions,
        metricsContainer: StatisticsValuesConsumer
    ) {
        metricsContainer.report(BooleanMetrics.KOTLIN_PROGRESSIVE_MODE, compilerOptions.progressiveMode.get())
        compilerOptions.apiVersion.orNull?.also { v ->
            metricsContainer.report(StringMetrics.KOTLIN_API_VERSION, v.version)
        }
        compilerOptions.languageVersion.orNull?.also { v ->
            metricsContainer.report(StringMetrics.KOTLIN_LANGUAGE_VERSION, v.version)
        }
        if (name.contains("Test"))
            metricsContainer.report(BooleanMetrics.TESTS_EXECUTED, true)
        else
            metricsContainer.report(BooleanMetrics.COMPILATION_STARTED, true)
    }
}
internal object CompileKotlinJsTaskMetrics : FusMetrics {
    internal fun collectMetrics(incrementalCompilation: Boolean, metricsContainer: StatisticsValuesConsumer) {
        metricsContainer.report(BooleanMetrics.JS_KLIB_INCREMENTAL, incrementalCompilation)
    }
}

internal object CompileKotlinJsIrLinkMetrics : FusMetrics {
    internal fun collectMetrics(
        compilerArgs: K2JSCompilerArguments,
        incrementalJsIr: Boolean,
        metricsConsumer: StatisticsValuesConsumer,
    ) {
        metricsConsumer.report(BooleanMetrics.JS_IR_INCREMENTAL, incrementalJsIr)
        val newArgs = K2JSCompilerArguments()
        parseCommandLineArguments(ArgumentUtils.convertArgumentsToStringList(compilerArgs), newArgs)
        metricsConsumer.report(
            StringMetrics.JS_OUTPUT_GRANULARITY,
            if (newArgs.irPerModule)
                KotlinJsIrOutputGranularity.PER_MODULE.name.toLowerCaseAsciiOnly()
            else
                KotlinJsIrOutputGranularity.WHOLE_PROGRAM.name.toLowerCaseAsciiOnly()
        )
    }
}

internal object KotlinMetadataConfigurationMetrics : FusMetrics {
    internal fun collectMetrics(metricContainer: MetricContainer) {
        metricContainer.put(BooleanMetrics.ENABLED_HMPP, true)
    }
}

internal object KotlinProjectConfigurationMetrics : FusMetrics {
    internal fun collectMetrics(project: Project) = collectProjectConfigurationTimeMetrics(project)

}

internal object UrlRepoConfigurationMetrics : FusMetrics {
    internal fun collectMetrics(
        length: Long,
        downloadDuration: Long,
        metricsConsumer: StatisticsValuesConsumer
    ) {
        metricsConsumer.report(NumericalMetrics.ARTIFACTS_DOWNLOAD_SPEED, length * 1000 / downloadDuration)
    }
}

internal object KotlinJsIrTargetMetrics : FusMetrics {
    internal fun collectMetrics(isBrowserConfigured: Boolean, isNodejsConfigured: Boolean, project: Project) {
        project.addConfigurationMetrics { metricContainer ->
            when {
                isBrowserConfigured && isNodejsConfigured -> metricContainer.put(StringMetrics.JS_TARGET_MODE, "both")
                isBrowserConfigured -> metricContainer.put(StringMetrics.JS_TARGET_MODE, "browser")
                isNodejsConfigured -> metricContainer.put(StringMetrics.JS_TARGET_MODE, "nodejs")
                !isBrowserConfigured && !isNodejsConfigured -> metricContainer.put(StringMetrics.JS_TARGET_MODE, "none")
            }
        }

    }
}

internal object MultiplatformTargetMetrics : FusMetrics {
    internal fun collectMetrics(target: KotlinTarget, project: Project) {
        /* Report the platform to tbe build stats service */
        val targetName = if (target is KotlinNativeTarget) target.konanTarget.name
        else target.platformType.name
        project.addConfigurationMetrics {
            it.put(StringMetrics.MPP_PLATFORMS, targetName)
        }
    }
}