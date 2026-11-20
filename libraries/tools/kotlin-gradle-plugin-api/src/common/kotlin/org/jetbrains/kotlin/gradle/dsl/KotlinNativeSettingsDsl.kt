/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

/**
 * Build-wide Kotlin/Native settings, exposed as `kotlin { native { ... } }` in `settings.gradle.kts`
 * by the `org.jetbrains.kotlin.ecosystem` plugin.
 *
 * Each property caps how many tasks of one kind run at the same time across the whole build.
 * Its convention is the Gradle property of the same name, so `kotlin.native.linkMaxWorkers=2`
 * in `gradle.properties` and `linkMaxWorkers = 2` here have the same effect.
 * An explicit value, or a provider with custom logic, overrides the Gradle property.
 * When neither is set, there is no limit.
 *
 * ```
 * // settings.gradle.kts
 * kotlin {
 *     native {
 *         linkMaxWorkers = 2
 *         compileMaxWorkers = providers.environmentVariable("CI").map { 2 }.orElse(4)
 *     }
 * }
 * ```
 *
 * The values are read once, after `settings.gradle.kts` is evaluated.
 *
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 *
 * @since 2.5.20
 */
@ExperimentalKotlinGradlePluginApi
interface KotlinNativeSettingsDsl {

    /**
     * The maximum number of Kotlin/Native compilation tasks that run at the same time.
     *
     * Convention: the `kotlin.native.compileMaxWorkers` Gradle property.
     *
     * @since 2.5.20
     */
    val compileMaxWorkers: Property<Int>

    /**
     * The maximum number of Kotlin/Native link tasks that run at the same time.
     *
     * Convention: the `kotlin.native.linkMaxWorkers` Gradle property.
     *
     * @since 2.5.20
     */
    val linkMaxWorkers: Property<Int>

    /**
     * The maximum number of C interop commonizer tasks that run at the same time.
     *
     * Convention: the `kotlin.native.commonizeCInteropMaxWorkers` Gradle property.
     *
     * @since 2.5.20
     */
    val commonizeCInteropMaxWorkers: Property<Int>
}
