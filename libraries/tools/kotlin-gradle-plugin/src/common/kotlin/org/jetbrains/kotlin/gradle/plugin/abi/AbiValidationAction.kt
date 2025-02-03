/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtensionOrNull
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.internal.abi.AbiValidationExtensionImpl
import org.jetbrains.kotlin.gradle.internal.abi.AbiValidationMultiplatformExtensionImpl
import org.jetbrains.kotlin.gradle.internal.abi.configure
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.findExtension

private const val EXTENSION_NAME = "abiValidation"

/**
 * Sets up Application Binary Interface (ABI) validation as part of the Kotlin Gradle plugin. This was previously known as the Binary Compatibility validator.
 */
internal val AbiValidationSetupAction = KotlinProjectSetupCoroutine {
    when {
        kotlinJvmExtensionOrNull != null -> {
            val extension = kotlinJvmExtension
            extension.extensions.createAbiValidationExtension(this).configure(this)
        }
        kotlinExtension is KotlinAndroidProjectExtension -> {
            val extension = kotlinExtension as KotlinAndroidProjectExtension
            extension.extensions.createAbiValidationExtension(this).configure(this)
        }
        multiplatformExtensionOrNull != null -> {
            val extension = multiplatformExtension
            extension.extensions.createAbiValidationMultiplatformExtension(this).configure(this)
        }
    }

    // wait until all compilations are configured
    KotlinPluginLifecycle.Stage.AfterFinaliseCompilations.await()

    val abiClasspath = prepareAbiClasspath()
    when {
        kotlinJvmExtensionOrNull != null -> {
            val extension = kotlinJvmExtension
            val abiValidation = extension.findExtension<AbiValidationExtensionImpl>(EXTENSION_NAME)
                ?: throw IllegalStateException("Kotlin extension not found: $EXTENSION_NAME")
            val target = extension.target

            abiValidation.variants.configureEach { variant ->
                variant.finalizeJvmOrAndroidVariant(this, abiClasspath, target)
            }
        }

        kotlinAndroidExtensionOrNull != null -> {
            val extension = kotlinAndroidExtension
            val abiValidation = extension.findExtension<AbiValidationExtensionImpl>(EXTENSION_NAME)
                ?: throw IllegalStateException("Kotlin extension not found: $EXTENSION_NAME")
            val target = extension.target

            abiValidation.variants.configureEach { variant ->
                variant.finalizeJvmOrAndroidVariant(this, abiClasspath, target)
            }
        }
        multiplatformExtensionOrNull != null -> {
            val extension = multiplatformExtension
            val abiValidation = extension.findExtension<AbiValidationMultiplatformExtensionImpl>(EXTENSION_NAME)
                ?: throw IllegalStateException("Kotlin extension not found: $EXTENSION_NAME")

            val targets = extension.awaitTargets()

            abiValidation.variants.configureEach { variant ->
                variant.finalizeMultiplatformVariant(this, abiClasspath, targets)
            }
        }
    }
}

private fun ExtensionContainer.createAbiValidationExtension(project: Project): AbiValidationExtension {
    return create(
        AbiValidationExtension::class.java,
        EXTENSION_NAME,
        AbiValidationExtensionImpl::class.java,
        project.name,
        project.layout,
        project.objects,
        project.tasks
    )
}

private fun ExtensionContainer.createAbiValidationMultiplatformExtension(project: Project): AbiValidationMultiplatformExtension {
    return create(
        AbiValidationMultiplatformExtension::class.java,
        EXTENSION_NAME,
        AbiValidationMultiplatformExtensionImpl::class.java,
        project.name,
        project.layout,
        project.objects,
        project.tasks
    )
}
