package org.jetbrains.kotlin.idea.statistics

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.internal.statistic.utils.getPluginInfoById
import org.jetbrains.kotlin.idea.KotlinPluginUtil

class KotlinMoveRefactoringFUSCollector : CounterUsagesCollector() {
    override fun getGroup(): EventLogGroup = GROUP

    companion object {
        private val GROUP = EventLogGroup("kotlin.ide.refactoring.move", 1)

        private val lagging = EventFields.Long("lagging")
        private val entity = EventFields.Enum("entity", MovedEntity::class.java)
        private val destination = EventFields.Enum("destination", MoveRefactoringDestination::class.java)
        private val numberOfEntities = EventFields.Int("number_of_entities")
        private val areSettingsChanged = EventFields.Boolean("are_settings_changed")
        private val succeeded = EventFields.Boolean("succeeded")
        private val pluginInfo = EventFields.PluginInfo

        private val event = GROUP.registerVarargEvent(
            "Finished",
            lagging,
            entity,
            destination,
            numberOfEntities,
            areSettingsChanged,
            succeeded,
            pluginInfo,
        )

        /**
         * @param isDefault is something changed in Move Refactoring Dialog check-boxes state
         */
        fun log(
            timeStarted: Long,
            timeFinished: Long,
            numberOfEntities: Int,
            entity: MovedEntity,
            destination: MoveRefactoringDestination,
            isDefault: Boolean,
            isSucceeded: Boolean
        ) = event.log(
            this.lagging.with(timeFinished - timeStarted),
            this.entity.with(entity),
            this.destination.with(destination),
            this.numberOfEntities.with(numberOfEntities),
            this.areSettingsChanged.with(isDefault),
            this.succeeded.with(isSucceeded),
            this.pluginInfo.with(getPluginInfoById(KotlinPluginUtil.KOTLIN_PLUGIN_ID)),
        )
    }

    enum class MoveRefactoringDestination {
        PACKAGE, FILE, DECLARATION
    }

    enum class MovedEntity {
        FUNCTIONS, CLASSES, MIXED, MPPCLASSES, MPPFUNCTIONS, MPPMIXED, PACKAGE, FILES
    }


}