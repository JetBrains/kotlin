/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchy.ModuleName
import org.jetbrains.kotlin.gradle.plugin.awaitFinalValue
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.sources.android.AndroidVariantType
import org.jetbrains.kotlin.gradle.plugin.sources.android.type

internal suspend fun ModuleName.Companion.orNull(compilation: KotlinCompilation<*>): ModuleName? =
    when (compilation) {
        is KotlinJvmAndroidCompilation -> orNull(compilation.target, compilation.androidVariant.type)
        else -> when (compilation.name) {
            "main" -> main
            "test" -> test
            else -> ModuleName(compilation.name)
        }
    }

internal suspend fun ModuleName.Companion.orNull(
    target: KotlinAndroidTarget,
    variantType: AndroidVariantType
): ModuleName? = when (variantType) {
    AndroidVariantType.Main ->
        target.main.targetHierarchy.module.awaitFinalValue() ?: main
    AndroidVariantType.UnitTest ->
        target.unitTest.targetHierarchy.module.awaitFinalValue() ?: test
    AndroidVariantType.InstrumentedTest ->
        target.instrumentedTest.targetHierarchy.module.awaitFinalValue() ?: instrumentedTest
    AndroidVariantType.Unknown -> null
}
