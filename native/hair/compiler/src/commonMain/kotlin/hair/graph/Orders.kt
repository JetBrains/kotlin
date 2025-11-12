package hair.graph

fun <N> dfs(graph: FlowGraph<N>): Sequence<N> {
    val stack = mutableListOf(graph.succs(graph.root).iterator())
    val visited = mutableSetOf<N>()
    return generateSequence(graph.root) {
        while (stack.isNotEmpty()) {
            val top = stack.last()
            if (top.hasNext()) {
                val next = top.next()
                if (next !in visited) {
                    visited += next
                    stack += graph.succs(next).iterator()
                    return@generateSequence next
                }
            } else {
                stack.removeLast()
            }
        }
        null
    }
}

fun <N> postOrder(graph: FlowGraph<N>): Sequence<N> {
    val stack = mutableListOf(graph.root to graph.succs(graph.root).iterator())
    val visited = mutableSetOf<N>()
    return generateSequence {
        while (stack.isNotEmpty()) {
            val (top, topsNext) = stack.last()
            if (topsNext.hasNext()) {
                val next = topsNext.next()
                if (next !in visited) {
                    visited += next
                    stack += next to graph.succs(next).iterator()
                }
            } else {
                stack.removeLast()
                return@generateSequence top
            }
        }
        null
    }
}

fun <N> postOrder(roots: List<N>, graph: DiGraph<N>): Sequence<N> {
    val stack = mutableListOf<Pair<N, Iterator<N>>>()
    for (root in roots) {
        stack += root to graph.succs(root).iterator()
    }
    val visited = mutableSetOf<N>()
    return generateSequence {
        while (stack.isNotEmpty()) {
            val (top, topsNext) = stack.last()
            if (topsNext.hasNext()) {
                val next = topsNext.next()
                if (next !in visited) {
                    visited += next
                    stack += next to graph.succs(next).iterator()
                }
            } else {
                stack.removeLast()
                return@generateSequence top
            }
        }
        null
    }
}

fun <N> topSort(graph: FlowGraph<N>): List<N> {
    // TODO check
    return postOrder(graph).toList().reversed()
}
