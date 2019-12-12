package org.jetbrains.kotlin.tools.projectWizard.settings

interface DisplayableSettingItem {
    val text: String
    val greyText: String?
        get() = null
}

val DisplayableSettingItem.fullText
    get() = text + greyText?.let { "($it)" }.orEmpty()
