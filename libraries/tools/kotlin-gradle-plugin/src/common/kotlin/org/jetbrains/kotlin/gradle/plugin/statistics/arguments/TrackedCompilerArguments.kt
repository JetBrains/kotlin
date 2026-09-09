/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics.arguments

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrOutputGranularity
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.StringListMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

internal object TrackedCompilerArguments {
    /**
     * THE list of compiler arguments tracked as FUS metrics.
     */
    val ALL: List<TrackedCompilerArgument<*>> = trackedCompilerArguments {
        forArguments<CommonCompilerArguments> {
            stringMetric(StringMetrics.KOTLIN_LANGUAGE_VERSION, CommonCompilerArguments::languageVersion)
            stringMetric(StringMetrics.KOTLIN_API_VERSION, CommonCompilerArguments::apiVersion)
            booleanMetric(BooleanMetrics.KOTLIN_PROGRESSIVE_MODE, CommonCompilerArguments::progressiveMode)
            booleanMetric(BooleanMetrics.KOTLIN_SEPARATE_KMP_COMPILATION_ENABLED, CommonCompilerArguments::separateKmpCompilationScheme)
        }

        forArguments<K2JVMCompilerArguments> {
            stringListMetric(
                StringListMetrics.JVM_DEFAULTS, "-jvm-default", "-Xjvm-default",
                extract = { it.jvmDefaultMode() },
                convert = JvmDefaultMode::description,
            )
        }

        forArguments<K2JSCompilerArguments> {
            withCondition(K2JSCompilerArguments::irProduceJs) {
                booleanMetric(BooleanMetrics.JS_SOURCE_MAP, K2JSCompilerArguments::sourceMap, allowImplicit = true)
                booleanMetric(BooleanMetrics.JS_GENERATE_DTS, K2JSCompilerArguments::generateDts, allowImplicit = true)
                stringListMetric(StringListMetrics.JS_PROPERTY_LAZY_INITIALIZATION, K2JSCompilerArguments::irPropertyLazyInitialization, allowImplicit = true)
                stringMetric(StringMetrics.JS_ES_TARGET, K2JSCompilerArguments::target, default = DEFAULT_VALUE)
                stringMetric(StringMetrics.JS_MODULE_SYSTEM, K2JSCompilerArguments::moduleKind, default = DEFAULT_VALUE)
                stringMetric(StringMetrics.JS_OUTPUT_GRANULARITY, K2JSCompilerArguments::irPerModule) { perModule ->
                    val granularity = when (perModule){
                        true -> KotlinJsIrOutputGranularity.PER_MODULE
                        false -> KotlinJsIrOutputGranularity.WHOLE_PROGRAM
                    }
                    granularity.name.toLowerCaseAsciiOnly()
                }
            }
        }

        forArguments<K2NativeCompilerArguments> {
            selectFlagMetric(K2NativeCompilerArguments::binaryOptions) { it.binaryOption("gc")?.let(::gcMetric) }
            selectFlagMetric(K2NativeCompilerArguments::binaryOptions) {
                BooleanMetrics.ENABLED_SWIFT_EXPORT.takeIf { _ -> it.binaryOption("swiftExport") == "true" }
            }
        }
    }
}

private const val DEFAULT_VALUE = "default"

@Suppress("DEPRECATION")
private fun K2JVMCompilerArguments.jvmDefaultMode(): JvmDefaultMode? = when {
    jvmDefaultStable != null -> JvmDefaultMode.fromStringOrNull(jvmDefaultStable)
    jvmDefault != null -> JvmDefaultMode.fromStringOrNullOld(jvmDefault)
    else -> null
}

// The value of a single `-Xbinary=<name>=<value>` option, or `null` when it was not specified.
private fun K2NativeCompilerArguments.binaryOption(name: String): String? =
    binaryOptions.firstOrNull { it.startsWith("$name=") }?.substringAfter('=')

private fun gcMetric(value: String): BooleanMetrics? = when (value.toLowerCaseAsciiOnly()) {
    "noop" -> BooleanMetrics.ENABLED_NOOP_GC
    "stwms", "stop_the_world_mark_and_sweep" -> BooleanMetrics.ENABLED_STWMS_GC
    "pmcs", "parallel_mark_concurrent_sweep" -> BooleanMetrics.ENABLED_PMCS_GC
    "cms", "concurrent_mark_and_sweep" -> BooleanMetrics.ENABLED_CMS_GC
    else -> null
}
