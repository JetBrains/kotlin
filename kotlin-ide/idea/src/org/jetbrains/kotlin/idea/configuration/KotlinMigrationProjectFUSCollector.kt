package org.jetbrains.kotlin.idea.configuration

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.internal.statistic.utils.getPluginInfoById
import org.jetbrains.kotlin.idea.KotlinPluginUtil

class KotlinMigrationProjectFUSCollector : CounterUsagesCollector() {
    override fun getGroup(): EventLogGroup = GROUP

    companion object {
        private val GROUP = EventLogGroup("kotlin.ide.migrationTool", 2)

        private val oldLanguageVersion = EventFields.StringValidatedByRegexp("old_language_version", "version_lang_api")
        private val oldApiVersion = EventFields.StringValidatedByRegexp("old_api_version", "version_lang_api")
        private val oldStdlibVersion = EventFields.StringValidatedByRegexp("old_stdlib_version", "version_stdlib")
        private val pluginInfo = EventFields.PluginInfo

        private val notificationEvent = GROUP.registerVarargEvent(
            "Notification",
            oldLanguageVersion,
            oldApiVersion,
            oldStdlibVersion,
            pluginInfo,
        )

        private val runEvent = GROUP.registerEvent(
            "Run"
        )

        fun logNotification(migrationInfo: MigrationInfo) {
            notificationEvent.log(
                this.oldLanguageVersion.with(migrationInfo.oldLanguageVersion.versionString),
                this.oldApiVersion.with(migrationInfo.oldApiVersion.versionString),
                this.oldStdlibVersion.with(migrationInfo.oldStdlibVersion),
                this.pluginInfo.with(getPluginInfoById(KotlinPluginUtil.KOTLIN_PLUGIN_ID))
            )
        }

        fun logRun() {
            runEvent.log()
        }
    }
}