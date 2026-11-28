/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.internal

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.kotlin.documentation.KdElementId

@InternalDokkaApi
internal interface InheritanceBuilder {
    fun build(documentables: Map<KdElementId, Documentable>): List<InheritanceNode>
}

@InternalDokkaApi
internal data class InheritanceNode(
    val declarationId: KdElementId,
    val children: List<InheritanceNode> = emptyList(),
    val interfaces: List<KdElementId> = emptyList(),
    val isInterface: Boolean = false
) {
    override fun equals(other: Any?): Boolean = other is InheritanceNode && other.declarationId == declarationId
    override fun hashCode(): Int = declarationId.hashCode()
}
