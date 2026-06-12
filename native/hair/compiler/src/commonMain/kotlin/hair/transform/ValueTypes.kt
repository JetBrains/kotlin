package hair.transform

import hair.ir.nodes.*
import hair.sym.HairType
import hair.utils.toWorklist

/**
 * Asserts that this node's [hair.ir.nodes.Node.valueType] has been computed and returns it.
 *
 * Call [computeValueTypes] first; throws for uncomputed nodes.
 */
fun Node.requireValueType(): HairType =
    valueType ?: error("value type not computed for $this")

/**
 * Populates [hair.ir.nodes.Node.valueType] for every value-producing node in this session.
 *
 * **Algorithm** — Worklist fixpoint over the flat HairType lattice (⊥ = `null`, top = any
 * [HairType]):
 *
 * 1. Seed the worklist with every [ValueNode] in the session.
 * 2. For each node dequeued, call [typeRule]: if it returns a [HairType], assign it and
 *    re-queue all still-unresolved [ValueNode] uses (they may now be resolvable).
 *    If it returns `null`, the node's dependencies are not yet known — it will be re-queued
 *    when one of its inputs is resolved (step 2 above).
 * 3. Repeat until the worklist is empty.
 *
 * **Convergence** — guaranteed by three properties:
 * - *Monotone*: [Node.valueType] transitions only from `null` → [HairType]; once set it is
 *   never changed.
 * - *Finite state*: each node can change state at most once, and there are finitely many nodes.
 * - *Progress*: every worklist step either assigns a type (strictly reducing remaining `null`
 *   nodes) or is discarded (node already resolved or dependencies still missing). Since each
 *   type assignment re-queues only uses, and each node can be assigned a type at most once,
 *   the total number of productive steps is bounded by |nodes|.
 *
 * **Cyclic Phis** — e.g. `φ = Φ(init, Neg(φ))`:
 * SSA guarantees every Phi has at least one non-back-edge input (`init`). Once `init` is
 * resolved, `φ` resolves (from `init`'s type), then `Neg(φ)` resolves (from `φ`'s type).
 * No special cycle-breaking is needed.
 *
 * **Deferred** (remain `null` after this call):
 * - [Param] when [paramType] returns `null` (e.g. callers without function-signature access).
 * - [Catch] — WIP.
 * - Control-flow nodes — do not implement [ValueNode].
 *
 * @param paramType Resolves the [HairType] for a [Param] node by its index in the enclosing
 *   function's parameter list. Returns `null` for unknown params (leaving [Node.valueType]
 *   unset). Callers with access to the enclosing [hair.sym.HairFunction] should pass a lambda
 *   that indexes into [hair.sym.HairFunction.parameterTypes].
 *
 * See `native/hair/TYPING.md` for design rationale.
 */
fun SessionBase.computeValueTypes(paramType: (Param) -> HairType? = { null }) {
    val worklist = allNodes<ValueNode>().toWorklist()
    for (node in worklist) {
        if (node.valueType != null) continue
        val type = node.typeRule(paramType) ?: continue
        node.valueType = type
        // Re-queue unresolved ValueNode uses: they may now be resolvable.
        node.uses
            .filterIsInstance<ValueNode>()
            .filter { it.valueType == null }
            .forEach { worklist.add(it) }
    }
}

// ---------------------------------------------------------------------------
// Type rule
// ---------------------------------------------------------------------------

/**
 * Returns the [HairType] this node produces, or `null` if a dependency's type is not yet known.
 *
 * Structural rules return immediately (they depend only on form parameters, not on other
 * nodes' types). Data-flow rules ([Param], [Neg], [Not], [Phi]) return `null` until their
 * inputs are resolved.
 */
private fun ValueNode.typeRule(paramType: (Param) -> HairType?): HairType? = when (this) {
    // Structural — resolved from form parameters alone:
    is ConstI -> HairType.INT
    is ConstL -> HairType.LONG
    is ConstF -> HairType.FLOAT
    is ConstD -> HairType.DOUBLE
    is Null -> HairType.REFERENCE
    is ConstTypeInfo -> HairType.REFERENCE

    is ArithBinaryOp -> opType.toHairType()
    is Cmp -> HairType.INT // result is always INT (boolean-as-int)

    is Cast -> targetType // SignExtend / ZeroExtend / Truncate / Reinterpret

    is Load -> type
    is LoadField -> field.type
    is LoadGlobal -> field.type
    is Store -> HairType.VOID

    is AnyNew -> HairType.REFERENCE // New / NewArray

    is IsInstanceOf -> HairType.INT
    is CheckCast -> HairType.REFERENCE

    is TypeInfo -> HairType.REFERENCE

    is AnyCall -> when (this) {
        is InvokeStatic -> function.resultHairType
        is InvokeVirtual -> function.resultHairType
    }

    is UnitValue -> HairType.REFERENCE
    is NoValue -> HairType.VOID

    // Data-flow — propagate the operand's type; null until the operand is resolved:
    is Neg -> (operand as? ValueNode)?.valueType
    is Not -> (operand as? ValueNode)?.valueType

    // Param — resolved via the caller-supplied callback:
    is Param -> paramType(this)

    // Phi — join of input types; null until at least one meaningful input is resolved.
    // Exception phis (all inputs are Unwind nodes) → EXCEPTION, the valid type for
    // exception-object flows.
    is Phi -> phiTypeRule()
}

/**
 * Computes a [Phi]'s type from its inputs.
 * Returns `null` when no input has a known type yet (will be re-queued by the worklist).
 */
private fun Phi.phiTypeRule(): HairType? {
    // Exception phi: all inputs are Unwind (non-ValueNode) — the value IS an exception object.
    val valueInputs = joinedValues.filterIsInstance<ValueNode>()
    if (valueInputs.isEmpty()) return HairType.EXCEPTION

    // Skip NoValue placeholders inserted by SSA construction.
    val meaningfulInputs = valueInputs.filter { it !is NoValue }
    if (meaningfulInputs.isEmpty()) return null

    // v1 invariant: all meaningful inputs share the same type.
    return meaningfulInputs.firstNotNullOfOrNull { it.valueType }
}
