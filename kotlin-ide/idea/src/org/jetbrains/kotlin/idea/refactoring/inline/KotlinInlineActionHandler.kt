package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.lang.Language
import com.intellij.lang.refactoring.InlineActionHandler
import org.jetbrains.kotlin.idea.KotlinLanguage

abstract class KotlinInlineActionHandler : InlineActionHandler() {
    override fun isEnabledForLanguage(language: Language) = language == KotlinLanguage.INSTANCE
}