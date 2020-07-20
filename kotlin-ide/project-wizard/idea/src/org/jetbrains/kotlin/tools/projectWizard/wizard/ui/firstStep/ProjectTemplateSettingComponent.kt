package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.firstStep

import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SeparatorWithText
import com.intellij.util.ui.JBUI
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.DropDownSettingType
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.reference
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates.ProjectTemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.projectTemplates.applyProjectTemplate
import org.jetbrains.kotlin.tools.projectWizard.projectTemplates.*
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.*
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.SettingComponent
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting.ValidationIndicator
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JComponent

class ProjectTemplateSettingComponent(
    context: Context
) : SettingComponent<ProjectTemplate, DropDownSettingType<ProjectTemplate>>(
    ProjectTemplatesPlugin::template.reference,
    context
) {
    override val validationIndicator: ValidationIndicator? get() = null
    override val forceLabelCenteringOffset: Int? = 4
    private val templateDescriptionComponent = TemplateDescriptionComponent().asSubComponent()

    private val templateGroups = setting.type.values
        .groupBy { it.projectKind }
        .map { (group, templates) ->
            ListWithSeparators.ListGroup(group.text, templates)
        }

    private val list = ListWithSeparators(
        templateGroups,
        render = { value ->
            icon = value.icon
            append(value.title)
        },
        onValueSelected = { value = it }
    )

    private val borderedPanel = list.addBorder(BorderFactory.createLineBorder(JBColor.border()))

    override val component: JComponent = borderPanel {
        addToCenter(borderPanel { addToCenter(list) }.addBorder(JBUI.Borders.empty(0,/*left*/ 3, 0, /*right*/ 3)))
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
}

private val ProjectTemplate.icon: Icon
    get() = when (this) {
        BackendApplicationProjectTemplate -> KotlinIcons.Wizard.JVM
        MultiplatformApplicationProjectTemplate -> KotlinIcons.Wizard.MULTIPLATFORM
        ConsoleApplicationProjectTemplate -> KotlinIcons.Wizard.CONSOLE
        MultiplatformLibraryProjectTemplate -> KotlinIcons.Wizard.MULTIPLATFORM_LIBRARY
        FullStackWebApplicationProjectTemplate -> KotlinIcons.Wizard.WEB
        NativeApplicationProjectTemplate -> KotlinIcons.Wizard.NATIVE
        FrontendApplicationProjectTemplate -> KotlinIcons.Wizard.JS
        MultiplatformMobileApplicationProjectTemplate -> KotlinIcons.Wizard.MULTIPLATFORM_MOBILE
        MultiplatformMobileLibraryProjectTemplate -> KotlinIcons.Wizard.MULTIPLATFORM_MOBILE_LIBRARY
        NodeJsApplicationProjectTemplate -> KotlinIcons.Wizard.NODE_JS
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