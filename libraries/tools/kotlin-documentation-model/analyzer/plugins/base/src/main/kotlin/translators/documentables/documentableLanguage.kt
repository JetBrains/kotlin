package org.jetbrains.dokka.base.translators.documentables

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.PsiDocumentableSource
import org.jetbrains.dokka.model.WithSources

internal enum class DocumentableLanguage {
    JAVA, KOTLIN
}

internal fun WithSources.documentableLanguage(sourceSet: DokkaConfiguration.DokkaSourceSet): DocumentableLanguage =
    when (sources[sourceSet]) {
        is PsiDocumentableSource -> DocumentableLanguage.JAVA
        else -> DocumentableLanguage.KOTLIN
    }