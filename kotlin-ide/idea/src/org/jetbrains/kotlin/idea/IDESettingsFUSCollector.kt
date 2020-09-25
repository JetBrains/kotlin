/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.internal.statistic.utils.getPluginInfoById
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.codeInsight.KotlinCodeInsightSettings
import org.jetbrains.kotlin.idea.codeInsight.KotlinCodeInsightWorkspaceSettings
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionsManager
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings

class IDESettingsFUSCollector : ProjectUsagesCollector() {
    override fun getGroup() = GROUP

    override fun getMetrics(project: Project): Set<MetricEvent> {
        if (PlatformVersion.isAndroidStudio()) {
            return emptySet()
        }

        val metrics = mutableSetOf<MetricEvent>()
        val pluginInfo = getPluginInfoById(KotlinPluginUtil.KOTLIN_PLUGIN_ID)

        // filling up scriptingAutoReloadEnabled Event
        for (definition in ScriptDefinitionsManager.getInstance(project).getAllDefinitions()) {
            if (definition.canAutoReloadScriptConfigurationsBeSwitchedOff) {
                val scriptingAutoReloadEnabled = KotlinScriptingSettings.getInstance(project).autoReloadConfigurations(definition)
                metrics.add(scriptingAREvent.metric(definition.name, scriptingAutoReloadEnabled, pluginInfo))
            }
        }

        val settings: KotlinCodeInsightSettings = KotlinCodeInsightSettings.getInstance()
        val projectSettings: KotlinCodeInsightWorkspaceSettings = KotlinCodeInsightWorkspaceSettings.getInstance(project)

        // filling up addUnambiguousImportsOnTheFly and optimizeImportsOnTheFly Events
        metrics.add(unambiguousImportsEvent.metric(settings.addUnambiguousImportsOnTheFly, pluginInfo))
        metrics.add(optimizeImportsEvent.metric(projectSettings.optimizeImportsOnTheFly, pluginInfo))

        return metrics
    }

    companion object {
        private val GROUP = EventLogGroup("kotlin.ide.settings", 3)

        // scriptingAutoReloadEnabled Event
        private val scriptingAREnabledField = EventFields.Boolean("enabled")
        private val scriptingDefNameField = EventFields.String(
            "definition_name", listOf(
                "KotlinInitScript",
                "KotlinSettingsScript",
                "KotlinBuildScript",
                "Script_definition_for_extension_scripts_and_IDE_console",
                "MainKtsScript",
                "Kotlin_Script"
            )
        )
        private val scriptingPluginInfoField = EventFields.PluginInfo

        private val scriptingAREvent = GROUP.registerEvent(
            "scriptingAutoReloadEnabled",
            scriptingDefNameField,
            scriptingAREnabledField,
            scriptingPluginInfoField
        )

        // addUnambiguousImportsOnTheFly Event
        private val unambiguousImportsEvent =
            GROUP.registerEvent("addUnambiguousImportsOnTheFly", EventFields.Boolean("enabled"), EventFields.PluginInfo)

        // optimizeImportsOnTheFly Event
        private val optimizeImportsEvent =
            GROUP.registerEvent("optimizeImportsOnTheFly", EventFields.Boolean("enabled"), EventFields.PluginInfo)
    }
}
