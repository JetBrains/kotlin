# Experiment: Speed up lowerings with a pre-built IR element index

## Context

Profiling Kotlin/Native compilation on Ktor (with async-profiler `-e cpu`, no safepoint bias) shows `runAllLowerings` consuming **~33%** of total compile time. Each lowering inside `runAllLowerings` currently re-traverses the whole IR tree per fragment per phase, even when it only cares about a small subset of node types.

**Hypothesis**: pre-building an index of the IR elements lowerings target — once per fragment, then kept fresh across multiple buckets — lets each lowering iterate the index directly instead of walking the tree. For lowerings that match a sparse subset of nodes, this should reduce visitor dispatch overhead and CPU-cache pressure.

**Guinea pig**: the three consecutive string-concatenation lowerings (`flattenStringConcatenationPhase` → `stringConcatenationPhase` → `stringConcatenationTypeNarrowingPhase`, positions #48–50 in [LOWERINGS.md](kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/driver/phases/LOWERINGS.md)). They target `IrStringConcatenation` and a few specific `IrCall` patterns — sparse in real code, ideal for testing whether the index pays off.

## Design decisions (locked in)

| Decision | Choice |
|---|---|
| **Index shape** | Multi-type, keyed by IR class: `Map<KClass<out IrElement>, MutableList<IrElement>>` |
| **Indexed types (for this experiment)** | `IrStringConcatenation` and `IrCall` — the union of types consumed by the three lowerings |
| **Index scope** | Per-fragment (one index per `BackendJobFragment`'s `IrModuleFragment`); applied across multiple buckets, not just string-concat |
| **Freshness model** | Lowerings explicitly notify the index on mutation (`added` / `removed` / `replaced`) |
| **Backend strategy** | Native-only forks of the lowerings; common classes in `compiler/ir/backend.common/` unchanged |
| **Naming convention** | `Native*` prefix in `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/lower/` |
| **A/B measurement** | User runs their existing asprof script against Ktor on baseline vs. experiment branch |

## What each lowering actually consumes

Verified by reading the implementations:

| Lowering | Element types inspected | Entry method |
|---|---|---|
| `FlattenStringConcatenationLowering` | `IrStringConcatenation`, `IrCall` (filtered: `String.plus`, `Any.toString`, `Any?.toString`) | `visitExpression(IrExpression)` |
| `StringConcatenationLowering` | `IrStringConcatenation` | `visitStringConcatenation(IrStringConcatenation)` |
| `StringConcatenationTypeNarrowing` *(already Native)* | `IrCall` (filtered: appendable functions) | `visitCall(IrCall)` |

Union: **`IrStringConcatenation` + `IrCall`**. The index holds all instances of these two types per file; the lowerings apply their own filters when iterating.

## Files to modify

| File | Change |
|---|---|
| `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/lower/IrElementIndex.kt` *(new)* | Define `IrElementIndex` data structure, builder visitor, mutation API. |
| `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/NativeGenerationState.kt` | Add a nullable `irElementIndex: IrElementIndex?` field for per-fragment storage. |
| `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/lower/NativeFlattenStringConcatenationLowering.kt` *(new)* | Fork of `FlattenStringConcatenationLowering`; iterates index instead of walking IR; notifies index on mutation. |
| `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/lower/NativeStringConcatenationLowering.kt` *(new)* | Fork of `StringConcatenationLowering`; same pattern. |
| `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/lower/StringConcatenationTypeNarrowing.kt` | Modify in-place — already Native-only; switch from `visitCall` traversal to index iteration; notify index. |
| `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/driver/phases/NativeLoweringPhases.kt` | Replace the three string-concat phase declarations with index-aware variants; split `getLoweringsAfterInlining()` into pre-string-concat / string-concat / post-string-concat sections. |
| `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/driver/phases/TopLevelPhases.kt` | In `runAllLowerings`, after `constEvaluationPhase` and before the post-inlining bucket starts, build the index per fragment; pass it through `NativeGenerationState`. |

## `IrElementIndex` shape

Backing each indexed type is an **`IndexBucket`** — an `ArrayList` paired with an `IdentityHashMap<T, Int>` for O(1) swap-remove. This pattern gives O(1) `add`/`remove`/`replace` with dense, cache-friendly iteration. Identity equality (default `Object` `hashCode`/`equals`) matches IR element identity — no risk of accidentally collapsing structurally-equal nodes.

```kotlin
class IndexBucket<T : IrElement> {
    private val items = ArrayList<T>()
    private val indexOf = IdentityHashMap<T, Int>()

    fun add(e: T) {
        indexOf[e] = items.size
        items.add(e)
    }

    fun remove(e: T) {
        val i = indexOf.remove(e) ?: return
        val lastIdx = items.size - 1
        if (i != lastIdx) {
            val last = items[lastIdx]
            items[i] = last
            indexOf[last] = i
        }
        items.removeAt(lastIdx)
    }

    fun forEachSnapshot(action: (T) -> Unit) {
        val n = items.size
        for (i in 0 until n) action(items[i])
    }
}

class IrElementIndex private constructor(
    @PublishedApi internal val buckets: Map<KClass<out IrElement>, IndexBucket<*>>
) {
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : IrElement> forEach(noinline action: (T) -> Unit) {
        (buckets[T::class] as? IndexBucket<T>)?.forEachSnapshot(action)
    }

    fun added(element: IrElement)       // dispatch to the right bucket via element::class
    fun removed(element: IrElement)
    fun replaced(old: IrElement, new: IrElement)  // sugar over removed + added

    companion object {
        /** The only way to obtain an `IrElementIndex`. Walks `irFile` once and dispatches
         *  each visited element to the bucket of its `::class`, if that class is in `types`. */
        fun buildFor(irFile: IrFile, types: Set<KClass<out IrElement>>): IrElementIndex {
            val buckets: Map<KClass<out IrElement>, IndexBucket<*>> =
                types.associateWith { IndexBucket<IrElement>() }
            irFile.acceptVoid(object : IrVisitorVoid() {
                override fun visitElement(element: IrElement) {
                    @Suppress("UNCHECKED_CAST")
                    (buckets[element::class] as? IndexBucket<IrElement>)?.add(element)
                    element.acceptChildrenVoid(this)
                }
            })
            return IrElementIndex(buckets)
        }
    }
}
```

- **Stricter constructor contract**: `IrElementIndex` cannot be instantiated directly — only via `IrElementIndex.buildFor(file, types)`. This guarantees the index is always in a fully-built state when handed to a lowering; there is no "empty / uninitialized" window.
- The factory does ONE walk per `IrFile`, dispatching each visited node into its type's bucket.
- `forEach<T>` iterates the bucket via index (not via an iterator), so mid-iteration `removed`/`added` is safe — swap-remove may move a not-yet-visited element into a slot we've already passed (it gets skipped, which is fine since we're iterating a snapshot of pre-existing nodes) and append a new element after the current iteration window (also skipped — that's the desired semantic, the new node is dealt with by a later phase or rebuild).
- `ArrayList` grows dynamically; no max size or saturation logic needed.
- Mutation notifications keep the index consistent for the next lowering in the bucket.
- `@PublishedApi internal val buckets` is required so the `inline fun forEach` can access it from call sites; it remains effectively private to the module.

## Implementation steps

1. **Add `IrElementIndex.kt`** — data structure + builder visitor that walks an `IrFile` once and populates buckets for the requested `KClass` set.
2. **Add field to `NativeGenerationState`** — `var irElementIndex: IrElementIndex? = null`.
3. **Split `getLoweringsAfterInlining()` in `NativeLoweringPhases.kt`**:
   - `getLoweringsAfterInliningPreStringConcat(): LoweringList` — entries before `flattenStringConcatenationPhase`.
   - `getStringConcatLoweringsIndexed(): LoweringList` — the three Native* variants that consume the index.
   - `getLoweringsAfterInliningPostStringConcat(): LoweringList` — entries from `defaultParameterExtentPhase` onward.
4. **In `runAllLowerings`**, replace the single call to `state.context.config.getLoweringsAfterInlining()` with:
   - run pre-string-concat lowerings
   - per fragment: `state.irElementIndex = IrElementIndex().apply { build(file, setOf(IrStringConcatenation::class, IrCall::class)) }` for each file
   - run indexed string-concat lowerings
   - clear / null out `irElementIndex`
   - run post-string-concat lowerings
5. **Implement `NativeFlattenStringConcatenationLowering`**:
   - Pull from `NativeGenerationState.irElementIndex` (passed via the lowering's context).
   - Iterate `index.get<IrStringConcatenation>()` and `index.get<IrCall>()` (with the same `isStringPlusCall` / `isToStringCall` filters as the original).
   - When replacing an `IrCall(String.plus)` with an `IrStringConcatenation`, call `index.replaced(oldCall, newConcatenation)`.
   - Preserve correctness with the existing `collectStringConcatenationArguments` recursion — that recursion is *into* the matched node, not over the whole file, so the index doesn't change its behavior.
6. **Implement `NativeStringConcatenationLowering`**:
   - Iterate `index.get<IrStringConcatenation>()`.
   - Each replacement (StringBuilder-based form) calls `index.removed(oldConcatenation)`. New `IrCall`s synthesized in the StringBuilder pattern should be added via `index.added(...)` only if subsequent in-bucket lowerings need to see them. (For this experiment: `StringConcatenationTypeNarrowing` *does* care about `IrCall`s — so yes, notify on added.)
7. **Modify `StringConcatenationTypeNarrowing`**:
   - Iterate `index.get<IrCall>()` with the existing appendable-call filter.
   - On replacement, notify the index.
8. **Wire it up in `NativeLoweringPhases.kt`** — replace the three phase val declarations with Native* variants; update the prerequisite sets.
9. **Build, run focused tests** (string-concat codegen tests) to confirm semantics unchanged.
10. **User measures**: runs their asprof script against Ktor on baseline (master) vs. experiment branch; compares frames under `runAllLowerings`.

## Risks and gotchas

- **`IrCall` is dense**. Indexing every `IrCall` in a file gives a large list (probably orders of magnitude larger than `IrStringConcatenation`). The per-iteration filter cost in `NativeFlattenStringConcatenationLowering` (`isStringPlusCall`, `isToStringCall`) may eat the savings from skipping tree traversal. If the experiment shows no win, the next step is to either filter at index-build time (only index `IrCall`s where `symbol.owner.name == PLUS || symbol.owner.name == TO_STRING`) or narrow the indexed type set.
- **Mutation correctness**. The "explicit notify" contract means any missed `added`/`removed`/`replaced` call leaves the index stale. Subsequent lowerings then either miss work or operate on dead nodes. Audit each mutation site in the three lowerings carefully.
- **Index iteration vs. mutation in the same lowering**. A lowering iterating `index.get<IrStringConcatenation>()` while mutating must either iterate a copy or use `replaced` semantics that update the list in place. The naive `forEach` will fail on `ConcurrentModificationException`.
- **`FlattenStringConcatenationLowering` is `IrElementTransformerVoid`**, walking bottom-up. The original behavior relies on `transformChildrenVoid` recursion to handle nested concatenations. The indexed variant gets a flat list — order of processing matters if outer concatenations contain inner ones the lowering also rewrites. Confirm processing order is correct (probably: process inner before outer, or trust that `collectStringConcatenationArguments` flattens recursively from any starting point).
- **Cross-fragment state**. `NativeGenerationState` is per-fragment. Index is per-file but stored on the generation state. Multiple files in one fragment: either one index spanning all files (build once across all files of the fragment), or per-file rebuild on each lowering's `lower(IrFile)`. Latter is simpler; the former amortizes the build cost across files but complicates the "freshness across buckets" goal.

## Verification

**Correctness**:
- Build with `./gradlew :compiler:test --tests "*StringConcatenation*"` and `./gradlew :kotlin-native:backend.native:test --tests "*StringConcat*"` — must pass on the experiment branch the same as master.
- Compile and run a small synthetic file that exercises `String.plus`, `Any?.toString`, nested concatenations, the StringBuilder fast-path — diff IR dumps (or compiled output) before/after.
- Run the broader Native test suite for any string-related test to catch regressions.

**Performance** (user does this):
- asprof Ktor build on master → baseline flamegraph.
- asprof Ktor build on experiment branch → experiment flamegraph.
- Compare time spent in `flatten*`, `stringConcat*`, `stringConcatenationTypeNarrowing` frames. Also note: time spent in the new `IrElementIndex.build` should be smaller than the time saved across the three lowerings.
- If the win is < ~0.5% wall time, the experiment doesn't justify generalizing the index to other lowerings. Document the finding either way.

## Open follow-ups (not part of this experiment)

- Filtering at index-build time (e.g., only `IrCall` to `plus`/`toString`).
- Generalizing the index to span more lowerings (e.g., bucket B23 — five cast/operator phases).
- Considering a different storage location (per-`IrFile` attribute vs. per-`NativeGenerationState`).
- Reusing the index across larger spans of `runAllLowerings`, not just the string-concat triple.
