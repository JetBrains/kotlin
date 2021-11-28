package org.jetbrains.kotlin.utils

import java.util.*

// Copied from Kotlin org.jetbrains.kotlin.utils.DFS class
@Suppress("MemberVisibilityCanBePrivate", "MemberVisibilityCanBePrivate", "unused")
object DFS {
    fun <N, R> dfs(nodes: Collection<N>, neighbors: Neighbors<N>, visited: Visited<N>, handler: NodeHandler<N, R>): R {
        for (node in nodes) {
            doDfs(node, neighbors, visited, handler)
        }
        return handler.result()
    }

    fun <N, R> dfs(
            nodes: Collection<N>,
            neighbors: Neighbors<N>,
            handler: NodeHandler<N, R>
    ): R {
        return dfs(nodes, neighbors, VisitedWithSet(), handler)
    }

    fun <N> ifAny(
            nodes: Collection<N>,
            neighbors: Neighbors<N>,
            predicate: Function1<N, Boolean>
    ): Boolean {
        val result = BooleanArray(1)
        return dfs(nodes, neighbors, object : AbstractNodeHandler<N, Boolean?>() {
            override fun beforeChildren(current: N): Boolean {
                if (predicate.invoke(current)) {
                    result[0] = true
                }
                return !result[0]
            }

            override fun result(): Boolean {
                return result[0]
            }
        })!!
    }

    fun <N, R> dfsFromNode(node: N, neighbors: Neighbors<N>, visited: Visited<N>, handler: NodeHandler<N, R>): R {
        doDfs(node, neighbors, visited, handler)
        return handler.result()
    }

    fun <N> dfsFromNode(
            node: N,
            neighbors: Neighbors<N>,
            visited: Visited<N>
    ) {
        dfsFromNode(node, neighbors, visited, object : AbstractNodeHandler<N, Void?>() {
            override fun result(): Void? {
                return null
            }
        })
    }

    fun <N> topologicalOrder(nodes: Iterable<N>, neighbors: Neighbors<N>, visited: Visited<N>): List<N> {
        val handler = TopologicalOrder<N>()
        for (node in nodes) {
            doDfs(node, neighbors, visited, handler)
        }
        return handler.result()
    }

    fun <N> topologicalOrder(nodes: Iterable<N>, neighbors: Neighbors<N>): List<N> {
        return topologicalOrder(nodes, neighbors, VisitedWithSet())
    }

    fun <N> doDfs(current: N, neighbors: Neighbors<N>, visited: Visited<N>, handler: NodeHandler<N, *>) {
        if (!visited.checkAndMarkVisited(current)) return
        if (!handler.beforeChildren(current)) return
        for (neighbor in neighbors.getNeighbors(current)) {
            doDfs(neighbor, neighbors, visited, handler)
        }
        handler.afterChildren(current)
    }

    interface NodeHandler<N, R> {
        fun beforeChildren(current: N): Boolean
        fun afterChildren(current: N)
        fun result(): R
    }

    interface Neighbors<N> {
        fun getNeighbors(current: N): Iterable<N>
    }

    interface Visited<N> {
        fun checkAndMarkVisited(current: N): Boolean
    }

    abstract class AbstractNodeHandler<N, R> : NodeHandler<N, R> {
        override fun beforeChildren(current: N): Boolean {
            return true
        }

        override fun afterChildren(current: N) {}
    }

    class VisitedWithSet<N> @JvmOverloads constructor(private val visited: MutableSet<N> = HashSet()) : Visited<N> {
        override fun checkAndMarkVisited(current: N): Boolean {
            return visited.add(current)
        }
    }

    abstract class CollectingNodeHandler<N, R, C : Iterable<R>> protected constructor(protected val result: C) : AbstractNodeHandler<N, C>() {
        override fun result(): C {
            return result
        }
    }

    abstract class NodeHandlerWithListResult<N, R> protected constructor() : CollectingNodeHandler<N, R, LinkedList<R>>(LinkedList<R>())
    class TopologicalOrder<N> : NodeHandlerWithListResult<N, N>() {
        override fun afterChildren(current: N) {
            result.addFirst(current)
        }
    }
}