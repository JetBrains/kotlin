/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.core.entity.settings


import org.jetbrains.kotlin.tools.projectWizard.core.Reader
import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingValidator
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settingValidator
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase

abstract class SettingBuilder<V : Any, T : SettingType<V>>(
    private val path: String,
    private val title: String,
    private val neededAtPhase: GenerationPhase
) {
    var isAvailable: Reader.() -> Boolean = { true }
    open var defaultValue: SettingDefaultValue<V>? = null

    var validateOnProjectCreation = true
    var isSavable: Boolean = false
    var isRequired: Boolean? = null
    var description: String? = null

    fun value(value: V) = SettingDefaultValue.Value(value)
    fun dynamic(getter: Reader.(SettingReference<V, SettingType<V>>) -> V?) =
        SettingDefaultValue.Dynamic(getter)

    protected var validator =
        SettingValidator<V> { ValidationResult.OK }

    fun validate(validator: SettingValidator<V>) {
        this.validator = this.validator and validator
    }

    fun validate(validator: Reader.(V) -> ValidationResult) {
        this.validator = this.validator and settingValidator(
            validator
        )
    }


    abstract val type: T

    fun buildInternal() = InternalSetting(
        path = path,
        title = title,
        description = description,
        defaultValue = defaultValue,
        isAvailable = isAvailable,
        isRequired = isRequired ?: (defaultValue == null),
        isSavable = isSavable,
        neededAtPhase = neededAtPhase,
        validator = validator,
        validateOnProjectCreation = validateOnProjectCreation,
        type = type
    )
}