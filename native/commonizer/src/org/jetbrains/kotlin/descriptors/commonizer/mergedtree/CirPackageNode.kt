/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import gnu.trove.THashMap
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirPackage
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.NullableLazyValue

class CirPackageNode(
    override val targetDeclarations: CommonizedGroup<CirPackage>,
    override val commonDeclaration: NullableLazyValue<CirPackage>,
    override val fqName: FqName,
    val moduleName: Name
) : CirNodeWithFqName<CirPackage, CirPackage> {

    val properties: MutableMap<PropertyApproximationKey, CirPropertyNode> = THashMap()
    val functions: MutableMap<FunctionApproximationKey, CirFunctionNode> = THashMap()
    val classes: MutableMap<Name, CirClassNode> = THashMap()
    val typeAliases: MutableMap<Name, CirTypeAliasNode> = THashMap()

    override fun <R, T> accept(visitor: CirNodeVisitor<R, T>, data: T) =
        visitor.visitPackageNode(this, data)

    override fun toString() = CirNode.toString(this)
}
