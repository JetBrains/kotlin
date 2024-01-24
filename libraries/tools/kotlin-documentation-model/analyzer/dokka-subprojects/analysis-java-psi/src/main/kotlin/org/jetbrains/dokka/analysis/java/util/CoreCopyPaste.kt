/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.util

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.AncestryNode
import org.jetbrains.dokka.model.TypeConstructor

// TODO [beresnev] copy-pasted
internal fun AncestryNode.typeConstructorsBeingExceptions(): List<TypeConstructor> {
    fun traverseSupertypes(ancestry: AncestryNode): List<TypeConstructor> =
        listOf(ancestry.typeConstructor) + (ancestry.superclass?.let(::traverseSupertypes) ?: emptyList())

    return superclass?.let(::traverseSupertypes)?.filter { type -> type.dri.isDirectlyAnException() } ?: emptyList()
}

// TODO [beresnev] copy-pasted
internal fun DRI.isDirectlyAnException(): Boolean =
    toString().let { stringed ->
        stringed == "kotlin/Exception///PointingToDeclaration/" ||
                stringed == "java.lang/Exception///PointingToDeclaration/"
    }
