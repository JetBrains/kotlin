/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.kotlinSourceSetsIncludingDefault
import org.jetbrains.kotlin.gradle.plugin.sources.withDependsOnClosure

internal fun KotlinSharedNativeCompilation.getImplicitlyDependingNativeCompilations(): Set<KotlinNativeCompilation> {
    val multiplatformExtension = project.multiplatformExtensionOrNull ?: return emptySet()
    val allParticipatingSourceSetsOfCompilation = allParticipatingSourceSets()

    return multiplatformExtension.targets
        .flatMap { target -> target.compilations }
        .filterIsInstance<KotlinNativeCompilation>()
        .filter { nativeCompilation -> nativeCompilation.allParticipatingSourceSets().containsAll(allParticipatingSourceSetsOfCompilation) }
        .toSet()
}

/**
 * Some implementations of [KotlinCompilation] do not contain the default source set in
 * [KotlinCompilation.kotlinSourceSets] or [KotlinCompilation.allKotlinSourceSets]
 * see KT-45412
 */
private fun KotlinCompilation<*>.allParticipatingSourceSets(): Set<KotlinSourceSet> {
    return kotlinSourceSetsIncludingDefault.withDependsOnClosure
}
