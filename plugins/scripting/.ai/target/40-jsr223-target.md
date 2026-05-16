# Target — JSR-223

Two work items: (1) bindings, (2) remote compilation scenario.

## Bindings

### Problem

`javax.script.Bindings` maps name → value (Java level). When the user evaluates a script with bindings, those names must be accessible as variables inside the script.

K1 path: bindings were injected as **provided properties** on `LazyScriptDescriptor` / `FirScript`. K2 script compilation reuses the same mechanism via `FirScript.parameters`.

K2 REPL path: snippets compile as `FirReplSnippet`, not `FirScript`. Snippet shape is:
- `FirRegularClass` (the snippet's body wrapper class)
- `$$eval` function with implicit receivers as parameters
- Statements live in eval body

Bindings need to land **somewhere accessible to user code in the snippet**. Four candidate designs:

### Option A — snippet class ctor params

Bindings become fields on the snippet wrapper class, initialised via ctor params.

- **Plug-in points**: `FirReplSnippetConfiguratorExtensionImpl` adds fields + ctor params on the snippet class. `Fir2IrReplSnippetConfiguratorExtensionImpl` translates. `K2ReplEvaluator` passes binding values at instantiation.
- **Pros**: mirrors script-side `providedProperties` mechanism; binding identity stable per-snippet.
- **Cons**: each snippet instance carries the whole binding map; rebinding requires a new snippet instance.

### Option B — `$$eval` function params

Bindings become local params on `$$eval`.

- **Plug-in points**: `FirReplSnippetConfiguratorExtensionImpl.configureEvalBody` adds params; eval signature widens.
- **Pros**: simplest plumbing; `$$eval` already takes implicit receivers.
- **Cons**: rebinding mid-session changes the eval signature → cross-snippet references break. Identity is per-call, not per-snippet.

### Option C — state object fields (sketch)

`IrReplSnippet.stateObject` is the IR-level shared-state hook. Use it to carry bindings.

- **Plug-in points**:
  - `K2ReplCompiler` creates / threads a "state object" for the REPL session; binding map lives there.
  - `Fir2IrReplSnippetConfiguratorExtensionImpl` ensures the state object has a property per binding (lazy, generated as bindings appear).
  - `ReplSnippetsToClassesLowering` already plumbs `stateObject` into the generated snippet class; emit getter calls in `$$eval` for binding references.
  - **Resolve EP** (`FirReplSnippetResolveExtensionImpl.getSnippetScope`) extends the per-snippet scope with synthetic properties matching the binding map at compile time.
- **Pros**: shared across snippets by construction; rebinding is "set the field" not "rebuild signature"; matches the REPL "running state" semantics.
- **Cons**: state object lifecycle gets entangled with binding lifecycle; needs versioning if bindings can be added/removed between snippets; more plumbing than A or B.

### Option D — implicit snippets via refinement-DSL callback (recommended)

Treat bindings as **REPL data** handled by the script *definition*, not the compiler. Extend the public refinement DSL with a callback that returns optional **implicit snippets** to compile + eval before the user's current snippet. The REPL harness runs them transparently. The actual logic (binding diffing, declaration emission, anything else a definition wants to inject pre-snippet) lives in the definition's callback.

This makes the mechanism generic — JSR-223 bindings, magic commands, kernel-side prelude rewrites, parameter injection, IDE-driven "before each cell" rewrites — all become instances of the same primitive.

#### Public API addition (sketch)

`libraries/scripting/common/api/scriptCompilation.kt` gains:

```
typealias InferImplicitSnippetsBeforeHandler =
    (ScriptConfigurationRefinementContext) -> ResultWithDiagnostics<List<SourceCode>?>

val ScriptCompilationConfigurationKeys.inferImplicitSnippetsBefore
    by PropertiesCollection.key<InferImplicitSnippetsBeforeHandler>()

fun ScriptCompilationConfiguration.Builder.inferImplicitSnippetsBefore(
    handler: InferImplicitSnippetsBeforeHandler,
) { ScriptCompilationConfiguration.inferImplicitSnippetsBefore(handler) }
```

Exact name TBD (`inferImplicitSnippetsBefore`, `prependSnippets`, `additionalSnippetsBefore` — pick during impl). Returning `null` or empty list = no implicit snippets. Multiple handlers compose (run in registration order, results concatenated).

#### Harness wiring

`K2ReplCompiler.compile(snippet, config)`:
1. Run the existing refinement chain on `snippet`'s config (`refineConfiguration { beforeParsing / onAnnotations / beforeCompiling }`).
2. **New step**: invoke `inferImplicitSnippetsBefore` handler chain. Collect returned `SourceCode`s in order.
3. For each implicit snippet: recursive compile through this same entry (callback can re-fire — needs depth/cycle guard).
4. Compile the user snippet against the post-implicit state.

`K2ReplEvaluator.eval(compiled, config)`:
- Eval each implicit snippet in order, then the user snippet.
- Hide implicit snippets from `Invocable.getInvocables()` and public history enumeration.

`FirReplHistoryProvider`:
- Implicit snippets register through normal `putSnippet` → subsequent user snippets see their declarations via the existing `FirReplHistoryScope`.
- May carry an "implicit" tag so callers wanting user-only enumeration can filter (see open questions).

The configurator + resolve EPs in `plugins/scripting/scripting-compiler` need no changes — implicit snippets are just snippets.

#### JSR-223 binding handler under this design

A `Jsr223BindingsConfigurator` (new, in `libraries/scripting/jvm-host` or a dedicated module) installs the callback on the JSR-223 `ScriptCompilationConfiguration`:

```
ScriptCompilationConfiguration {
    inferImplicitSnippetsBefore { ctx ->
        val current = ctx.hostConfiguration[jsr223Bindings]
        val prev    = ctx.collectedData[lastSeenBindings]
        val diff    = diffBindings(prev, current)
        if (diff.isEmpty()) ResultWithDiagnostics.Success(null)
        else ResultWithDiagnostics.Success(
            listOfNotNull(
                if (prev == null) bootstrapBindingsAccessorSnippet() else null,
                generateBindingDelegateSnippet(diff),
            )
        )
    }
}
```

- Bootstrap snippet declares the canonical accessor: `val bindings: ScriptBindings = ...` (typed wrapper over the JSR-223 `Bindings`). Emitted once.
- Diff snippet declares delegating properties for new/changed names: `val foo: Foo by bindings`. Existing names unchanged → not re-emitted.
- Removed names: open — declare nothing (delegate fails at access) or generate a fresh snippet that shadows with a "removed" marker. Decide during prototyping.

`KotlinJsr223ScriptEngineImpl` installs `Jsr223BindingsConfigurator` into the engine's `ScriptCompilationConfiguration`. No new compiler-internal EP. No bespoke logic in `K2ReplCompiler`.

#### Pros

- Most REPL-native: bindings appear as declarations in cells.
- Customization lives on the public refinement DSL (per [target/00-principles.md](00-principles.md) P8). Compiler infra stays agnostic.
- The same primitive unblocks **other** future definitions (magic commands, kernel preludes, parameter injection, etc.) — generic intermediate-cell mechanism for free.
- Composable: multiple definitions in the same engine each contribute their implicit snippets.

#### Cons

- Adds one public API entry — needs API-stability discipline.
- Extra snippets in history (bounded; one per change event).
- Needs cycle/depth guard in the harness recursion.
- Removal semantics for bindings need a tiny convention (see above).

### Recommendation

**Option D** — implement it. Customization lives in script definitions; the compiler/harness stays generic; the mechanism is a building block for future REPL features.

Options A/C remain fallbacks if D's harness work proves prohibitive. Option B is not recommended (identity instability across snippets).

### Cross-cutting

- Threading from engine to handler: `KotlinJsr223ScriptEngineImpl` exposes `Bindings` to the handler via `ScriptingHostConfiguration` (new key) or per-call `collectedData`. Pick during impl.
- Files likely touched (option D):
  - `libraries/scripting/common/api/scriptCompilation.kt` — new config key + DSL helper
  - `plugins/scripting/scripting-compiler/src/.../impl/K2ReplCompiler.kt` — harness recursion + history tagging
  - `plugins/scripting/scripting-compiler/src/.../impl/K2ReplEvaluator.kt` — eval ordering + invocable filtering
  - `libraries/scripting/jvm-host/.../KotlinJsr223ScriptEngineImpl.kt` — install `Jsr223BindingsConfigurator`
  - New: `libraries/scripting/jvm-host/.../Jsr223BindingsConfigurator.kt` (binding diff + snippet generation)
- For options A/C/fallbacks the FIR configurator + Fir2Ir + resolve impls would be touched — Option D avoids that entirely.

### Test plan

- `KotlinJsr223ScriptEngineIT.kt` already has binding tests under K1. Bring them in scope for K2.
- Add cross-snippet binding tests: bind → use → rebind → use again.
- If option D: assert that intermediate cells appear in `FirReplHistoryProvider.getSnippets()` and are correctly invisible to user-facing snippet enumeration.

## Remote (out-of-process) compilation

### Context

The current K1 daemon-based remote JSR-223 compilation **is in use** — at least one IntelliJ-side implementation relies on it. Reason: the IntelliJ process cannot host the Kotlin compiler in-process (IntelliJ-platform dependency conflict). The compiler-side IntelliJ-platform-dep cleanup will eventually unblock in-process hosting, but it's a long road. K1 retirement lands first, so the **current bridge will break before that** regardless of our choice. We'd prefer not to break it. So the goal is to design a remote-compilation path that survives K1 removal and aligns with K2 REPL infrastructure.

### Problem with the current shape

Today's daemon REPL keeps **compilation state in the daemon process**: `K2ReplCompilationState` carries `lastCompiledSnippet`, `sessionFactoryContext`, `moduleDataProvider`, `sharedLibrarySession`, plus the `FirReplHistoryProvider` is in-memory (`FirReplSnippetResolveExtensionImpl`). The daemon must persist that state across remote invocations, age it correctly, garbage-collect it, etc. This is fragile and expensive.

### Target: stateless snippet compilation

Move the state out of the compiler:
- Each compiled snippet is serialised as **class files + sidecar metadata** sufficient to reconstruct what subsequent snippets need to resolve against.
- The REPL compiler becomes **stateless**: every call takes (current snippet source, prior snippet artifacts) → (new snippet artifacts).
- The caller (IDE, remote client, BTA) owns the artifact set and the history order.

### Reconstruction sketch

EPs that need a storage-backed impl:

| EP | Today | Stateless-mode impl |
|---|---|---|
| `FirReplHistoryProvider` | In-memory `FirReplHistoryProvider` impl | Reads prior snippet symbols from deserialised class metadata + sidecar; ordered list is supplied by caller |
| `FirReplSnippetResolveExtension.getSnippetScope` | Reads in-memory `FirReplHistoryScope` | Builds a scope from reconstructed `FirReplSnippetSymbol`s |
| `FirReplSnippetResolveExtension.updateResolved` | Updates in-memory history | Writes sidecar metadata for the just-compiled snippet (consumed on next call) |
| `K2ReplCompilationState` | Cross-call session reuse | Fresh state per call; prior snippets enter as classpath + reconstructed FIR symbols |

What needs serialising per snippet:
- Bytecode (the snippet wrapper class with `$$eval` and any nested declarations) — already produced by `ReplSnippetsToClassesLowering`.
- `.kotlin_metadata` (class-level) — already produced; covers most member signatures.
- **Sidecar** for snippet-specific bits not captured by class metadata: snippet name, history index, declared `FirReplSnippetSymbol` shape, default-imports list, link to result property (`$$result`), state-object class reference (if option C is taken for bindings). Format TBD — JSON or compact binary; version-stable.

### Compatibility implications

- The current daemon-based bridge breaks when K1 + daemon REPL go away (steps 4 + 11 in [50-migration-plan.md](50-migration-plan.md)). There is **no smooth migration**; the IntelliJ consumer will need to switch to the new stateless protocol.
- Transport options for the new protocol:
  - **Build Tools API** — natural fit, but BTA's stable-API discipline may slow iteration. New op: `CompileReplSnippetOperation(prevArtifacts: List<Path>, snippetSource: SourceCode) → SnippetArtifact`.
  - **Direct embedded compiler** — once in-process hosting becomes feasible (post IntelliJ-platform-dep cleanup), the same stateless API works without IPC.
- The two transport options share the same compiler-side stateless core, so designing the core right is what matters.

### Open verification items

- Confirm `FirReplSnippetSymbol` + `getSnippetScope` actually work over reconstructed-from-class symbols. Possible blockers: nested class symbol reconstruction, default-import imports, snippet-receiver propagation. Prototype needed.
- Sidecar format: choose between JSON, protobuf, or hand-rolled binary; ensure version-stable.
- Performance: reconstructing FIR for N prior snippets on every call is O(N²) in the worst case; need caching strategy (LRU FIR cache on caller side?).

### Recommendation

**Design and prototype the stateless model now**, not later. The bindings work (above) and the stateless work can run in parallel — they touch overlapping but separable code. Output of the prototype: confirm/disconfirm that the existing EPs are sufficient, identify any required EP additions, and freeze the sidecar format.

## Parser path note

`K2ReplCompiler` (`plugins/scripting/scripting-compiler/src/.../impl/K2ReplCompiler.kt:351-359`) is hybrid: PSI for `KtFileScriptSource`, LT otherwise. JSR-223 embedders typically pass `KtFileScriptSource`, so the **default JSR-223 path keeps PSI alive today**. Tracked: **KT-83498**.

Implication: until KT-83498 lands, "fully PSI-free K2 scripting" is true for scripts but **not for JSR-223 / REPL**. Embedders wanting zero-PSI snippets must avoid `KtFileScriptSource` (or wait for KT-83498).

Once KT-83498 lands: align `K2ReplCompiler` with `ScriptJvmK2CompilerImpl` shape — accept `convertToFir` lambda; default to LT. This unifies the parser-agnostic seam across script and snippet pipelines.

## Sequence

1. Prototype + land K2 bindings (option D recommended; A/C as fallback) — restores feature parity with K1
2. **Prototype stateless remote compilation** (in parallel with 1) — confirm EPs are sufficient, freeze sidecar format
3. Land **KT-83498** — full LT path for `K2ReplCompiler`; align with `ScriptJvmK2CompilerImpl` shape (parser-agnostic seam)
4. Delete K1 fallback path in jvm-host (`legacyRepl*.kt`)
5. Delete daemon REPL (independent — see [30-embedding-target.md](30-embedding-target.md))
6. Once stateless prototype validates: design transport (BTA op vs direct in-process) and migrate IntelliJ consumer.
