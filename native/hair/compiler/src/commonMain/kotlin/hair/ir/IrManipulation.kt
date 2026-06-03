package hair.ir

import hair.ir.nodes.*
import hair.ir.nodes.Node
import hair.opt.Normalization
import hair.opt.eliminateDeadBlocks
import hair.opt.eliminateDeadFoam
import hair.utils.isEmpty

class RawNodeBuilder(override val session: Session) : NodeBuilder {
    override fun onNodeBuilt(node: Node): Node = node
}

object RawArgsUpdater : ArgumentUpdater {
    override fun onArgUpdate(node: Node, index: Int, oldValue: Node?, newValue: Node?) {
        // FIXME consider moving into the base updateArg
        require(newValue != oldValue)
        oldValue?.removeUse(node)
        newValue?.addUse(node)
    }
}

open class NormalizingNodeBuilder(override val session: Session) : NodeBuilder {
    // FIXME use proper args updater
    val normalization = Normalization(session, this, RawArgsUpdater)

    override fun onNodeBuilt(node: Node): Node = normalization.normalize(node).also {
        // node must still be not-GVNed
        require(node !in session.allNodes())
    }
}

open class NormalizingGvnNodeBuilder(session: Session) : NormalizingNodeBuilder(session) {
    override fun onNodeBuilt(node: Node): Node = session.gvn(super.onNodeBuilt(node))
}

class RegisteringNodeBuilder(session: Session) : NormalizingGvnNodeBuilder(session) {
    override fun onNodeBuilt(node: Node): Node = super.onNodeBuilt(node).also {
        if (!it.registered) session.register(it)
        require(it.registered)
    }
}

class ArgumentUpdaterImpl(val session: Session, nodeBuilder: NodeBuilder) : ArgumentUpdater {
    val normalization = Normalization(session, nodeBuilder, this)
    override fun onArgUpdate(node: Node, index: Int, oldValue: Node?, newValue: Node?) {
        require(newValue != oldValue)
        oldValue?.removeUse(node)
        if (newValue != null) {
            val normal = normalization.normalize(node)
            val replacement: Node = if (normal != node) {
                normal
            } else {
                session.gvn(normal)
            }

            if (replacement != node) {
                // FIXME introduce common tool
                (node as? Controlling)?.nextOrNull?.let {
                    it.control = normal as Controlling
                }
                node.replaceValueUsesAndKill(replacement)
            } else {
                newValue.addUse(replacement)
            }
        }
    }
}

private val Session.nodeBuilder: RegisteringNodeBuilder get() = RegisteringNodeBuilder(this)
private val Session.argUpdater: ArgumentUpdaterImpl get() = ArgumentUpdaterImpl(this, nodeBuilder)

context(nodeBuilder: NodeBuilder)
val normalization: Normalization get() = when (nodeBuilder) {
    is NormalizingNodeBuilder -> nodeBuilder.normalization
    else -> error("Should not reach here")//NormalizationImpl(nodeBuilder.session, nodeBuilder, SimpleArgsUpdater)
}

fun <T> Session.buildInitialIR(
    builderAction: context(NodeBuilder, ControlFlowBuilder) ArgumentUpdater.() -> T
): T {
    return context(nodeBuilder, ControlFlowBuilder(entry)) {
        argUpdater.builderAction().also {
            eliminateDeadBlocks()
            eliminateDeadFoam()
            verify()
        }
    }
}

fun <T> Session.modifyIR(
    builderAction: context(NodeBuilder, NoControlFlowBuilder) ArgumentUpdater.() -> T
): T {
    return context(nodeBuilder, NoControlFlowBuilder) {
        argUpdater.builderAction().also {
            eliminateDeadFoam()
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
            eliminateDeadBlocks()
            eliminateDeadFoam()
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

context(_: ArgumentUpdater)
fun Node.kill() {
    require(uses.isEmpty())
    args.withIndex().forEach { [index, _] -> args[index] = null }
    deregister()
}

context(_: ArgumentUpdater)
fun Node.replaceValueUsesAndKill(replacement: Node) {
    replaceValueUses(replacement)
    kill()
}
