/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

internal val CreateSecondaryNonPackedCompilationOutputVariantsSideEffect = KotlinTargetSideEffect { target ->
    when (target) {
        is KotlinJvmTarget -> TODO()
        is KotlinNativeTarget -> TODO()
        is KotlinJsIrTarget -> TODO()
    }
}

internal const val NON_PACKED_KLIB_VARIANT_NAME = "non-packed-klib"