/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

interface NodeVisitor<R, T> {
    fun visitRootNode(node: RootNode, data: T): R
    fun visitModuleNode(node: ModuleNode, data: T): R
    fun visitPackageNode(node: PackageNode, data: T): R
    fun visitPropertyNode(node: PropertyNode, data: T): R
    fun visitFunctionNode(node: FunctionNode, data: T): R
    fun visitClassNode(node: ClassNode, data: T): R
    fun visitClassConstructorNode(node: ClassConstructorNode, data: T): R
    fun visitTypeAliasNode(node: TypeAliasNode, data: T): R
}
