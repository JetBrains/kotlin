/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.core.entity.settings

import org.jetbrains.kotlin.tools.projectWizard.core.EventManager
import org.jetbrains.kotlin.tools.projectWizard.core.entity.path
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class SettingContext {
    private val values = mutableMapOf<String, Any>()
    private val pluginSettings = mutableMapOf<String, PluginSetting<*, *>>()
    val eventManager = EventManager()

    @Suppress("UNCHECKED_CAST")
    operator fun <V : Any, T : SettingType<V>> get(
        reference: SettingReference<V, T>
    ): V? = values[reference.path] as? V

    operator fun <V : Any, T : SettingType<V>> set(
        reference: SettingReference<V, T>,
        newValue: V
    ) {
        values[reference.path] = newValue
        eventManager.fireListeners(reference)
    }

    @Suppress("UNCHECKED_CAST")
    fun <V : Any, T : SettingType<V>> getPluginSetting(pluginSettingReference: PluginSettingReference<V, T>) =
        pluginSettings[pluginSettingReference.path] as PluginSetting<V, T>

    @Suppress("UNCHECKED_CAST")
    private fun <V : Any, T : SettingType<V>> getPluginSetting(pluginSettingReference: PluginSettingPropertyReference<V, T>) =
        pluginSettings[pluginSettingReference.path] as? PluginSetting<V, T>

    private fun <V : Any, T : SettingType<V>> setPluginSetting(
        pluginSettingReference: PluginSettingPropertyReference<V, T>,
        setting: PluginSetting<V, T>
    ) {
        pluginSettings[pluginSettingReference.path] = setting
    }

    fun <V : Any, T : SettingType<V>> settingDelegate(
        create: (path: String) -> SettingBuilder<V, T>
    ): ReadOnlyProperty<Any, PluginSetting<V, T>> = object : ReadOnlyProperty<Any, PluginSetting<V, T>> {
        override fun getValue(thisRef: Any, property: KProperty<*>): PluginSetting<V, T> {
            @Suppress("UNCHECKED_CAST")
            val reference = property as PluginSettingPropertyReference<V, T>
            getPluginSetting(reference)?.let { return it }
            val setting = PluginSetting(create(reference.path).buildInternal())
            setPluginSetting(reference, setting)
            return setting
        }
    }
}