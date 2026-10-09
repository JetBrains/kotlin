// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier4_workloads

import kotlin.native.runtime.GC

class GraphNode(val id: Int) {
    var parent: GraphNode? = null
    val children = ArrayList<GraphNode>()
    var crossLink: GraphNode? = null
}

fun buildComplexGraph(breadth: Int, depth: Int): GraphNode {
    val root = GraphNode(0)
    var currentLevel = arrayListOf(root)

    var nextId = 1
    for (d in 0 until depth) {
        val nextLevel = ArrayList<GraphNode>()
        for (parent in currentLevel) {
            for (b in 0 until breadth) {
                val child = GraphNode(nextId++)
                child.parent = parent
                parent.children.add(child)
                nextLevel.add(child)
            }
        }
        for (i in 0 until nextLevel.size) {
            nextLevel[i].crossLink = nextLevel[(i + 1) % nextLevel.size]
        }
        currentLevel = nextLevel
    }
    return root
}

fun traverseGraph(root: GraphNode): Int {
    var count = 0
    val queue = arrayListOf(root)
    while (queue.isNotEmpty()) {
        val node = queue.removeAt(0)
        count++
        for (child in node.children) {
            queue.add(child)
        }
    }
    return count
}

fun box(): String {
    val graph = buildComplexGraph(3, 4)

    GC.collect()

    val totalNodes = traverseGraph(graph)
    if (totalNodes != 121) {
        return "FAIL complex object graph: expected 121 nodes, got $totalNodes"
    }

    val firstChild = graph.children.firstOrNull()
    if (firstChild?.crossLink == null) {
        return "FAIL cross link missing"
    }

    return "OK"
}
