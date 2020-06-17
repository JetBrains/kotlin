/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirDeclaration
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.storage.NullableLazyValue

interface CirNode<T : CirDeclaration, R : CirDeclaration> {
    val targetDeclarations: CommonizedGroup<T>
    val commonDeclaration: NullableLazyValue<R>

    fun <R, T> accept(visitor: CirNodeVisitor<R, T>, data: T): R

    companion object {
        inline val CirNode<*, *>.indexOfCommon: Int
            get() = targetDeclarations.size

        internal inline val CirNode<*, *>.dimension: Int
            get() = targetDeclarations.size + 1

        fun toString(node: CirNode<*, *>) = buildString {
            if (node is CirNodeWithFqName) {
                append("fqName=").append(node.fqName).append(", ")
            }
            append("target=")
            node.targetDeclarations.joinTo(this)
            append(", common=")
            append(if (node.commonDeclaration.isComputed()) node.commonDeclaration() else "<NOT COMPUTED>")
        }
    }
}

