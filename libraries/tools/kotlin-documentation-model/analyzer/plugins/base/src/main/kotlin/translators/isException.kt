package org.jetbrains.dokka.base.translators

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.AncestryNode
import org.jetbrains.dokka.model.TypeConstructor

internal fun AncestryNode.typeConstructorsBeingExceptions(): List<TypeConstructor> {
    fun traverseSupertypes(ancestry: AncestryNode): List<TypeConstructor> =
        listOf(ancestry.typeConstructor) + (ancestry.superclass?.let(::traverseSupertypes) ?: emptyList())

    return superclass?.let(::traverseSupertypes)?.filter { type -> type.dri.isDirectlyAnException() } ?: emptyList()
}

internal fun DRI.isDirectlyAnException(): Boolean =
    toString().let { stringed ->
        stringed == "kotlin/Exception///PointingToDeclaration/" ||
                stringed == "java.lang/Exception///PointingToDeclaration/"
    }
