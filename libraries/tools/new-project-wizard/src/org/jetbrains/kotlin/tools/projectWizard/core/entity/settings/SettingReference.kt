/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.core.entity.settings

import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.Identificator
import org.jetbrains.kotlin.tools.projectWizard.core.Reader
import org.jetbrains.kotlin.tools.projectWizard.core.safeAs
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.ModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Module
import org.jetbrains.kotlin.tools.projectWizard.templates.Template
import kotlin.reflect.KClass


sealed class SettingReference<out V : Any, out T : SettingType<V>> {
    @get:NonNls
    abstract val path: String
    abstract val type: KClass<out T>

    abstract fun Reader.getSetting(): Setting<V, T>

    final override fun toString() = path
    final override fun equals(other: Any?) = other.safeAs<SettingReference<*, *>>()?.path == path
    final override fun hashCode() = path.hashCode()
}

data class PluginSettingReference<out V : Any, out T : SettingType<V>>(
    override val path: String,
    override val type: KClass<@UnsafeVariance T>
) : SettingReference<V, T>() {

    @Suppress("UNCHECKED_CAST")
    constructor(setting: PluginSetting<V, T>) :
            this(setting.path, setting.type::class as KClass<T>)

    override fun Reader.getSetting(): Setting<V, T> = pluginSetting
}

inline val <V : Any, reified T : SettingType<V>> PluginSetting<V, T>.reference: PluginSettingReference<V, T>
    get() = PluginSettingReference(path, T::class)

sealed class ModuleConfiguratorSettingReference<V : Any, T : SettingType<V>> : SettingReference<V, T>() {
    abstract val descriptor: ModuleConfigurator
    abstract val moduleId: Identificator
    abstract val setting: ModuleConfiguratorSetting<V, T>

    override val path: String
        get() = "${descriptor.id}/$moduleId/${setting.path}"

    override val type: KClass<out T>
        get() = setting.type::class

    override fun Reader.getSetting(): Setting<V, T> = setting
    abstract val module: Module?
}

data class ModuleBasedConfiguratorSettingReference<V : Any, T : SettingType<V>>(
    override val descriptor: ModuleConfigurator,
    override val module: Module,
    override val setting: ModuleConfiguratorSetting<V, T>
) : ModuleConfiguratorSettingReference<V, T>() {
    override val moduleId: Identificator
        get() = module.identificator
}

data class IdBasedConfiguratorSettingReference<V : Any, T : SettingType<V>>(
    override val descriptor: ModuleConfigurator,
    override val moduleId: Identificator,
    override val setting: ModuleConfiguratorSetting<V, T>
) : ModuleConfiguratorSettingReference<V, T>() {
    override val module: Module? = null
}

sealed class TemplateSettingReference<V : Any, T : SettingType<V>> : SettingReference<V, T>() {
    abstract val descriptor: Template
    abstract val setting: TemplateSetting<V, T>
    abstract val sourcesetId: Identificator

    override val path: String
        get() = "${descriptor.id}/$sourcesetId/${setting.path}"

    override val type: KClass<out T>
        get() = setting.type::class

    override fun Reader.getSetting(): Setting<V, T> = setting
    abstract val module: Module?
}

data class ModuleBasedTemplateSettingReference<V : Any, T : SettingType<V>>(
    override val descriptor: Template,
    override val module: Module,
    override val setting: TemplateSetting<V, T>
) : TemplateSettingReference<V, T>() {
    override val sourcesetId: Identificator
        get() = module.identificator
}

data class IdBasedTemplateSettingReference<V : Any, T : SettingType<V>>(
    override val descriptor: Template,
    override val sourcesetId: Identificator,
    override val setting: TemplateSetting<V, T>
) : TemplateSettingReference<V, T>() {
    override val module: Module? = null
}

/*inline val <V : Any, reified T : SettingType<V>> PluginSettingPropertyReference<V, T>.reference: PluginSettingReference<V, T>
    get() = PluginSettingReference(this, T::class)

typealias PluginSettingPropertyReference<V, T> = KProperty1<out Plugin, PluginSetting<V, T>>*/

