package hair.graph

import hair.utils.*


interface Dominators<N> {
    /** Immediate dominator */
    fun idom(n: N): N
    fun doms(n: N): Sequence<N>
    /** The set of nodes where dominance ends (n does not dominate any of them) */
    fun dominanceFrontier(n: N): Sequence<N>
    val tree: FlowGraph<N> // FIXME more of a rooted graph

    fun common(n1: N, n2: N): N

    // FIXME proper check?
    fun dominates(who: N, whom: N): Boolean = (common(who, whom) == who)

    companion object {
        // A Simple, Fast Dominance Algorithm
        fun <N> sfda(g: FlowGraph<N>): Dominators<N> {
            val postOrder = postOrder(g).toList()
            val order = postOrder.withIndex().associate { it.value to it.index }.asTotalMap()

            val idom = MutableList<N?>(postOrder.size) { null }
            fun intersect(n1: N, n2: N): N {
                var finger1 = n1
                var finger2 = n2
                while (finger1 != finger2) {
                    while (order[finger1] < order[finger2]) {
                        finger1 = idom[order[finger1]]!! // FIXME here !! ?
                    }
                    while (order[finger2] < order[finger1]) {
                        finger2 = idom[order[finger2]]!!
                    }
                }
                return finger1!!
            }
            idom[order[g.root]] = g.root
            whileChanged {
                for (b in postOrder.filterNot { it == g.root }) {
                    var newIdom: N = g.preds(b).filter { idom[order[it]] != null }.firstOrNull() ?: continue
                    for (p in g.preds(b).drop(1)) {
                        if (idom[order[p]] != null) { // FIXME hide id
                            newIdom = intersect(p, newIdom)
                        }
                    }
                    if (idom[order[b]] != newIdom) {
                        idom[order[b]] = newIdom
                        changed()
                    }
                }
            }

            val domFront = postOrder.associateWith { mutableListOf<N>() }.asTotalMap()

            for (n in postOrder) {
                if (g.preds(n).count() >= 2) {
                    for (p in g.preds(n)) {
                        var runner = p
                        while (runner != idom[order[n]]) {
                            domFront[runner] += n
                            runner = idom[order[runner]]!!
                        }
                    }
                }
            }

            return object : Dominators<N> {
                override fun idom(n: N): N = idom[order[n]]!!
                override fun doms(n: N): Sequence<N> = TODO("Not yet implemented")
                override fun dominanceFrontier(n: N): Sequence<N> = domFront[n].asSequence()

                override val tree: FlowGraph<N> by lazy {
                    val dominee = postOrder.groupBy { idom(it) }.mapValues { it.value.filter { it != g.root } }
                    object : FlowGraph<N> {
                        override val root = g.root
                        override fun preds(n: N) = sequenceOf(idom(n))
                        override fun succs(n: N) = dominee[n]?.asSequence() ?: emptySequence()
                    }
                }

                override fun common(n1: N, n2: N) = intersect(n1, n2)
            }
        }
    }
}