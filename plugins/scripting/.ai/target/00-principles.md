# Target — Principles

Guiding rules for the cleanup. Each principle has a "why" and a "how to apply".

## P1. K2-only frontend

**Rule**: Drop K1 code paths in scripting/REPL once the K1 frontend is retired from the rest of the compiler.

**Why**: K1 is being removed compiler-wide. Carrying parallel scripting paths costs maintenance and confuses contributors.

**How to apply**: For every artifact tagged K1 in [current/90-legacy-inventory.md](../current/90-legacy-inventory.md), schedule removal. If a K1 piece is still load-bearing (e.g., classpath discovery feeds K2 too), migrate first.

## P2. Single canonical path per role

**Rule**: One implementation per concern. No `legacy*`, `obsolete*`, `Generic*`, or `createLegacy()` siblings.

**Why**: Duplication invites drift — bug fixes go to one path and not the other.

**How to apply**: Audit `libraries/scripting/jvm-host` and `plugins/scripting/scripting-compiler` for `legacy*` / `obsolete*` files; remove after K1 retires. Collapse `JvmScriptCompiler` to the K2 ctor only.

## P3. PSI-free where it pays

**Rule**: Prefer LightTree over PSI in production paths. Don't fragment for principle — add a LT path only when there's a real cost from PSI.

**Why**: PSI couples to IntelliJ platform — heavy, slow, and a dependency we want to shed.

**How to apply**:
- Scripts: **already PSI-free on K2 path.** `ScriptJvmK2CompilerImpl` uses `convertToFirViaLightTree`. CLI route hardwires LT (`JvmCliScriptEvaluationExtension`). Nothing to do for scripts in this principle's scope.
- REPL snippets: still partly PSI-bound. `K2ReplCompiler.kt:351-359` routes `KtFileScriptSource` through PSI; `FirReplSnippetConfiguratorExtensionImpl.kt:173` still does `scriptSource.psi as? KtScript`. Tracked under **KT-83498**.
- After K1 frontend retirement: PSI-side `KtScript` stays only as long as embedders still pass `KtFileScriptSource`. Possibly drop after that audit.

## P4. No first-party REPL — provide REPL API for external REPLs

**Rule**: We do not ship a Kotlin REPL product. The `kotlinc -Xrepl` shell, the daemon-side REPL, and `cli-base/cli/common/repl/*` all go. What stays is the **REPL API** in `libraries/scripting/common` + the K2 REPL pipeline in `plugins/scripting/scripting-compiler` — these are the means for external REPLs to be built on top of our infrastructure.

**Why**: A good REPL is a product (UI, sessions, history, completion, error formatting, integration with notebooks/IDEs/notebook-like tools). We don't have the resources to maintain a competitive first-party REPL. The half-baked CLI/daemon REPL drifts and ages, while embedders (Jupyter kernel, IntelliJ scratch files, etc.) already build their own UX on top of the API. Keeping a half-product blocks API evolution and confuses users.

**How to apply**:
- Delete the inventory in [current/40-embedding-cli-daemon.md](../current/40-embedding-cli-daemon.md): `-Xrepl`, `JvmCliReplShellExtension`, daemon REPL methods, `KotlinRemoteReplService`, `cli-base/repl/*`.
- Keep `-script` (file evaluation) — that's a script feature, not a REPL feature.
- Keep `ReplCompiler` / `ReplEvaluator` interfaces in `libraries/scripting/common`. Keep `K2ReplCompiler`, the FIR REPL EPs, `IrReplSnippet` lowering. These are the API surface.
- JSR-223 is a **separate concern** — see P4a.

## P4a. JSR-223 — an "accidental" REPL

**Note (not rule)**: `javax.script.ScriptEngine` is a script-engine API in spec but in practice ends up used as a REPL (`eval()` repeatedly with shared bindings). The fit is awkward and the implementation carries quirks (binding semantics, `Invocable`, classloader subtleties). 

We keep JSR-223 support because the ecosystem expects it (Maven Polyglot, scripting in app servers, etc.). Two design considerations stay open:
- Should JSR-223 ride on the REPL pipeline (current K2 choice) or on **pure scripting** (single-call `eval`, no history) under the hood? The latter sidesteps the bindings-vs-snippet shape mismatch but loses cross-`eval` state. Decision pending — see [40-jsr223-target.md](40-jsr223-target.md).
- Remote (out-of-process) JSR-223 compilation is a real-world need (current IntelliJ impl uses it to avoid hosting the compiler in-process). See P9 + [40-jsr223-target.md](40-jsr223-target.md) for the stateless-compilation direction.

## P5. No IntelliJ-platform / IDE coupling in compiler-side scripting

**Rule**: The compiler-side scripting modules (`plugins/scripting/*`) must not depend on `intellij-community` plugin code or own copy-pasted IDE plugin code.

**Why**: `scripting-ide-common` is literally copy-pasted from the IntelliJ monorepo (per its README). It rots independently and forces synchronization.

**How to apply**: Drop `plugins/scripting/scripting-ide-common`, `scripting-ide-services`, `scripting-ide-services-embeddable`, `scripting-ide-services-test`. Future reimplementation possible, but in a different form and definitely without K1.

**Scope note**: `libraries/scripting/intellij` is a separate matter — it's a tiny **public API surface** (the `IdeScriptConfigurationControlFacade` EP) used by IntelliJ plugin authors to wire custom-scripts support into the IDE. It **stays**. It does not depend on IntelliJ platform itself; it merely defines an EP that IDE-side plugins can implement.

## P6. Definitions: classpath markers stay, but be SPI-explicit

**Rule**: Keep `META-INF/kotlin/script/templates/*.classname` discovery while it has users (main-kts, third-party defs), but document it explicitly and stop deprecation drift.

**Why**: KT-82551 marks classpath discovery deprecated, yet there's no successor. Embedders depend on it.

**How to apply**: Either un-deprecate and document, or design a successor SPI (`ServiceLoader<ScriptDefinitionContributor>`?) before removal — open question.

## P7. Deprecation discipline

**Rule**: Mark → wait one release → delete. No "deprecated since 2.2.0" still present in 2.6.0 without justification.

**Why**: Stale deprecations multiply, confuse, and accumulate technical debt.

**How to apply**: Audit `@Deprecated` in `libraries/scripting/`. Anything older than two releases → delete.

## P8. Customization seams are the only public extension points

**Rule**: User customizations go through:
- `ScriptCompilationConfiguration` refinement DSL (`refineConfiguration { ... }`)
- `ScriptEvaluationConfiguration` refinement
- `ExternalDependenciesResolver` SPI
- `KotlinScript`-annotated definition classes

No new compiler-internal EPs leaked to users.

**Why**: Limits API surface; lets us refactor FIR/IR freely.

**How to apply**: When tempted to expose FIR/IR-level seams (e.g., `FirReplSnippetConfiguratorExtension`), ask: can it be expressed via refinement config first?

**Planned addition**: a refinement-DSL callback that returns optional **implicit snippets** to compile + eval before the user's current snippet (`inferImplicitSnippetsBefore` or similar — exact name TBD). The REPL harness handles them transparently; specific logic (JSR-223 bindings, magic commands, kernel preludes, parameter injection) lives in the definition's handler. This keeps with the principle — a generic "pre-snippet rewrite" mechanism stays on the public refinement surface rather than leaking into compiler-internal EPs. See [40-jsr223-target.md](40-jsr223-target.md) Option D.

## P9. Build Tools API is the integration surface

**Rule**: Embedders, Gradle, Maven and friends integrate via BTA. Daemon-based and direct-compiler-API integrations go.

**Why**: BTA is stable, versioned, frontend-agnostic. It's the agreed integration surface for the wider Kotlin ecosystem.

**How to apply**: When new integration appears (e.g., remote compilation for JSR-223), prefer extending BTA.
