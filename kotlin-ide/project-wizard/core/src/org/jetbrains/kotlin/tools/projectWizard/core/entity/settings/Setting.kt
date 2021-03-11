/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.core.entity.settings

import org.jetbrains.kotlin.tools.projectWizard.core.ActivityCheckerOwner
import org.jetbrains.kotlin.tools.projectWizard.core.Checker
import org.jetbrains.kotlin.tools.projectWizard.core.Reader

import org.jetbrains.kotlin.tools.projectWizard.core.entity.Entity
import org.jetbrains.kotlin.tools.projectWizard.core.entity.EntityWithValue
import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingValidator
import org.jetbrains.kotlin.tools.projectWizard.core.entity.Validatable
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase


typealias AnySetting = Setting<*, *>

interface Setting<out V : Any, out T : SettingType<V>> : Entity, ActivityCheckerOwner, Validatable<V> {
    val title: String
    val description: String?
    val defaultValue: SettingDefaultValue<V>?
    val isRequired: Boolean
    val isSavable: Boolean
    val neededAtPhase: GenerationPhase
    val validateOnProjectCreation: Boolean
    val type: T
}

data class InternalSetting<out V : Any, out T : SettingType<V>>(
    override val path: String,
    override val title: String,
    override val description: String?,
    override val defaultValue: SettingDefaultValue<V>?,
    override val isAvailable: Checker,
    override val isRequired: Boolean,
    override val isSavable: Boolean,
    override val neededAtPhase: GenerationPhase,
    override val validator: SettingValidator<@UnsafeVariance V>,
    override val validateOnProjectCreation: Boolean,
    override val type: T
) : Setting<V, T>, EntityWithValue<V>()

sealed class SettingImpl<out V : Any, out T : SettingType<V>> :
    Setting<V, T>

class PluginSetting<out V : Any, out T : SettingType<V>>(
    internal: InternalSetting<V, T>
) : SettingImpl<V, T>(), Setting<V, T> by internal

class ModuleConfiguratorSetting<out V : Any, out T : SettingType<V>>(
    internal: InternalSetting<V, T>
) : SettingImpl<V, T>(), Setting<V, T> by internal

class TemplateSetting<out V : Any, out T : SettingType<V>>(
    internal: InternalSetting<V, T>
) : SettingImpl<V, T>(), Setting<V, T> by internal


sealed class SettingDefaultValue<out V : Any> {
    data class Value<V : Any>(val value: V) : SettingDefaultValue<V>()
    data class Dynamic<V : Any>(
        val getter: Reader.(SettingReference<V, SettingType<V>>) -> V?
    ) : SettingDefaultValue<V>()
}


sealed class SettingSerializer<out V : Any> {
    object None : SettingSerializer<Nothing>()
    data class Serializer<V : Any>(
        val fromString: (String) -> V?,
        val toString: (V) -> String = Any::toString
    ) : SettingSerializer<V>()
}


