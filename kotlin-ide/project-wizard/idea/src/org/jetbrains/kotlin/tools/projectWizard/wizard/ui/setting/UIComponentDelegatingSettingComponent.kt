/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingType
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.TitleComponentAlignment
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components.UIComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components.valueForSetting
import javax.swing.JComponent

abstract class UIComponentDelegatingSettingComponent<V : Any, T : SettingType<V>>(
    reference: SettingReference<V, T>,
    context: Context
) : SettingComponent<V, T>(reference, context) {
    abstract val uiComponent: UIComponent<V>

    override val alignment: TitleComponentAlignment
        get() = uiComponent.alignTarget?.let { TitleComponentAlignment.AlignAgainstSpecificComponent(it) }
            ?: TitleComponentAlignment.AlignAgainstMainComponent

    // As there is one in UIComponent
    override val validationIndicator: ValidationIndicator? = null

    override fun onInit() {
        super.onInit()
        if (value == null) {
            read { valueForSetting(uiComponent, reference) }?.let { value = it }
        }
        value?.let(uiComponent::updateUiValue)
    }

    override fun focusOn() {
        uiComponent.focusOn()
    }

    override fun onValueUpdated(reference: SettingReference<*, *>?) {
        super.onValueUpdated(reference)
        if (reference == this.reference) {
            if (uiComponent.getUiValue() != value) {
                value?.let(uiComponent::updateUiValue)
            }
        }
    }

    override val component: JComponent by lazy(LazyThreadSafetyMode.NONE) { uiComponent.component }
}