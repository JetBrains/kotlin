/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirProperty
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.storage.NullableLazyValue

class CirPropertyNode(
    override val targetDeclarations: CommonizedGroup<CirProperty>,
    override val commonDeclaration: NullableLazyValue<CirProperty>
) : CirNodeWithLiftingUp<CirProperty, CirProperty> {
    override fun <T, R> accept(visitor: CirNodeVisitor<T, R>, data: T) =
        visitor.visitPropertyNode(this, data)

    override fun toString() = CirNode.toString(this)
}
