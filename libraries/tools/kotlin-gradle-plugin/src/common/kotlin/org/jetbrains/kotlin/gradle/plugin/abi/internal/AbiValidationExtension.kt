/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi.internal

import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.abi.*
import org.jetbrains.kotlin.gradle.tasks.abi.*
import org.jetbrains.kotlin.gradle.utils.named
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.gradle.utils.property
import javax.inject.Inject

internal const val ABI_VALIDATION_EXTENSION_NAME = "abiValidation"

@ExperimentalAbiValidation
internal abstract class AbiValidationExtensionImpl @Inject constructor(
    objects: ObjectFactory,
    tasks: TaskContainer,
) : AbiValidationVariantSpecImpl(objects, tasks), AbiValidationExtension {
    final override val enabled: Property<Boolean> = objects.property<Boolean>().convention(false)

    @Deprecated("Variants DSL was removed and is no longer supported.", level = DeprecationLevel.ERROR)
    override val variants: NamedDomainObjectContainer<AbiValidationVariantSpec>
        get() {
            throw GradleException("Variants DSL was removed and is no longer supported.")
        }
}

internal fun ExtensionContainer.createAbiValidationExtension(project: Project): AbiValidationExtension {
    return create(
        AbiValidationExtension::class.java,
        ABI_VALIDATION_EXTENSION_NAME,
        AbiValidationExtensionImpl::class.java,
        project.objects,
        project.tasks
    )
}

@ExperimentalAbiValidation
internal open class AbiValidationVariantSpecImpl(objects: ObjectFactory, tasks: TaskContainer) :
    AbiValidationVariantSpec {
    override val filters: AbiFiltersSpec = objects.AbiFiltersSpecImpl()

    override val legacyDump: AbiValidationLegacyDumpExtension = objects.AbiValidationLegacyDumpExtensionImpl(tasks)
}

@ExperimentalAbiValidation
internal abstract class AbiValidationLegacyDumpExtensionImpl @Inject constructor(
    private val tasks: TaskContainer
) : AbiValidationLegacyDumpExtension {
    override val legacyCheckTaskProvider: TaskProvider<KotlinLegacyAbiCheckTask>
        get() = tasks.named<KotlinLegacyAbiCheckTask>(KotlinAbiCheckTaskImpl.NAME)

    override val legacyDumpTaskProvider: TaskProvider<KotlinLegacyAbiDumpTask>
        get() = tasks.named<KotlinLegacyAbiDumpTask>(KotlinAbiDumpTaskImpl.NAME)

    override val legacyUpdateTaskProvider: TaskProvider<Task>
        get() = tasks.named(KotlinAbiUpdateTask.NAME)
}

internal fun ObjectFactory.AbiValidationLegacyDumpExtensionImpl(
    tasks: TaskContainer,
): AbiValidationLegacyDumpExtensionImpl = newInstance<AbiValidationLegacyDumpExtensionImpl>(tasks)

@ExperimentalAbiValidation
internal abstract class AbiValidationMultiplatformExtensionImpl @Inject constructor(
    objects: ObjectFactory,
    tasks: TaskContainer
) : AbiValidationMultiplatformVariantSpecImpl(objects, tasks), AbiValidationMultiplatformExtension {

    final override val enabled: Property<Boolean> = objects.property<Boolean>().convention(false)

    override val klib: AbiValidationKlibKindExtension = objects.AbiValidationKlibKindExtension()

    @Deprecated("Variants DSL was removed and is no longer supported.", level = DeprecationLevel.ERROR)
    override val variants: NamedDomainObjectContainer<AbiValidationMultiplatformVariantSpec>
        get() {
            throw GradleException("Variants DSL was removed and is no longer supported.")
        }
}

internal fun ExtensionContainer.createAbiValidationMultiplatformExtension(project: Project): AbiValidationMultiplatformExtension {
    return create(
        AbiValidationMultiplatformExtension::class.java,
        ABI_VALIDATION_EXTENSION_NAME,
        AbiValidationMultiplatformExtensionImpl::class.java,
        project.objects,
        project.tasks
    )
}

@ExperimentalAbiValidation
internal open class AbiValidationMultiplatformVariantSpecImpl(objects: ObjectFactory, tasks: TaskContainer) :
    AbiValidationVariantSpecImpl(objects, tasks), AbiValidationMultiplatformVariantSpec {
    override val klib: AbiValidationKlibKindExtension = objects.AbiValidationKlibKindExtension()
}
