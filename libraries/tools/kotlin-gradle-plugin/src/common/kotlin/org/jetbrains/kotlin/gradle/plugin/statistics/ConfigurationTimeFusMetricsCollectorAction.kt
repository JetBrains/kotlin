/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine

/**
 * Check the configuration state and report relevant FUS events.
 */
internal val ConfigurationTimeFusMetricsCollectorAction = KotlinProjectSetupCoroutine {
    KotlinSourceSetMetrics.collectMetrics(project)
}
