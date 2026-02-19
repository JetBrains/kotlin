/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree

import org.jetbrains.kotlin.commonizer.cir.CirName
import org.jetbrains.kotlin.commonizer.cir.CirRoot
import org.jetbrains.kotlin.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.commonizer.utils.CommonizerMap
import org.jetbrains.kotlin.storage.NullableLazyValue

class CirRootNode(
    val dependencies: CirProvidedClassifiers,
    override val targetDeclarations: CommonizedGroup<CirRoot>,
    override val commonDeclaration: NullableLazyValue<CirRoot>
) : CirNode<CirRoot, CirRoot> {
    val modules: MutableMap<CirName, CirModuleNode> = CommonizerMap()

    override fun <T, R> accept(visitor: CirNodeVisitor<T, R>, data: T): R =
        visitor.visitRootNode(this, data)

    override fun toString() = CirNode.toString(this)
}
