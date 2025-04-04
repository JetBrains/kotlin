/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.toolchain

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHostForKlibCompilation
import org.jetbrains.kotlin.gradle.targets.native.toolchain.KotlinNativeBundleArtifactFormat.addKotlinNativeBundleConfiguration

internal val NativeToolchainProjectSetupAction = KotlinProjectSetupCoroutine {
    val kotlinTargets = project.multiplatformExtension.awaitTargets()
    if (!project.nativeProperties.isToolchainEnabled.get()) return@KotlinProjectSetupCoroutine
    KotlinPluginLifecycle.Stage.AfterFinaliseCompilations.await()
    if (kotlinTargets.flatMap { target -> target.compilations }
            .filterIsInstance<AbstractKotlinNativeCompilation>()
            .any { it.target.enabledOnCurrentHostForKlibCompilation }
    ) {
        addKotlinNativeBundleConfiguration(project)
        KotlinNativeBundleArtifactFormat.setupAttributesMatchingStrategy(project.dependencies.attributesSchema)
        KotlinNativeBundleArtifactFormat.setupTransform(project)
    }
}