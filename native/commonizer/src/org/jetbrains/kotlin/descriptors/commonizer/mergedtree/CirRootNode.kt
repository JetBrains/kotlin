/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import gnu.trove.THashMap
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirRoot
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.NullableLazyValue

class CirRootNode(
    override val targetDeclarations: CommonizedGroup<CirRoot>,
    override val commonDeclaration: NullableLazyValue<CirRoot>
) : CirNode<CirRoot, CirRoot> {
    val modules: MutableMap<Name, CirModuleNode> = THashMap()

    override fun <T, R> accept(visitor: CirNodeVisitor<T, R>, data: T): R =
        visitor.visitRootNode(this, data)

    override fun toString() = CirNode.toString(this)
}
