/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics.arguments

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.StringListMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics

internal object TrackedCompilerArguments {
    /**
     * THE list of compiler arguments tracked as FUS metrics.
     */
    val ALL: List<TrackedCompilerArgument<*>> = trackedCompilerArguments {
        forArguments<K2JVMCompilerArguments> {
            stringListMetric(
                StringListMetrics.JVM_DEFAULTS, "-jvm-default", "-Xjvm-default",
                extract = { it.jvmDefaultMode() },
                convert = JvmDefaultMode::description,
            )
        }

        forArguments<K2JSCompilerArguments> {
            withCondition(K2JSCompilerArguments::irProduceJs) {
                booleanMetric(BooleanMetrics.JS_SOURCE_MAP, K2JSCompilerArguments::sourceMap)
                booleanMetric(BooleanMetrics.JS_GENERATE_DTS, K2JSCompilerArguments::generateDts)
                stringListMetric(StringListMetrics.JS_PROPERTY_LAZY_INITIALIZATION, K2JSCompilerArguments::irPropertyLazyInitialization)
                stringMetric(StringMetrics.JS_ES_TARGET, K2JSCompilerArguments::target, default = "default")
                stringMetric(StringMetrics.JS_MODULE_SYSTEM, K2JSCompilerArguments::moduleKind, default = "default")
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun K2JVMCompilerArguments.jvmDefaultMode(): JvmDefaultMode? = when {
    jvmDefaultStable != null -> JvmDefaultMode.fromStringOrNull(jvmDefaultStable)
    jvmDefault != null -> JvmDefaultMode.fromStringOrNullOld(jvmDefault)
    else -> null
}
