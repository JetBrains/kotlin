package hair.ir

import hair.graph.FlowGraph
import hair.ir.nodes.*
import hair.utils.shouldNotReachHere


val ControlFlow.block: Block get() = generateSequence(this) { when (it) {
    is Block -> null
    is Controlled -> it.prevControl!!
    else -> shouldNotReachHere(it)
}}.last() as Block

val Block.phies: Sequence<Phi>
    get() = uses.asSequence().filterIsInstance<Phi>()

// TODO rename?
val Block.spine: Sequence<Controlled>
    get() = generateSequence(this.nextControl) {
        when (it) {
            is Throwing -> TODO()
            is Controlling -> it.nextControl
            is BlockEnd -> null
            else -> shouldNotReachHere(it)
        }
    }

val Block.blockEnd: BlockEnd
    get() = blockEndOrNull!!

// TODO cache in block ?
val Block.blockEndOrNull: BlockEnd?
    get() = spine.lastOrNull() as? BlockEnd

val BlockEnd.nextBlocks: List<Block>
    get() = when (this) {
        is NoExit -> emptyList()
        is SingleExit -> listOf(this.exit)
        is TwoExits -> listOf(this.trueExit, this.falseExit)
    }.filterNotNull().map { it as Block } // FIXME filterNotNull

val Block.nextBlocks: List<Block>
    get() = blockEndOrNull?.nextBlocks ?: emptyList()

fun Session.cfg(): FlowGraph<Block> = object : FlowGraph<Block> {
    override val root = entryBlock
    override fun preds(n: Block): Sequence<Block> = when (n) {
        is ControlMerge -> n.enters.asSequence()
        is XBlock -> n.throwers.asSequence()
        is Controlled -> n.prevControl?.let { sequenceOf(it) } ?: emptySequence()
        else -> shouldNotReachHere(n)
    }
    override fun succs(n: Block): Sequence<Block> = when (n) {
        is Controlling -> n.nextControl?.let { sequenceOf(it) } ?: emptySequence()
        is Throw -> n.handler?.let { sequenceOf(it) } ?: emptySequence()
        is SingleExit -> n.exit?.let { sequenceOf(it) } ?: emptySequence()
        is TwoExits -> listOfNotNull(n.trueExit, n.falseExit).asSequence()
        is NoExit -> emptySequence()
        else -> shouldNotReachHere(n)
    }
}
