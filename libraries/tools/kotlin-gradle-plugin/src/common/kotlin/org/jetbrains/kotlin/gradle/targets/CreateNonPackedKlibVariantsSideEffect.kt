/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationVariant
import org.jetbrains.kotlin.gradle.internal.attributes.setAttributeTo
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.attributes.KlibPackaging
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.native.internal.cInteropApiElementsConfigurationName
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.tasks.locateTask

internal val CreateNonPackedKlibVariantsSideEffect = KotlinTargetSideEffect { target ->
    when (target) {
        is KotlinNativeTarget -> target.createNativeKlibSecondaryVariants()
        is KotlinJsIrTarget -> target.createJsKlibSecondaryVariants()
    }
}

private fun KotlinJsIrTarget.createJsKlibSecondaryVariants() {
    val apiElements = project.configurations.getByName(apiElementsConfigurationName)
    val secondaryVariant = createSecondaryKlibVariant(project, apiElements)
    secondaryVariant.artifact(compilations.getByName(MAIN_COMPILATION_NAME).compileTaskProvider.map { it.klibDirectory.get() })
}

private fun KotlinNativeTarget.createNativeKlibSecondaryVariants() {
    val apiElements = project.configurations.getByName(apiElementsConfigurationName)
    val mainCompilation = compilations.getByName(MAIN_COMPILATION_NAME)
    // main non-packed artifact
    val secondaryMainVariant = createSecondaryKlibVariant(project, apiElements)
    secondaryMainVariant.artifact(mainCompilation.compileTaskProvider.map { it.klibDirectory.get() })

    // cinterop non-packed artifacts
    val cinteropApiElements = project.configurations.getByName(cInteropApiElementsConfigurationName(this))
    val secondaryCinteropVariant = createSecondaryKlibVariant(project, cinteropApiElements)
    mainCompilation.cinterops.configureEach { cinterop ->
        val cInteropTask = project.locateTask<CInteropProcess>(cinterop.interopProcessingTaskName)
            ?: error("${cinterop.interopProcessingTaskName} not found during registration of secondary variants")
        secondaryMainVariant.artifact(cInteropTask.map { it.klibDirectory.get() })
        secondaryCinteropVariant.artifact(cInteropTask.map { it.klibDirectory.get() })
    }
}

private fun createSecondaryKlibVariant(
    project: Project,
    configuration: Configuration,
): ConfigurationVariant {
    KlibPackaging.setAttributeTo(project, configuration.attributes, true)
    return configuration.outgoing.variants.create(NON_PACKED_KLIB_VARIANT_NAME).apply {
        KlibPackaging.setAttributeTo(project, attributes, false)
    }
}

internal const val NON_PACKED_KLIB_VARIANT_NAME = "non-packed-klib"