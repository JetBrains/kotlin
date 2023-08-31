/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.internal

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Documentable

@InternalDokkaApi
public interface InheritanceBuilder {
    public fun build(documentables: Map<DRI, Documentable>): List<InheritanceNode>
}

@InternalDokkaApi
public data class InheritanceNode(
    val dri: DRI,
    val children: List<InheritanceNode> = emptyList(),
    val interfaces: List<DRI> = emptyList(),
    val isInterface: Boolean = false
) {
    override fun equals(other: Any?): Boolean = other is InheritanceNode && other.dri == dri
    override fun hashCode(): Int = dri.hashCode()
}
