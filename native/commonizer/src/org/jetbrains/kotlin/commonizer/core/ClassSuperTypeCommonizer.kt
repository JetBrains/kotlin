/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.mergedtree.CirClassifierIndex
import org.jetbrains.kotlin.commonizer.mergedtree.CirKnownClassifiers
import org.jetbrains.kotlin.commonizer.mergedtree.findClass
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.tooling.core.withClosure

private typealias Supertypes = List<CirType>

/**
 * Using the [TypeCommonizer] on all supertypes to find the list of common supertypes across
 * all platforms. This commonizer will also resolve all transitive supertypes to find potential common supertypes.
 *
 * ```
 * // dependencies
 * interface A
 * interface B: A
 *
 * // target A
 * class X: A
 *
 * // target B
 * class X: B
 *
 * ```
 *
 * will commonize to
 * ```
 * class X: A // <- common supertype
 * ```
 */
internal class ClassSuperTypeCommonizer(
    private val classifiers: CirKnownClassifiers,
    private val typeCommonizer: TypeCommonizer
) : SingleInvocationCommonizer<Supertypes> {

    override fun invoke(values: List<Supertypes>): Supertypes {
        if (values.isEmpty()) return emptyList()
        if (values.all { it.isEmpty() }) return emptyList()

        val supertypesTrees = resolveSupertypesTree(values)
        val supertypesGroups = buildSupertypesGroups(supertypesTrees)

        return supertypesGroups.mapNotNull { supertypesGroup ->
            typeCommonizer(supertypesGroup.types)
        }
    }

    /**
     * For every supertype listed in [values] a full [SupertypesTree] will be resolved.
     * This tree represents the supertype-hierarchy:
     *
     * ```
     * interface A
     * interface B: A
     * interface C: B, A
     * ```
     *
     * will become a tree like
     * ```
     *               C
     *              |\
     *             |  \
     *            B    A
     *            |
     *            A
     *
     * ```
     *
     */
    private fun resolveSupertypesTree(values: List<Supertypes>): List<SupertypesTree> {
        return values.mapIndexed { index: Int, supertypes: Supertypes ->
            val classifierIndex = classifiers.classifierIndices[index]
            val resolver = SimpleCirSupertypesResolver(classifiers.classifierIndices[index], classifiers.commonDependencies)
            val nodes = supertypes.filterIsInstance<CirClassType>().map { type -> createTypeNode(classifierIndex, resolver, type) }
            SupertypesTree(nodes)
        }
    }

    /**
     * Builds [SupertypesGroup] (a group representing one type for every platform) that will be enqueued for type commonization.
     * To find out which types shall  be grouped this implementation will go through every single node in all trees (BFS!)
     * If a certain type can be found on all other platforms, then a group is build.
     * This types and all transitively "covered" supertypes will be marked as 'consumed' and therefore will be 'effectively removed'
     * from the tree.
     *
     * This grouping implementation will also be very careful about *not* grouping two groups that could effectively
     * represent a 'ClassKind' (to avoid commonizing with two abstract class supertypes)
     */
    private fun buildSupertypesGroups(trees: List<SupertypesTree>): List<SupertypesGroup> {
        val groups = mutableListOf<SupertypesGroup>()
        var allowClassTypes = true

        trees.flatMap { tree -> tree.allNodes }.forEach { node ->
            if (node.isConsumed) return@forEach
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
                    visitingNode.isConsumed = true
                }
            }
        }
    }

    private fun buildTypeGroup(trees: List<SupertypesTree>, classifierId: CirEntityId): SupertypesGroup? {
        val nodes = trees.map { otherTree: SupertypesTree ->
            otherTree.allNodes.find { otherNode -> otherNode.type.classifierId == classifierId && !otherNode.isConsumed } ?: return null
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
    var isConsumed: Boolean = false
) {
    val allNodes: Set<TypeNode> by lazy {
        this.withClosure(TypeNode::supertypes)
    }

    override fun toString(): String {
        return "TypeNode(${type.classifierId})"
    }
}
