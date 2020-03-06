/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.core.context

import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingType
import org.jetbrains.kotlin.tools.projectWizard.core.service.ServicesManager


open class SettingsWritingContext(context: Context, servicesManager: ServicesManager, isUnitTestMode: Boolean) :
    WritingContext(
        context,
        servicesManager,
        isUnitTestMode
    ) {

    fun <V : Any, T : SettingType<V>> SettingReference<V, T>.setValue(newValue: V) {
        context.settingContext[this] = newValue
    }

    fun <V : Any, T : SettingType<V>> SettingReference<V, T>.setSettingValueToItsDefaultIfItIsNotSetValue() {
        val defaultValue = savedOrDefaultValue ?: return
        if (notRequiredSettingValue == null) {
            setValue(defaultValue)
        }
    }
}