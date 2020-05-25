/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

interface CirNodeVisitor<R, T> {
    fun visitRootNode(node: CirRootNode, data: T): R
    fun visitModuleNode(node: CirModuleNode, data: T): R
    fun visitPackageNode(node: CirPackageNode, data: T): R
    fun visitPropertyNode(node: CirPropertyNode, data: T): R
    fun visitFunctionNode(node: CirFunctionNode, data: T): R
    fun visitClassNode(node: CirClassNode, data: T): R
    fun visitClassConstructorNode(node: CirClassConstructorNode, data: T): R
    fun visitTypeAliasNode(node: CirTypeAliasNode, data: T): R
}
