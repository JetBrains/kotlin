/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.core.entity.properties

import org.jetbrains.kotlin.tools.projectWizard.core.entity.EntityReference
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.ModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module

sealed class PropertyReference<out T : Any> : EntityReference() {
    abstract val property: Property<T>
}

class PluginPropertyReference<out T: Any>(override val property: PluginProperty<T>): PropertyReference<T>() {
    override val path: String
        get() = property.path
}

class ModuleConfiguratorPropertyReference<out T : Any>(
    val configurator: ModuleConfigurator,
    val module: Module,
    override val property: ModuleConfiguratorProperty<T>
) : PropertyReference<T>() {
    override val path: String
        get() = "${configurator.id}/${module.identificator}/${property.path}"
}