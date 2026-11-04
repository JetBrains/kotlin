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

    override val argUpdater: ArgumentUpdater = ArgumentUpdaterImpl(this)
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

val Session.nodeBuilder get() = (this as SessionImpl).nodeBuilder
val Session.normalization get() = (this as SessionImpl).normalization

class WiringUpNodeBuilder internal constructor(override val session: SessionImpl) : NodeBuilder {
    override fun onNodeBuilt(node: Node): Node = session.wireUp(node)

    override fun onArgUpdate(node: Node, index: Int, oldValue: Node?, newValue: Node?) =
        session.argUpdater.onArgUpdate(node, index, oldValue, newValue)
}

class ArgumentUpdaterImpl(val session: SessionImpl) : ArgumentUpdater {
    override fun onArgUpdate(node: Node, index: Int, oldValue: Node?, newValue: Node?) {
        require(newValue != oldValue)
        oldValue?.removeUse(node)
        if (newValue != null) {
            val replacement = node
                .let { session.normalization.normalize(it) }
                .let { session.gvn(it) }

            if (replacement != node) {
                // DCE replaces ValueNodes with Unreachable
                val valueReplacement =
                    if (node is ValueNode && (replacement is Unreachable)) context(session.nodeBuilder) { NoValue() }
                    else replacement

                // FIXME not just replaceWith, because exception edge creates extra mess
                when (node) {
                    is Controlling -> node.rewireAndKill(replacement as Controlling, valueReplacement)
                    else -> {
                        node.replaceValueUses(valueReplacement)
                        node.kill()
                    }
                }

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

/**
 * The core of every replacement: re-points the control-spine successor at [newControl], redirects value uses to
 * [value], then kills this node.
 */
context(argUpdater: ArgumentUpdater)
private fun Controlling.rewireAndKill(newControl: Controlling, newValue: Node?) {
    val next = nextOrNull
    if (next != null) with(argUpdater) {
        next.control = newControl
    }
    if (newValue != null) replaceValueUses(newValue)
    require(uses.isEmpty()) { "$this still has uses after rewiring; pass a value replacement" }
    kill()
}

// FIXME can't require ValueNode here, because many builders erase types to Node and it's not easy to refactor
context(_: ArgumentUpdater)
fun Node.replaceValueNodeWith(value: Node) {
    require(this !is ControlFlow) {
        "$this is part of the control flow; use replaceWith(control, value), spliceOut or replaceThrowing"
    }
    replaceValueUses(value)
    kill()
}

context(_: ArgumentUpdater)
fun BlockBody.replaceWith(newControl: Controlling, newValue: Node?) {
    requireNoExceptionEdge()
    rewireAndKill(newControl, newValue)
}

context(_: ArgumentUpdater)
fun BlockBody.replaceWith(node: BlockBody) = replaceWith(node, node)

context(_: NodeBuilder, _: ArgumentUpdater)
fun BlockEntry.replaceWith(newControl: Controlling, newValue: Node = NoValue()) = rewireAndKill(newControl, newValue)

/**
 * Removes this node from the control, joining its predecessor straight to its successor.
 * Value uses are redirected to [valueReplacement], which may be left out only if there are none.
 */
context(_: ArgumentUpdater)
fun BlockBody.spliceOut(valueReplacement: Node? = null) = replaceWith(control, valueReplacement)

/** Replaces a throwing control flow node with another one, carrying the exception edge over. */
context(argUpdater: ArgumentUpdater)
fun BlockBodyWithException.replaceThrowing(replacement: BlockBodyWithException) {
    unwind?.let { with(argUpdater) { it.thrower = replacement } }
    rewireAndKill(replacement, replacement)
}

context(argUpdater: ArgumentUpdater)
fun BlockExit.replaceWith(exit: BlockExit) {
    val target = next
    with(argUpdater) {
        for ([index, pred] in target.preds.withIndex()) {
            if (pred == this@replaceWith) target.preds[index] = exit
        }
    }
    kill()
}

context(_: NodeBuilder, argUpdater: ArgumentUpdater)
fun If.fold(taken: IfProjection) {
    require(taken.owner == this)
    val dropped = if (taken == trueExit) falseExit else trueExit
    val control = control
    // Detach first, so that the Goto is the only control successor of the predecessor.
    with(argUpdater) { controlOrNull = null }
    taken.replaceWith(Goto(control))
    dropped.replaceWith(session.unreachable)
    kill()
}

private fun BlockBody.requireNoExceptionEdge() {
    if (this is Throwing) require(unwind == null) {
        "$this has an exception edge; use replaceThrowing"
    }
}
