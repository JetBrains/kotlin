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
val Phi.inputs: Map<Node, BlockEnd> get() = joinedValues.withIndex().associate { (idx, value) ->
    value to block.enters[idx]
}

val Escape.initiallyEscaped: Node get() = when (val orig = origin) {
    is Escape -> orig.initiallyEscaped
    else -> orig
}

val Node.unproxy: Node get() = when (this) {
    is ProxyProjection -> this.origin.unproxy
    else -> this
}

