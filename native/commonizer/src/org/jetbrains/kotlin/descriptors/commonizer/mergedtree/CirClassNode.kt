/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import gnu.trove.THashMap
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClass
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.NullableLazyValue

class CirClassNode(
    override val targetDeclarations: CommonizedGroup<CirClass>,
    override val commonDeclaration: NullableLazyValue<CirClass>,
    override val classId: ClassId
) : CirNodeWithClassId<CirClass, CirClass> {

    val constructors: MutableMap<ConstructorApproximationKey, CirClassConstructorNode> = THashMap()
    val properties: MutableMap<PropertyApproximationKey, CirPropertyNode> = THashMap()
    val functions: MutableMap<FunctionApproximationKey, CirFunctionNode> = THashMap()
    val classes: MutableMap<Name, CirClassNode> = THashMap()

    override fun <T, R> accept(visitor: CirNodeVisitor<T, R>, data: T): R =
        visitor.visitClassNode(this, data)

    override fun toString() = CirNode.toString(this)
}
