package hair.ir.nodes

import hair.utils.*

abstract class BuiltinNodeVisitor<R> {
    abstract fun visitNode(node: Node): R

    open fun visitControlling(node: Controlling): R = visitNode(node)
    open fun visitControlMerge(node: ControlMerge): R = visitControlling(node)
    open fun visitSpinal(node: Spinal): R = visitControlling(node)
    open fun visitBlockEnd(node: BlockEnd): R = visitNode(node)
    open fun visitNoExit(node: NoExit): R = visitBlockEnd(node)
    open fun visitSingleExit(node: SingleExit): R = visitBlockEnd(node)
    open fun visitTwoExits(node: TwoExits): R = visitBlockEnd(node)
    open fun visitThrowExit(node: ThrowExit): R = visitNoExit(node)
    open fun visitThrowingSpinal(node: ThrowingSpinal): R = visitSpinal(node)
}

// TODO remove
interface Projection : Node {
    val ownerIndex: Int
}


interface ControlFlow : Node

interface Controlled : ControlFlow {
    val prevControl: Controlling?
}

private fun Controlled.setControlling(value: Controlling?) {
    when (this) {
        is Spinal -> this.prevControl = value
        is BlockEnd -> this.prevControl = value
        else -> shouldNotReachHere(this)
    }
}

sealed class Controlling(form: Form, args: List<Node?>) : NodeBase(form, args), ControlFlow {
    var nextControl: Controlled? = null
        set(value) {
            val oldValue = field
            field = value
            updateControlled(oldValue, value)
        }

    private fun updateControlled(oldValue: Controlled?, newValue: Controlled?) {
        if (oldValue == newValue) return
        // TODO other updates?
        oldValue?.setControlling(null)
        newValue?.setControlling(this)
    }
}

abstract class ControlMerge(form: Form, args: List<Node?>) : Controlling(form, args) {
    // FIXME protect from external updates
    val enters: MutableList<ControlFlow> = mutableListOf()
}

abstract class Spinal(form: Form, args: List<Node?>) : Controlling(form, args), Controlled {
    override var prevControl: Controlling? = null
        internal set
}

sealed class BlockEnd(form: Form, args: List<Node?>) : NodeBase(form, args), Controlled {
    override var prevControl: Controlling? = null
        internal set

    protected fun updateExit(oldValue: ControlMerge?, newValue: ControlMerge?) {
        if (oldValue == newValue) return
        // TODO other updates?
        oldValue?.enters?.remove(this)
        newValue?.enters?.add(this)
    }
}

abstract class NoExit(form: Form, args: List<Node?>) : BlockEnd(form, args)

abstract class SingleExit(form: Form, args: List<Node?>) : BlockEnd(form, args) {
    var exit: ControlMerge? = null
        set(value) {
            val oldValue = field
            field = value
            updateExit(oldValue, value)
        }
}

abstract class TwoExits(form: Form, args: List<Node?>) : BlockEnd(form, args) {
    var trueExit: ControlMerge? = null
        set(value) { // FIXME fight copy&paste here
            val oldValue = field
            field = value
            updateExit(oldValue, value)
        }
    var falseExit: ControlMerge? = null
        set(value) {
            val oldValue = field
            field = value
            updateExit(oldValue, value)
        }
}

// TODO switch?

// exceptions

interface Throwing : Controlled {
    var handler: ControlMerge?
}

abstract class ThrowExit(form: Form, args: List<Node?>) : NoExit(form, args), Throwing {
    override var handler: ControlMerge? = null
        set(value) {
            val oldValue = field
            field = value
            updateExit(oldValue, value)
        }
}

abstract class ThrowingSpinal(form: Form, args: List<Node?>) : Spinal(form, args), Throwing {
    // FIXME copy&pasted
    private fun updateExit(oldValue: ControlMerge?, newValue: ControlMerge?) {
        if (oldValue == newValue) return
        // TODO other updates?
        oldValue?.enters?.remove(this)
        newValue?.enters?.add(this)
    }
    override var handler: ControlMerge? = null
        set(value) {
            val oldValue = field
            field = value
            updateExit(oldValue, value)
        }
}

