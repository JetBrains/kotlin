# Browser hot-reload loop (wasm-js) — PATCH MODULE mode, FILE-GRANULAR deltas

The spike's loop running in the product host: a real page, no WASI
anywhere, the engine (V8) as the only type police. Every edit compiles
and pushes a PATCH MODULE whose emission is file-granular — the delta
defines only the files the IC marked modified; the clean files'
declarations are imports resolved against the live patch chain (the
multimodule `__fn$`/`__vt$`/`__rt$` service-import convention is the
ABI, under the artifact's own module name) — against a stdlib BASE wasm
that is instantiated once per session. No flag, no manifest: the dirty
set is the IC's own per-file fingerprint comparison, and a cold cache
emits the full patch by construction (`../docs/DESIGN.md` §7 has the design
and the honest measurements: the function set is exactly the dirty
file's; at this app's two-file scale the byte size barely moves — the
per-class cell/payload machinery and types dominate — but the clean
files' bodies are what stops shipping, which scales with real modules).
This is the harness for the Compose hot-reload work — everything here
(instantiate against the live chain's exports, sweep every owner's slot
setters, migrate held objects, ferry the image registry) carries over;
Compose adds the recomposition invalidation hook and group-key
stability.

## Run it

```bash
node dev.mjs                 # http://localhost:8123
# (KOTLIN_DIST=/path/to/dist to point at a different compiler build)
```

Then edit a file in `app/src/` and save. The dev server recompiles
(`-Xwasm-hot-reload`; emission is file-granular — only the edited file
is defined in the patch), ferries the registry, and the page hot-applies
it within ~1–2 s. The page's **demo script** panel lists the edits to
try, with what to expect for each — keep it side by side with the code.

## What each kind of edit does

| edit | mechanism | what you see |
|---|---|---|
| body change | new module, same function signatures → the glue sweeps every live `__hot_set$X` setter with the new `__hot_entry$X` | the new body, immediately, on the same instances |
| behavior change | same — bodies read live state through slots | buttons jump differently; state untouched |
| field added | payload version chain (registry from the live module's custom section) + compiler-generated stepper; the held instance migrates IN PLACE on first touch | the v1-born instance shows the new field as its type default (`null`); a fresh instance shows the initializer — that contrast is the migration demo |
| field deleted | migrate again: surviving fields copy, the field is simply gone (nothing is shadowed) | the held instance keeps its other values; old code that still read the deleted field would trap — the invalidation trampoline |
| base-class body change | vtable entries read slots like everything else | the base instance runs the new body; overrides still win — correct dispatch through the swap, same instances |
| method added (non-virtual, final class) | never enters the vtable — no type changes; the additive/Julia case | a clean swap; the new method rides along |
| method added (OPEN, on an open class) | the vtable struct grows (a new type) but the vtable is behind the mutable root-typed cell field — no signature changes; old instances migrate their vtable on next dispatch (the compiler-generated stepper installs the current vtable + itable; a grown vtable is a SUBTYPE of the old, so old code keeps working on migrated instances) | a clean swap; the new method is callable on instances born BEFORE it existed — SBCL's wrapper-update on wasm |
| class supertype gained (or finality flipped) | the cell SUBTYPING CHAIN is part of every signature mentioning the class → the ENGINE rejects those slots at the setters, for free | a partial patch: what the engine accepted is swapped, the old code keeps running for the rest; the banner lists the rejected slots — revert to recover |
| compile error | never reaches the engine; the server keeps serving the last good version | the app keeps running; the banner shows the compiler's message |
| runtime trap in a swapped body | the patch swaps in; the body traps when called | the banner shows the exception while the rest of the app keeps running; fix the body and save |

Failures surface in a dismissible banner (never a full-screen
takeover) that clears itself when a good patch lands; the demo script
panel on the page walks every beat above with the exact edits and
expectations.

## The pieces

- `app/src/` — the demo app in TWO files: `model.kt` (Model + its
  exports — the state story) and `zoo.kt` (the Animal hierarchy +
  `make`/`intro` — the dispatch story). Multi-file is the file
  granularity the delta link keys on: a Model beat edits model.kt, a
  zoo beat edits zoo.kt. The page holds the instances as wasm GC
  objects in JS (state) and passes them back in on every call.
- `glue.mjs` — the hot-loop glue, browser APIs only: `loadBase` (the
  stdlib base, once — its generated import object provides the wasm-js
  intrinsics), `loadPatch` (a patch instantiated against the LIVE base
  AND, for deltas, the live patch chain — a delta's clean-file imports
  under the artifact's own module name resolve to the chain's merged
  exports, newest-first), `liveView` (the instance chain: exports
  resolve newest-instance-first, SKIPPING dead definitions — an
  engine-rejected entry means that patch's export of the name can't
  serve the live world and the previous live version keeps running),
  `readRegistry` via `WebAssembly.Module.customSections`, `applyPatch`
  (sweeps EVERY live instance's setters — a redefined function's slot
  lives in whichever generation defined its file last; engine
  rejections collected per-setter), `applyDelta` (the one call per
  edit: load, sweep, mark dead, push).
- `build.mjs` — the shared compile step: the two-step CLI in multimodule
  mode (stable identities; the base emits once into the build dir — the
  output phase skips it thereafter — and the patch every edit), the
  registry ferry (read the last patch's custom section, inject
  `__hotRegistry` into the next source).
- `dev.mjs` — the dev server: static files + `/version` + recompile on
  save (the gate's v1 sources are the COMMITTED src files — the on-disk
  files are your playground).
- `test.mjs` — the Node gate (same V8; no browser needed):
  `node test.mjs` runs v1→v8 with held instances — body swap with state
  preserved, field addition with in-place migration (fresh instance gets
  the initializer), field deletion, base-class body change with the
  override still winning, an additive method accepted, an OPEN method
  addition HOT (the whole chorus dispatches the new method on v1-born
  instances through vtable migration, overrides still winning), a
  class-supertype gain ENGINE-REJECTED with the old code still running,
  and the GHOST-instance beats (a held instance of a since-deleted class
  traps the honest "illegal cast" while the rest of the zoo migrates).

## Known current limits (all on the roadmap)

- emission is FILE-granularity: only the IC-modified files are defined
  in the patch. Function granularity WITHIN a dirty file remains (the
  per-function content-hash manifest — ROADMAP §1: unchanged
  functions in an edited file still re-emit today);
- brand-new exports (functions that didn't exist in v1) can't attach to
  the frozen live instance — they ride the patch instance; the page
  reaches them through the latest patch's exports;
- instances of a class REMOVED from the source (a page still holding a
  deleted class's object) trap with "illegal cast" when new code
  dispatches through a redefined base — honest, but the page could also
  drop them from held collections;
- new fields migrate with type defaults, not initializers;
- no Compose yet: the app paints text, not UI — the recomposition hook
  (`__hot_invalidate`) and reload-stable group keys are the next
  milestone (see `../docs/DESIGN.md` §8).
