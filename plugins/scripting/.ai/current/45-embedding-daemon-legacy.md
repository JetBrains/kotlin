# Current — Embedding: Daemon REPL + legacy CLI REPL helpers (REMOVED)

> **When to consult**: Historical reference or when debugging the error-returning stubs.
> **Cache lifetime**: stable
> **Last verified**: 2026-09-04

Active CLI surface lives in [`40-embedding-cli.md`](40-embedding-cli.md). Everything below has been removed or stubbed out as of 2.5.0.

## `-Xrepl` CLI flag

`compiler/arguments/src/.../arguments/description/CommonCompilerArguments.kt`:

| Arg | Type | Since | Status |
|---|---|---|---|
| `-Xrepl` | Boolean | 2.2.0 | **REMOVED** (2.5.0) |

`replMode` plumbing in `compiler/cli/.../AbstractConfigurationPhase.kt` and `JvmConfigurationPipelinePhase.kt` has been deleted.

## `CompileService` REPL methods — STUBBED

`compiler/daemon/daemon-common/src/.../daemon/common/CompileService.kt`:

| Method | Status |
|---|---|
| `leaseReplSession` | Returns `CallResult.Error("REPL is not supported by the daemon anymore")` |
| `replCreateState` | Returns `CallResult.Error(...)` |
| `replCheck` | Returns `CallResult.Error(...)` |
| `replCompile` | Returns `CallResult.Error(...)` |

The RMI interfaces (`CompileService` methods, `ReplStateFacade`) are KEPT for binary protocol compatibility, but all implementations now return errors.

## Server-side — REMOVED

| File | Status |
|---|---|
| `compiler/daemon/src/.../daemon/KotlinRemoteReplService.kt` | **DELETED** |
| `compiler/daemon/src/.../daemon/RemoteReplStateFacadeImpl.kt` | **DELETED** |

## Client-side — KEPT (Stubs)

| File | Notes |
|---|---|
| `compiler/daemon/daemon-client/src/main/kotlin/.../KotlinRemoteReplCompilerClient.kt` | Kept for consumer compatibility; receiving errors from daemon. |
| `same dir` | `RemoteReplCompilerState.kt` (serializable state for RMI) — Kept. |

## CLI-base REPL helpers — REMOVED / REDUCED

`compiler/cli/cli-base/src/.../cli/common/repl/` (Reduced to 2 files):

| File | Status |
|---|---|
| `ReplApi.kt` | **REDUCED**: Keeps only compile/check protocol types; eval half deleted. |
| `ReplState.kt` | **REDUCED**: Keeps `LineId`, `ReplHistoryRecord`, etc.; helpers deleted. |
| `GenericReplEvaluator.kt`, `BasicReplState.kt`, etc. | **DELETED** |
| `KotlinJsr223Jvm*` | **DELETED** |

What remains in `cli-base` exists ONLY to support the surviving (but error-returning) daemon RMI protocol types.

## CLI REPL shell extension

`JvmCliReplShellExtension` and `ShellExtension` / `ReplFactoryExtension` EPs have been deleted.

## Status summary

| Subsystem | Status |
|---|---|
| `-Xrepl` flag | REMOVED |
| `JvmCliReplShellExtension` | REMOVED |
| Daemon `CompileService` REPL methods | STUBBED (Return Error) |
| `KotlinRemoteReplService` | REMOVED |
| `KotlinRemoteReplCompilerClient` | KEPT (Compatibility Stub) |
| `cli-base/.../cli/common/repl/*` | REDUCED to protocol types |
