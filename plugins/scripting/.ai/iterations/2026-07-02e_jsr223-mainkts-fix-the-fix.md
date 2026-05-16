# JSR-223 K2 bindings — "fix the fix": stop leaking `javax.script.*` class loading into plain main-kts compiles — 2026-07-02e

## Overview

The 2026-07-02d addendum reported `MainKtsIT`'s `testCachedReflection` and `testCacheWithFileLocation` as "2 pre-existing unrelated failures unchanged". The user pointed out this was wrong: both tests started failing not after the 2026-07-02d (rediscovery-bypass) change, but one iteration earlier, after 2026-07-02c (the Q16 second-implicit-receiver fix) — with `error: javax/script/ScriptContext (script-file-location-default.main.kts): java.lang.NoClassDefFoundError: javax/script/ScriptContext`, which is surprising since these tests run **plain** `.main.kts` scripts via the `kotlin` launcher, with no JSR-223 engine involved at all, and so should never need `ScriptContext` as an implicit receiver.

## Workstream / Issue

JSR-223 K2 bindings (Option D), migration-plan step 1 — a regression follow-up to the Q16 (G10) fix.

## Investigation

`MainKtsScriptDefinition` wires `beforeCompiling(::configureExposedJsr223Context)` unconditionally, regardless of whether the host is a JSR-223 engine. The function itself correctly guards on `context.compilationConfiguration[ScriptCompilationConfiguration.jsr223.getScriptContext]?.invoke() == null` and returns early for non-JSR-223 hosts — that guard was never removed and isn't the bug.

The 2026-07-02c fix, however, introduced this **top-level** property in `propertiesFromContext.kt`:

```kotlin
private val REQUIRED_IMPLICIT_RECEIVERS = listOf(ScriptContext::class, ScriptTemplateWithBindings::class)

fun configureExposedJsr223Context(context: ScriptConfigurationRefinementContext): ... {
    if (... == null) return ...
    ...
    val missingReceivers = REQUIRED_IMPLICIT_RECEIVERS.filter { ... }
    ...
}
```

A top-level `val` in Kotlin is compiled into a static field of the file-facade class (`PropertiesFromContextKt`), initialized by that class's `<clinit>`. The JVM runs `<clinit>` the first time **any** static member of the class is accessed — not just the first time that specific property is read. Since `configureExposedJsr223Context` is a top-level function in the very same file/class, simply *calling* it — even to immediately take the early-return branch — forces `PropertiesFromContextKt.<clinit>` to run, which evaluates `listOf(ScriptContext::class, ScriptTemplateWithBindings::class)` and, in doing so, loads `javax.script.ScriptContext` **unconditionally**, regardless of whether the JSR-223 guard would have skipped it.

Previously (before 2026-07-02c), the single-receiver version referenced `ScriptContext::class` only inside the guarded function body (after the early return), so the class was resolved lazily — only actually touched for real JSR-223 usage. The refactor to a top-level list (done for the "idempotent, listable" receiver-set check) silently changed this from lazy to eager, leaking the class-loading requirement to every caller of the file, including plain `.main.kts` compiles that never asked for JSR-223 semantics.

Whatever restricted classpath/module set the `kotlin` launcher's compile-daemon runs the `MainKtsIT` CLI-subprocess tests under apparently doesn't resolve `javax.script.ScriptContext` (a JDK `java.scripting`-module class) — plausible under a limited-module compiler-daemon launch — so the now-eager load fails hard with `NoClassDefFoundError`, surfacing as a compile error for scripts that have nothing to do with JSR-223.

## Fix

`libraries/scripting/jvm-host/src/kotlin/script/experimental/jvmhost/jsr223/propertiesFromContext.kt`: moved the receiver-class list back to a **local** `val requiredImplicitReceivers = listOf(ScriptContext::class, ScriptTemplateWithBindings::class)`, computed *inside* `configureExposedJsr223Context` **after** the `getScriptContext == null` early-return guard — restoring the pre-Q16 behavior where these classes are resolved lazily and only for actual JSR-223 usage. No other top-level declaration in the file references `ScriptContext`/`ScriptTemplateWithBindings` eagerly (the eval-time overload's `Jsr223ScriptTemplateWithBindings` is a separate top-level *class* declaration, not a top-level property — declaring a class doesn't force its superclass to load at file-init time, only at first instantiation, which already happens inside the guarded eval-time function).

## Verification

- `:kotlin-main-kts-test:test --tests MainKtsIT.testCachedReflection --tests MainKtsIT.testCacheWithFileLocation` — both PASS.
- Full `MainKtsIT` suite: **16/16, 0 failures** (previously 2 failing).
- Regression check — `:kotlin-scripting-jsr223-test:test` (`KotlinJsr223ScriptEngineIT`) **23/0/0**; `:kotlin-scripting-compiler:test --tests CustomK2ReplTest` **19/0/0**; `:kotlin-main-kts-test:test` `MainKtsTest` **23/0/1-skip**, `CacheDirectoryDetectorTest` **9/0/0**.
- `MainKtsJsr223Test` still has only the one pre-existing, unrelated `testWithImport` failure (`TODO("KT-77583")`, light-tree REPL-snippet support — migration step 2 / KT-83498), unaffected by this change.

## Key Learnings

- A top-level `val`/`object` in Kotlin is eagerly initialized by the enclosing file/class's `<clinit>`, triggered by **first access to any member of that file/class**, not just first access to that specific property. Any class-loading-triggering expression (e.g. a `::class` reference) placed in such a property leaks its class-loading cost to every caller of the file — including callers that take an early-return branch specifically designed to skip that work.
- This is exactly the kind of regression that's easy to miss in review: the diff "looks" like a pure refactor (single receiver → list of receivers, both still gated by the same runtime `if` check), but moving the list *outside* the function changed its evaluation semantics from lazy-and-conditional to eager-and-unconditional.
- Promoted to **G14** in `current/80-known-gotchas.md`; **G10** cross-references this follow-up.
