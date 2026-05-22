package hair.ir.nodes

import hair.sym.HairType

/**
 * The in-IR value type produced by a [ValueNode], as a single [HairType].
 *
 * This is the v1 "framework for valueType computation" — a centralised dispatch
 * over the concrete node class. Each value-producing node has exactly one branch
 * below; adding a new value-producing node means marking it `ValueNode` in the
 * generator DSL and adding one branch here.
 *
 * Future work: replace the right-hand side with a richer lattice (intervals,
 * known nullness, known class, etc.). The public `valueType: HairType` accessor
 * will become a projection from that lattice. See `native/hair/TYPING.md`.
 *
 * Deferred (intentionally not in the [ValueNode] hierarchy in v1):
 *   - [Param]  — needs the enclosing function signature.
 *   - `Neg` / `Not` — tied to settling supported arithmetic widths.
 *   - [Catch]  — WIP.
 */
val ValueNode.valueType: HairType get() = when (this) {
    is ConstI -> HairType.INT
    is ConstL -> HairType.LONG
    is ConstF -> HairType.FLOAT
    is ConstD -> HairType.DOUBLE
    is Null -> HairType.REFERENCE
    is ConstTypeInfo -> HairType.REFERENCE

    is ArithBinaryOp -> opType.toHairType()
    is Cmp -> HairType.INT  // boolean-as-int

    is Cast -> targetType   // SignExtend/ZeroExtend/Truncate/Reinterpret

    is Phi -> type

    is Load -> type
    is LoadField -> field.type
    is LoadGlobal -> field.type
    // Store / StoreField / StoreGlobal are marked AnyLoad in the DSL (pre-existing
    // shape — see Memory.kt); their valueType is meaningless. Treat as VOID.
    is Store -> HairType.VOID

    is AnyNew -> HairType.REFERENCE  // New, NewArray

    is IsInstanceOf -> HairType.INT
    is CheckCast -> HairType.REFERENCE

    is TypeInfo -> HairType.REFERENCE

    is AnyCall -> when (this) {
        is InvokeStatic -> function.resultHairType
        is InvokeVirtual -> function.resultHairType
    }

    is UnitValue -> HairType.REFERENCE  // theUnitInstanceRef
    is NoValue -> HairType.VOID  // placeholder for "no value flowing here"
}
