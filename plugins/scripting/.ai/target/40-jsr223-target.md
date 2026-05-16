# Target — JSR-223

> **When to consult**: JSR-223 bindings (Option D canonical home) or remote compilation design. For historic options A/B/C rationale see `40-jsr223-options-archive.md`.
> **Cache lifetime**: mutable-on-prototype
> **Last verified**: 2026-07-05 ("Direct on-daemon `ScriptEngine`" section updated — `DaemonReplCompiler` no longer uses any bespoke artifact codec at all: snippets compile as chained REPL snippets on the compiler's *regular* frontend/backend, priors are fed back purely via the classpath + their `ClassId`s, and a compiler-level bug (`KtScript.isReplSnippet` ignoring runtime marking once a stub exists) had to be fixed to make this work)

Two work items: (1) bindings, (2) remote compilation scenario.

## Bindings

### Problem

`javax.script.Bindings` maps name → value (Java level). When the user evaluates a script with bindings, those names must be accessible as variables inside the script.

K1 path: bindings were injected as **provided properties** on `LazyScriptDescriptor` / `FirScript`. K2 script compilation reuses the same mechanism via `FirScript.parameters`.

K2 REPL path: snippets compile as `FirReplSnippet`, not `FirScript`. Snippet shape is:
- `FirRegularClass` (the snippet's body wrapper class)
- `$$eval` function with implicit receivers as parameters
- Statements live in eval body

Bindings need to land **somewhere accessible to user code in the snippet**. Four candidate designs were considered (A/B/C archived in [`40-jsr223-options-archive.md`](40-jsr223-options-archive.md)); Option D is recommended.

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

Options A/C remain fallbacks if D's harness work proves prohibitive — see [`40-jsr223-options-archive.md`](40-jsr223-options-archive.md). Option B is not recommended (identity instability across snippets).

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
- **Sidecar** for snippet-specific bits not captured by class metadata: snippet name, history index, declared `FirReplSnippetSymbol` shape, default-imports list, link to result property (`$$result`), state-object class reference (if option C is taken for bindings — see [`40-jsr223-options-archive.md`](40-jsr223-options-archive.md)). Format TBD — JSON or compact binary; version-stable.

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

### Reusable BTA-backed `ScriptEngine` — landed 2026-07-02g

Now that the BTA transport (Q5d) is fully landed (daemon execution + genuine subprocess coverage), a first, reusable JSR-223 `KotlinJsr223JvmScriptEngineFactoryBase` implementation driving compilation entirely through `CompileReplSnippetOperation` exists: `KotlinJsr223BtaScriptEngineImpl` / `KotlinJsr223BtaScriptEngineFactory` in the new module `libraries/scripting/jsr223-bta` (`:kotlin-scripting-jsr223-bta`). Design/scope:

- **Not a `javax.script.ScriptEngineFactory` service** — no `META-INF/services` registration, no separate module to smoke-test it through real `ScriptEngineManager` lookup (yet). Callers (including this module's own tests, `KotlinJsr223BtaScriptEngineTest`) construct the factory directly. A dedicated `*-test` module mirroring `jsr223-test`'s `KotlinJsr223ScriptEngineIT` service-registration smoke test is future work if/when this engine is promoted.
- **Execution**: `ExecutionPolicy.WithDaemon` only (the op rejects `InProcess` by design, per the "Out-of-process-only finalised" Q5d note above).
- **State**: one engine instance == one REPL session; a small in-process `BtaReplSnippetSession` (own reimplementation, not a reuse of the `internal` `SnippetArtifactEvaluator`) incrementally defines + runs each new artifact's classes on a shared classloader, using the public `SnippetArtifactCodec`/`SnippetArtifactHeader` wire types.
- **Bindings/implicit receivers are out of scope for this first cut** — `$$eval` is invoked with no arguments; `CompileReplSnippetOperation` has no receiver-passing support at all yet. Adding it would need a new BTA option (analogous to `ADDITIONAL_CLASSPATH`) plus `BtaReplSnippetSession` support for instantiating/passing receiver values — not attempted here.
- **Gotcha found while wiring this up**: the daemon-side `-Xplugin` re-registration of the scripting compiler plugin (`CompileReplSnippetOperationImpl.createScriptingPluginServicesJar`) only works when the actual **shaded/embedded** `kotlin-build-tools-impl` jar (with the scripting-compiler classes relocated + bundled in) is on the daemon's compiler classpath — a plain/incomplete build of that jar (e.g. one built only to satisfy a compile-classpath resolution, not an actual packaging task) silently produces a `ClassNotFoundException` on the *unrelocated* plugin class name inside the daemon log, with the BTA op surfacing it as an empty-diagnostics `Failure`. Symptom looks like a missing-dependency bug; the actual fix is just making sure the full/shaded jar gets built (a stale/partial artifact from an unrelated task graph was the cause here, not a wiring bug) — see [iterations/2026-07-02g_jsr223-bta-engine.md](../iterations/2026-07-02g_jsr223-bta-engine.md).
- **Removed (2026-07-05, later same day)**: once the on-daemon example (below) migrated to the compiler's regular compilation-artifact pipeline, `AbstractScriptEvaluationExtension.compileReplSnippet` (the compile-only entry point `CompileReplSnippetOperationImpl` depended on) was deleted, leaving this whole module non-functional with no in-tree consumer. Per explicit instruction, the module (`libraries/scripting/jsr223-bta`, `:kotlin-scripting-jsr223-bta`), `CompileReplSnippetOperation`/`ReplSnippetCompilationResult` (BTA API), `CompileReplSnippetOperationImpl` (BTA impl), `SnippetArtifactCodec` (the wire envelope it alone needed), and `ReplSnippetCompilationTest` were all deleted outright rather than kept in a disabled/stubbed state — this section is kept as historical design record. Reimplementing BTA-backed REPL-snippet compilation on top of the same regular-pipeline architecture as the on-daemon example (see "Follow-up fix #9" below) is deferred to a future iteration.

### Direct on-daemon `ScriptEngine` (no BTA) — landed 2026-07-03

The BTA-backed engine above is difficult to embed into IntelliJ (BTA is a heavier dependency surface there). A second, BTA-free variant now exists as a **portable example**: `KotlinJsr223DaemonScriptEngineImpl` / `KotlinJsr223DaemonScriptEngineFactory` in `libraries/examples/scripting/jsr223-daemon` (`:examples:scripting-jsr223-daemon`) — deliberately placed under `libraries/examples/scripting` (not `libraries/scripting`, unlike the BTA sibling), since it's meant to be copied wholesale into the IntelliJ repo. Design/scope:

- **Same shape and same scope limitations as the BTA engine** (not a registered `javax.script.ScriptEngineFactory` service; `$$eval` invoked with no arguments — bindings/implicit receivers deferred), but the transport differs: it connects to the compile daemon **directly** via the daemon-client API (`CompilerId.makeCompilerId` + `KotlinCompilerRunnerUtils.newDaemonConnection` + a plain `CompileService.compile(...)` call), with **no BTA dependency and no new daemon RMI interface** — the snippet rides the same regular compile path that a `kotlinc` invocation would, delivered as a plain source-root file (not `-script`/`-expression` — see the follow-up fix below for why).
- **`DaemonReplCompiler`** (superseding an earlier from-scratch `DaemonReplSnippetSession`/`DaemonReplSnippetCompiler`-only compile loop, see "Follow-up fix #3" below) is a `kotlin.script.experimental.api.ReplCompiler<CompiledSnippet>` — the *only* substitution made on top of the stock REPL infrastructure that `KotlinJsr223ScriptEngineImpl` itself uses (`KotlinJsr223JvmScriptEngineBase` for state/eval-loop plumbing, an unmodified `K2ReplEvaluator` for running snippets). It decodes the daemon's wire artifact into a real `kotlin.script.experimental.jvm.impl.KJvmCompiledScript` (only the public `SnippetArtifactCodec`/`SnippetArtifactHeader`/`KJvmCompiledModuleInMemoryImpl` types are needed — the decode helpers used by the in-process `SnippetArtifactEvaluator` remain `internal` to `:kotlin-scripting-compiler`), so the stock `K2ReplEvaluator` can evaluate it exactly as an in-process-compiled snippet, including its existing cross-snippet classloader chaining.
- **Gotcha found while wiring this up**: the plain `:kotlin-compiler` project jar does **not** bundle the daemon's own main class (`org.jetbrains.kotlin.daemon.KotlinCompileDaemon`) — in `prepare/compiler/build.gradle.kts`, `:kotlin-daemon`/`:kotlin-daemon-client` are listed only under `distLibraryProjects` (copied as separate jars into `dist/kotlinc/lib/`), never `fatJarContents` (what's actually baked into the compiler jar). A direct (non-BTA, non-`dist/kotlinc`) daemon-client integration needs to assemble its own daemon-launch classpath explicitly — this module's `daemonCompilerClasspath` test configuration now includes `:kotlin-daemon` explicitly to fix this. See [iterations/2026-07-03_jsr223-daemon-engine.md](../iterations/2026-07-03_jsr223-daemon-engine.md).
- **Follow-up fix #1 (same day)**: source was originally passed via `-expression <source>` (CLI-argument-quoting-fragile, and semantically an eval-a-string entry point). Switched to writing the snippet to a temp file and passing `-script <path>` instead. This surfaced a real bug in **shared** scripting-compiler-plugin code: `compileReplSnippet` (`AbstractScriptEvaluationExtension.kt`) only re-wrapped the incoming source into a `StringScriptSource` when `repl-snippet-name` differed from the source's own name (a guard sized for `-expression`'s fixed `script.kts` naming); a same-named file-backed source slipped through unwrapped into `K2ReplStatelessCompiler`, which needs a real IntelliJ `LocalFileSystem`/VFS to resolve a file-backed source (never on the bare CLI/daemon classpath) → `NoClassDefFoundError`. Fixed by always re-wrapping into `StringScriptSource(snippet.text, explicitName ?: snippet.name)` in `compileReplSnippet`, regardless of name match — benefits any other caller of this compile-only entry point, not just this module. See the iteration file's addendum.
- **Follow-up fix #2 (same day)**: `-script <path>` was *still* delivered through `ScriptEvaluationExtension.eval()` — the same entry point `kotlinc script.kts` uses to *run* a script — so a snippet whose body throws at runtime risked the exception surfacing **inside the daemon**, not at the evaluator; `REPL_SNIPPET_COMPILATION_MODE` only short-circuits that entry *before* evaluation, which is a runtime check, not a structural guarantee. Fixed by moving snippet delivery **entirely off** `ScriptEvaluationExtension`, onto the regular JVM source-root pipeline (which has no evaluation code path at all): `ScriptingProcessSourcesBeforeCompilingExtension.processSources` (shared plugin code, already home to `-Xallow-any-scripts-in-source-roots` handling) now short-circuits on `REPL_SNIPPET_COMPILATION_MODE`, calling `compileReplSnippet` directly per source and returning `emptyList()` (the snippet is never handed to the regular frontend/backend). Three CLI flags are now required together: `-Xallow-any-scripts-in-source-roots` (accept the `.kts` at all), `-Xuse-fir-lt=false` (the extension hook only fires on the *PSI-based* source-collection path, not the default light-tree path — without this the snippet was silently accepted into nothing: no artifact, no diagnostic, exit code `0`; this flag is deprecated and the one part of this design most likely to need revisiting), and `-Xallow-no-source-files` (suppress the resulting "no source files" error). See the iteration file's second addendum for the full diagnosis path.
- **Follow-up fix #3 (2026-07-05, rebuild)**: the module was flagged as a from-scratch implementation that barely touched the existing K2 REPL infrastructure. Rebuilt on top of it instead: `KotlinJsr223JvmScriptEngineBase`'s `replCompiler`/`replEvaluator` properties were loosened from the concrete `K2ReplCompiler`/`K2ReplEvaluator` types to their interfaces (`ReplCompiler<CompiledSnippet>`/`ReplEvaluator<CompiledSnippet, KJvmEvaluatedSnippet>`) — the only shared-infra change needed, a pure widening. `KotlinJsr223DaemonScriptEngineImpl` now extends `KotlinJsr223JvmScriptEngineBase<DaemonReplState>` and drives an unmodified `K2ReplEvaluator`; the only substitution is the new `DaemonReplCompiler` in place of `K2ReplCompiler`. `DaemonReplSnippetSession` (the from-scratch reflection-based artifact-replay session) is gone — `K2ReplEvaluator` now does that job, including cross-snippet classloader chaining, for free.
- **Follow-up fix #4 (2026-07-05, later same day)**: after the rebuild, `DaemonReplSnippetCompiler` (the low-level daemon transport: CLI-argument building, daemon spawn/connect, `CompileService.compile(...)` call, message collection) and `DaemonReplCompiler` (the `ReplCompiler<CompiledSnippet>` wrapping it) were two separate classes for no remaining reason — `DaemonReplCompiler` was `DaemonReplSnippetCompiler`'s only caller, and the split was a leftover of the pre-rebuild design where `DaemonReplSnippetCompiler` used to be shared by both the compiler and the now-deleted `DaemonReplSnippetSession`. Merged `DaemonReplSnippetCompiler`'s transport logic (and its `NoOpCompilationResults`/`CollectingMessageCollector` helpers) directly into `DaemonReplCompiler` as private members; `DaemonReplSnippetCompilationResult` became a private nested `DaemonCompilationResult` (its `Success` case no longer needs to carry diagnostics — only `decodeCompiledSnippet` needs the artifact bytes on success). `DaemonReplSnippetCompiler.kt` was deleted; one class now owns the whole compile-request lifecycle.
- **Follow-up fix #5 (2026-07-05, later same day)**: flagged that `runDaemonCompile` connected to (and released from) the daemon on **every single snippet compile**, which is not how a daemon client is supposed to behave — with a zero shutdown delay this could let the daemon shut itself down between every snippet. Fixed by caching the daemon connection lazily in `DaemonReplCompiler` (created once, on the first `compile` call, via a double-checked-locked `getOrCreateConnection`) and reusing it for the compiler's whole lifetime; `DaemonReplCompiler` now implements `AutoCloseable` (`close()` releases the session once, letting the daemon's own idle-shutdown settings decide when to actually exit) plus a test-only `forceShutdownDaemon()` (hard `compileService.shutdown()`, mirroring `BaseDaemonSessionTest.stopDaemons()` in `compiler/daemon/daemon-tests`). Also replaced the ad-hoc `daemonRunFilesPath`/`daemonLogsPath`/`shutdownDelayMilliseconds` constructor parameters with nullable `daemonJVMOptions`/`daemonOptions`/`daemonLogOptions`, threaded from `KotlinJsr223DaemonScriptEngineFactory`'s constructor; `null` now reproduces exactly what `newDaemonConnection`'s own no-arg overload computes internally, rather than being re-synthesized inline. `KotlinJsr223DaemonScriptEngineImpl` gained `close()`/`forceShutdownDaemonForTests()` passthroughs, mirroring `KotlinJsr223BtaScriptEngineImpl.close()`.
- **Follow-up fix #6 (2026-07-05, later same day)**: flagged that fix #5's `@Volatile connection` + manual double-checked-locked `getOrCreateConnection` just reimplemented `kotlin.lazy`'s own default (`SYNCHRONIZED`) thread-safety — replaced with a `connectionLazy: Lazy<CompileServiceSession>` field/delegated `connection` property; `close()`/`forceShutdownDaemon()` now guard with `connectionLazy.isInitialized()` instead of a manual lock. Also flagged that `messageCollector` (a fresh `CollectingMessageCollector()` per `compileOnDaemon` call) meant a *successful* compile's own messages (deprecation notices, etc.) were silently dropped — only `Failure` ever read `messageCollector.messages`. Fixed by making `messageCollector` a single compiler-level field, explicitly `clear()`-ed immediately before every `compileOnDaemon` call (so a later snippet's report can never carry an earlier snippet's stale messages), with `DaemonCompilationResult.Success` now also carrying `messages: List<String>`, mapped by `compile()` into `ScriptDiagnostic`s (`Severity.WARNING`) and passed to `asSuccess(reports)`. New `DaemonReplCompilerTest.kt` exercises `DaemonReplCompiler` directly to lock in both behaviors.
- **Follow-up fix #7 (2026-07-05, later same day)**: flagged that `DaemonCompilationResult` (from fix #6) only ever existed to hand data back to `compileOnDaemon`'s one caller, `compile`'s loop body, which immediately unwrapped it — a pure data-copying round trip. Fixed by inlining `compileOnDaemon`'s body directly into `compile`'s per-snippet loop and deleting `DaemonCompilationResult` entirely. Also swept the class's comments for historical framing (references to what a *previous* version of the class did) and trimmed them to state only the current behavior, concisely.
- **Follow-up fix #8 (2026-07-05, later same day)**: flagged that `repl-snippet-artifact-output` (the self-contained `SnippetArtifactCodec` blob) narrows the possible usage scenarios compared to letting the evaluation side read the compiled classes via a regular classloader. Investigated: the blob format exists purely for the Build Tools API's `CompileReplSnippetOperation`, whose published `ReplSnippetCompilationResult.Success.artifact` type is a plain `ByteArray` with no shared-filesystem assumption — a constraint this same-machine daemon caller doesn't have. Implemented a coexisting "regular output" scheme (new `SnippetArtifactDirectoryCodec`, selected by `compileReplSnippet` whenever `-d`/`JVMConfigurationKeys.OUTPUT_DIRECTORY` is set) and migrated `DaemonReplCompiler` to it: each snippet compiles with `-d <dir>` instead of `repl-snippet-artifact-output`, priors are prior snippets' own output directories reused in place, and `decodeCompiledSnippet` wraps a plain `KJvmCompiledModuleFromClassPath` classloader over the directory instead of decoding a blob. This surfaced (and fixed) a real bug in shared code: `buildSnippetArtifactFromCompile` was silently including a non-class `META-INF/*.kotlin_module` output in `SnippetArtifact.classFiles`, invisible on the opaque blob format but fatal once classes are loaded through a real classloader. BTA is unaffected — it still uses the original blob scheme, since it still needs it.

- **Follow-up fix #9 (2026-07-05, later same day)**: two review complaints — "you're still using a custom decoder for directory" (`SnippetArtifactDirectoryCodec`, fix #8) and "you're changing the script evaluation extension, that should not be involved in the compilation at all" (`compileReplSnippet`'s home in `AbstractScriptEvaluationExtension.kt`) — led to replacing the entire bespoke-artifact scheme with the compiler's fully **regular** compilation-artifact path. `AbstractScriptEvaluationExtension.kt` and `ScriptingProcessSourcesBeforeCompilingExtension.kt` were reverted to their pre-existing shape (no `compileReplSnippet`/`REPL_SNIPPET_COMPILATION_MODE` involvement at all beyond a new, minimal `markAsReplSnippet()` call); `SnippetArtifactDirectoryCodec` was deleted outright. In its place: a `.repl.kts` source, once marked as a REPL snippet, now compiles through the *unmodified* regular JVM frontend/backend (enabled by `-Xallow-any-scripts-in-source-roots`); a new `repl-snippet-regular-mode` plugin option additionally registers the FIR REPL-snippet extensions (`FirReplSnippetConfiguratorExtension`/`FirReplSnippetResolveExtension`/`Fir2IrReplSnippetConfiguratorExtension` — previously only ever registered by `K2ReplCompiler`'s own test-only in-process registrar, never by the regular CLI/daemon plugin registrar) plus a `.repl.kts`-scoped `ScriptDefinition` (so the emitted class gets a real `$$result` field); a new `ClasspathBackedFirReplHistoryProvider` (sibling of `ArtifactBackedFirReplHistoryProvider`, sharing its embedded-`.kotlin_metadata`-sidecar reconstruction logic via a new `StatelessReplSnippetSupport.kt`) resolves prior snippets purely from an ordered `List<ClassId>` (new `repl-snippet-prior-class` option) reachable on the regular classpath — no artifact blob/header of any kind. `DaemonReplCompiler` now writes `-d <dir>`, `-cp <priorDirs>`, `repl-snippet-prior-class=<ClassId>` per prior, and predicts each snippet's wrapper class name from its own source file name via the public `NameUtils.getSnippetTargetClassName` (no round-trip needed). **Real compiler bug found and fixed along the way**: `KtScript.isReplSnippet`'s getter (`compiler/psi/psi-api`) read `greenStub?.isReplSnippet ?: copyableUserData`, so once a *physical, on-disk* file has a stub (as any regular CLI/daemon source-root file does), the stub's necessarily-`false` value permanently overrides `markAsReplSnippet()` — this only ever worked for `K2ReplCompiler`'s own stub-free, string-backed PSI. Fixed by checking the copyable user data first (OR-ing both sources) — confirmed via direct `kotlinc` invocation + `javap` that a marked `.repl.kts` file now produces a real `$$eval`/`INSTANCE`/`$$result`-bearing class, chains correctly against a prior via classpath+ClassId, and that the on-daemon JSR-223 example's full test suite (13/13) passes end-to-end on this design. The Build Tools API's `CompileReplSnippetOperationImpl` was intentionally left **not reworked** onto this scheme (deferred): its two daemon-driven integration tests (`ReplSnippetCompilationTest`) are `@Disabled` with an explanation, since they depended on the now-removed `compileReplSnippet`/`REPL_SNIPPET_COMPILATION_MODE` branch.

- **Follow-up fix #10 (2026-07-05, later still)**: with production compilation on the regular pipeline (fix #9), `K2ReplStatelessCompiler`/`SnippetArtifactEvaluator` were only reachable from `K2ReplStatelessCompilerTest`'s own two end-to-end tests and the FIR-diagnostics-corpus stateless-mode facade — no production caller used them anymore. Rewrote `testStatelessReplCompilesSnippetAgainstPriorArtifact`/`testStatelessReplExecutesMultiSnippetSequence` to drive `K2JVMCompiler` directly (new in-process `RegularPipelineReplCompiler` test helper, mirroring `DaemonReplCompiler`'s own argument shape) paired with a real `K2ReplEvaluator`, instead of `K2ReplStatelessCompiler` + the reflective `SnippetArtifactEvaluator` replay — so the test suite exercises the same pipeline production code actually uses. `SnippetArtifactEvaluator.kt` deleted outright. `K2ReplStatelessCompiler` remains in use only by `testStateObjectFqNameMismatchIsRejected` (its own validation) and `FirReplStatelessCompilerFacade`.

- **Follow-up fix #11 (2026-07-05, final)**: `K2ReplStatelessCompiler` had zero remaining production callers after fix #9/#10, so it — and everything that existed solely to serve it — was deleted outright: `ArtifactBackedFirReplHistoryProvider.kt`, `FirReplStatelessCompilerFacade.kt`/`AbstractReplStatelessDiagnosticsTest` (the FIR-diagnostics-corpus stateless-mode test double, plus its `TestGenerator.kt` entry and generated test class), `K2ReplStatelessCompilerTest`'s remaining two direct unit tests (`testHeaderProtoRoundtrip`/`testStateObjectFqNameMismatchIsRejected`), `SnippetArtifactHeader`/`SnippetArtifactHeaderProtoCodec`/`toArtifact`/`decodeHeader`/the `SnippetArtifact` class itself (`SnippetArtifactSidecar`/`SnippetArtifactSidecarProtoCodec`/`REPL_SIDECAR_PLUGIN_ID` are untouched — still the sole reconstruction payload `ClasspathBackedFirReplHistoryProvider` reads), and the dead best-effort/observer machinery in `K2ReplCompiler.kt` (`snippetCompilationObserver`/`sourceSessionReadyObserver`/`bestEffortBackend`/`elideErrorBodiedEvalFunctions`) that existed only to let `K2ReplStatelessCompiler` capture a partial artifact under FIR errors — `compileImpl` is back to a plain build-FIR → check-errors → convert-to-IR → codegen → check-errors flow. This surfaced one real compile break (`Fir2IrReplSnippetConfiguratorExtensionImpl`'s type check against the now-deleted `ArtifactBackedFirReplHistoryProvider`), fixed by narrowing it to `ClasspathBackedFirReplHistoryProvider` only. `K2ReplStatelessCompilerTest.kt`/`K2ReplStatelessCompilerTest` (now testing only the regular pipeline + the sidecar wire format, not `K2ReplStatelessCompiler` at all) renamed to `ReplSnippetRegularPipelineTest.kt`/`ReplSnippetRegularPipelineTest`.

## Parser path note

`K2ReplCompiler` is hybrid: PSI for `KtFileScriptSource`, LT otherwise. JSR-223 embedders typically pass `KtFileScriptSource`, so the **default JSR-223 path keeps PSI alive today**. Tracked: **KT-83498** — see [`50-migration-plan.md#2-land-kt-83498--full-lighttree-path-for-k2replcompiler`](50-migration-plan.md) and [`../current/10-compiler-representation.md`](../current/10-compiler-representation.md) for line anchors.

Implication: until KT-83498 lands, "fully PSI-free K2 scripting" is true for scripts but **not for JSR-223 / REPL**. Embedders wanting zero-PSI snippets must avoid `KtFileScriptSource` (or wait for KT-83498).

Once KT-83498 lands: align `K2ReplCompiler` with `ScriptJvmK2CompilerImpl` shape — accept `convertToFir` lambda; default to LT. This unifies the parser-agnostic seam across script and snippet pipelines.

## Sequence

1. Prototype + land K2 bindings (option D recommended; A/C as fallback) — restores feature parity with K1
2. **Prototype stateless remote compilation** (in parallel with 1) — confirm EPs are sufficient, freeze sidecar format
3. Land **KT-83498** — full LT path for `K2ReplCompiler`; align with `ScriptJvmK2CompilerImpl` shape (parser-agnostic seam)
4. Delete K1 fallback path in jvm-host (`legacyRepl*.kt`)
5. Delete daemon REPL (independent — see [30-embedding-target.md](30-embedding-target.md))
6. Once stateless prototype validates: design transport (BTA op vs direct in-process) and migrate IntelliJ consumer.
