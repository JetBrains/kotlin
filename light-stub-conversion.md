# Light Stub Builder Conversion

Converts Kotlin source stub building from the AST-based `DefaultStubBuilder` to the platform's
light-tree path (`LightLanguageStubDefinition` + `LightStubBuilder` + `LightStubElementFactory`),
so indexing builds stubs directly from the parser's `LighterAST` without materializing PSI/AST trees.

## What the change does

- `KotlinLanguageStubDefinition` implements `LightLanguageStubDefinition`; `parseContentsLight` reuses
  `KotlinLightParser` (the same light parser as the FIR light-tree frontend) and mirrors `KotlinParser`'s
  file-vs-script decision (extension + `isCompiled`).
- New `KtFileLightStubBuilder : LightStubBuilder` computes the file stub kind from the light tree:
  package name, script detection, facade fqName (incl. `@file:JvmName` / `@file:JvmPackageName` /
  `@file:JvmMultifileClass`), and the invalid-decompiled-file marker. `customStubBuilder` delegation is kept.
- All 37 stub factories (`compiler/psi/psi-impl/.../stubs/factories/`) implement `LightStubElementFactory`:
  each has a light `createStub(tree, node, parentStub)` / `shouldCreateStub` next to the PSI-based one.
  Shared readers live in `stubs/light/lightTreeUtils.kt` and `lightFqNames.kt`; every helper documents the
  exact PSI implementation it mirrors.
- Registration switched to `registerLightStubFactory` + `registerStubSerializer`
  (`registerStubSerializingFactory` rejects light factories by design).
- The old `KtFileStubBuilder` stays as the AST-based reference implementation — and the light builder
  *delegates to it* when the file's AST is already materialized (see insights below).
- `SourceStubsTestEngine` gained a permanent cross-check: every source stub test additionally rebuilds the
  stubs from an unparsed copy of the file through the real indexing path (`parseContentsLight`, asserting
  the AST is never expanded) and compares the rendered trees.

## Validation

- `:analysis:stubs:test` (3237 tests) and `:compiler:psi:psi-impl:test` green.
- Monorepo sweep (`StubBuilderBenchmarkTest`, `KOTLIN_STUB_BENCHMARK_*` env vars): **209,479** `.kt`/`.kts`
  files from the IntelliJ monorepo produce **byte-identical stub trees** via both builders (re-verified after the
  rebase onto KT-90421 and the lazy light parsing) — through both
  the tree-backed light view and the genuine FCTS indexing path. 0 mismatches, 0 failures.

## Performance

Rebased onto master with KT-90421 (the AST builder skips unparsed code blocks without stubbed declarations),
so the AST baseline below is the *new*, already faster one. 209,479 files, 723 MB of sources, 3 iterations
after warmup, all within ±1 %.

| Path (per full pass over the corpus)          | Time    | Allocated |
|-----------------------------------------------|---------|-----------|
| AST top-level parse alone (bodies stay lazy)  | ~18.5 s | —         |
| Light lazy parse alone                        | ~11.9 s | —         |
| **AST stub build** (lazy parse + skip + walk) | ~30.8 s | ~33.7 GB  |
| **Light stub build** (lazy parse + skip + walk) | ~25.8 s | ~43.5 GB  |

Light is **1.19× faster** than the current AST builder. It allocates ~30 % *more* in total (the parser-backed
light tree rebuilds child lists and token nodes on every `getChildren`), but all of it is short-lived garbage,
while the AST path leaves a materialized AST attached to the file.

History of the light path on this corpus: 44 s (eager light parse, before the rebase) → 29.5 s (lazy light
parse + light `skipChildProcessingWhenBuildingStubs`) → 25.8 s (hotspot fixes below). The AST path went
54 s → 32 s from KT-90421 and → 30.8 s from the shared factory lookup fix.

### Optimizations in this round

- **Lazy light parsing.** `parseContentsLight` parses with `createForTopLevel`, so blocks and lambdas are
  collapsed; `BLOCK` and `LAMBDA_EXPRESSION` element types implement `ILightLazyParseableElementType`, so any
  light-tree consumer still expands them transparently on `getChildren`.
- **Light `skipChildProcessingWhenBuildingStubs`**, mirroring the AST one: collapsed code blocks/lambdas whose
  tokens contain no `class`/`interface`/`object`/`fun` keyword are skipped. The keyword scan visits the
  builder's already-lexed token types (`LighterLazyParseableNode.accept`) instead of re-lexing.
- **Light contract pre-check**: a collapsed body is lexed (`StubBuildingLexing.mayHaveLegacyContract`) before
  being parsed to inspect its first statement.
- **AST `skipChildProcessingWhenBuildingStubs` on the light builder** delegates to `KtFileStubBuilder` — needed
  because binding stubs to a loaded AST (`calcStubTree`) walks the AST through the *builder's* skip hook; without
  it every body was expanded.
- Package name of the file taken from the file stub (walked up the stub chain) instead of re-walking the package
  directive for every declaration.
- Modifier mask computed from one `getChildren` call instead of ~50 `firstChildOfType` calls.
- Import fqName collected in one pass per dot level and joined once, instead of chained `FqName.child`.
- `KtStubElementFactories.get` uses an array indexed by `IElementType.index` instead of a hash map (also speeds
  up the AST path).
- Tried and reverted: a children-caching `LighterAST` wrapper — `IdentityHashMap` overhead outweighed the saved
  child-list rebuilds (+2 s).

### Remaining cost breakdown (light, JFR)

~42 % parsing/lexing, ~10 % walk `getChildren`, ~9 % import directives (`hasErrorElements` + name reads), the rest
spread across factories. Most of the remaining stub-side cost is `FCTSBackedLighterAST.getChildren` rebuilding
child lists, which is inherent to the platform API.

## Insights

1. **fqNames must be computed from the tree parent chain, not the stub chain.** The stub parent chain
   skips unstubbed parents (blocks), so a local function's stub hangs directly off the enclosing function's
   stub — deriving a qualified name from it would make locals look non-local. The light implementation
   mirrors `KtNamedDeclarationUtil` / `safeFqNameForLazyResolve` node-type by node-type.
2. **Walking a tree-backed light view is ~2.3× slower than walking the AST directly** (wrapper allocation
   per node). Hence the hybrid: light path for unparsed files (indexing), AST path when the tree is already
   materialized (`calcStubTree` on open files). The platform's `LightStubBuilder.FORCED_AST` thread-local
   must still be consumed by the light path, or it leaks into the next build.
3. **Parser recovery shapes leak into stub semantics.** Two bugs the test corpus caught: destructuring
   entries can be wrapped in `ERROR_ELEMENT`s (KT-58563), and a delegation call *without* a callee counts
   as an explicit delegation call (`isImplicit == false` requires a present-but-empty callee).
4. **`PsiFileFactory.createFileFromText(name, type, text)` force-parses the AST** (`markAsCopy = true`
   internally); benchmarking the unparsed path requires the 6-arg overload with `markAsCopy = false`.
   The first benchmark round silently measured an already-parsed corpus — the "light is slower" numbers
   were actually the tree-backed-view effect from insight 2.
5. **The new stub registry is wired via Analysis API XML**
   (`analysis-api-impl-base.xml`), not by the bare `KotlinCoreEnvironment` — stub-building tests and
   benchmarks must run inside the Analysis API test framework.
6. Equivalence of the two builders is now enforced three ways: committed `.stubs.txt` baselines, the
   per-test light-vs-AST cross-check in `SourceStubsTestEngine`, and the manual monorepo sweep.
7. **Env-driven benchmark runs need `--no-configuration-cache`**: the configuration cache serializes the test
   task's inherited environment, so a changed `KOTLIN_STUB_BENCHMARK_VALIDATE` is silently ignored.
8. **Making `BLOCK`/`LAMBDA_EXPRESSION` `ILightLazyParseableElementType` also changes IDE incremental reparse
   diffing** (`PsiBuilderImpl`'s comparator takes a separate branch for light-lazy types, as Java code blocks do).
