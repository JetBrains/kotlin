package org.jetbrains.dokka.transformers.documentation

import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.plugability.DokkaContext

interface PreMergeDocumentableTransformer {
    operator fun invoke(modules: List<DModule>): List<DModule>
}