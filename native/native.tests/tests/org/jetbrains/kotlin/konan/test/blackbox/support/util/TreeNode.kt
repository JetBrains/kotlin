/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.konan.test.blackbox.support.PackageName

internal interface TreeNode<T> {
    val packageSegment: PackageName
    val items: Collection<T>
    val children: Collection<TreeNode<T>>

    companion object {
        fun <T> oneLevel(vararg items: T) = oneLevel(listOf(*items))

        fun <T> oneLevel(items: Iterable<T>): List<TreeNode<T>> = listOf(object : TreeNode<T> {
            override val packageSegment get() = PackageName.EMPTY
            override val items = items.toList()
            override val children get() = emptyList<TreeNode<T>>()
        })
    }
}

internal fun <T, R> Collection<T>.buildTree(extractPackageName: (T) -> PackageName, transform: (T) -> R): Collection<TreeNode<R>> {
    val groupedItems: Map<PackageName, List<R>> = groupBy(extractPackageName).mapValues { (_, items) -> items.map(transform) }

    // Fast pass.
    when (groupedItems.size) {
        0 -> return TreeNode.oneLevel()
        1 -> return TreeNode.oneLevel(groupedItems.values.first())
    }

    // Long pass.
    val root = TreeBuilder<R>(PackageName.EMPTY)

    // Populate the tree.
    groupedItems.forEach { (packageName, items) ->
        var node = root
        packageName.segments.forEach { packageSegment ->
            val packageSegmentAsName = PackageName(listOf(packageSegment))
            node = node.childrenMap.computeIfAbsent(packageSegmentAsName) { TreeBuilder(packageSegmentAsName) }
        }
        node.items += items
    }

    // Skip meaningless nodes starting from the root.
    val meaningfulNode = root.skipMeaninglessNodes().apply { compress() }

    // Compress the resulting tree.
    return if (meaningfulNode.items.isNotEmpty() || meaningfulNode.childrenMap.isEmpty())
        listOf(meaningfulNode)
    else
        meaningfulNode.childrenMap.values
}

private class TreeBuilder<T>(override var packageSegment: PackageName) : TreeNode<T> {
    override val items = mutableListOf<T>()
    val childrenMap = hashMapOf<PackageName, TreeBuilder<T>>()
    override val children: Collection<TreeBuilder<T>> get() = childrenMap.values
}

private tailrec fun <T> TreeBuilder<T>.skipMeaninglessNodes(): TreeBuilder<T> =
    if (items.isNotEmpty() || childrenMap.size != 1)
        this
    else
        childrenMap.values.first().skipMeaninglessNodes()

private fun <T> TreeBuilder<T>.compress() {
    while (items.isEmpty() && childrenMap.size == 1) {
        val childNode = childrenMap.values.first()

        items += childNode.items

        childrenMap.clear()
        childrenMap += childNode.childrenMap

        packageSegment = joinPackageNames(packageSegment, childNode.packageSegment)
    }

    childrenMap.values.forEach { it.compress() }
}
