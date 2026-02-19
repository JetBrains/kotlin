/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets

import org.jetbrains.kotlin.gradle.internal.attributes.setAttributeTo
import org.jetbrains.kotlin.gradle.plugin.attributes.KlibPackaging
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.native.internal.locateOrCreateCInteropDependencyConfiguration

internal val ConfigureNonPackedKlibConsumingSideEffect = KotlinTargetSideEffect { target ->
    when (target) {
        is KotlinNativeTarget -> target.configureNonPackedNativeKlibConsuming()
        is KotlinJsIrTarget -> target.configureNonPackedJsKlibConsuming()
    }
}

private fun KotlinJsIrTarget.configureNonPackedJsKlibConsuming() {
    compilations.configureEach { compilation ->
        KlibPackaging.setAttributeTo(project, compilation.configurations.compileDependencyConfiguration.attributes, false)
        // K/JS uses runtime classpath for the linking phase
        val runtimeClasspath = compilation.configurations.runtimeDependencyConfiguration ?: error("$compilation has no runtime classpath")
        KlibPackaging.setAttributeTo(project, runtimeClasspath.attributes, false)
    }
}

private fun KotlinNativeTarget.configureNonPackedNativeKlibConsuming() {
    compilations.configureEach { compilation ->
        KlibPackaging.setAttributeTo(project, compilation.configurations.compileDependencyConfiguration.attributes, false)
        val cinteropResolvableConfiguration = project.locateOrCreateCInteropDependencyConfiguration(compilation)
        KlibPackaging.setAttributeTo(project, cinteropResolvableConfiguration.attributes, false)
    }
}