package org.jetbrains.kotlin.analysis.kotlin.internal

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.WithSources

@InternalDokkaApi
enum class DocumentableLanguage {
    JAVA, KOTLIN
}

@InternalDokkaApi
interface DocumentableSourceLanguageParser {
    fun getLanguage(documentable: Documentable, sourceSet: DokkaConfiguration.DokkaSourceSet): DocumentableLanguage?
}
