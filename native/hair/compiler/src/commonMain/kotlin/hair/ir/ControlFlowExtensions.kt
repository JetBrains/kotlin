package hair.ir

import hair.graph.FlowGraph
import hair.ir.nodes.*
import hair.utils.singleOrNone


// TODO rename
val ControlFlow.block: BlockEntry get() = generateSequence(this) { when (it) {
    is BlockEntry -> null
    is Controlled -> it.control
    is Projection -> it.owner
    is Unwind -> it.thrower.block
    is Unreachable -> error("Should not reach here")
}}.last() as BlockEntry

// FIXME always use the first use slot?
val Controlling.nextOrNull: Controlled? get() = uses.firstOrNull { it is Controlled && it.control == this }?.let { it as Controlled }
val Controlling.next: Controlled get() = nextOrNull!!

val BlockExit.next: BlockEntry get() = uses.filterIsInstance<BlockEntry>().single()

val Throwing.unwind: Unwind? get() = uses.filterIsInstance<Unwind>().singleOrNone() // FIXME always has to be a handler?

val Unwind.handler: BlockEntry get() = this.next

// TODO rename?
val BlockEntry.spine: Sequence<Controlled>
    get() = generateSequence(this.next) {
        when (it) {
            is Controlling -> it.next
            is BlockEnd -> null
        }
    }

// TODO rename
val BlockEntry.blockEnd: BlockEnd
    get() = blockEndOrNull!!

val BlockExit.blockEnd: BlockEnd
    get() = when (this) {
        is BlockEnd -> this
        is IfProjection -> owner
        is Unwind -> error("Should not reach here") // FIXME
        is Unreachable -> error("Should not reach here")
    }

val If.trueExit: TrueExit get() = uses.single { it is TrueExit } as TrueExit
val If.falseExit: FalseExit get() = uses.single { it is FalseExit } as FalseExit

// TODO cache in block ?
val BlockEntry.blockEndOrNull: BlockEnd?
    get() = spine.lastOrNull() as? BlockEnd

val BlockEntry.predBlock: Sequence<BlockEntry>
    get() = preds.asSequence().filter { it !is Unreachable }.map { it.block }

val BlockEntry.exits: Sequence<BlockExit>
    get() = spine.flatMap {
        when (it) {
            is Throwing -> it.unwind?.let { sequenceOf<BlockExit>(it) } ?: sequenceOf()
            is If -> it.uses.map { it as IfProjection } // TODO other projectables?
            is Goto -> sequenceOf(it)
            else -> sequenceOf()
        }
    }

val BlockEntry.nextBlocks: Sequence<BlockEntry>
    get() = exits.map { it.next }

fun Session.cfg(): FlowGraph<BlockEntry> = object : FlowGraph<BlockEntry> {
    override val root = entry
    override fun preds(n: BlockEntry) = n.predBlock
    override fun succs(n: BlockEntry) = n.nextBlocks
}
