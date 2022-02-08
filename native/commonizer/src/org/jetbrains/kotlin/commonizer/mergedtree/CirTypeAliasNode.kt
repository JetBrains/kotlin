/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree

import org.jetbrains.kotlin.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.commonizer.cir.CirTypeAlias
import org.jetbrains.kotlin.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.storage.NullableLazyValue

class CirTypeAliasNode(
    val id: CirEntityId,
    override val targetDeclarations: CommonizedGroup<CirTypeAlias>,
    override val commonDeclaration: NullableLazyValue<CirTypeAlias>,
) : CirClassifierNode<CirTypeAlias, CirTypeAlias>, CirNodeWithLiftingUp<CirTypeAlias, CirTypeAlias> {

    override fun <T, R> accept(visitor: CirNodeVisitor<T, R>, data: T): R =
        visitor.visitTypeAliasNode(this, data)

    override fun toString() = CirNode.toString(this)
}
