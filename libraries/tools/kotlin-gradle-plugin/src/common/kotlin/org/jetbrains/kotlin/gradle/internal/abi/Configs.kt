/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.abi

import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.model.ObjectFactory
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationKlibKindExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationVariantSpec.Companion.MAIN_VARIANT_NAME
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.plugin.abi.AbiValidationPaths
import org.jetbrains.kotlin.gradle.plugin.abi.AbiValidationPaths.LEGACY_ACTUAL_DUMP_DIR
import org.jetbrains.kotlin.gradle.plugin.abi.AbiValidationPaths.LEGACY_KLIB_DUMP_EXTENSION
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinLegacyAbiCheckTaskImpl
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinLegacyAbiDumpTaskImpl
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinLegacyAbiUpdateTask
import org.jetbrains.kotlin.gradle.utils.newInstance

/**
 * Creates an instance of [AbiValidationKlibKindExtension].
 */
internal fun ObjectFactory.AbiValidationKlibKindExtension(): AbiValidationKlibKindExtension = newInstance<AbiValidationKlibKindExtension>()

/**
 * Configures the extension for Kotlin/JVM or Kotlin Android Gradle plugins.
 */
@ExperimentalAbiValidation
internal fun AbiValidationExtension.configure(project: Project) {
    this as AbiValidationExtensionImpl

    configureCommon(project.layout)
    configureLegacyTasks(project.name, project.tasks, project.layout)

    // add main root report variant
    variants.add(this)
}

/**
 * Configures the extension for Kotlin Multiplatform Gradle plugins.
 */
@ExperimentalAbiValidation
internal fun AbiValidationMultiplatformExtension.configure(project: Project) {
    this as AbiValidationMultiplatformExtensionImpl

    configureCommon(project.layout)
    configureMultiplatform()
    configureLegacyTasks(project.name, project.tasks, project.layout)

    // add main root report variant
    variants.add(this)
}

/**
 * Initializes [this] report variant with default values for all Kotlin Gradle plugin types.
 */
@ExperimentalAbiValidation
internal fun AbiValidationVariantSpecImpl.configureCommon(layout: ProjectLayout) {
    if (name == MAIN_VARIANT_NAME) {
        // configure main report variant
        legacyDump.referenceDumpDir.convention(layout.projectDirectory.dir(AbiValidationPaths.LEGACY_DEFAULT_REFERENCE_DUMP_DIR))
    } else {
        // configure custom report variant
        legacyDump.referenceDumpDir.convention(
            layout.projectDirectory.dir(AbiValidationPaths.LEGACY_DEFAULT_REFERENCE_DUMP_DIR + (if (name == MAIN_VARIANT_NAME) "" else "-$name"))
        )
    }
}

/**
 * Initializes report variant with default values for all Kotlin Gradle plugin types.
 */
@ExperimentalAbiValidation
internal fun AbiValidationMultiplatformVariantSpecImpl.configureMultiplatform() {
    klib.enabled.convention(true)
    klib.keepUnsupportedTargets.convention(true)
}

/**
 * Creates and preconfigures legacy tasks for [this] report variant.
 */
@ExperimentalAbiValidation
internal fun AbiValidationVariantSpecImpl.configureLegacyTasks(projectName: String, tasks: TaskContainer, layout: ProjectLayout) {
    val variantName = name
    val klibFileName = "$projectName$LEGACY_KLIB_DUMP_EXTENSION"

    val referenceDir = legacyDump.referenceDumpDir
    val filters = filters
    val dumpDir =
        layout.buildDirectory.dir(LEGACY_ACTUAL_DUMP_DIR + (if (variantName == MAIN_VARIANT_NAME) "" else "-$variantName"))

    val dumpTaskProvider =
        tasks.register(KotlinLegacyAbiDumpTaskImpl.nameForVariant(variantName), KotlinLegacyAbiDumpTaskImpl::class.java) {
            it.dumpDir.convention(dumpDir)
            it.referenceKlibDump.convention(referenceDir.map { dir -> dir.file(klibFileName) })
            it.keepUnsupportedTargets.convention(true)
            it.klibIsEnabled.convention(true)
            it.variantName.convention(variantName)

            it.klib.convention(it.klibInput.map { targets -> if (it.klibIsEnabled.get()) targets else emptyList() })

            it.includedClasses.convention(filters.included.classes)
            it.includedAnnotatedWith.convention(filters.included.annotatedWith)
            it.excludedClasses.convention(filters.excluded.classes)
            it.excludedAnnotatedWith.convention(filters.excluded.annotatedWith)
        }

    tasks.register(KotlinLegacyAbiCheckTaskImpl.nameForVariant(variantName), KotlinLegacyAbiCheckTaskImpl::class.java) {
        it.actualDir.convention(dumpTaskProvider.map { t -> t.dumpDir.get() })
        it.referenceDir.convention(referenceDir)
        it.variantName.convention(variantName)
    }

    tasks.register(KotlinLegacyAbiUpdateTask.nameForVariant(variantName), KotlinLegacyAbiUpdateTask::class.java) {
        it.actualDir.convention(dumpTaskProvider.map { t -> t.dumpDir.get() })
        it.referenceDir.convention(referenceDir)
        it.variantName.convention(variantName)
    }
}
