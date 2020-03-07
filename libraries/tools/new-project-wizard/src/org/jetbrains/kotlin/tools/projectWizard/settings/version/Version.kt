package org.jetbrains.kotlin.tools.projectWizard.settings.version

import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem

data class Version(override val text: String) : DisplayableSettingItem {
    override fun toString(): String = text

    companion object {
        fun fromString(string: String) = Version(string)

        val parser: Parser<Version> = valueParser { value, path ->
            val (stringVersion) = value.parseAs<String>(path)
            safe { fromString(stringVersion) }.mapFailure {
                ParseError("Bad version format for setting `$path`")
            }.get()
        }
    }
}

