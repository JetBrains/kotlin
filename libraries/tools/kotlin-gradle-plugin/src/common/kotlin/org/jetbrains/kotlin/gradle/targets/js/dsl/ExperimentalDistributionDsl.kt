/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

/**
 * Marker for the experimental Kotlin JS and Wasm distribution feature.
 *
 * The distribution feature is used to control the output of the final bundle.
 *
 * For more information, see https://kotl.in/kotlin-js-distribution-target
 *
 * @see org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsSubTargetDsl.distribution
 * @see org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrSubTarget.distribution
 */
// KT-77217 Decide on stabilising this feature.
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Target(AnnotationTarget.FUNCTION)
annotation class ExperimentalDistributionDsl
