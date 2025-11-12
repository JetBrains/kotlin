package hair.ir

import hair.ir.nodes.Node
import hair.ir.nodes.NodeBuilder
import hair.ir.nodes.SimpleArgsUpdater
import hair.ir.nodes.*
import hair.opt.NormalizationImpl
import hair.utils.forEachInWorklist
import hair.utils.withWorklist

context(_: NodeBuilder, _: ArgsUpdater)
fun Session.cloneNodes(originals: Sequence<Node>, replacementProvider: (Node) -> Node?): Map<Node, Node> {
    val destinationSession = this
    val clones = mutableMapOf<Node, Node>()
    val replacements = mutableMapOf<Node, Node>()
    val shallowCloner = ShallowNodeCloner(object : NodeBuilder {
        override val session = destinationSession
        override fun normalize(node: Node): Node = node
        override fun <N : Node> register(node: N): N = node
    })
    fun cloneShallow(original: Node): Node = clones.getOrPut(original) {
        val replacement = replacementProvider(original)
        require(replacement == null || replacement.session == destinationSession)
        if (replacement != null) {
            replacements[original] = replacement
            replacement
        } else {
            // TODO common base type for all nested projections
            if (original is IfProjection) {
                val ifClone = cloneShallow(original.owner) as If
                when (original) {
                    is If.True -> ifClone.trueExit
                    is If.False -> ifClone.falseExit
                }
            } else {
                original.accept(shallowCloner)
            }
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
        val registered = it.register()
        val normal = normalization.normalize(registered)
        if (normal != it) {
            it.replaceValueUses(normal)
        }
    }

    return clones
}