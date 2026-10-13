package hair.ir

import hair.ir.nodes.Node
import hair.ir.nodes.NodeBuilder
import hair.ir.nodes.*
import hair.utils.forEachInWorklist

context(_: NodeBuilder, _: ArgumentUpdater)
fun Session.cloneNodes(originals: Sequence<Node>, replacementProvider: (Node) -> Node?): Map<Node, Node> {
    val destinationSession = this
    val clones = mutableMapOf<Node, Node>()
    val replacements = mutableMapOf<Node, Node>()
    val rawNodeBuilder: NodeBuilder = object : NodeBuilder {
        override val session: Session = this@cloneNodes
        override fun onNodeBuilt(node: Node): Node = node
    }
    val shallowCloner = ShallowNodeCloner(rawNodeBuilder)
    fun cloneShallow(original: Node): Node = clones.getOrPut(original) {
        val replacement = replacementProvider(original)
        require(replacement == null || replacement.session == destinationSession)
        if (replacement != null) {
            replacements[original] = replacement
            replacement
        } else {
            original.accept(shallowCloner)
        }
    }

    for (original in originals) {
        cloneShallow(original)
    }

    for (original in originals) {
        if (original in replacements) continue
        val shallowClone = clones[original]!!
        for ([idx, originalArg] in original.args.withIndex()) {
            shallowClone.args[idx] = clones[originalArg] ?: originalArg
        }
    }

    val normalization = normalization
    forEachInWorklist(clones.values) {
        val wired = it// TODO.wire()
        val normal = normalization.normalize(wired)
        if (normal != it) {
            it.replaceValueUses(normal)
        }
    }

    return clones
}
