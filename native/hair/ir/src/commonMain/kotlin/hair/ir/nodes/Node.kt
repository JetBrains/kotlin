package hair.ir.nodes

import hair.ir.NodeVisitor
import kotlin.collections.plus
import kotlin.collections.remove

interface Node {
    // structure
    val id: Int
    val form: Form
    val session get() = form.session

    val args: ArgsList

    // derived
    val uses: Sequence<Node>

    val registered: Boolean

    fun ensureUnique(): Node = form.ensureUnique(this)

    fun addUse(use: Node)

    fun removeUse(use: Node)

    fun <R> accept(visitor: NodeVisitor<R>): R

    fun paramName(index: Int): String
}

sealed class NodeBase(final override val form: Form, args: List<Node?>) : Node {
    // TODO var?
    final override var id: Int = - session.nextNodeId.also { session.nextNodeId = it.inc() }

    internal var args_: Array<Node?> = args.toTypedArray()
    final override val args: ArgsList get() = ArgsList(this)

    // TODO: inline ArrayList
//    internal val uses_: Array<Node?> = arrayOfNulls(4) // TODO choose size based on statistics
//    internal var usesCount: Int = 0
    internal val uses_ = mutableListOf<Node>()
    final override val uses: Sequence<Node>
        get() = uses_.asSequence()//.filterNotNull()

    final override fun toString() = "$id = ${form.fullName}(${args.joinToString(", ") { it?.id.toString() }})"

    final override val registered: Boolean
        get() = id > 0

    override fun addUse(use: Node) {
        require(use !is Unreachable)
        uses_.add(use)
    }

    override fun removeUse(use: Node) {
        uses_.remove(use)
    }
}


// FIXME move?
class ControlFlowBuilder(at: Controlling) {
    var lastControl: Controlling? = at

    inline fun <N : Controlling> appendControl(next: () -> N): N = next().also { lastControl = it }

    inline fun <N : Node> appendControlled(next: (Controlling) -> N): N =
        next(lastControl!!).also {
            // TODO make separate appendBlockBody ?
            lastControl = it as? Controlling
        }
}

object NoControlFlowBuilder