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
import org.jetbrains.kotlin.gradle.utils.registerKlibArtifact

internal val CreateNonPackedKlibVariantsSideEffect = KotlinTargetSideEffect { target ->
    when (target) {
        is KotlinNativeTarget -> target.createNativeKlibSecondaryVariants()
        is KotlinJsIrTarget -> target.createJsKlibSecondaryVariants()
    }
}

private fun KotlinJsIrTarget.createJsKlibSecondaryVariants() {
    val apiElements = project.configurations.getByName(apiElementsConfigurationName)
    val secondaryApiVariant = createSecondaryKlibVariant(project, apiElements)
    val mainCompilation = compilations.getByName(MAIN_COMPILATION_NAME)
    secondaryApiVariant.registerKlibArtifact(
        mainCompilation.compileTaskProvider.map { it.klibDirectory.get() },
        mainCompilation.compilationName
    )

    val runtimeElements = project.configurations.getByName(runtimeElementsConfigurationName)
    val secondaryRuntimeVariant = createSecondaryKlibVariant(project, runtimeElements)
    secondaryRuntimeVariant.registerKlibArtifact(
        mainCompilation.compileTaskProvider.map { it.klibDirectory.get() },
        mainCompilation.compilationName
    )
}

private fun KotlinNativeTarget.createNativeKlibSecondaryVariants() {
    val apiElements = project.configurations.getByName(apiElementsConfigurationName)
    val mainCompilation = compilations.getByName(MAIN_COMPILATION_NAME)
    // main non-packed artifact
    val secondaryMainVariant = createSecondaryKlibVariant(project, apiElements)
    secondaryMainVariant.registerKlibArtifact(
        mainCompilation.compileTaskProvider.map { it.klibDirectory.get() },
        mainCompilation.compilationName
    )

    // cinterop non-packed artifacts
    val cinteropApiElements = project.configurations.getByName(cInteropApiElementsConfigurationName(this))
    val secondaryCinteropVariant = createSecondaryKlibVariant(project, cinteropApiElements)
    mainCompilation.cinterops.configureEach { cinterop ->
        val cInteropTask = project.locateTask<CInteropProcess>(cinterop.interopProcessingTaskName)
            ?: error("${cinterop.interopProcessingTaskName} not found during registration of secondary variants")
        secondaryMainVariant.registerKlibArtifact(
            cInteropTask.map { it.klibDirectory.get() },
            mainCompilation.compilationName,
            cinterop.classifier
        )
        secondaryCinteropVariant.registerKlibArtifact(
            cInteropTask.map { it.klibDirectory.get() },
            mainCompilation.compilationName,
            cinterop.classifier
        )
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