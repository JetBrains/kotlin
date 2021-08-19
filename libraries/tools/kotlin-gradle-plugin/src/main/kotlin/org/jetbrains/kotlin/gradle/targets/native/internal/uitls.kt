/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet

internal inline fun KotlinMultiplatformExtension.forAllSharedNativeCompilations(
    crossinline action: (compilation: KotlinSharedNativeCompilation) -> Unit
) {
    targets.withType(KotlinMetadataTarget::class.java).all { target ->
        target.compilations.withType(KotlinSharedNativeCompilation::class.java).all { compilation ->
            action(compilation)
        }
    }
}

internal inline fun KotlinMultiplatformExtension.forAllDefaultKotlinSourceSets(
    crossinline action: (sourceSet: DefaultKotlinSourceSet) -> Unit
) {
    sourceSets.withType(DefaultKotlinSourceSet::class.java).all { sourceSet ->
        action(sourceSet)
    }
}