/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi

import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.internal.abi.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.findExtension

/**
 * Sets up Application Binary Interface (ABI) validation as part of the Kotlin Gradle plugin. This was previously known as the Binary Compatibility validator.
 */
internal val AbiValidationSetupAction = KotlinProjectSetupCoroutine {
    val abiClasspath = prepareAbiClasspath()

    when {
        kotlinJvmExtensionOrNull != null -> {
            val extension = kotlinJvmExtension
            extension.extensions.createAbiValidationExtension(this).configure(this)
        }
        kotlinAndroidExtensionOrNull != null -> {
            val extension = kotlinAndroidExtension
            extension.extensions.createAbiValidationExtension(this).configure(this)
        }
        multiplatformExtensionOrNull != null -> {
            val extension = multiplatformExtension
            extension.extensions.createAbiValidationMultiplatformExtension(this).configure(this)
        }
    }

    // wait until all compilations are configured
    KotlinPluginLifecycle.Stage.AfterFinaliseCompilations.await()

    when {
        kotlinJvmExtensionOrNull != null -> {
            val extension = kotlinJvmExtension
            val abiValidation = extension.findExtension<AbiValidationExtensionImpl>(ABI_VALIDATION_EXTENSION_NAME)
                ?: throw IllegalStateException("Kotlin extension not found: $ABI_VALIDATION_EXTENSION_NAME")
            val target = extension.target

            abiValidation.variants.configureEach { variant ->
                variant.finalizeJvmOrAndroidVariant(this, abiClasspath, target)
            }
        }

        kotlinAndroidExtensionOrNull != null -> {
            val extension = kotlinAndroidExtension
            val abiValidation = extension.findExtension<AbiValidationExtensionImpl>(ABI_VALIDATION_EXTENSION_NAME)
                ?: throw IllegalStateException("Kotlin extension not found: $ABI_VALIDATION_EXTENSION_NAME")
            val target = extension.target

            abiValidation.variants.configureEach { variant ->
                variant.finalizeJvmOrAndroidVariant(this, abiClasspath, target)
            }
        }
        multiplatformExtensionOrNull != null -> {
            val extension = multiplatformExtension
            val abiValidation = extension.findExtension<AbiValidationMultiplatformExtensionImpl>(ABI_VALIDATION_EXTENSION_NAME)
                ?: throw IllegalStateException("Kotlin extension not found: $ABI_VALIDATION_EXTENSION_NAME")

            val targets = extension.awaitTargets()

            abiValidation.variants.configureEach { variant ->
                variant.finalizeMultiplatformVariant(this, abiClasspath, targets)
            }
        }
    }
}
