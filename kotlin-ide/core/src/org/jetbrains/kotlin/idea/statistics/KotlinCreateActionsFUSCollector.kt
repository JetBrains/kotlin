package org.jetbrains.kotlin.idea.statistics

import com.intellij.internal.statistic.eventLog.EventFields
import com.intellij.internal.statistic.eventLog.EventFields.String
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.validator.ValidationResultType
import com.intellij.internal.statistic.eventLog.validator.rules.EventContext
import com.intellij.internal.statistic.eventLog.validator.rules.impl.CustomValidationRule
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.internal.statistic.utils.getPluginInfoById
import org.jetbrains.kotlin.idea.KotlinPluginUtil

class KotlinCreateActionsFUSCollector : CounterUsagesCollector() {
    override fun getGroup(): EventLogGroup = GROUP

    companion object {
        private val GROUP = EventLogGroup("kotlin.ide.create", 1)
        val newFileEvent = GROUP.registerEvent(
            "NewFile",
            EventFields.Enum("FileTemplate", NewFileTemplates::class.java),
            EventFields.PluginInfo)

        fun logFileTemplate(template: String) {
            newFileEvent.log(NewFileTemplates.getFullName(template),
                             getPluginInfoById(KotlinPluginUtil.KOTLIN_PLUGIN_ID))
        }

        val newProjectEvent = GROUP.registerEvent(
            "NewProject",
            String("Group").withCustomRule("kotlin_wizard_group"),
            String("ProjectTemplate").withCustomRule("kotlin_wizard_template"),
            EventFields.PluginInfo)

        fun logProjectTemplate(group: String, template: String) {
            newProjectEvent.log(group, template, getPluginInfoById(KotlinPluginUtil.KOTLIN_PLUGIN_ID))
        }
    }

    enum class NewFileTemplates(val templateName: String) {
        CLASS("Kotlin Class"),
        FILE("Kotlin File"),
        INTERFACE("Kotlin Interface"),
        DATACLASS("Kotlin Data Class"),
        ENUM("Kotlin Enum"),
        SEALEDCLASS("Kotlin Sealed Class"),
        ANNOTATION("Kotlin Annotation"),
        OBJECT("Kotlin Object"),
        SCRATCH("Kotlin Scratch"),
        SCRIPT("Kotlin Script"),
        WORKSHEET("Kotlin Worksheet"),
        UNKNOWN("unknown");

        companion object {
            fun getFullName(templateName: String): NewFileTemplates {
                for (name in values())
                    if (templateName == name.templateName) return name
                return UNKNOWN
            }
        }
    }
}

class WizardGroupValidationRule : CustomValidationRule() {
    private val groups = listOf("Java", "Kotlin", "Gradle")

    override fun acceptRuleId(ruleId: String?) =
        ruleId == "kotlin_wizard_group"

    override fun doValidate(data: String, context: EventContext): ValidationResultType {
        try {
            return if (groups.contains(data)) {
                ValidationResultType.ACCEPTED
            }
            else {
                ValidationResultType.REJECTED
            }
        }
        catch (e: Exception) {
            return ValidationResultType.REJECTED
        }
    }
}

class WizardTemplateValidationRule : CustomValidationRule() {
    private val groups = listOf("JVM_|_IDEA",
                                            "JS_|_IDEA",
                                            "Native_|_Gradle",
                                            "Multiplatform_Library_|_Gradle",
                                            "JS_Client_and_JVM_Server_|_Gradle",
                                            "Mobile_Android/iOS_|_Gradle",
                                            "Mobile_Shared_Library_|_Gradle",
                                            "Kotlin/JVM",
                                            "Kotlin/JS",
                                            "Kotlin/JS_for_browser",
                                            "Kotlin/JS_for_Node.js",
                                            "Kotlin/Multiplatform_as_framework",
                                            "Kotlin/Multiplatform")

    override fun acceptRuleId(ruleId: String?) =
        ruleId == "kotlin_wizard_template"

    override fun doValidate(data: String, context: EventContext): ValidationResultType {
        try {
            return if (groups.contains(data)) {
                ValidationResultType.ACCEPTED
            }
            else {
                ValidationResultType.REJECTED
            }
        }
        catch (e: Exception) {
            return ValidationResultType.REJECTED
        }
    }
}