# Why the light stub path allocates more than the AST path

Context: [light-stub-conversion.md](light-stub-conversion.md). On the IntelliJ monorepo corpus (209,479 files, 723 MB), per full pass:

| Path             | Time    | Allocated |
|------------------|---------|-----------|
| AST stub build   | ~30.8 s | ~33.7 GB  |
| Light stub build | ~25.8 s | ~43.5 GB  |

The light path is faster but allocates ~10 GB (~30 %) more. Only the total per pass was measured; the allocation was not split
into parse vs walk. The explanation below comes from the CPU profile (~10 % of light time in `getChildren` during the walk) and
from reading the platform code. A JFR allocation profile should be the first step to confirm it.

## Building is cheaper on the light path, reading is more expensive

- **Parsing.** Both paths run the same `PsiBuilderImpl` and produce the same `StartMarker`/`DoneMarker` production list. The AST
  path then materializes it into `CompositeElement`/`LeafElement` nodes. The light path stops at the markers. So the light parse
  allocates less (and is faster: 11.9 s vs 18.5 s parse-only).
- **Reading.** AST nodes are allocated once and linked (`firstChildNode`/`treeNext`), so walking and repeated child lookups allocate
  nothing. A `FCTSBackedLighterAST` stores no child lists, so every `getChildren` recomputes them and allocates. Stub building
  reads the same node many times:
  - `LightStubBuilder`'s walk, which also calls the skip hook per child;
  - the stub factory (`firstChildOfType`, `hasModifier`, `nodeText`, name/type readers);
  - sometimes parent fqName lookups.

  So light allocation grows with the number of *reads*, AST allocation with the number of *nodes*.

## What one `getChildren` allocates

Sources in the monorepo (`~/work/dev`):

- `community/platform/core-api/src/com/intellij/lang/FCTSBackedLighterAST.java`, `getChildren`:
  - a `Ref<LighterASTNode[]>`;
  - a `LighterASTNodeList` wrapper (`AbstractList` over the array).
- `community/platform/core-impl/src/com/intellij/lang/impl/PsiBuilderImpl.java`, `MyTreeStructure.getChildren`:
  - a fresh `LighterASTNode[10]`, grown with `Arrays.copyOf` (`ensureCapacity`). It is handed out and `nodes = null` is reset, so
    it is never reused;
  - a new token node for **every token child, on every call** (`insertLeaf`): `SingleLexemeNode` from `myLexemePool`,
    `TokenRangeNode` (collapsed markers) from `myRangePool`;
  - production children (`StartMarker`s) are not allocated: they are already linked via `myFirstChild`/`myNext`.
- The token pools are `LimitedPool`s that are only refilled by `MyTreeStructure.disposeChildren`. `FCTSBackedLighterAST` never
  calls it (it can't know when the caller is done with the list), so through `LighterAST` the pools only ever allocate.
- Consequence for identity: `Token` doesn't override `equals`/`hashCode`, so the same token has a *different* object on each
  `getChildren` call. Anything keyed by node identity only works for `StartMarker`s and `LazyParseableToken`s.
- Lazy chameleons are fine: `LazyParseableToken` instances are cached in `builder.myChameleonCache` and their parse result in
  `myParsed`, so collapsed blocks are created and parsed once.
- `MyTreeStructure.toString(node)` returns `myText.subSequence(...)`, so text reads allocate too, but the same holds for parts of
  the AST path.

## What was already tried on the Kotlin side

- A children-caching `LighterAST` wrapper (`IdentityHashMap<LighterASTNode, List<LighterASTNode>>`) was **2 s slower**. Hashing and
  map growth cost more than the rebuilt lists saved, and token children could not be cache keys anyway (unstable identity).
- Point fixes in the factories helped instead: one `getChildren` for the modifier mask instead of ~50 `firstChildOfType`, and the
  package name taken from the file stub instead of re-walking the package directive.

Takeaway: a cache only pays off if it sits *in the node* (a field), not in a side map.

## Platform-side experiment ideas

Roughly in order of effort:

1. **Cache the children array on `StartMarker`.** Add `LighterASTNode[] myChildrenCache` (plus the count, or an exact-size array),
   fill it in `MyTreeStructure.getChildren`, and return it on later calls. Tokens then get a stable identity for free.
   `FCTSBackedLighterAST` can wrap the cached array without copying (it is already `@Unmodifiable`).
   Open questions:
   - Retained memory: a light tree with cached children is closer to a small AST. Stub building throws the tree away right away,
     but other light tree consumers may keep it longer.
   - Callers of the raw `FlyweightCapableTreeStructure` that call `disposeChildren` (e.g. the incremental reparse diff in
     `PsiBuilderImpl`) must not recycle cached tokens into the pools. Either skip recycling when cached, or cache only for
     `FCTSBackedLighterAST`.
   - Opt-in vs always on: perhaps a flag on `MyTreeStructure`, enabled by `LightStubBuilder`/`FCTSBackedLighterAST`.
2. **Allocate leaves once per tree.** Instead of caching per parent, keep a per-builder `SingleLexemeNode[]` indexed by lexeme
   index, created lazily. This gives stable token identity and no per-call token allocation, at the cost of one array slot per
   lexeme.
3. **Cheaper call shape without caching.** Drop the `Ref` and `LighterASTNodeList` wrappers (e.g. let `MyTreeStructure` return an
   exact-size array that `FCTSBackedLighterAST` wraps via `Arrays.asList`), and size the array from a cheap child count instead of
   the 10 + `copyOf` growth. Smaller win, but no memory or semantics questions.
4. **Child-iteration API.** For the walk and `firstChildOfType`-style lookups, a visitor / first-child-next-sibling API on
   `LighterAST` would avoid materializing lists at all. This needs an API change and Kotlin-side adoption.

## How to measure

- `analysis/stubs/tests/org/jetbrains/kotlin/analysis/stubs/StubBuilderBenchmarkTest.kt` prints time and allocated bytes
  (`ThreadMXBean.getCurrentThreadAllocatedBytes`) for:
  - AST parse only;
  - light parse only;
  - AST stub build;
  - light stub build.
- Driven by the env vars `KOTLIN_STUB_BENCHMARK_ROOT` (e.g. `~/work/dev`), `KOTLIN_STUB_BENCHMARK_LIMIT` and
  `KOTLIN_STUB_BENCHMARK_VALIDATE`. Run with `--no-configuration-cache`, or changed env vars are silently ignored.
- To test a patched platform, the Kotlin build has to pick up locally built `core-api`/`core-impl` jars (e.g. via a local
  Maven/Gradle substitution of `com.jetbrains.intellij.platform:core-impl`).
- Baseline to beat: light stub build ~25.8 s / ~43.5 GB. Results stayed within ±1 % across 3 iterations after warmup.
- Before and after, take a JFR allocation profile (`jdk.ObjectAllocationSample`) to confirm where the ~10 GB comes from:
  `SingleLexemeNode`, `TokenRangeNode`, `LighterASTNode[]`, `LighterASTNodeList`, `Ref`.
