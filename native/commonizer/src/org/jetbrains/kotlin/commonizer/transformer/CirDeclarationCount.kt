/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.transformer

import org.jetbrains.kotlin.commonizer.cir.ArtificialCirDeclaration
import org.jetbrains.kotlin.commonizer.mergedtree.*

internal data class CirDeclarationCount(
    val artificialNodeCount: Int,
    val nonArtificialNodeCount: Int
)

internal data class MutableCirDeclarationCount(
    var artificialNodeCount: Int = 0,
    var nonArtificialNodeCount: Int = 0
) {
    fun toCirDeclarationCount() = CirDeclarationCount(
        artificialNodeCount = artificialNodeCount,
        nonArtificialNodeCount = nonArtificialNodeCount
    )
}

internal fun CirNode<*, *>.countCirDeclarations(): CirDeclarationCount {
    val counter = MutableCirDeclarationCount()
    accept(CirNodeCounterVisitor, counter)
    return counter.toCirDeclarationCount()
}

private object CirNodeCounterVisitor : CirNodeVisitor<MutableCirDeclarationCount, Unit> {

    private operator fun MutableCirDeclarationCount.plusAssign(node: CirNode<*, *>) {
        node.targetDeclarations.forEach { declaration ->
            if (declaration == null) return@forEach
            if (declaration is ArtificialCirDeclaration) artificialNodeCount++ else nonArtificialNodeCount++
        }
    }

    override fun visitRootNode(node: CirRootNode, data: MutableCirDeclarationCount) {
        data += node
        node.modules.forEach { (_, module) -> module.accept(this, data) }
    }

    override fun visitModuleNode(node: CirModuleNode, data: MutableCirDeclarationCount) {
        data += node
        node.packages.forEach { (_, pkg) -> pkg.accept(this, data) }
    }

    override fun visitPackageNode(node: CirPackageNode, data: MutableCirDeclarationCount) {
        data += node
        node.typeAliases.forEach { (_, typeAlias) -> typeAlias.accept(this, data) }
        node.properties.forEach { (_, property) -> property.accept(this, data) }
        node.functions.forEach { (_, function) -> function.accept(this, data) }
        node.classes.forEach { (_, clazz) -> clazz.accept(this, data) }
    }

    override fun visitPropertyNode(node: CirPropertyNode, data: MutableCirDeclarationCount) {
        data += node
    }

    override fun visitFunctionNode(node: CirFunctionNode, data: MutableCirDeclarationCount) {
        data += node
    }

    override fun visitClassNode(node: CirClassNode, data: MutableCirDeclarationCount) {
        data += node
        node.properties.forEach { (_, property) -> property.accept(this, data) }
        node.functions.forEach { (_, function) -> function.accept(this, data) }
        node.constructors.forEach { (_, constructor) -> constructor.accept(this, data) }
        node.classes.forEach { (_, clazz) -> clazz.accept(this, data) }
    }

    override fun visitClassConstructorNode(node: CirClassConstructorNode, data: MutableCirDeclarationCount) {
        data += node
    }

    override fun visitTypeAliasNode(node: CirTypeAliasNode, data: MutableCirDeclarationCount) {
        data += node
    }
}