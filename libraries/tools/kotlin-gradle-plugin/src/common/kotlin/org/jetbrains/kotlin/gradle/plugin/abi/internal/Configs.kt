/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi.internal

import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.tasks.TaskContainer
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinAbiCheckTaskImpl
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinAbiDumpTaskImpl
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinAbiUpdateTask

/**
 * Configures the extension for Kotlin/JVM or Kotlin Android Gradle plugins.
 */
@ExperimentalAbiValidation
internal fun AbiValidationExtensionImpl.configure(project: Project) {
    referenceDumpDir.convention(project.layout.projectDirectory.dir(AbiValidationPaths.LEGACY_DEFAULT_REFERENCE_DUMP_DIR))
    keepLocallyUnsupportedTargets.convention(true)
}

/**
 * Registers and preconfigures ABI validation's tasks.
 */
@ExperimentalAbiValidation
internal fun AbiValidationExtension.registerTasks(
    projectName: String,
    tasks: TaskContainer,
    layout: ProjectLayout
) {
    val klibFileName = "$projectName${AbiValidationPaths.LEGACY_KLIB_DUMP_EXTENSION}"

    val referenceDir = referenceDumpDir
    val filters = filters
    val dumpDir =
        layout.buildDirectory.dir(AbiValidationPaths.ACTUAL_DUMP_DIR)

    val dumpTaskProvider =
        tasks.register(KotlinAbiDumpTaskImpl.NAME, KotlinAbiDumpTaskImpl::class.java) {
            it.dumpDir.convention(dumpDir)
            it.referenceKlibDump.convention(referenceDir.map { dir -> dir.file(klibFileName) })
            it.keepLocallyUnsupportedTargets.convention(true)
            it.klibIsEnabled.convention(true)

            it.klib.convention(it.klibInput.map { targets -> if (it.klibIsEnabled.get()) targets else emptyList() })

            it.includedClasses.convention(filters.include.byNames)
            it.includedAnnotatedWith.convention(filters.include.annotatedWith)
            it.excludedClasses.convention(filters.exclude.byNames)
            it.excludedAnnotatedWith.convention(filters.exclude.annotatedWith)

            it.description = "Dumps the public Application Binary Interface (ABI) into files in the build directory."
            // task should be hidden from the task list
            it.group = null
        }

    val checkTaskProvider = tasks.register(KotlinAbiCheckTaskImpl.NAME, KotlinAbiCheckTaskImpl::class.java) {
        it.actualDir.convention(dumpTaskProvider.map { t -> t.dumpDir.get() })
        it.referenceDir.convention(referenceDir)

        it.description = "Checks that the public Application Binary Interface (ABI) of the current project code matches" +
                "the reference dump file"
        it.group = LifecycleBasePlugin.VERIFICATION_GROUP
    }

    val updateTaskProvider = tasks.register(KotlinAbiUpdateTask.NAME, KotlinAbiUpdateTask::class.java) {
        it.actualDir.convention(dumpTaskProvider.map { t -> t.dumpDir.get() })
        it.referenceDir.convention(referenceDir)

        it.description = "Writes the public Application Binary Interface (ABI) of the current code to the reference dump file."
        it.group = LifecycleBasePlugin.VERIFICATION_GROUP
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
    tasks.register("checkLegacyAbi") { task ->
        task.dependsOn(checkTaskProvider)
        task.doFirst {
            val projectPath = it.path.substringBeforeLast(":")
            it.logger.warn("Task ${it.path} is deprecated, use $projectPath:$checkTaskName instead")
        }
    }

    val updateTaskName = updateTaskProvider.name
    tasks.register("updateLegacyAbi") { task ->
        task.dependsOn(updateTaskProvider)
        task.doFirst {
            val projectPath = it.path.substringBeforeLast(":")
            it.logger.warn("Task ${it.path} is deprecated, use $projectPath:$updateTaskName instead")
        }
    }
}
