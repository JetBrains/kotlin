package hair.transform

import hair.compilation.FunctionCompilation
import hair.ir.Session
import hair.ir.nodes.*
import hair.ir.nodes.NodeBase
import hair.ir.type
import hair.sym.HairType
import hair.utils.toWorklist

// TODO hide in a context
val ValueNode.valueType: HairType
    get() = (this as NodeBase).valueTypeOrNull ?: error("value type not computed for $this")

context(_: FunctionCompilation)
inline fun <T> Session.withValueTypes(action: () -> T): T {
    computeValueTypes()
    return action()
}

inline fun <T> Session.withValueTypes(compilation: FunctionCompilation, action: () -> T): T = context(compilation) {
    withValueTypes(action)
}

context(_: FunctionCompilation)
fun Session.computeValueTypes() {
    // Worklist fixpoint algorithm over the flat HairType lattice (bottom = `null`, top = any [HairType])
    val worklist = allNodes<ValueNode>().toWorklist()
    for (node in worklist) {
        node as NodeBase
        if (node.valueTypeOrNull != null) continue
        val type = node.typeRule() ?: continue
        node.valueTypeOrNull = type
        // Re-queue unresolved ValueNode uses: they may now be resolvable.
        node.uses
            .filterIsInstance<ValueNode>()
            .filter { (it as NodeBase).valueTypeOrNull == null }
            .forEach { worklist.add(it) }
    }
}

/** Returns the [HairType] this node produces, or `null` if a dependency's type is not yet known. */
context(compilation: FunctionCompilation)
private fun ValueNode.typeRule(): HairType? = when (this) {
    is Const -> type
    is ConstBoolean -> HairType.BOOLEAN
    is Null -> HairType.REFERENCE
    is ConstTypeInfo -> HairType.NATIVE_POINTER

    is ArithBinaryOp -> opType.toHairType()
    is Cmp -> HairType.BOOLEAN

    is Cast -> targetType

    is Load -> type
    is LoadField -> field.type
    is LoadGlobal -> field.type

    is AnyNew -> HairType.REFERENCE

    is IsInstanceOf -> HairType.INT
    is CheckCast -> HairType.REFERENCE

    is TypeInfo -> HairType.REFERENCE

    is InvokeStatic -> function.resultHairType
    is InvokeVirtual -> function.resultHairType

    is UnitValue -> HairType.REFERENCE
    is NoValue -> HairType.NOTHING

    is Inv -> (operand as NodeBase).valueTypeOrNull
    is Not -> HairType.BOOLEAN

    is Param -> compilation.function.parameterTypes[index]

    is Phi -> joinedValues.firstNotNullOfOrNull { (it as NodeBase).valueTypeOrNull }

    is LoadArrayElement -> elementType
    is ArraySize -> HairType.INT
}

