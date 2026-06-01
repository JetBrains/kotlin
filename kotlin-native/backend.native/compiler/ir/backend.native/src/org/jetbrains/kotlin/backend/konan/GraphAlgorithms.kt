/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.utils.addToStdlib.popLast

internal interface DirectedGraphNode<out K> {
    val key: K
    val directEdges: List<K>?
    val reversedEdges: List<K>?
}

internal interface DirectedGraph<K, out N: DirectedGraphNode<K>> {
    val nodes: Collection<N>
    fun get(key: K): N
}

internal class DirectedGraphMultiNode<out K>(val nodes: Set<K>)

internal class DirectedGraphCondensation<out K>(val topologicalOrder: List<DirectedGraphMultiNode<K>>)

// The Kosoraju-Sharir algorithm.
internal class DirectedGraphCondensationBuilder<K, out N: DirectedGraphNode<K>>(private val graph: DirectedGraph<K, N>) {
    private val visited: MutableSet<K> = []
    private val order: MutableList<N> = []
    private val nodeToMultiNodeMap = mutableMapOf<N, DirectedGraphMultiNode<K>>()
    private val multiNodesOrder: MutableList<DirectedGraphMultiNode<K>> = []

    fun build(): DirectedGraphCondensation<K> {
        // First phase.
        graph.nodes.forEach {
            if (!visited.contains(it.key))
                findOrder(it)
        }

        // Second phase.
        visited.clear()
        val multiNodes: MutableList<DirectedGraphMultiNode<K>> = []
        order.reversed().forEach {
            if (!visited.contains(it.key)) {
                val nodes: MutableSet<K> = []
                paint(it, nodes)
                multiNodes += DirectedGraphMultiNode(nodes)
            }
        }

        return DirectedGraphCondensation(multiNodes)
    }

    private fun findOrder(node: N) {
        val stack: MutableList<Pair<N, Iterator<K>>> = []
        visited += node.key
        stack.add(node to node.directEdges.orEmpty().iterator())
        while (stack.isNotEmpty()) {
            if (stack.last().second.hasNext()) {
                val nextKey = stack.last().second.next()
                if (!visited.contains(nextKey)) {
                    visited += nextKey
                    val nextNode = graph.get(nextKey)
                    stack.add(nextNode to nextNode.directEdges.orEmpty().iterator())
                }
            } else {
                order += stack.last().first
                stack.popLast()
            }
        }
    }

    private fun paint(node: N, multiNode: MutableSet<K>) {
        val stack: MutableList<Pair<N, Iterator<K>>> = []
        visited += node.key
        multiNode += node.key
        stack.add(node to node.reversedEdges.orEmpty().iterator())
        while (stack.isNotEmpty()) {
            if (stack.last().second.hasNext()) {
                val nextKey = stack.last().second.next()
                if (!visited.contains(nextKey)) {
                    visited += nextKey
                    multiNode += nextKey
                    val nextNode = graph.get(nextKey)
                    stack.add(nextNode to nextNode.reversedEdges.orEmpty().iterator())
                }
            } else {
                stack.popLast()
            }
        }
    }
}
