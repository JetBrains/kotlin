package hair.ir

import hair.ir.nodes.*
import hair.ir.nodes.Node
import hair.opt.NormalizationImpl
import hair.opt.eliminateDeadBlocks
import hair.opt.eliminateDeadFoam
import hair.utils.isEmpty

class RawNodeBuilder(override val session: Session) : NodeBuilder {
    override fun onNodeBuilt(node: Node): Node = node
}

object RawArgsUpdater : ArgsUpdater {
    override fun onArgUpdate(node: Node, index: Int, oldValue: Node?, newValue: Node?) {
        // FIXME consider moving into the base updateArg
        require(newValue != oldValue)
        oldValue?.removeUse(node)
        newValue?.addUse(node)
    }
}

open class NormalizingNodeBuilder(override val session: Session) : NodeBuilder {
    // FIXME use proper args updater
    val normalization = NormalizationImpl(session, this, RawArgsUpdater)

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

class ArgsUpdaterImpl(val session: Session, nodeBuilder: NodeBuilder) : ArgsUpdater {
    val normalization = NormalizationImpl(session, nodeBuilder, this)
    override fun onArgUpdate(node: Node, index: Int, oldValue: Node?, newValue: Node?) {
        require(newValue != oldValue)
        oldValue?.removeUse(node)
        if (newValue != null) {
            val normal = normalization.normalize(node)
            val replacement: Node = if (normal != node) {
                normal
            } else {
                val uniq = session.gvn(normal)
                newValue.addUse(uniq)
                uniq
            }

            if (replacement != node) {
                // FIXME introduce common tool
                (node as? Controlling)?.nextOrNull?.let {
                    it.control = normal as Controlling
                }
                node.replaceValueUsesAndKill(replacement)
            }
        }
    }
}

private val Session.nodeBuilder: RegisteringNodeBuilder get() = RegisteringNodeBuilder(this)
private val Session.argsUpdater: ArgsUpdaterImpl get() = ArgsUpdaterImpl(this, nodeBuilder)

context(nodeBuilder: NodeBuilder)
val normalization: Normalization get() = when (nodeBuilder) {
    is NormalizingNodeBuilder -> nodeBuilder.normalization
    else -> error("Should not reach here")//NormalizationImpl(nodeBuilder.session, nodeBuilder, SimpleArgsUpdater)
}

fun <T> Session.buildInitialIR(
    builderAction: context(NodeBuilder, ArgsUpdater, ControlFlowBuilder) () -> T
): T {
    return context(nodeBuilder, argsUpdater, ControlFlowBuilder(entry)) {
        builderAction().also {
            eliminateDeadBlocks()
            eliminateDeadFoam()
            // TODO merge with modifyIR
        }
    }
}

fun <T> Session.modifyIR(
    builderAction: context(NodeBuilder, ArgsUpdater, NoControlFlowBuilder) () -> T
): T {
    return context(nodeBuilder, argsUpdater, NoControlFlowBuilder) {
        val result = builderAction()

        eliminateDeadBlocks()
        eliminateDeadFoam()

        result
    }
}

fun <T> Session.modifyControlFlow(
    at: Controlling,
    builderAction: context(NodeBuilder, ArgsUpdater, ControlFlowBuilder) () -> T
): T {
    return context(nodeBuilder, argsUpdater, ControlFlowBuilder(at)) {
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
    val prev = control
    controlOrNull = null
    next.control = prev
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
