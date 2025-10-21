/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import kotlin.reflect.KFunction
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType

fun KotlinMultiplatformExtension.enableAllKotlinTargets(
    excludedTargets: Set<String> = setOf(
        // Disable Android because it requires that all projects have AGP,
        // and adding AGP would be slow and complicates most tests.
        "androidTarget",
    ),
) {
    // Find all KotlinMultiplatformExtension functions that
    // - have no args,
    // - are not deprecated,
    // - and return a KotlinTarget.
    // We assume these are KMP targets (but this assumption could change...)
    KotlinMultiplatformExtension::class.members
        .filterIsInstance<KFunction<*>>()
        .filter { it.annotations.none { annotation -> annotation is Deprecated } }
        .filter { it.parameters.size == 1 }
        .filter { it.parameters.single().type.isSubtypeOf(KotlinMultiplatformExtension::class.starProjectedType) }
        .filter { it.returnType.isSubtypeOf(KotlinTarget::class.starProjectedType) }
        .filter { it.name !in excludedTargets }
        .forEach {
            it.call(this)
        }
}
