/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.internal

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.WithSources

@InternalDokkaApi
public enum class DocumentableLanguage {
    JAVA, KOTLIN
}

@InternalDokkaApi
public interface DocumentableSourceLanguageParser {
    public fun getLanguage(documentable: Documentable, sourceSet: DokkaConfiguration.DokkaSourceSet): DocumentableLanguage?
}
