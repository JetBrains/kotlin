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
}}.last() as BlockEntry

val BlockEntry.phies: Sequence<Phi>
    get() = uses.asSequence().filterIsInstance<Phi>()

// FIXME always use the first use slot?
val Controlling.next: Controlled get() = uses.first { it is Controlled && it.control == this } as Controlled

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

// TODO cache in block ?
val BlockEntry.blockEndOrNull: BlockEnd?
    get() = spine.lastOrNull() as? BlockEnd

val BlockEntry.predBlock: Sequence<BlockEntry>
    get() = preds.asSequence().map { it.block }

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
