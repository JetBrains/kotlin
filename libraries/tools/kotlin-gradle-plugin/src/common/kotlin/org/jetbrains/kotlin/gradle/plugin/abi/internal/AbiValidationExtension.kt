/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi.internal

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.compilerRunner.btapi.BuildSessionService
import org.jetbrains.kotlin.gradle.dsl.abi.AbiFiltersSpec
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.abi.BinariesSource
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinAbiCheckTaskImpl
import org.jetbrains.kotlin.gradle.tasks.abi.KotlinAbiUpdateTask
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.gradle.utils.propertyWithConvention
import javax.inject.Inject

@ExperimentalAbiValidation
internal abstract class AbiValidationExtensionImpl @Inject constructor(
    objects: ObjectFactory,
    private val projectName: String,
    private val tasks: TaskContainer,
    private val layout: ProjectLayout,
    private val buildSessionService: Provider<BuildSessionService>,
    private val configurations: ConfigurationContainer
) : AbiValidationExtension {
    private var activated = false

    internal fun activate() {
        if (!activated) {
            activated = true
            registerTasks(projectName, tasks, layout, buildSessionService, configurations)
        }
    }

    internal val isActivated: Boolean get() = activated

    @Deprecated(
        "Property was removed, to enable ABI validation call function abiValidation(), abiValidation { ... } or read abiValidation property.",
        level = DeprecationLevel.ERROR
    )

    override val filters: AbiFiltersSpec = objects.AbiFiltersSpecImpl()

    override val referenceDumpDir: DirectoryProperty = objects.directoryProperty()

    override val keepLocallyUnsupportedTargets: Property<Boolean> = objects.property<Boolean>()

    override val binariesSource: Property<BinariesSource> = objects.propertyWithConvention<BinariesSource>(BinariesSource.MAIN_COMPILATION)

    override val checkTaskProvider: TaskProvider<Task>
        get() = tasks.named(KotlinAbiCheckTaskImpl.NAME)
    override val updateTaskProvider: TaskProvider<Task>
        get() = tasks.named(KotlinAbiUpdateTask.NAME)
}

internal fun Project.AbiValidationExtensionImpl(): AbiValidationExtensionImpl =
    objects.newInstance(
        AbiValidationExtensionImpl::class.java,
        objects,
        name,
        tasks,
        layout,
        BuildSessionService.registerIfAbsent(this),
        configurations
    )
