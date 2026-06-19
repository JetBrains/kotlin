package hair.transform

import hair.compilation.FunctionCompilation
import hair.ir.Session
import hair.ir.nodes.*
import hair.ir.nodes.NodeBase
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
    // Worklist fixpoint algorithm over the flat HairType lattice (⊥ = `null`, top = any [HairType])
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

// ---------------------------------------------------------------------------
// Type rule
// ---------------------------------------------------------------------------

/** Returns the [HairType] this node produces, or `null` if a dependency's type is not yet known. */
context(compilation: FunctionCompilation)
private fun ValueNode.typeRule(): HairType? = when (this) {
    // Structural — resolved from form parameters alone:
    is ConstI -> HairType.INT
    is ConstL -> HairType.LONG
    is ConstF -> HairType.FLOAT
    is ConstD -> HairType.DOUBLE
    is Null -> HairType.REFERENCE
    is ConstTypeInfo -> HairType.NATIVE_POINTER

    is ArithBinaryOp -> opType.toHairType()
    is Cmp -> HairType.INT

    is Cast -> targetType

    is Load -> type
    is LoadField -> field.type
    is LoadGlobal -> field.type
    is Store -> HairType.VOID

    is AnyNew -> HairType.REFERENCE // New / NewArray

    is IsInstanceOf -> HairType.INT
    is CheckCast -> HairType.REFERENCE

    is TypeInfo -> HairType.REFERENCE

    is InvokeStatic -> function.resultHairType
    is InvokeVirtual -> function.resultHairType

    is UnitValue -> HairType.REFERENCE
    is NoValue -> HairType.VOID

    // Data-flow — propagate the operand's type; null until the operand is resolved:
    is Inv -> (operand as NodeBase).valueTypeOrNull
    is Not -> HairType.INT

    // Param — resolved via the caller-supplied callback:
    is Param -> compilation.function.parameterTypes[index]

    // Phi — join of input types; null until at least one meaningful input is resolved.
    is Phi -> joinedValues.firstNotNullOfOrNull { (it as NodeBase).valueTypeOrNull }
}

