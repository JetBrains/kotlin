/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree

import org.jetbrains.kotlin.commonizer.cir.CirModule
import org.jetbrains.kotlin.commonizer.cir.CirPackageName
import org.jetbrains.kotlin.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.commonizer.utils.CommonizerMap
import org.jetbrains.kotlin.storage.NullableLazyValue

class CirModuleNode(
    override val targetDeclarations: CommonizedGroup<CirModule>,
    override val commonDeclaration: NullableLazyValue<CirModule>
) : CirNode<CirModule, CirModule> {
    val packages: MutableMap<CirPackageName, CirPackageNode> = CommonizerMap()

    override fun <T, R> accept(visitor: CirNodeVisitor<T, R>, data: T) =
        visitor.visitModuleNode(this, data)

    override fun toString() = CirNode.toString(this)
}
