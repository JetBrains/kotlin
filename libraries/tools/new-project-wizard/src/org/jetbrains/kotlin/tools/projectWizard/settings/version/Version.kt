package org.jetbrains.kotlin.tools.projectWizard.settings.version

import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem

data class Version(@NonNls override val text: String) : DisplayableSettingItem {
    override fun toString(): String = text

    companion object {
        fun fromString(@NonNls string: String) = Version(string)

        val parser: Parser<Version> = valueParser { value, path ->
            val (stringVersion) = value.parseAs<String>(path)
            safe { fromString(stringVersion) }.mapFailure {
                ParseError(KotlinNewProjectWizardBundle.message("version.error.bad.format", path))
            }.get()
        }
    }
}

