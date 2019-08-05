/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.ir

interface Declaration

interface Node<D : Declaration> {
    val target: List<D?>
    val common: D?

    fun <R, T> accept(visitor: NodeVisitor<R, T>, data: T): R
}

class RootNode(
    override val target: List<Root>,
    override val common: Root
) : Node<Root> {
    val modules: MutableList<ModuleNode> = ArrayList()

    override fun <R, T> accept(visitor: NodeVisitor<R, T>, data: T): R =
        visitor.visitRootNode(this, data)
}

class ModuleNode(
    override val target: List<Module?>,
    override val common: Module?
) : Node<Module> {
    val packages: MutableList<PackageNode> = ArrayList()

    override fun <R, T> accept(visitor: NodeVisitor<R, T>, data: T) =
        visitor.visitModuleNode(this, data)
}

class PackageNode(
    override val target: List<Package?>,
    override val common: Package?
) : Node<Package> {
    val properties: MutableList<PropertyNode> = ArrayList()

    override fun <R, T> accept(visitor: NodeVisitor<R, T>, data: T) =
        visitor.visitPackageNode(this, data)
}

class PropertyNode(
    override val target: List<Property?>,
    override val common: Property?
) : Node<Property> {
    override fun <R, T> accept(visitor: NodeVisitor<R, T>, data: T) =
        visitor.visitPropertyNode(this, data)
}

internal val <D : Declaration> Node<D>.indexOfCommon: Int
    get() = target.size

internal val <D : Declaration> Node<D>.dimension: Int
    get() = target.size + 1
