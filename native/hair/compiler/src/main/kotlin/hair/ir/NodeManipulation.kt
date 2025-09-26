package hair.ir

import hair.ir.nodes.*

// TODO move into control manipulator
fun Spinal.removeFromControl() {
//    for (arg in args) {
//        arg.removeUse(this)
//        //updateArg(arg, null) {} // FIXME find a better way
//    }
    prevControl!!.nextControl = nextControl
    require(uses.isEmpty())
    //require(registered)
    deregister() // FIXME make node lists concurrent modifiable
}

fun <N: Spinal> Session.insertAfter(point: Controlling, createNode: ControlFlowBuilder.() -> N): N {
    val next = point.nextControl
    val node = modifyControlFlow(point) { createNode() }
    node.nextControl = next
    return node
}

fun <N: Spinal> Session.insertBefore(point: Controlled, createNode: ControlFlowBuilder.() -> N): N {
    return insertAfter(point.prevControl!!, createNode)
}
