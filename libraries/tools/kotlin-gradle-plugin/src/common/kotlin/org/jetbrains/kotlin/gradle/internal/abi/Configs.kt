/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.abi

import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationKlibKindExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationVariantSpec.Companion.MAIN_VARIANT_NAME
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.plugin.abi.AbiValidationPaths
import org.jetbrains.kotlin.gradle.plugin.abi.AbiValidationPaths.LEGACY_ACTUAL_DUMP_DIR
import org.jetbrains.kotlin.gradle.plugin.abi.AbiValidationPaths.LEGACY_KLIB_DUMP_EXTENSION
import org.jetbrains.kotlin.gradle.tasks.abi.AbiToolsTask
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinAbiCheckTaskImpl
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinAbiDumpTaskImpl
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinAbiUpdateTask
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
    configureLegacyTasks(project.name, project.tasks, project.layout, enabled)

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
    configureLegacyTasks(project.name, project.tasks, project.layout, enabled)

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
internal fun AbiValidationVariantSpecImpl.configureLegacyTasks(
    projectName: String,
    tasks: TaskContainer,
    layout: ProjectLayout,
    isEnabled: Property<Boolean>,
) {
    val variantName = name
    val klibFileName = "$projectName$LEGACY_KLIB_DUMP_EXTENSION"

    val referenceDir = legacyDump.referenceDumpDir
    val filters = filters
    val dumpDir =
        layout.buildDirectory.dir(LEGACY_ACTUAL_DUMP_DIR + (if (variantName == MAIN_VARIANT_NAME) "" else "-$variantName"))

    val dumpTaskProvider =
        tasks.register(KotlinAbiDumpTaskImpl.nameForVariant(variantName), KotlinAbiDumpTaskImpl::class.java) {
            it.dumpDir.convention(dumpDir)
            it.referenceKlibDump.convention(referenceDir.map { dir -> dir.file(klibFileName) })
            it.keepUnsupportedTargets.convention(true)
            it.klibIsEnabled.convention(true)
            it.variantName.convention(variantName)

            it.klib.convention(it.klibInput.map { targets -> if (it.klibIsEnabled.get()) targets else emptyList() })

            it.includedClasses.convention(filters.include.byNames)
            it.includedAnnotatedWith.convention(filters.include.annotatedWith)
            it.excludedClasses.convention(filters.exclude.byNames)
            it.excludedAnnotatedWith.convention(filters.exclude.annotatedWith)

            it.description = "Dumps the public Application Binary Interface (ABI) into files in the build directory " +
                    "for the '$variantName' variant."
            // task should be hidden from the task list
            it.group = null

            it.onlyIf { isEnabled.get() }
        }

    val checkTaskProvider = tasks.register(KotlinAbiCheckTaskImpl.nameForVariant(variantName), KotlinAbiCheckTaskImpl::class.java) {
        it.actualDir.convention(dumpTaskProvider.map { t -> t.dumpDir.get() })
        it.referenceDir.convention(referenceDir)
        it.variantName.convention(variantName)

        it.description = "Checks that the public Application Binary Interface (ABI) of the current project code matches" +
                "the reference dump file for the '$variantName' variant."
        it.group = LifecycleBasePlugin.VERIFICATION_GROUP

        it.onlyIf { isEnabled.get() }
    }

    val updateTaskProvider = tasks.register(KotlinAbiUpdateTask.nameForVariant(variantName), KotlinAbiUpdateTask::class.java) {
        it.actualDir.convention(dumpTaskProvider.map { t -> t.dumpDir.get() })
        it.referenceDir.convention(referenceDir)
        it.variantName.convention(variantName)

        it.description = "Writes the public Application Binary Interface (ABI) of the current code to the reference dump " +
                "file for the '$variantName' variant."
        it.group = LifecycleBasePlugin.VERIFICATION_GROUP

        it.onlyIf { isEnabled.get() }
    }

    /**
     * Creating of the temporary tasks for backward compatibility with previous naming.
     *
     * Although BCV is still in an experimental state, some projects (for example, coroutines) use it,
     * so it will be convenient if we implement a smooth migration method.
     *
     * Short deprecation cycle:
     * - create tasks with old names and deprecation warnings (current state)
     * - throw exception if tasks with old names are used
     * - remove tasks with old names
     */
    val checkTaskName = checkTaskProvider.name
    tasks.register(AbiToolsTask.composeTaskName("checkLegacyAbi", variantName)) { task ->
        task.dependsOn(checkTaskProvider)
        task.doFirst {
            val projectPath = it.path.substringBeforeLast(":")
            it.logger.warn("Task ${it.path} is deprecated, use $projectPath:$checkTaskName instead")
        }
    }

    val updateTaskName = updateTaskProvider.name
    tasks.register(AbiToolsTask.composeTaskName("updateLegacyAbi", variantName)) { task ->
        task.dependsOn(updateTaskProvider)
        task.doFirst {
            val projectPath = it.path.substringBeforeLast(":")
            it.logger.warn("Task ${it.path} is deprecated, use $projectPath:$updateTaskName instead")
        }
    }
}
