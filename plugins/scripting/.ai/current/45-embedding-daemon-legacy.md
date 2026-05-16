# Current — Embedding: Daemon REPL + legacy CLI REPL helpers (ALL REMOVE)

> **When to consult**: ONLY when executing migration steps 4, 5, or 6 (delete daemon REPL / `-Xrepl` / `cli-base/repl/*`).
> **Cache lifetime**: mutable-per-iteration (shrinks to zero as steps land)
> **Last verified**: 2026-05-16

Active CLI surface lives in [`40-embedding-cli.md`](40-embedding-cli.md). Everything below is REMOVE per [`target/30-embedding-target.md`](../target/30-embedding-target.md) and [`target/50-migration-plan.md`](../target/50-migration-plan.md) steps 4/5/6.

## `-Xrepl` CLI flag

`compiler/arguments/src/.../arguments/description/CommonCompilerArguments.kt`:

| Arg | Type | Since | Status |
|---|---|---|---|
| `-Xrepl` | Boolean | 2.2.0 | **deprecated, REMOVE** (migration-plan step 5) |

`replMode` plumbing in `compiler/cli/.../AbstractConfigurationPhase.kt` — REMOVE.

## `CompileService` REPL methods — ALL REMOVE (step 4)

`compiler/daemon/daemon-common/src/.../daemon/common/CompileService.kt` (lines 151–179):

| Method | Purpose |
|---|---|
| `leaseReplSession` | Allocate session |
| `releaseReplSession` | Free session |
| `replCreateState` | Create state facade |
| `replCheck` | Pre-compile check |
| `replCompile` | Compile snippet |

Supporting type: `ReplStateFacade`.

## Server-side — REMOVE

| File | Class | Notes |
|---|---|---|
| `compiler/daemon/src/.../daemon/KotlinRemoteReplService.kt` | `KotlinJvmReplServiceBase` | Extends `ReplCompileAction`, `ReplCheckAction`. Lazy `ReplCompiler` via `ReplFactoryExtension`. |

## Client-side — REMOVE

| File | Class |
|---|---|
| `compiler/daemon/daemon-client/src/main/kotlin/.../KotlinRemoteReplCompilerClient.kt` | `KotlinRemoteReplCompilerClient` |
| same dir | `RemoteReplCompilerState.kt` (serializable state for RMI) |

## CLI-base REPL helpers — REMOVE (step 6)

`compiler/cli/cli-base/src/.../cli/common/repl/` (~13 files):

| File | Purpose |
|---|---|
| `GenericReplEvaluator.kt` | K1 REPL evaluator base |
| `GenericReplCompilingEvaluator.kt` | Compile + eval combo |
| `BasicReplState.kt`, `AggregatedReplState.kt`, `ReplState.kt`, `ReplHistory.kt` | K1 REPL state |
| `KotlinJsr223Jvm*` | Legacy JSR-223 wrappers (pre-K2) |
| plus others — full enumeration in [`90-legacy-inventory.md`](90-legacy-inventory.md) |

Referenced by:
- The K1 daemon REPL service
- `JvmReplCompiler` / `JvmReplEvaluator` in `libraries/scripting/jvm-host/legacy*.kt`
- `GenericReplCompiler` in `plugins/scripting/scripting-compiler/.../repl/`

## CLI REPL shell extension

`JvmCliReplShellExtension` registered by `ScriptingCompilerConfigurationComponentRegistrar` (K1) — entry point for the in-process REPL when `-Xrepl` is used. Goes with `-Xrepl` removal (step 5).

## Status summary

| Subsystem | Status |
|---|---|
| `-Xrepl` flag | REMOVE (step 5) |
| `JvmCliReplShellExtension` | REMOVE (step 5) |
| Daemon `CompileService` REPL methods | REMOVE (step 4) |
| `KotlinRemoteReplService` | REMOVE (step 4) |
| `KotlinRemoteReplCompilerClient` + `RemoteReplCompilerState` | REMOVE (step 4) |
| `cli-base/.../cli/common/repl/*` | REMOVE (step 6) |
