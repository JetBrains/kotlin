/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics.plugins

import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics

/**
 * Represents an enumeration of plugins that can be observed during statistics collecting.
 *
 * Each plugin is associated with a title and a boolean statistic metric.
 *
 * @property title The title of the plugin.
 * @property metric The boolean statistic metric associated with the plugin.
 */
enum class ObservablePlugins(
    val title: String,
    val metric: BooleanMetrics,
) {
    DOKKA_PLUGIN("org.jetbrains.dokka", BooleanMetrics.ENABLED_DOKKA),
    KOTLIN_JS_PLUGIN("org.jetbrains.kotlin.js", BooleanMetrics.KOTLIN_JS_PLUGIN_ENABLED),
    COCOAPODS_PLUGIN("org.jetbrains.kotlin.native.cocoapods", BooleanMetrics.COCOAPODS_PLUGIN_ENABLED),
    KOVER_PLUGIN("org.jetbrains.kotlinx.kover", BooleanMetrics.KOTLINX_KOVER_GRADLE_PLUGIN_ENABLED),
    SERIALIZATION_PLUGIN("org.jetbrains.kotlin.plugin.serialization", BooleanMetrics.KOTLINX_SERIALIZATION_GRADLE_PLUGIN_ENABLED),
    ATOMICFU_PLUGIN("org.jetbrains.kotlinx.atomicfu", BooleanMetrics.KOTLINX_ATOMICFU_GRADLE_PLUGIN_ENABLED),
    BINARY_COMPATIBILITY_VALIDATOR_PLUGIN("org.jetbrains.kotlinx.binary-compatibility-validator", BooleanMetrics.KOTLINX_BINARY_COMPATIBILITY_GRADLE_PLUGIN_ENABLED),
}