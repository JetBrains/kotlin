/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree

import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.NullableLazyValue

interface CirNode<T : CirDeclaration, R : CirDeclaration> {
    val target: List<T?>
    val common: NullableLazyValue<R>

    fun <R, T> accept(visitor: CirNodeVisitor<R, T>, data: T): R
}

interface CirNodeWithFqName<T : CirDeclaration, R : CirDeclaration> : CirNode<T, R> {
    val fqName: FqName
}

class CirRootNode(
    override val target: List<CirRoot?>,
    override val common: NullableLazyValue<CirRoot>
) : CirNode<CirRoot, CirRoot> {
    class ClassifiersCacheImpl : CirClassifiersCache {
        override val classes = HashMap<FqName, CirClassNode>()
        override val typeAliases = HashMap<FqName, CirTypeAliasNode>()
    }

    val modules: MutableList<CirModuleNode> = ArrayList()
    val cache = ClassifiersCacheImpl()

    override fun <R, T> accept(visitor: CirNodeVisitor<R, T>, data: T): R =
        visitor.visitRootNode(this, data)

    override fun toString() = toString(this)
}

class CirModuleNode(
    override val target: List<CirModule?>,
    override val common: NullableLazyValue<CirModule>
) : CirNode<CirModule, CirModule> {
    val packages: MutableList<CirPackageNode> = ArrayList()

    override fun <R, T> accept(visitor: CirNodeVisitor<R, T>, data: T) =
        visitor.visitModuleNode(this, data)

    override fun toString() = toString(this)
}

class CirPackageNode(
    override val target: List<CirPackage?>,
    override val common: NullableLazyValue<CirPackage>
) : CirNodeWithFqName<CirPackage, CirPackage> {
    lateinit var moduleName: Name
    override lateinit var fqName: FqName

    val properties: MutableList<CirPropertyNode> = ArrayList()
    val functions: MutableList<CirFunctionNode> = ArrayList()
    val classes: MutableList<CirClassNode> = ArrayList()
    val typeAliases: MutableList<CirTypeAliasNode> = ArrayList()

    override fun <R, T> accept(visitor: CirNodeVisitor<R, T>, data: T) =
        visitor.visitPackageNode(this, data)

    override fun toString() = toString(this)
}

class CirPropertyNode(
    override val target: List<CirProperty?>,
    override val common: NullableLazyValue<CirProperty>
) : CirNode<CirProperty, CirProperty> {
    override fun <R, T> accept(visitor: CirNodeVisitor<R, T>, data: T) =
        visitor.visitPropertyNode(this, data)

    override fun toString() = toString(this)
}

class CirFunctionNode(
    override val target: List<CirFunction?>,
    override val common: NullableLazyValue<CirFunction>
) : CirNode<CirFunction, CirFunction> {
    override fun <R, T> accept(visitor: CirNodeVisitor<R, T>, data: T) =
        visitor.visitFunctionNode(this, data)

    override fun toString() = toString(this)
}

class CirClassNode(
    override val target: List<CirClass?>,
    override val common: NullableLazyValue<CirClass>
) : CirNodeWithFqName<CirClass, CirClass> {
    override lateinit var fqName: FqName

    val constructors: MutableList<CirClassConstructorNode> = ArrayList()
    val properties: MutableList<CirPropertyNode> = ArrayList()
    val functions: MutableList<CirFunctionNode> = ArrayList()
    val classes: MutableList<CirClassNode> = ArrayList()

    override fun <R, T> accept(visitor: CirNodeVisitor<R, T>, data: T): R =
        visitor.visitClassNode(this, data)

    override fun toString() = toString(this)
}

class CirClassConstructorNode(
    override val target: List<CirClassConstructor?>,
    override val common: NullableLazyValue<CirClassConstructor>
) : CirNode<CirClassConstructor, CirClassConstructor> {
    override fun <R, T> accept(visitor: CirNodeVisitor<R, T>, data: T): R =
        visitor.visitClassConstructorNode(this, data)

    override fun toString() = toString(this)
}

class CirTypeAliasNode(
    override val target: List<CirTypeAlias?>,
    override val common: NullableLazyValue<CirClassifier>
) : CirNodeWithFqName<CirTypeAlias, CirClassifier> {
    override lateinit var fqName: FqName

    override fun <R, T> accept(visitor: CirNodeVisitor<R, T>, data: T): R =
        visitor.visitTypeAliasNode(this, data)

    override fun toString() = toString(this)
}

interface CirClassifiersCache {
    val classes: Map<FqName, CirClassNode>
    val typeAliases: Map<FqName, CirTypeAliasNode>
}

internal inline val CirNode<*, *>.indexOfCommon: Int
    get() = target.size

internal inline val CirNode<*, *>.dimension: Int
    get() = target.size + 1

private fun toString(node: CirNode<*, *>) = buildString {
    if (node is CirNodeWithFqName) {
        append("fqName=").append(node.fqName).append(", ")
    }
    append("target=")
    node.target.joinTo(this)
    append(", common=")
    append(if (node.common.isComputed()) node.common() else "<NOT COMPUTED>")
}
