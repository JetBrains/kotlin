/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.toolchain

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.internal.properties.nativeProperties
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.crossCompilationOnCurrentHostSupported
import org.jetbrains.kotlin.gradle.targets.native.toolchain.KotlinNativeBundleArtifactFormat.addKotlinNativeBundleConfiguration

internal val NativeToolchainProjectSetupAction = KotlinProjectSetupCoroutine {
    val kotlinTargets = project.multiplatformExtension.awaitTargets()
    if (!project.nativeProperties.isToolchainEnabled.get()) return@KotlinProjectSetupCoroutine
    KotlinPluginLifecycle.Stage.AfterFinaliseCompilations.await()

    // register kotlin native bundle configuration resolution
    // when it is necessary i.e. there are native compilations that can be invoked on the current host
    if (kotlinTargets.flatMap { target -> target.compilations }
            .filterIsInstance<AbstractKotlinNativeCompilation>()
            .any { it.crossCompilationOnCurrentHostSupported.getOrThrow() }
    ) {
        project.configureKotlinNativeBundleConfigurationResolution()
    }
}

internal fun Project.configureKotlinNativeBundleConfigurationResolution() {
    /** this method can be called from multiple sources:
     * * [NativeToolchainProjectSetupAction]
     * * [org.jetbrains.kotlin.gradle.targets.native.internal.configureRootGradleProjectForKotlinCommonizer]
     *
     * Thus, it should be idempotent
     * */
    val idempotencyKey = "org.jetbrains.kotlin.configureKotlinNativeBundleConfigurationResolution"
    if (extensions.extraProperties.has(idempotencyKey)) return
    extensions.extraProperties[idempotencyKey] = true

    addKotlinNativeBundleConfiguration(this)
    KotlinNativeBundleArtifactFormat.setupAttributesMatchingStrategy(dependencies.attributesSchema)
    KotlinNativeBundleArtifactFormat.setupTransform(this)
}