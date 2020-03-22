package org.jetbrains.kotlin.tools.projectWizard.settings

interface DisplayableSettingItem {
    val text: String
    val greyText: String?
        get() = null
}
