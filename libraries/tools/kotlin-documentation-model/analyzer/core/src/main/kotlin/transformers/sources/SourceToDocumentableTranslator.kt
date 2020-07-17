package org.jetbrains.dokka.transformers.sources

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.plugability.DokkaContext

interface SourceToDocumentableTranslator {
    fun invoke(sourceSet: DokkaSourceSet, context: DokkaContext): DModule
}