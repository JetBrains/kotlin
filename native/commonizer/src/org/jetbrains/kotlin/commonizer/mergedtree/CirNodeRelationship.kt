/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.commonizer.mergedtree

internal sealed class CirNodeRelationship {

    class PreferredNode(val node: CirNode<*, *>) : CirNodeRelationship()

    class ParentNode(val node: CirNode<*, *>) : CirNodeRelationship()

    class Composite private constructor(val relationships: List<CirNodeRelationship>) : CirNodeRelationship() {
        companion object {
            operator fun CirNodeRelationship.plus(other: CirNodeRelationship): Composite {
                if (this is Composite) {
                    return if (other is Composite) {
                        Composite(this.relationships + other.relationships)
                    } else Composite(this.relationships + other)
                } else if (other is Composite) {
                    return Composite(listOf(this) + other.relationships)
                }
                return Composite(listOf(this, other))
            }
        }
    }

    companion object {
        fun ParentNode(node: CirNode<*, *>?): ParentNode? = if (node != null) CirNodeRelationship.ParentNode(node) else null
    }
}

