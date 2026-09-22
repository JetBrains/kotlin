/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics.arguments

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.statistics.metrics.StringListMetrics

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
    }
}

@Suppress("DEPRECATION")
private fun K2JVMCompilerArguments.jvmDefaultMode(): JvmDefaultMode? = when {
    jvmDefaultStable != null -> JvmDefaultMode.fromStringOrNull(jvmDefaultStable)
    jvmDefault != null -> JvmDefaultMode.fromStringOrNullOld(jvmDefault)
    else -> null
}
