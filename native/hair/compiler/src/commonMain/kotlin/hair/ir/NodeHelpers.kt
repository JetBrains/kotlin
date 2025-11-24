package hair.ir

import hair.ir.nodes.*
import hair.utils.closure

val Phi.extendedFamilyPhies: Set<Phi> get() = closure(this) {
    (it.joinedValues + it.uses).filterIsInstance<Phi>()
}

val Phi.extendedFamily: Set<Node> get() = extendedFamilyPhies.let { phiesFamily ->
    phiesFamily.flatMap { it.joinedValues }.toSet() + phiesFamily
}

val Phi.allPossibleValues get() = closure<Node>(this) {
    when (it) {
        is Phi -> it.joinedValues
        else -> emptyList()
    }
}.filterNot { it is Phi }

// FIXME better name
val Phi.inputs: Map<Node, BlockExit> get() = joinedValues.withIndex().associate { (idx, value) ->
    // FIXME support handlers?
    value to block.preds[idx]
}

fun Node.unproject() = when (this) {
    is Projection -> owner
    else -> this
}

//fun Jumping.unproject(): BlockExit = when (this) {
//    is IfProjection -> owner
//    is Goto -> this
//    is Handler -> this
//}
