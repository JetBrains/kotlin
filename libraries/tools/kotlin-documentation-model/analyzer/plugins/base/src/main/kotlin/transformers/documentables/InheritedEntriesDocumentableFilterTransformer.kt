package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext

class InheritedEntriesDocumentableFilterTransformer(context: DokkaContext) :
    SuppressedByConditionDocumentableFilterTransformer(context) {

    override fun shouldBeSuppressed(d: Documentable): Boolean {
        @Suppress("UNCHECKED_CAST")
        val inheritedMember = (d as? WithExtraProperties<Documentable>)?.extra?.get(InheritedMember)
        val containsInheritedFrom = inheritedMember?.inheritedFrom?.any { entry -> entry.value != null } ?: false

        return context.configuration.suppressInheritedMembers && containsInheritedFrom
    }
}