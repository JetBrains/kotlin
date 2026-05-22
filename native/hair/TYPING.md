# Adding types to IR nodes

## Type sets

Three concerns, kept distinct:

- **`ArithmeticType`** (`INT`, `LONG`, `FLOAT`, `DOUBLE`) — the set of
  operations defined for the arithmetic op nodes. A node like `Add` only makes
  sense on these four types; widening `Add` to `HairType.REFERENCE` would be a
  type error. Lives in `sym`.
- **`HairType`** (existing: `VOID`, `INT`, `LONG`, `FLOAT`, `DOUBLE`,
  `REFERENCE`, `EXCEPTION`) — the *in-IR value type*. What flows along SSA
  edges. This is what `valueType` returns. Narrow widths (BYTE/SHORT/BOOLEAN)
  do not exist at HaIR level; loads/stores carry the access width via
  `HairType`, and narrower stores/loads are encoded via separate Cast / extend
  nodes when needed.
- **Storage type** — *not* a HaIR-level concept (for now). Field/Global access
  width is carried by the underlying `Field` / `Global` symbol, which already
  has a `type: HairType`. If a memory subsystem with narrow widths becomes
  necessary, a dedicated enum can be introduced later, but it would live in
  `sym` next to `HairType`, not on `Node`.
- **LLVM type** — *not* a HaIR-level concept. `HairToBitcode` already has the
  `HairType.asLLVMType()` mapping; that's the right place for it.

## Goal

Every value-producing IR node should expose its type information through two
distinct concepts:

- **op type** — intrinsic to the node's form. Defines the *operation* the node
  performs. Part of the form (so part of the value-numbering key where
  applicable). Available the moment the node exists. Different node groups
  carry op type with different types (e.g. arithmetic ops use
  `ArithmeticType`, casts/phis/loads use `HairType`); we do **not** introduce
  a uniform `Node.opType` property at this stage. Where the name is currently
  generic (`type` on arithmetic ops), rename to `opType` and narrow to
  `ArithmeticType` so the property is type-honest.
- **`valueType: HairType`** — computed on demand. Describes the set of values
  the node can produce. For now this is a single `HairType` and will, in most
  cases, simply equal whatever op type the node carries (lifted to `HairType`).
  It is *not* part of the form and does *not* participate in GVN. Designed to
  grow into a full abstract-interpretation lattice (range, nullness, known
  class, etc.) later. Implemented as a centralised extension property over a
  `ValueNode` marker interface — adding a new value-producing node means adding
  a `when` branch in one place. That's the "framework for valueType
  computation."

The first concrete consumer of `valueType` is the HaIR → LLVM lowering: LLVM
requires explicit types on `phi` instructions, so we must be able to ask any
node (in particular, any Phi input) for its produced type.

## Scope of this change

In scope:

1. Introduce a `ValueNode` marker interface; have all value-producing node
   interfaces / abstract classes / nodes extend it.
2. Add `opType: HairType` on `ValueNode`. Wire every concrete value-producing
   node (excluding the deferred set below) to a concrete source of truth.
3. Add `valueType: HairType` on `ValueNode`, computed (extension property or
   visitor). v1 implementation: `valueType = opType` for everything that has an
   `opType`. Keep the API stable so the body can later be replaced by a real
   lattice computation.
4. Extend the IR generator (`native/hair/ir/generator/`) so the DSL can declare
   how `opType` is sourced for each node / abstract class / interface, and emit
   the appropriate override.

Out of scope (deferred — leave as dummy / unimplemented for now):

- `Param.opType` — needs to read the enclosing function's signature. The
  function symbol isn't on `Param.form` today, and threading a `HairFunction`
  through forms vs. sessions is a separate design decision. For v1, `Param`
  may either not declare `opType` or return a placeholder (TBD when picked up).
- `Neg.opType` / `Not.opType` — tied up with the open question of "what
  arithmetic widths do we ultimately support." Punt: these two do not expose
  `opType` in v1. (`valueType` for these can still fall back to operand
  inspection if it ever becomes necessary, but it is *not* a v1 requirement.)
- `Catch` — heavy WIP; ignore.
- Control-flow nodes — do not implement `ValueNode`. They carry no `opType` /
  `valueType`.
- The `valueType` lattice itself — v1 reuses `HairType`. The richer lattice
  (intervals, nullness, known classes, …) is a follow-up project.

## Per-node opType source-of-truth

For each node group, the generator must emit an `opType` override that reads
from the listed source. The exact emission shape is up to the generator design
(see "Generator extension" below), but the result is equivalent to:

| Node(s)                                              | `opType` source                                                |
|------------------------------------------------------|----------------------------------------------------------------|
| `Add`, `Sub`, `Mul`, `Div`, `Rem`, `And`, `Or`, `Xor`, `Shl`, `Shr`, `Ushr` | existing form param `type`                                     |
| `Cmp`                                                | existing form param `type` — the *operand* type; result type is INT and that is a `valueType` concern, not `opType` |
| `Phi`                                                | existing form param `type`                                     |
| `Load`, `Store`                                      | existing form param `type`                                     |
| `SignExtend`, `ZeroExtend`, `Truncate`, `Reinterpret`| existing form param `targetType`                               |
| `ConstI`                                             | constant `HairType.INT`                                        |
| `ConstL`                                             | constant `HairType.LONG`                                       |
| `ConstF`                                             | constant `HairType.FLOAT`                                      |
| `ConstD`                                             | constant `HairType.DOUBLE`                                     |
| `Null`                                               | constant `HairType.REFERENCE`                                  |
| `New`, `NewArray`                                    | constant `HairType.REFERENCE`                                  |
| `ConstTypeInfo`, `TypeInfo`                          | constant `HairType.REFERENCE`                                  |
| `IsInstanceOf`                                       | constant `HairType.INT` (boolean as int)                       |
| `LoadField`, `StoreField`                            | `field.type` (chained off the form's `Field`)                  |
| `LoadGlobal`, `StoreGlobal`                          | `field.type` (chained off the form's `Global`)                 |
| `InvokeStatic`, `InvokeVirtual`                      | `function.resultHairType`                                      |
| `UnitValue`, `NoValue`                               | `HairType.VOID` (TBD — keep an eye out for callers that assume non-VOID) |
| `Use`                                                | n/a — this is effectively a control node already (`BlockBody`), not a value producer |
| `Param`                                              | **deferred** (dummy / not declared in v1)                      |
| `Neg`, `Not`                                         | **deferred** (not declared in v1)                              |
| `Catch`                                              | **deferred** (WIP)                                             |

Notes:

- For nodes where multiple inherited interfaces / parents could each declare
  `opType` differently, the most specific declaration wins. The generator
  already has a notion of "most specific declaration" via `superDecl(...)`; the
  `opType` rule should hook into the same machinery.
- For `Store` family (`Store`, `StoreField`, `StoreGlobal`): these don't produce
  a value in the usual sense; their `opType` still describes the *operation*
  (the width / field type being stored). Keep them as `ValueNode`-bearing for
  consistency, or split them off — the implementer's call. If `valueType` ever
  needs to disagree with `opType` for stores (probably VOID for stores), that's
  the place to deviate.

## `ValueNode` interface

- Introduce `interface ValueNode : Node { val opType: HairType }` (location: in
  the hand-written part of `ir/src/commonMain/kotlin/hair/ir/nodes/`, alongside
  `Node`).
- In the DSL, mark interfaces / abstract classes / nodes as value-producing.
  Most naturally this is a flag on the DSL builder (e.g. `node(... valueNode =
  true)`, or by having the relevant interfaces extend a built-in `ValueNode`
  interface in the DSL). Pick whichever fits the existing DSL idioms best.
- Control-flow interfaces / classes (`ControlFlow`, `Controlling`, `Throwing`,
  `BlockExit`, `Projection`, `Controlled`, `BlockBody`, `BlockBodyWithException`,
  `BlockEnd`, `Unreachable`, `BlockEntry`, the `If` family, `Return`, `Goto`,
  `Throw`, `Unwind`, `TrueExit`, `FalseExit`) do *not* implement `ValueNode`.
- Some nodes are both control-flow (e.g. `BlockBody`-derived) *and* produce a
  value — e.g. `New`, `NewArray`, `InvokeStatic`, `InvokeVirtual`, `Load`,
  `Store`, the var ops. These nodes do implement `ValueNode` in addition to
  their control-flow markers. Verify the generator can emit a class with two
  unrelated marker interfaces.

## `valueType`

- Hand-written extension on `ValueNode`. Simplest v1 implementation:

  ```kotlin
  val ValueNode.valueType: HairType get() = opType
  ```

  with a per-class override mechanism (a `when` over the sealed hierarchy, or
  an open property the generator emits, or a dedicated visitor). Pick what
  feels least likely to drift.
- For the LLVM-lowering case, `Phi.valueType == Phi.opType` is enough.
- Out of scope for v1 but worth keeping the shape friendly to: a Phi whose
  inputs are all the same constant could later report a `valueType` narrower
  than its declared `opType`.

## Generator extension

The DSL (`native/hair/ir/generator/src/main/kotlin/hair/ir/generator/`) needs
a way to declare the `opType` source for each value-producing element. Sketch:

- Add a small "op-type rule" concept in `toolbox/Model.kt`:
  - `OpTypeRule.FromFormParam(name: String)` — emits `<formParam>`.
  - `OpTypeRule.FromFormParamChain(formParamName: String, accessor: String)` —
    emits `<formParam>.<accessor>` (e.g. `field.type`,
    `function.resultHairType`).
  - `OpTypeRule.Constant(expr: String)` — emits a constant expression
    (`HairType.INT`, etc.).
  - `OpTypeRule.Inherited` (implicit) — the parent declaration is used.
- Add corresponding DSL surface in `toolbox/ModelDSL.kt`:
  - `Element.opType(...)` — accepts one of the rule shapes. Probably with a
    couple of overloads for ergonomics: `opType(formParam: FormParam)`,
    `opType(formParam: FormParam, chain: String)`, `opType(constant: String)`.
- The DSL marks the element as a value node when an `opType` rule is provided,
  *or* when the element inherits one (via `ValueNode` membership).
- The generator (`toolbox/Generator.kt`) emits:
  - `override val opType: HairType get() = <rule>` on concrete node classes.
  - `abstract val opType: HairType` (or `val opType: HairType` on interfaces)
    where appropriate to satisfy the `ValueNode` contract.
  - Adds `ValueNode` to the supers list for elements that participate.

Keep the rule shapes minimal — just enough to express the table above. Resist
the temptation to add control-flow constructs, conditional rules, etc.; if a
rule turns out to be inexpressible, that's a sign we should look at it
separately.

## Verification

After implementation:

- `Session.allNodes<ValueNode>().forEach { it.opType }` should not throw for
  any node *except* the deferred set (Param, Neg, Not, Catch).
- The existing test suite must continue to pass without modification.
- Add a smoke test that constructs nodes of each value-producing kind and
  asserts the expected `opType`.

## Open items to revisit

- `Param.opType`: pick (a) form param vs. (b) Session lookup.
- `Neg.opType` / `Not.opType`: tied to settling the supported arithmetic
  widths.
- `Store*.valueType`: do stores have a meaningful produced value, or should
  they report VOID and not implement `ValueNode` at all?
- Whether to fold the per-class constants (`ConstI` etc.) into a single
  `Const(type, value)` node — orthogonal to this change but interacts with it.
- `Cmp` result type: today `Cmp.type` (the form param) is the *operand* type;
  `opType` reflects that. When the lattice grows, `valueType` for `Cmp` should
  be a refined "INT in {0,1}" rather than just INT.
