/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.abi

import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.TaskContainer
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationMultiplatformVariantSpec
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationVariantSpec
import org.jetbrains.kotlin.gradle.dsl.abi.VariantConfigurator
import org.jetbrains.kotlin.gradle.internal.NamedDomainObjectConfiguratorImpl
import javax.inject.Inject

internal open class VariantConfiguratorImpl @Inject constructor(
    private val projectName: String,
    private val layout: ProjectLayout,
    private val objects: ObjectFactory,
    private val tasks: TaskContainer,
) : NamedDomainObjectConfiguratorImpl<AbiValidationVariantSpec>(objects), VariantConfigurator<AbiValidationVariantSpec> {
    override val type: Class<AbiValidationVariantSpec> get() = AbiValidationVariantSpec::class.java

    override fun factory(name: String): AbiValidationVariantSpec {
        return AbiValidationVariantSpecImpl(name, objects, tasks)
    }

    override fun preConfigure(element: AbiValidationVariantSpec) {
        element.configureCommon(layout)
        element.configureLegacyTasks(projectName, tasks, layout)
    }

    override fun register(name: String, configure: AbiValidationVariantSpec.() -> Unit) {
        super.doRegister(name, configure)
    }

    override fun register(name: String) {
        super.doRegister(name, null)
    }

    fun add(variant: AbiValidationVariantSpecImpl) {
        doAdd(variant)
    }
}

internal open class MultiplatformVariantConfiguratorImpl @Inject constructor(
    private val projectName: String,
    private val layout: ProjectLayout,
    private val objects: ObjectFactory,
    private val tasks: TaskContainer,
) : NamedDomainObjectConfiguratorImpl<AbiValidationMultiplatformVariantSpec>(objects),
    VariantConfigurator<AbiValidationMultiplatformVariantSpec> {
    override val type: Class<AbiValidationMultiplatformVariantSpec> get() = AbiValidationMultiplatformVariantSpec::class.java

    override fun factory(name: String): AbiValidationMultiplatformVariantSpecImpl {
        return AbiValidationMultiplatformVariantSpecImpl(name, objects, tasks)
    }

    override fun preConfigure(element: AbiValidationMultiplatformVariantSpec) {
        element.configureCommon(layout)
        element.configureMultiplatform()
        element.configureLegacyTasks(projectName, tasks, layout)
    }

    override fun register(name: String, configure: AbiValidationMultiplatformVariantSpec.() -> Unit) {
        super.doRegister(name, configure)
    }

    override fun register(name: String) {
        super.doRegister(name, null)
    }

    fun add(variant: AbiValidationMultiplatformVariantSpecImpl) {
        doAdd(variant)
    }
}