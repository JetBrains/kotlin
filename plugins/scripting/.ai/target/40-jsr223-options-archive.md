# Target — JSR-223 bindings: archived options A / B / C

> **When to consult**: only when reopening the bindings design (Option D in [`40-jsr223-target.md`](40-jsr223-target.md) is the chosen path). Cache-stable historic rationale.
> **Cache lifetime**: stable
> **Last verified**: 2026-05-16

These options were considered and rejected (or sidelined as fallbacks) when settling Option D. Kept here so the trade-offs aren't lost.

## Problem recap

`javax.script.Bindings` maps name → value (Java level). Bindings must be accessible as variables inside the script.

K1 path injected them as **provided properties** on `LazyScriptDescriptor` / `FirScript`. K2 script compilation reuses the same via `FirScript.parameters`. K2 REPL uses `FirReplSnippet` (different shape — embedded `FirRegularClass` + `$$eval`), so the script-side mechanism doesn't transfer directly. Four options considered.

## Option A — snippet class ctor params

Bindings become fields on the snippet wrapper class, initialised via ctor params.

- **Plug-in points**: `FirReplSnippetConfiguratorExtensionImpl` adds fields + ctor params on the snippet class. `Fir2IrReplSnippetConfiguratorExtensionImpl` translates. `K2ReplEvaluator` passes binding values at instantiation.
- **Pros**: mirrors script-side `providedProperties` mechanism; binding identity stable per-snippet.
- **Cons**: each snippet instance carries the whole binding map; rebinding requires a new snippet instance.

**Status**: fallback if Option D harness work proves prohibitive.

## Option B — `$$eval` function params

Bindings become local params on `$$eval`.

- **Plug-in points**: `FirReplSnippetConfiguratorExtensionImpl.configureEvalBody` adds params; eval signature widens.
- **Pros**: simplest plumbing; `$$eval` already takes implicit receivers.
- **Cons**: rebinding mid-session changes the eval signature → cross-snippet references break. Identity is per-call, not per-snippet.

**Status**: not recommended (identity instability across snippets).

## Option C — state object fields

`IrReplSnippet.stateObject` is the IR-level shared-state hook. Use it to carry bindings.

- **Plug-in points**:
  - `K2ReplCompiler` creates / threads a "state object" for the REPL session; binding map lives there.
  - `Fir2IrReplSnippetConfiguratorExtensionImpl` ensures the state object has a property per binding (lazy, generated as bindings appear).
  - `ReplSnippetsToClassesLowering` already plumbs `stateObject` into the generated snippet class; emit getter calls in `$$eval` for binding references.
  - **Resolve EP** (`FirReplSnippetResolveExtensionImpl.getSnippetScope`) extends the per-snippet scope with synthetic properties matching the binding map at compile time.
- **Pros**: shared across snippets by construction; rebinding is "set the field" not "rebuild signature"; matches the REPL "running state" semantics.
- **Cons**: state object lifecycle gets entangled with binding lifecycle; needs versioning if bindings can be added/removed between snippets; more plumbing than A or B.

**Status**: fallback if Option D harness work proves prohibitive.

## Why Option D won

A/B/C all bake JSR-223 bindings semantics into compiler-internal EPs (`FirReplSnippetConfiguratorExtension`, `Fir2IrReplSnippetConfiguratorExtension`, `FirReplSnippetResolveExtension`). That violates P8 ("customization seams are the only public extension points"). Option D moves the logic to the public refinement DSL via implicit snippets — same primitive then unblocks magic commands, kernel preludes, parameter injection, etc.

See [`40-jsr223-target.md`](40-jsr223-target.md) for the live Option D design.
