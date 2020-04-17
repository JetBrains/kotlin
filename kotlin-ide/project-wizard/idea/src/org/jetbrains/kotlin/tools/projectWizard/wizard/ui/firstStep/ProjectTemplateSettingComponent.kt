package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.firstStep

import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.DropDownSettingType
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates.ProjectTemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates.applyProjectTemplate
import org.jetbrains.kotlin.tools.projectWizard.projectTemplates.ProjectTemplate
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.*
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.SettingComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.ValidationIndicator
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.SwingConstants

class ProjectTemplateSettingComponent(
    context: Context
) : SettingComponent<ProjectTemplate, DropDownSettingType<ProjectTemplate>>(
    ProjectTemplatesPlugin::template.reference,
    context
) {
    override val validationIndicator: ValidationIndicator? get() = null
    override val forceLabelCenteringOffset: Int? = 4
    private val templateDescriptionComponent = TemplateDescriptionComponent().asSubComponent()

    private val list = ImmutableSingleSelectableListWithIcon(
        setting.type.values,
        renderValue = { value ->
            icon = value.projectKind.icon
            append(value.title)
        },
        onValueSelected = { value = it }
    )

    private val scrollPane = ScrollPaneFactory.createScrollPane(list).apply {
        preferredSize = Dimension(minimumSize.width, HEIGHT)
    }

    override val component: JComponent = borderPanel {
        addToCenter(borderPanel { addToCenter(scrollPane) }.addBorder(JBUI.Borders.empty(0,/*left*/ 3, 0, /*right*/ 3)))
        addToBottom(templateDescriptionComponent.component.addBorder(JBUI.Borders.empty(/*top*/8,/*left*/ 3, 0, 0)))
    }

    private fun applySelectedTemplate() = modify {
        value?.let(::applyProjectTemplate)
    }

    override fun onValueUpdated(reference: SettingReference<*, *>?) {
        super.onValueUpdated(reference)
        if (reference == ProjectTemplatesPlugin::template.reference) {
            applySelectedTemplate()
            value?.let { template ->
                list.setSelectedValue(template, true)
                templateDescriptionComponent.setTemplate(template)
            }
        }
    }

    override fun onInit() {
        super.onInit()
        if (setting.type.values.isNotEmpty()) {
            list.selectedIndex = 0
            value = setting.type.values.firstOrNull()
        }
    }

    companion object {
        private const val HEIGHT = 230
    }
}

class TemplateDescriptionComponent : Component() {
    private val descriptionLabel = CommentLabel().apply {
        preferredSize = Dimension(preferredSize.width, 45)
    }

    fun setTemplate(template: ProjectTemplate) {
        descriptionLabel.text = template.description.asHtml()
    }

    override val component: JComponent = descriptionLabel
}