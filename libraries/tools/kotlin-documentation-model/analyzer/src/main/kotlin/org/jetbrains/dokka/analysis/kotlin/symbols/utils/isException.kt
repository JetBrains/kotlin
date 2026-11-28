/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.utils

import org.jetbrains.dokka.model.AncestryNode
import org.jetbrains.dokka.model.TypeConstructor
import org.jetbrains.kotlin.documentation.KdElementId

internal fun AncestryNode.typeConstructorsBeingExceptions(): List<TypeConstructor> {
    fun traverseSupertypes(ancestry: AncestryNode): List<TypeConstructor> =
        listOf(ancestry.typeConstructor) + (ancestry.superclass?.let(::traverseSupertypes) ?: emptyList())

    return traverseSupertypes(this).filter { type -> type.declarationId.isDirectlyAnExceptionDeclarationId() }
}

internal fun KdElementId.isDirectlyAnExceptionDeclarationId(): Boolean =
    this == KdElementId("kotlin/Exception") || this == KdElementId("java.lang/Exception")
