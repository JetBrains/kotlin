/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.model

public data class AncestryNode(
    val typeConstructor: TypeConstructor,
    val superclass: AncestryNode?,
    val interfaces: List<AncestryNode>,
) {
    public fun allImplementedInterfaces(): List<TypeConstructor> {
        fun traverseInterfaces(ancestry: AncestryNode): List<TypeConstructor> =
            ancestry.interfaces.flatMap { listOf(it.typeConstructor) + traverseInterfaces(it) } +
                    (ancestry.superclass?.let(::traverseInterfaces) ?: emptyList())
        return traverseInterfaces(this).distinct()
    }
}
