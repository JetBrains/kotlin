package hair.ir

import hair.ir.nodes.*
import hair.sym.HairType
import hair.utils.closure

val BlockEntry.phies: Sequence<Phi> get() = uses.filterIsInstance<Phi>()

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

data class ValueAndExit(val value: Node, val exit: BlockExit)

val Phi.valuesAtExits: List<ValueAndExit> get() = joinedValues.withIndex().map { (index, value) ->
    ValueAndExit(value, block.preds[index])
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

val Const.type: HairType
    get() = when (value) {
        is Byte -> HairType.BYTE
        is Short -> HairType.SHORT
        is Int -> HairType.INT
        is Long -> HairType.LONG
        is Float -> HairType.FLOAT
        is Double -> HairType.DOUBLE
        else -> error("Unexpected number type $value")
    }

val ConstBoolean.value: Boolean
    get() = when (this) {
        is False -> false
        is True -> true
    }
