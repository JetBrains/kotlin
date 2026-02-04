/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi.internal

import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.await

/**
 * Sets up Application Binary Interface (ABI) validation as part of the Kotlin Gradle plugin. This was previously known as the Binary Compatibility validator.
 */
internal val AbiValidationSetupAction = KotlinProjectSetupCoroutine {
    val abiClasspath = prepareAbiClasspath()

    val kotlin = kotlinExtensionOrNull ?: return@KotlinProjectSetupCoroutine
    val abiValidation = kotlin.abiValidationInternal
    abiValidation.configure(project)

    // wait until all compilations are configured
    KotlinPluginLifecycle.Stage.AfterFinaliseCompilations.await()

    if (!abiValidation.isActivated) return@KotlinProjectSetupCoroutine

    tasks.named("check") { checkTask -> checkTask.dependsOn(abiValidation.checkTaskProvider) }

    when {
        kotlinJvmExtensionOrNull != null -> {
            val extension = kotlinJvmExtension
            val target = extension.target
            finalizeJvmVariant(this, abiClasspath, target)
        }

        kotlinAndroidExtensionOrNull != null -> {
            val extension = kotlinAndroidExtension
            val target = extension.target
            finalizeAndroidVariant(this, abiClasspath, target)
        }

        multiplatformExtensionOrNull != null -> {
            val extension = multiplatformExtension
            val targets = extension.awaitTargets()
            finalizeMultiplatformVariant(this, abiClasspath, targets, abiValidation.keepLocallyUnsupportedTargets)
        }
    }
}
