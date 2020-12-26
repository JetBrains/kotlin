package org.jetbrains.kotlin.tools.projectWizard.wizard

import org.jetbrains.kotlin.idea.projectWizard.WizardStatsService
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.SettingReference
import org.jetbrains.kotlin.tools.projectWizard.projectTemplates.ProjectTemplate
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import org.jetbrains.kotlin.tools.projectWizard.templates.Template
import java.nio.file.Path

object OnUserSettingChangeStatisticsLogger {
    fun <V: Any> logSettingValueChangedByUser(reference: SettingReference<V, *>, value: V) {
        logSettingValueChangedByUser(reference.path, value)
    }

    fun <V: Any> logSettingValueChangedByUser(settingId: String, value: V) {
        val id = settingId.idForFus()
        val stringValue = value.getAsSettingValueIfAcceptable() ?: return
        WizardStatsService.logDataOnSettingValueChanged(id, stringValue)
    }

    private fun String.idForFus() =
        substringAfterLast("/")

    private fun Any.getAsSettingValueIfAcceptable() = when (this) {
        is Boolean -> toString()
        is Enum<*> -> toString()
        is ProjectTemplate -> id
        is Template -> id
        is String -> null
        is Path -> null
        is Version -> null
        is List<*> -> null
        else -> error("Unknown setting value ${this::class.simpleName}")
    }
}