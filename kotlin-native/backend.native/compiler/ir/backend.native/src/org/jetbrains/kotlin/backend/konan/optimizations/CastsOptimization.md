# CastsOptimization

Removes type checks and casts whose outcome is statically known.

`BodyLoweringPass`, registered as `optimizeCastsPhase` in `NativeLoweringPhases.kt`, enabled when
`genericSafeCasts` is on (default: debug builds only).

Two passes over each body:

1. **Analyse** — `TypeCheckResolver` walks the IR and fills
   `typeCheckResults: Map<IrTypeOperatorCall, ALWAYS_SUCCEEDS | NEVER_SUCCEEDS | UNKNOWN>`.
2. **Rewrite** — `lower()` folds `is`/`as`/`as?` per those results: `is` → a constant, a succeeding
   `as` → `IMPLICIT_CAST`, a failing `as` → `throwClassCastException`, a failing `as?` → `null`.
   The argument is kept when it isn't pure (`keepingSideEffectsOf`).

## Predicates

At each IR node the analysis holds a **predicate**: what is known to be true on the paths reaching it.

- `LeafTerm` — `SimpleTerm.Is(v, C)` (`v is C`), `SimpleTerm.IsNull(v)` (`v == null`), or `ComplexTerm`
  for anything opaque (an unknown call). Terms live in `leafTerms`; a term index plus a polarity is one
  bit in a `CustomBitSet` (`index setTo true/false` → `bitIndex`).
- `Predicate` — CNF: `Conjunction(List<Disjunction>)`, plus `Empty` (⊤, nothing known) and `False` (⊥,
  unreachable). `Predicates.and/or/invert` keep it in CNF and drop subsumed disjunctions.

A type check is folded when `predicate & (v !is T)` is `False` (always succeeds) or `predicate & (v is T)`
is `False` (never succeeds) — see `tryOptimizeTypeCheck`. Contradiction only, not a full SAT solve, so it
under-approximates.

Two derived shapes carry per-outcome knowledge out of subexpressions:
`BooleanPredicate(ifTrue, ifFalse)` and `NullablePredicate(ifNull, ifNotNull)`. `buildBooleanPredicate`
pattern-matches `&&`, `||`, `!`, `==` (`matchAndAnd`, `matchOrOr`, `matchNot`, `matchEquality`) and
`?.` (`matchSafeCall`) so control flow expressed as `IrWhen` is understood as logic.

`ComplexTerm`s are dropped (assumed true) once they can no longer matter, to keep predicates small —
`optimizeAwayComplexTerms`, gated on the term's `depth` so only terms local to the current merge point
go. See the long comment on that function for why the timing matters.

## Variable aliases

`VisitorResult` is `(predicate, variable)`: the predicate after evaluating the node, and — if the node's
value came from a variable — which value declaration to attribute facts to.

`variableAliases: IrVariable -> IrValueDeclaration` maps a mutable variable to the value it currently
holds, an SSA-like stand-in so that reassignment doesn't invalidate earlier facts. Targets are
**phantom variables** (`createPhantomVariable`, named `x$0`, `x$1`, …) pinned to an IR node, so repeated
analysis of the same code yields the same phantom. `visitCall` does the same for trivial `val` getters,
giving `recv.prop` a phantom so facts can be attributed to a property.

> **Invariant: an alias target is never itself aliased** — `variableAliases` is a depth-1 forest.
> `buildIsSubtypeOfPredicate` resolves exactly one level, while `buildNullablePredicate` and
> `buildBooleanPredicate` resolve transitively; a longer chain makes those disagree, and a cycle makes
> the transitive ones recurse forever (KT-88316).

## Control flow

`mergeControlFlow(irElement) { cfmpInfo -> ... }` brackets any merge point (`IrWhen`, `IrReturnableBlock`,
loop exits). Each incoming path calls `cfmpInfo.merge(result)`, which ors the predicates and merges the
alias maps; a variable with conflicting aliases gets a fresh phantom — a phi node. A phi keeps a single
alias (`phiNodeAlias`) only when every path agreed and no non-variable value flowed in — a phi over
several values is too hard to express as a predicate.

`upperLevelPredicates` is a stack of enclosing predicates; `getFullPredicate` ands the stack from a given
level to get everything known at a point.

Loops run to a fixpoint. `visitWhileLoop` rewrites `while (c) B` to `if (c) { do B while (c) }` and
delegates to `handleDoWhileLoop`, which re-analyses the body up to `MAX_LOOP_ITERATIONS` until the
predicate and the alias map at the loop start stop changing. Values are pinned to IR nodes so successive
iterations produce the same terms and the fixpoint can converge; exit predicates from all iterations are
ored together to stay sound (see the comment in `handleDoWhileLoop`).

`getValueMergedVariableAliases` handles the flip side: a read whose alias differs between iterations
cannot return either alias, so it is pinned to `multipleValuesMarker` and the read falls back to a
phantom of its own — stable across iterations, and unaliased, so the invariant above holds.

`visitTry` conservatively forgets every variable changed in a `try`/`catch` clause, since the clause may
throw at any point.

## Bail-outs

`DivergingAnalysisError` is thrown on a predicate over `MaxSize`, nesting over `MAX_LOOPS_DEPTH`, or a
loop that hasn't converged in `MAX_LOOP_ITERATIONS`. `lower()` catches it and skips the body — giving up
is always safe. Anything else escaping the analysis is a compiler crash.

Set `-Xverbose-phases=OptimizeCasts` to get the `context.log` trace: predicates at each step, plus the
`variable -> alias` map at every loop iteration.
