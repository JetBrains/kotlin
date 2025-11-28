package hair.ir

import hair.ir.nodes.Node
import hair.ir.nodes.NodeBuilder
import hair.ir.nodes.*
import hair.utils.forEachInWorklist

context(_: NodeBuilder, _: ArgsUpdater)
fun Session.cloneNodes(originals: Sequence<Node>, replacementProvider: (Node) -> Node?): Map<Node, Node> {
    val destinationSession = this
    val clones = mutableMapOf<Node, Node>()
    val replacements = mutableMapOf<Node, Node>()
    val shallowCloner = ShallowNodeCloner(RawNodeBuilder(this))
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
        for ((idx, originalArg) in original.args.withIndex()) {
            shallowClone.args[idx] = clones[originalArg] ?: originalArg
        }
    }

    val normalization = normalization
    forEachInWorklist(clones.values) {
        val registered = it// TODO.register()
        val normal = normalization.normalize(registered)
        if (normal != it) {
            it.replaceValueUses(normal)
        }
    }

    return clones
}