/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClassifier
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirTypeAlias
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.storage.NullableLazyValue

class CirTypeAliasNode(
    override val targetDeclarations: CommonizedGroup<CirTypeAlias>,
    override val commonDeclaration: NullableLazyValue<CirClassifier>,
    override val fqName: FqName
) : CirNodeWithFqName<CirTypeAlias, CirClassifier> {

    override fun <R, T> accept(visitor: CirNodeVisitor<R, T>, data: T): R =
        visitor.visitTypeAliasNode(this, data)

    override fun toString() = CirNode.toString(this)
}
