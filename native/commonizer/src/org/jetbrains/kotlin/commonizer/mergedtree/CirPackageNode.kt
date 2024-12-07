/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.mergedtree

import org.jetbrains.kotlin.commonizer.cir.CirName
import org.jetbrains.kotlin.commonizer.cir.CirPackage
import org.jetbrains.kotlin.commonizer.cir.CirPackageName
import org.jetbrains.kotlin.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.commonizer.utils.CommonizerMap
import org.jetbrains.kotlin.commonizer.utils.firstNonNull
import org.jetbrains.kotlin.storage.NullableLazyValue

class CirPackageNode(
    override val targetDeclarations: CommonizedGroup<CirPackage>,
    override val commonDeclaration: NullableLazyValue<CirPackage>
) : CirNodeWithMembers<CirPackage, CirPackage> {

    override val properties: MutableMap<PropertyApproximationKey, CirPropertyNode> = CommonizerMap()
    override val functions: MutableMap<FunctionApproximationKey, CirFunctionNode> = CommonizerMap()
    override val classes: MutableMap<CirName, CirClassNode> = CommonizerMap()
    val typeAliases: MutableMap<CirName, CirTypeAliasNode> = CommonizerMap()

    val packageName: CirPackageName
        get() = targetDeclarations.firstNonNull().packageName

    override fun <T, R> accept(visitor: CirNodeVisitor<T, R>, data: T) =
        visitor.visitPackageNode(this, data)

    override fun toString() = CirNode.toString(this)
}
