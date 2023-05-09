/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy

import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchy.SourceSetTree
import org.jetbrains.kotlin.gradle.plugin.awaitFinalValue
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.sources.android.AndroidVariantType
import org.jetbrains.kotlin.gradle.plugin.sources.android.type

internal suspend fun SourceSetTree.Companion.orNull(compilation: KotlinCompilation<*>): SourceSetTree? =
    when (compilation) {
        is KotlinJvmAndroidCompilation -> orNull(compilation.target, compilation.androidVariant.type)
        else -> when (compilation.name) {
            "main" -> main
            "test" -> test
            else -> SourceSetTree(compilation.name)
        }
    }

internal suspend fun SourceSetTree.Companion.orNull(
    target: KotlinAndroidTarget,
    variantType: AndroidVariantType
): SourceSetTree? {
    val multiplatform = target.project.multiplatformExtensionOrNull ?: return null
    return when (variantType) {
        AndroidVariantType.Main ->
            multiplatform.targetHierarchy.android.main.sourceSetTree.awaitFinalValue() ?: main
        AndroidVariantType.UnitTest ->
            multiplatform.targetHierarchy.android.unitTest.sourceSetTree.awaitFinalValue() ?: test
        AndroidVariantType.InstrumentedTest ->
            multiplatform.targetHierarchy.android.instrumentedTest.sourceSetTree.awaitFinalValue() ?: instrumentedTest
        AndroidVariantType.Unknown -> null
    }
}
