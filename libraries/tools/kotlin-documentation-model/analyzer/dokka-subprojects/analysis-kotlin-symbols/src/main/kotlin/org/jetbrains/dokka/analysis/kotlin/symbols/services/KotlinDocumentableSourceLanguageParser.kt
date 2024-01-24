/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.services

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.java.util.PsiDocumentableSource
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.WithSources
import org.jetbrains.dokka.analysis.kotlin.internal.DocumentableLanguage
import org.jetbrains.dokka.analysis.kotlin.internal.DocumentableSourceLanguageParser

internal class KotlinDocumentableSourceLanguageParser : DocumentableSourceLanguageParser {

    /**
     * For members inherited from Java in Kotlin - it returns [DocumentableLanguage.KOTLIN]
     */
    override fun getLanguage(
        documentable: Documentable,
        sourceSet: DokkaConfiguration.DokkaSourceSet,
    ): DocumentableLanguage? {
        val documentableSource = (documentable as? WithSources)?.sources?.get(sourceSet) ?: return null
        return when (documentableSource) {
            is PsiDocumentableSource -> DocumentableLanguage.JAVA
            is KtPsiDocumentableSource -> DocumentableLanguage.KOTLIN
            else -> error("Unknown language sources: ${documentableSource::class}")
        }
    }
}
