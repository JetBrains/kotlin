/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirFunction
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.storage.NullableLazyValue

class CirFunctionNode(
    override val targetDeclarations: CommonizedGroup<CirFunction>,
    override val commonDeclaration: NullableLazyValue<CirFunction>
) : CirNode<CirFunction, CirFunction> {
    override fun <R, T> accept(visitor: CirNodeVisitor<R, T>, data: T) =
        visitor.visitFunctionNode(this, data)

    override fun toString() = CirNode.toString(this)
}
