package hair.graph

class TestGraph : DiGraph<TestGraph.Node> {
    data class Node(val id: Int) {
        internal val preds = linkedSetOf<Node>()
        internal val succs = linkedSetOf<Node>()

        fun addSucc(succ: Node) {
            succs.add(succ)
            succ.preds.add(this)
        }
    }

    override fun preds(n: Node) = n.preds.asSequence()
    override fun succs(n: Node) = n.succs.asSequence()

    val nodes = linkedSetOf<Node>()

    fun n(id: Int) = nodes.find { it.id == id } ?: (Node(id).also { nodes.add(it) })


    fun rootedAt(n: Node): FlowGraph<Node> = object : FlowGraph<Node>, DiGraph<Node> by this {
        override val root = n
    }

    fun rootedAt(id: Int) = rootedAt(n(id))

    // DSL

    infix fun Node.to(succ: Node): Node = succ.also { addSucc(it) }

}