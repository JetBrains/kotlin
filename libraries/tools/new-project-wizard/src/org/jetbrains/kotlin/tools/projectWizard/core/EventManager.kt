package org.jetbrains.kotlin.tools.projectWizard.core

import org.jetbrains.kotlin.tools.projectWizard.core.entity.SettingReference

class EventManager {
    private val listeners = mutableListOf<(SettingReference<*, *>?) -> Unit>()

    fun addSettingUpdaterEventListener(listener: (SettingReference<*, *>?) -> Unit) {
        listeners += listener
    }

    fun fireListeners(reference: SettingReference<*, *>?) {
        listeners.forEach { it(reference) }
    }
}