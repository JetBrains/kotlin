package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.model.doc.Suppress
import org.jetbrains.dokka.plugability.DokkaContext

class SuppressTagDocumentableFilter(val dokkaContext: DokkaContext) :
    SuppressedByConditionDocumentableFilterTransformer(dokkaContext) {
    override fun shouldBeSuppressed(d: Documentable): Boolean =
        d.documentation.any { (_, docs) -> docs.dfs { it is Suppress } != null }
}
