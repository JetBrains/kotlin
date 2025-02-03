/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.abi

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Task
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.abi.*
import org.jetbrains.kotlin.gradle.tasks.abi.*
import org.jetbrains.kotlin.gradle.utils.named
import javax.inject.Inject

@ExperimentalAbiValidation
internal abstract class AbiValidationExtensionImpl @Inject constructor(
    projectName: String,
    layout: ProjectLayout,
    objects: ObjectFactory,
    tasks: TaskContainer,
) : AbiValidationVariantSpecImpl(AbiValidationVariantSpec.MAIN_VARIANT_NAME, objects, tasks), AbiValidationExtension {
    final override val variants: NamedDomainObjectContainer<AbiValidationVariantSpec> =
        objects.domainObjectContainer(AbiValidationVariantSpec::class.java) { variantName ->
            val variant = AbiValidationVariantSpecImpl(variantName, objects, tasks)
            variant.configureCommon(layout)
            variant.configureLegacyTasks(projectName, tasks, layout)
            variant
        }
}

@ExperimentalAbiValidation
internal open class AbiValidationVariantSpecImpl(private val variantName: String, objects: ObjectFactory, tasks: TaskContainer) :
    AbiValidationVariantSpec {
    override val filters: AbiFiltersSpec = objects.AbiFiltersSpecImpl()

    override val legacyDump: AbiValidationLegacyDumpExtension = objects.AbiValidationLegacyDumpExtensionImpl(variantName, tasks)

    override fun getName(): String = variantName
}

@ExperimentalAbiValidation
internal abstract class AbiValidationLegacyDumpExtensionImpl @Inject constructor(
    private val variantName: String,
    private val tasks: TaskContainer
) : AbiValidationLegacyDumpExtension {
    override val legacyCheckTaskProvider: TaskProvider<KotlinLegacyAbiCheckTask>
        get() = tasks.named<KotlinLegacyAbiCheckTask>(KotlinLegacyAbiCheckTaskImpl.nameForVariant(variantName))

    override val legacyDumpTaskProvider: TaskProvider<KotlinLegacyAbiDumpTask>
        get() = tasks.named<KotlinLegacyAbiDumpTask>(KotlinLegacyAbiDumpTaskImpl.nameForVariant(variantName))

    override val legacyUpdateTaskProvider: TaskProvider<Task>
        get() = tasks.named(KotlinLegacyAbiUpdateTask.nameForVariant(variantName))
}

@ExperimentalAbiValidation
internal abstract class AbiValidationMultiplatformExtensionImpl @Inject constructor(
    projectName: String,
    layout: ProjectLayout,
    objects: ObjectFactory,
    tasks: TaskContainer
) :
    AbiValidationMultiplatformVariantSpecImpl(AbiValidationVariantSpec.MAIN_VARIANT_NAME, objects, tasks), AbiValidationMultiplatformExtension {

    override val variants: NamedDomainObjectContainer<AbiValidationMultiplatformVariantSpec> =
        objects.domainObjectContainer(AbiValidationMultiplatformVariantSpec::class.java) { name ->
            val variant = AbiValidationMultiplatformVariantSpecImpl(name, objects, tasks)
            variant.configureCommon(layout)
            variant.configureMultiplatform()
            variant.configureLegacyTasks(projectName, tasks, layout)
            variant
        }

    override val klib: AbiValidationKlibKindExtension = objects.AbiValidationKlibKindExtension()
}

@ExperimentalAbiValidation
internal open class AbiValidationMultiplatformVariantSpecImpl(variantName: String, objects: ObjectFactory, tasks: TaskContainer) :
    AbiValidationVariantSpecImpl(variantName, objects, tasks), AbiValidationMultiplatformVariantSpec {
    override val klib: AbiValidationKlibKindExtension = objects.AbiValidationKlibKindExtension()
}
