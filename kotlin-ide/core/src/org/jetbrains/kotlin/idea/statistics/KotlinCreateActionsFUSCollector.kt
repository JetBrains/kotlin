package org.jetbrains.kotlin.idea.statistics

import com.intellij.internal.statistic.eventLog.EventFields
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.internal.statistic.utils.getPluginInfoById
import org.jetbrains.kotlin.idea.KotlinPluginUtil

class KotlinCreateActionsFUSCollector : CounterUsagesCollector() {
    override fun getGroup(): EventLogGroup = GROUP

    companion object {
        private val GROUP = EventLogGroup("kotlin.ide.new.file", 1)

        val newFileEvent = GROUP.registerEvent(
            "NewFile",
            EventFields.Enum("FileTemplate", NewFileTemplates::class.java),
            EventFields.PluginInfo)

        fun logFileTemplate(template: String) {
            newFileEvent.log(NewFileTemplates.getFullName(template),
                             getPluginInfoById(KotlinPluginUtil.KOTLIN_PLUGIN_ID))
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
                return values().firstOrNull { it.templateName == templateName } ?: UNKNOWN
            }
        }
    }
}
