/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.util

import org.jetbrains.dokka.model.AncestryNode
import org.jetbrains.dokka.model.TypeConstructor
import org.jetbrains.kotlin.documentation.KdElementId

// TODO [beresnev] copy-pasted
internal fun AncestryNode.typeConstructorsBeingExceptions(): List<TypeConstructor> {
    fun traverseSupertypes(ancestry: AncestryNode): List<TypeConstructor> =
        listOf(ancestry.typeConstructor) + (ancestry.superclass?.let(::traverseSupertypes) ?: emptyList())

    return superclass?.let(::traverseSupertypes)?.filter { type -> type.declarationId.isDirectlyAnExceptionDeclarationId() } ?: emptyList()
}

// TODO [beresnev] copy-pasted
internal fun KdElementId.isDirectlyAnExceptionDeclarationId(): Boolean =
    this == KdElementId("kotlin/Exception") || this == KdElementId("java.lang/Exception")
