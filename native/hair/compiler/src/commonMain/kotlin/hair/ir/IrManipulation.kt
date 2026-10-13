package hair.ir

import hair.ir.nodes.*
import hair.ir.nodes.Node
import hair.opt.Normalization
import hair.opt.eliminateDeadNodes
import hair.utils.isEmpty

fun interface OnNodeBuilt {
    fun onNodeBuilt(node: Node): Node
}

class SessionImpl: Session() {
    val nodeBuilder: NodeBuilder = WiringUpNodeBuilder(this)
    val controlEditor: ControlEditor = WiringUpControlEditor(this)

    override val argUpdater: ArgumentUpdater = ArgumentUpdaterImpl(this)
    val normalization = Normalization(this)

    /** The modification scope currently open on this session, if any, see [modificationScope]. */
    internal var openScope: String? = null

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
val Session.controlEditor get() = (this as SessionImpl).controlEditor
val Session.normalization get() = (this as SessionImpl).normalization

class WiringUpNodeBuilder internal constructor(override val session: SessionImpl) : NodeBuilder {
    override fun onNodeBuilt(node: Node): Node = session.wireUp(node)

    override fun onArgUpdate(node: Node, index: Int, oldValue: Node?, newValue: Node?) =
        session.argUpdater.onArgUpdate(node, index, oldValue, newValue)
}

class WiringUpControlEditor internal constructor(override val session: SessionImpl) : ControlEditor {
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
                    is Controlling -> context(session.nodeBuilder) {
                        node.rewireAndKill(replacement as Controlling, valueReplacement)
                    }
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

/**
 * Runs [action] as the modification scope [name].
 *
 * Scopes don't nest. The graph is free of dead code and verified when a scope begins and when it ends; within a
 * scope, edits may leave dead code behind and break invariants temporarily.
 */
private inline fun <T, B : NodeBuilder> Session.modificationScope(
    name: String,
    builder: B,
    runDCE: Boolean,
    action: B.() -> T,
): T {
    this as SessionImpl
    check(openScope == null) { "Can't open $name inside $openScope: modification scopes don't nest" }
    openScope = name
    val result = try {
        builder.action().also {
            if (runDCE) context(controlEditor) { eliminateDeadNodes() }
        }
    } finally {
        openScope = null
    }
    verify()
    return result
}

fun <T> Session.buildInitialIR(
    builderAction: ControlBuilder.() -> T
): T = modificationScope("buildInitialIR", controlEditor, runDCE = true) {
    ControlBuilder(this, entry).builderAction()
}

fun <T> Session.modifyIR(
    runDCE: Boolean = true,
    builderAction: NodeBuilder.() -> T
): T = modificationScope("modifyIR", nodeBuilder, runDCE) {
    builderAction()
}

fun <T> Session.modifyControlFlow(
    runDCE: Boolean = true,
    builderAction: ControlEditor.() -> T
): T = modificationScope("modifyControlFlow", controlEditor, runDCE) {
    builderAction()
}


context(editor: ControlEditor)
fun <N : Node> Controlling.insertAfter(build: ControlBuilder.() -> N): N {
    // Captured before building: until it is re-pointed, this node has two control successors.
    val successor = next
    val controlBuilder = ControlBuilder(editor, this)
    val result = controlBuilder.build()
    with(editor) { successor.control = controlBuilder.control }
    return result
}

context(_: ControlEditor)
fun <N : Node> Controlled.insertBefore(build: ControlBuilder.() -> N): N =
    this@insertBefore.control.insertAfter(build)


/**
 * Replaces this control-spine node with an arbitrary control-flow sub-graph.
 *
 * If the replaced node produced a value, [build] must return its replacement, to which all value uses
 * are redirected; return `null` for non-value nodes.
 */
context(editor: ControlEditor)
fun BlockBody.replaceWithSubGraph(
    build: ControlBuilder.() -> Node?,
) {
    val original = this
    if (original is Throwing) require(original.unwind == null) {
        "replaceWithSubGraph cannot yet re-link the exception edge of $original"
    }
    // Captured before any rewiring: the spine neighbours the sub-graph is spliced between.
    val predecessor = original.control
    val successor = original.next

    with(editor) {
        // Detach the node up front so the predecessor's only live control successor is the sub-graph.
        original.controlOrNull = null

        val controlBuilder = ControlBuilder(editor, predecessor)
        val replacementValue = controlBuilder.build()

        val continuation = controlBuilder.control
        successor.control = continuation

        replacementValue?.let { original.replaceValueUses(it) }
        original.kill()
    }
}

context(_: ArgumentUpdaterBase)
private fun Node.kill() {
    require(uses.isEmpty())
    args.withIndex().forEach { [index, _] -> args[index] = null }
    unwireFromGraphAndDeregister()
}

/**
 * The core of every replacement: re-points the control-spine successor at [newControl], redirects value uses to
 * [value], then kills this node.
 */
context(builder: NodeBuilder)
private fun Controlling.rewireAndKill(newControl: Controlling, newValue: Node?) {
    val next = nextOrNull
    if (next != null) with(builder) { next.control = newControl }
    if (newValue != null) replaceValueUses(newValue)
    require(uses.isEmpty()) { "$this still has uses after rewiring; pass a value replacement" }
    kill()
}

// FIXME can't require ValueNode here, because many builders erase types to Node and it's not easy to refactor
context(_: NodeBuilder)
fun Node.replaceValueNodeWith(value: Node) {
    require(this !is ControlFlow) {
        "$this is part of the control flow; use replaceWith(control, value), spliceOut or replaceThrowing"
    }
    replaceValueUses(value)
    kill()
}

context(_: ControlEditor)
fun BlockBody.replaceWith(newControl: Controlling, newValue: Node?) {
    requireNoExceptionEdge()
    rewireAndKill(newControl, newValue)
}

context(_: ControlEditor)
fun BlockBody.replaceWith(node: BlockBody) = replaceWith(node, node)

context(_: ControlEditor)
fun BlockEntry.replaceWith(newControl: Controlling, newValue: Node = NoValue()) = rewireAndKill(newControl, newValue)

/**
 * Removes this node from the control, joining its predecessor straight to its successor.
 * Value uses are redirected to [valueReplacement], which may be left out only if there are none.
 */
context(_: ControlEditor)
fun BlockBody.spliceOut(valueReplacement: Node? = null) = replaceWith(control, valueReplacement)

/** Replaces a throwing control flow node with another one, carrying the exception edge over. */
context(editor: ControlEditor)
fun BlockBodyWithException.replaceThrowing(replacement: BlockBodyWithException) {
    unwind?.let { with(editor) { it.thrower = replacement } }
    rewireAndKill(replacement, replacement)
}

context(_: ControlEditor)
fun BlockExit.replaceWith(exit: BlockExit) {
    val target = next
    for ([index, pred] in target.preds.withIndex()) {
        if (pred == this@replaceWith) target.preds[index] = exit
    }
    kill()
}

context(editor: ControlEditor)
fun If.fold(taken: IfProjection) {
    require(taken.owner == this)
    val dropped = if (taken == trueExit) falseExit else trueExit
    val control = this@fold.control
    // Detach first, so that the Goto is the only control successor of the predecessor.
    with(editor) { controlOrNull = null }
    taken.replaceWith(Goto(control))
    dropped.replaceWith(session.unreachable)
    kill()
}

private fun BlockBody.requireNoExceptionEdge() {
    if (this is Throwing) require(unwind == null) {
        "$this has an exception edge; use replaceThrowing"
    }
}
