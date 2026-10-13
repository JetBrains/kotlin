package hair.ir

import hair.ir.nodes.*
import hair.ir.nodes.Node
import hair.opt.Normalization
import hair.opt.eliminateDead
import hair.utils.isEmpty

fun interface OnNodeBuilt {
    fun onNodeBuilt(node: Node): Node
}

class SessionImpl: Session() {
    val nodeBuilder: NodeBuilder = WiringUpNodeBuilder(this)

    val argUpdater: ArgumentUpdater = ArgumentUpdaterImpl(this)
    val normalization = Normalization(this)

    /** Adds the node to tha graph in its canonical form with uses wired */
    fun wireUp(node: Node): Node = node
        .let { normalization.normalize(it) }
        .let { gvn(it) }
        .also { wireToGraph(it) }
        .let { onNodeBuilt.onNodeBuilt(it) }

    private var onNodeBuilt: OnNodeBuilt = OnNodeBuilt { it }

    fun <T> withOnNodeBuiltHandler(onNodeBuiltProvider: (OnNodeBuilt) -> OnNodeBuilt, block: () -> T): T {
        val previous = onNodeBuilt
        onNodeBuilt = onNodeBuiltProvider(previous)
        try {
            return block()
        } finally {
            onNodeBuilt = previous
        }
    }
}

class WiringUpNodeBuilder internal constructor(override val session: SessionImpl) : NodeBuilder {
    override fun onNodeBuilt(node: Node): Node = session.wireUp(node)
}

val Session.nodeBuilder get() = (this as SessionImpl).nodeBuilder
val Session.argUpdater get() = (this as SessionImpl).argUpdater
val Session.normalization get() = (this as SessionImpl).normalization

class ArgumentUpdaterImpl(val session: SessionImpl) : ArgumentUpdater {
    override fun onArgUpdate(node: Node, index: Int, oldValue: Node?, newValue: Node?) {
        require(newValue != oldValue)
        oldValue?.removeUse(node)
        if (newValue != null) {
            val replacement = node
                .let { session.normalization.normalize(it) }
                .let { session.gvn(it) }

            if (replacement != node) {
                // FIXME introduce common tool
                (node as? Controlling)?.nextOrNull?.let {
                    it.control = replacement as Controlling
                }
                node.replaceValueUsesAndKill(replacement)
            } else {
                newValue.addUse(replacement)
            }
        }
    }
}

fun <T> Session.withOnNodeBuiltHandler(onNodeBuiltProvider: (OnNodeBuilt) -> OnNodeBuilt, block: () -> T): T =
    (this as SessionImpl).withOnNodeBuiltHandler(onNodeBuiltProvider, block)

fun <T> Session.observingBuiltNodes(observer: (Node) -> Unit, block: () -> T): T = withOnNodeBuiltHandler(
    { outer ->
        { node -> outer.onNodeBuilt(node).also { observer(it) } }
    },
    block
)

fun <T> Session.buildInitialIR(
    builderAction: context(NodeBuilder, ControlFlowBuilder) ArgumentUpdater.() -> T
): T {
    return context(nodeBuilder, ControlFlowBuilder(entry)) {
        argUpdater.builderAction().also {
            eliminateDead()
            verify()
        }
    }
}

fun <T> Session.modifyIR(
    runDCE: Boolean = true,
    builderAction: context(NodeBuilder, NoControlFlowBuilder) ArgumentUpdater.() -> T
): T {
    return context(nodeBuilder, NoControlFlowBuilder) {
        argUpdater.builderAction().also {
            if (runDCE) eliminateDead()
            verify()
        }
    }
}

fun <T> Session.modifyControlFlow(
    at: Controlling,
    builderAction: context(NodeBuilder, ControlFlowBuilder) ArgumentUpdater.() -> T
): T {
    return context(nodeBuilder, ControlFlowBuilder(at)) {
        argUpdater.builderAction().also {
            eliminateDead()
            verify()
        }
    }
}

// TODO require cfg modificator
context(_: ArgumentUpdater)
fun Controlled.eraseControl() {
    // FIXME use controlIndex or something
    args.erase(0)
}

// TODO move into control manipulator
context(argUpdater: ArgumentUpdater)
fun BlockBody.removeFromControl() = with(argUpdater) {
//    for (arg in args) {
//        arg.removeUse(this)
//        //updateArg(arg, null) {} // FIXME find a better way
//    }
    val prev = control
    controlOrNull = null
    next.control = prev
    //eraseControl()
    kill()
}

context(argUpdater: ArgumentUpdater)
fun <N: BlockBody> Session.insertAfter(point: Controlling, createNode: context(NodeBuilder, ControlFlowBuilder) () -> N): N {
    val next = point.next
    val node = modifyControlFlow(point) { createNode() }
    with(argUpdater) { next.control = node }
    return node
}

context(_: ArgumentUpdater)
fun <N: BlockBody> Session.insertBefore(point: Controlled, createNode: context(NodeBuilder, ControlFlowBuilder) () -> N): N {
    return insertAfter(point.control, createNode)
}

/**
 * Replaces this control-spine node with an arbitrary control-flow sub-graph.
 *
 * If the replaced node produced a value, [build] must return its replacement, to which all value uses
 * are redirected; return `null` for non-value nodes.
 */
fun BlockBody.replaceWithSubGraph(
    build: context(NodeBuilder, ControlFlowBuilder) ArgumentUpdater.() -> Node?,
) {
    val original = this
    if (original is Throwing) require(original.unwind == null) {
        "replaceWithSubGraph cannot yet re-link the exception edge of $original"
    }
    // Captured before any rewiring: the spine neighbours the sub-graph is spliced between.
    val predecessor = original.control
    val successor = original.next

    session.modifyControlFlow(predecessor) {
        // Detach the node up front so the predecessor's only live control successor is the sub-graph.
        original.controlOrNull = null

        val replacementValue = build()

        val continuation = contextOf<ControlFlowBuilder>().lastControl
        successor.control = continuation

        replacementValue?.let { original.replaceValueUses(it) }
        original.kill()
    }
}

context(_: ArgumentUpdater)
fun Node.kill() {
    require(uses.isEmpty())
    args.withIndex().forEach { [index, _] -> args[index] = null }
    unwireFromGraphAndDeregister()
}

context(_: ArgumentUpdater)
fun Node.replaceValueUsesAndKill(replacement: Node) {
    replaceValueUses(replacement)
    kill()
}
