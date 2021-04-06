package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext

class InheritedEntriesDocumentableFilterTransformer(context: DokkaContext) :
    SuppressedByConditionDocumentableFilterTransformer(context) {
    override fun shouldBeSuppressed(d: Documentable): Boolean =
        context.configuration.suppressInheritedMembers && (d as? WithExtraProperties<Documentable>)?.extra?.get(
            InheritedMember
        )?.inheritedFrom?.any { entry -> entry.value != null } ?: false
}