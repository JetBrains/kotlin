package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.components

import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingValidator
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.componentWithCommentAtRight
import javax.swing.JComponent

class CheckboxComponent(
    context: Context,
    labelText: String? = null,
    description: String? = null,
    initialValue: Boolean? = null,
    validator: SettingValidator<Boolean>? = null,
    onValueUpdate: (Boolean) -> Unit = {}
) : UIComponent<Boolean>(
    context,
    labelText = null,
    validator = validator,
    onValueUpdate = onValueUpdate
) {
    private val checkbox = JBCheckBox(labelText, initialValue ?: false).apply {
        font = UIUtil.getButtonFont()
        addChangeListener {
            fireValueUpdated(this@apply.isSelected)
        }
    }

    override val alignTarget: JComponent? get() = checkbox

    override val uiComponent = componentWithCommentAtRight(checkbox, description)

    override fun updateUiValue(newValue: Boolean) = safeUpdateUi {
        checkbox.isSelected = newValue
    }

    override fun getUiValue(): Boolean = checkbox.isSelected
}