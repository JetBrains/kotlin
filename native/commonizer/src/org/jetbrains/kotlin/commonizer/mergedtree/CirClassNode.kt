/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree

import org.jetbrains.kotlin.commonizer.cir.CirClass
import org.jetbrains.kotlin.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.commonizer.cir.CirName
import org.jetbrains.kotlin.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.commonizer.utils.CommonizerMap
import org.jetbrains.kotlin.storage.NullableLazyValue

class CirClassNode(
    val id: CirEntityId,
    override val targetDeclarations: CommonizedGroup<CirClass>,
    override val commonDeclaration: NullableLazyValue<CirClass>,
) : CirClassifierNode<CirClass, CirClass>, CirNodeWithMembers<CirClass, CirClass> {

    val constructors: MutableMap<ConstructorApproximationKey, CirClassConstructorNode> = CommonizerMap()
    override val properties: MutableMap<PropertyApproximationKey, CirPropertyNode> = CommonizerMap()
    override val functions: MutableMap<FunctionApproximationKey, CirFunctionNode> = CommonizerMap()
    override val classes: MutableMap<CirName, CirClassNode> = CommonizerMap()

    override fun <T, R> accept(visitor: CirNodeVisitor<T, R>, data: T): R =
        visitor.visitClassNode(this, data)

    override fun toString() = CirNode.toString(this)
}
