/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.core.entity.settings

import org.jetbrains.kotlin.tools.projectWizard.core.entity.path

class SettingContext(val onUpdated: (SettingReference<*, *>) -> Unit) {
    private val values = mutableMapOf<String, Any>()
    private val pluginSettings = mutableMapOf<String, PluginSetting<*, *>>()

    @Suppress("UNCHECKED_CAST")
    operator fun <V : Any, T : SettingType<V>> get(
        reference: SettingReference<V, T>
    ): V? = values[reference.path] as? V

    operator fun <V : Any, T : SettingType<V>> set(
        reference: SettingReference<V, T>,
        newValue: V
    ) {
        values[reference.path] = newValue
        onUpdated(reference)
    }


    val allPluginSettings: Collection<PluginSetting<*, *>>
        get() = pluginSettings.values

    @Suppress("UNCHECKED_CAST")
    fun <V : Any, T : SettingType<V>> getPluginSetting(pluginSettingReference: PluginSettingReference<V, T>) =
        pluginSettings[pluginSettingReference.path] as PluginSetting<V, T>

    @Suppress("UNCHECKED_CAST")
    fun <V : Any, T : SettingType<V>> getPluginSetting(pluginSettingReference: PluginSettingPropertyReference<V, T>) =
        pluginSettings[pluginSettingReference.path] as? PluginSetting<V, T>

    @Suppress("UNCHECKED_CAST")
    fun <V : Any, T : SettingType<V>> setPluginSetting(
        pluginSettingReference: PluginSettingPropertyReference<V, T>,
        setting: PluginSetting<V, T>
    ) {
        pluginSettings[pluginSettingReference.path] = setting
    }

    @Suppress("UNCHECKED_CAST")
    fun <V : Any, T : SettingType<V>> pluginSettingValue(setting: PluginSetting<V, T>): V? =
        values[setting.path] as? V
}