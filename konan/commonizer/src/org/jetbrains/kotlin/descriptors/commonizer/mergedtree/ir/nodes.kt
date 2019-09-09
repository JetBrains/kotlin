/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.storage.NullableLazyValue

interface Node<T : Declaration, R : Declaration> {
    val target: List<T?>
    val common: NullableLazyValue<R>

    fun <R, T> accept(visitor: NodeVisitor<R, T>, data: T): R
}

class RootNode(
    override val target: List<Root>,
    override val common: NullableLazyValue<Root>
) : Node<Root, Root> {
    class ClassifiersCacheImpl : ClassifiersCache {
        override val classes = HashMap<FqName, ClassNode>()
        override val typeAliases = HashMap<FqName, TypeAliasNode>()
    }

    val modules: MutableList<ModuleNode> = ArrayList()
    val cache = ClassifiersCacheImpl()

    override fun <R, T> accept(visitor: NodeVisitor<R, T>, data: T): R =
        visitor.visitRootNode(this, data)
}

class ModuleNode(
    override val target: List<Module?>,
    override val common: NullableLazyValue<Module>
) : Node<Module, Module> {
    val packages: MutableList<PackageNode> = ArrayList()

    override fun <R, T> accept(visitor: NodeVisitor<R, T>, data: T) =
        visitor.visitModuleNode(this, data)
}

class PackageNode(
    override val target: List<Package?>,
    override val common: NullableLazyValue<Package>
) : Node<Package, Package> {
    val properties: MutableList<PropertyNode> = ArrayList()
    val functions: MutableList<FunctionNode> = ArrayList()
    val classes: MutableList<ClassNode> = ArrayList()
    val typeAliases: MutableList<TypeAliasNode> = ArrayList()

    override fun <R, T> accept(visitor: NodeVisitor<R, T>, data: T) =
        visitor.visitPackageNode(this, data)
}

class PropertyNode(
    override val target: List<Property?>,
    override val common: NullableLazyValue<Property>
) : Node<Property, Property> {
    override fun <R, T> accept(visitor: NodeVisitor<R, T>, data: T) =
        visitor.visitPropertyNode(this, data)
}

class FunctionNode(
    override val target: List<Function?>,
    override val common: NullableLazyValue<Function>
) : Node<Function, Function> {
    override fun <R, T> accept(visitor: NodeVisitor<R, T>, data: T) =
        visitor.visitFunctionNode(this, data)
}

class ClassNode(
    override val target: List<ClassDeclaration?>,
    override val common: NullableLazyValue<ClassDeclaration>
) : Node<ClassDeclaration, ClassDeclaration> {
    lateinit var fqName: FqName

    val constructors: MutableList<ClassConstructorNode> = ArrayList()
    val properties: MutableList<PropertyNode> = ArrayList()
    val functions: MutableList<FunctionNode> = ArrayList()
    val classes: MutableList<ClassNode> = ArrayList()

    override fun <R, T> accept(visitor: NodeVisitor<R, T>, data: T): R =
        visitor.visitClassNode(this, data)
}

class ClassConstructorNode(
    override val target: List<ClassConstructor?>,
    override val common: NullableLazyValue<ClassConstructor>
) : Node<ClassConstructor, ClassConstructor> {
    override fun <R, T> accept(visitor: NodeVisitor<R, T>, data: T): R =
        visitor.visitClassConstructorNode(this, data)
}

class TypeAliasNode(
    override val target: List<TypeAlias?>,
    override val common: NullableLazyValue<ClassDeclaration>
) : Node<TypeAlias, ClassDeclaration> {
    lateinit var fqName: FqName

    override fun <R, T> accept(visitor: NodeVisitor<R, T>, data: T): R =
        visitor.visitTypeAliasNode(this, data)
}

interface ClassifiersCache {
    val classes: Map<FqName, ClassNode>
    val typeAliases: Map<FqName, TypeAliasNode>
}

internal inline val Node<*, *>.indexOfCommon: Int
    get() = target.size

internal inline val Node<*, *>.dimension: Int
    get() = target.size + 1
