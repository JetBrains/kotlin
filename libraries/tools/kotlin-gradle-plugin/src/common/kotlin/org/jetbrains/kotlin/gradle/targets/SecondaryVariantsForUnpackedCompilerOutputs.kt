/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets

import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.LibraryElements
import org.jetbrains.kotlin.gradle.artifacts.KlibPackaging
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.configuration.BaseKotlinCompileConfig.Companion.CLASSES_SECONDARY_VARIANT_NAME
import org.jetbrains.kotlin.gradle.utils.copyAttributesTo
import org.jetbrains.kotlin.gradle.utils.named
import org.jetbrains.kotlin.gradle.utils.whenEvaluated

internal val SecondaryVariantsForUnpackedCompilerOutputs = KotlinTargetSideEffect { target ->
    when (target) {
        is KotlinJvmTarget -> target.classesAsSecondaryVariant()
        is KotlinNativeTarget -> target.nativeKlibsAsSecondaryVariants()
        is KotlinJsIrTarget -> target.jsKlibsAsSecondaryVariants()
    }
}

private fun KotlinJvmTarget.classesAsSecondaryVariant() {
    if (!project.kotlinPropertiesProvider.addSecondaryClassesVariant) return

    val configuration = project.configurations.getByName(apiElementsConfigurationName)
    val mainCompilation = compilations.getByName(MAIN_COMPILATION_NAME)

    val apiClassesVariant = configuration.outgoing.variants.maybeCreate(CLASSES_SECONDARY_VARIANT_NAME)
    apiClassesVariant.attributes.attribute(
        LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
        project.objects.named(LibraryElements.CLASSES)
    )

    project.whenEvaluated {
        // Java-library plugin already has done all required work here
        if (project.plugins.hasPlugin("java-library")) return@whenEvaluated

        mainCompilation.output.classesDirs.files.forEach { classesDir ->
            apiClassesVariant.artifact(classesDir) {
                it.type = ArtifactTypeDefinition.JVM_CLASS_DIRECTORY
                it.builtBy(mainCompilation.output.classesDirs.buildDependencies)
            }
        }
    }
}

private fun KotlinNativeTarget.nativeKlibsAsSecondaryVariants() {
    if (!project.kotlinPropertiesProvider.enableUnpackedKlibs) return

    val configuration = project.configurations.getByName(apiElementsConfigurationName)
    val mainCompilation = compilations.getByName(MAIN_COMPILATION_NAME)

    val unpackedKlibVariant = configuration.outgoing.variants.maybeCreate(UNPACKED_KLIB_VARIANT_NAME)
    project.launch {
        KotlinPluginLifecycle.Stage.AfterFinaliseDsl
        unpackedKlibVariant.attributes {
            configuration.copyAttributesTo(project, it)
            it.attribute(KlibPackaging.attribute, KlibPackaging.UNPACKED)
        }
    }
    unpackedKlibVariant.artifact(mainCompilation.compileTaskProvider.map { it.outputFile.get() })
}

private fun KotlinJsIrTarget.jsKlibsAsSecondaryVariants() {
    if (!project.kotlinPropertiesProvider.enableUnpackedKlibs) return

    val configuration = project.configurations.getByName(apiElementsConfigurationName)
    val mainCompilation = compilations.getByName(MAIN_COMPILATION_NAME)

    val unpackedKlibVariant = configuration.outgoing.variants.maybeCreate(UNPACKED_KLIB_VARIANT_NAME)
    project.launch {
        KotlinPluginLifecycle.Stage.AfterFinaliseDsl
        unpackedKlibVariant.attributes {
            configuration.copyAttributesTo(project, it)
            it.attribute(KlibPackaging.attribute, KlibPackaging.UNPACKED)
        }
    }
    unpackedKlibVariant.artifact(mainCompilation.compileTaskProvider.flatMap { it.destinationDirectory })
}


internal const val UNPACKED_KLIB_VARIANT_NAME = "unpacked-klib"