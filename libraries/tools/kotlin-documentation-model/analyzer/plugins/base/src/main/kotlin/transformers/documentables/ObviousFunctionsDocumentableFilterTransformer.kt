package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.ObviousMember
import org.jetbrains.dokka.plugability.DokkaContext

class ObviousFunctionsDocumentableFilterTransformer(context: DokkaContext) : SuppressedByConditionDocumentableFilterTransformer(context) {
    override fun shouldBeSuppressed(d: Documentable): Boolean =
        context.configuration.suppressObviousFunctions && d is DFunction && d.extra[ObviousMember] != null
}