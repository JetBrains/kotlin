/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirClassType
import org.jetbrains.kotlin.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.commonizer.cir.CirType
import org.jetbrains.kotlin.commonizer.cir.SimpleCirSupertypesResolver
import org.jetbrains.kotlin.commonizer.mergedtree.CirClassifierIndex
import org.jetbrains.kotlin.commonizer.mergedtree.CirKnownClassifiers
import org.jetbrains.kotlin.commonizer.mergedtree.CirProvided
import org.jetbrains.kotlin.commonizer.mergedtree.findClass
import org.jetbrains.kotlin.commonizer.util.transitiveClosure
import org.jetbrains.kotlin.descriptors.ClassKind

typealias Supertypes = List<CirType>

internal class ClassSuperTypeCommonizer(
    private val classifiers: CirKnownClassifiers
) : SingleInvocationCommonizer<List<CirType>> {

    private val typeCommonizer = TypeCommonizer(classifiers)

    override fun invoke(values: List<Supertypes>): Supertypes {
        if (values.isEmpty()) return emptyList()
        if (values.all { it.isEmpty() }) return emptyList()

        val supertypesTrees = resolveSupertypesTree(values)
        val supertypesGroups = buildSupertypesGroups(supertypesTrees)

        return supertypesGroups.mapNotNull { supertypesGroup ->
            typeCommonizer.asCommonizer().commonize(supertypesGroup.types)
        }
    }

    private fun resolveSupertypesTree(values: List<Supertypes>): List<SupertypesTree> {
        return values.mapIndexed { index: Int, supertypes: Supertypes ->
            val classifierIndex = classifiers.classifierIndices[index]
            val resolver = SimpleCirSupertypesResolver(classifiers.classifierIndices[index], classifiers.commonDependencies)
            val nodes = supertypes.filterIsInstance<CirClassType>().map { type -> createTypeNode(classifierIndex, resolver, type) }
            SupertypesTree(nodes)
        }
    }

    private fun buildSupertypesGroups(trees: List<SupertypesTree>): List<SupertypesGroup> {
        val groups = mutableListOf<SupertypesGroup>()
        var allowClassTypes = true

        trees.flatMap { tree -> tree.allNodes }.forEach { node ->
            if (node.assignedGroup != null) return@forEach
            val candidateGroup = buildTypeGroup(trees, node.type.classifierId) ?: return@forEach
            if (containsAnyClassKind(candidateGroup)) {
                if (!allowClassTypes) return@forEach
                allowClassTypes = false
            }
            assignGroupToNodes(candidateGroup)
            groups.add(candidateGroup)
        }

        return groups
    }

    private fun containsAnyClassKind(group: SupertypesGroup): Boolean {
        return group.nodes.any { node -> isClassKind(node) }
    }

    private fun isClassKind(node: TypeNode): Boolean {
        if (node.index.findClass(node.type.classifierId)?.kind == ClassKind.CLASS) return true

        /*
        Looking into provided dependencies.
        We do not know if ExportedForwardDeclarations are always Classes, but for sake of safety,
        we just assume all of those are classes.
        */
        val providedClassifier = classifiers.commonDependencies.classifier(node.type.classifierId) ?: return false
        return providedClassifier is CirProvided.ExportedForwardDeclarationClass ||
                (providedClassifier is CirProvided.RegularClass && providedClassifier.kind == ClassKind.CLASS)
    }

    private fun assignGroupToNodes(group: SupertypesGroup) {
        val classifiersIds = group.nodes.map { rootNode -> rootNode.allNodes.map { it.type.classifierId }.toSet() }
        val coveredClassifierIds = classifiersIds.reduce { acc, list -> acc intersect list }

        group.nodes.forEach { rootNode ->
            rootNode.allNodes.forEach { visitingNode ->
                if (visitingNode.type.classifierId in coveredClassifierIds) {
                    visitingNode.assignedGroup = group
                }
            }
        }
    }

    private fun buildTypeGroup(trees: List<SupertypesTree>, classifierId: CirEntityId): SupertypesGroup? {
        val nodes = trees.map { otherTree: SupertypesTree ->
            otherTree.allNodes.find { otherNode -> otherNode.type.classifierId == classifierId } ?: return null
        }
        return SupertypesGroup(classifierId, nodes)
    }
}

private fun createTypeNode(index: CirClassifierIndex, resolver: SimpleCirSupertypesResolver, type: CirClassType): TypeNode {
    return TypeNode(
        index = index,
        type = type,
        supertypes = resolver.supertypes(type).map { supertype -> createTypeNode(index, resolver, supertype) }
    )
}

private class SupertypesGroup(
    val classifierId: CirEntityId,
    val nodes: List<TypeNode>
) {
    val types = nodes.map { it.type }

    init {
        check(nodes.all { it.type.classifierId == classifierId })
    }
}

private class SupertypesTree(
    val nodes: List<TypeNode>
) {
    val allNodes: List<TypeNode> = run {
        val size = nodes.sumOf { it.allNodes.size }
        nodes.flatMapTo(ArrayList(size)) { it.allNodes }
    }
}

private class TypeNode(
    val index: CirClassifierIndex,
    val type: CirClassType,
    val supertypes: List<TypeNode>,
    var assignedGroup: SupertypesGroup? = null
) {
    val allNodes: List<TypeNode> by lazy {
        val allSupertypes = transitiveClosure(this, TypeNode::supertypes)
        ArrayList<TypeNode>(allSupertypes.size + 1).also { list ->
            list.add(this)
            list.addAll(allSupertypes)
        }
    }

    override fun toString(): String {
        return "TypeNode(${type.classifierId})"
    }
}
