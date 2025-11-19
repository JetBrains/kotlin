package hair.ir

import hair.ir.nodes.*
import hair.ir.nodes.Node
import hair.opt.NormalizationImpl
import hair.opt.eliminateDead
import hair.utils.Worklist
import hair.utils.forEachInWorklist
import hair.utils.isEmpty

class NodeBuilderImpl(override val session: Session) : NodeBuilder {
    val normalization = NormalizationImpl(session, this)
    override fun normalize(node: Node): Node = normalization.normalize(node)
    override fun <N : Node> register(node: N): N = node.register()
}

// FIXME private?
private val Session.nodeBuilder: NodeBuilderImpl get() = NodeBuilderImpl(this)

context(nodeBuilder: NodeBuilder)
val normalization: Normalization get() = when (nodeBuilder) {
    is NodeBuilderImpl -> nodeBuilder.normalization
    else -> NormalizationImpl(nodeBuilder.session, nodeBuilder)
}

class ArgUpdatesWatcher : ArgsUpdater {
    val updatedNodes = Worklist<Node>()

    override fun onArgUpdate(node: Node, index: Int, oldValue: Node?, newValue: Node?) {
        updatedNodes.add(node)
    }
}

fun <T> Session.buildInitialIR(
    builderAction: context(NodeBuilder, ArgsUpdater, ControlFlowBuilder) () -> T
): T {
    return context(nodeBuilder, SimpleArgsUpdater, ControlFlowBuilder(entry)) {
        builderAction().also {
            eliminateDead()
            // TODO merge with modifyIR
        }
    }
}

fun <T> Session.modifyIR(
    builderAction: context(NodeBuilder, ArgsUpdater, NoControlFlowBuilder) () -> T
): T {
    val argsWatcher = ArgUpdatesWatcher()
    val result = context(nodeBuilder, argsWatcher, NoControlFlowBuilder) {
        val result = builderAction()

        eliminateDead()

        forEachInWorklist(allNodes()) { node ->
            // FIXME what about cyclic dependencies?
            // FIXME maybe find common grounds for control flow handling
            if (node !is ControlFlow && node.uses.isEmpty()) {
                // FIXME fix this registered/deregistered mess
                if (node.registered) {
                    addAll(node.args.filterNotNull())
                    node.deregister()
                }
            }
        }

        result
    }

    for (node in argsWatcher.updatedNodes) {
        val replacement = nodeBuilder.normalization.normalize(node)
        if (replacement != node) {
            with (argsWatcher) {
                // FIXME what about control here?
                node.replaceValueUses(replacement)
            }
        }
    }
    return result
}

fun <T> Session.modifyControlFlow(
    at: Controlling,
    builderAction: context(NodeBuilder, ArgsUpdater, ControlFlowBuilder) () -> T
): T {
    return context(nodeBuilder, SimpleArgsUpdater, ControlFlowBuilder(at)) {
        builderAction()
    }
}

// TODO require cfg modificator
context(_: ArgsUpdater)
fun Controlled.eraseControl() {
    // FIXME use controlIndex or something
    args.erase(0)
}

// TODO move into control manipulator
context(_: ArgsUpdater)
fun BlockBody.removeFromControl() {
//    for (arg in args) {
//        arg.removeUse(this)
//        //updateArg(arg, null) {} // FIXME find a better way
//    }
    next.control = control
    //eraseControl()
    require(uses.isEmpty())
    //require(registered)
    deregister() // FIXME make node lists concurrent modifiable
}

context(_: ArgsUpdater)
fun <N: BlockBody> Session.insertAfter(point: Controlling, createNode: context(NodeBuilder, ControlFlowBuilder) () -> N): N {
    val next = point.next
    val node = modifyControlFlow(point) { createNode() }
    next.control = node
    return node
}

context(_: ArgsUpdater)
fun <N: BlockBody> Session.insertBefore(point: Controlled, createNode: context(NodeBuilder, ControlFlowBuilder) () -> N): N {
    return insertAfter(point.control, createNode)
}

context(_: ArgsUpdater)
fun Node.kill() {
    require(uses.isEmpty())
    args.withIndex().forEach { (index, _) -> args[index] = null }
    deregister()
}

context(_: ArgsUpdater)
fun Node.replaceValueUsesAndKill(replacement: Node) {
    replaceValueUses(replacement)
    kill()
}
