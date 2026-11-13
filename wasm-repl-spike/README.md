# Kotlin/Wasm hot-reload spike

Two loops over one compiler mechanism (`-Xwasm-hot-reload`):

- **`browser/`** — the product harness: a page-shaped dev loop on
  wasm-js, no WASI. Every save compiles a FILE-GRANULAR delta — only the
  IC-modified files are defined; clean files' declarations import from
  the live patch chain — and the glue instantiates it, sweeps every
  owner instance's slot setters, and migrates held objects in place.
  State survives; the engine (V8) is the only type police. This is the
  harness for the Compose integration.
  **Run it:** `node browser/dev.mjs` → http://localhost:8123, then edit
  a file in `browser/app/src/`. **Gate:** `node browser/test.mjs`
  (v1–v16: swaps with state, field migration, dispatch, method
  additions hot, engine rejections, ghost instances, and the
  language-feature stress beats).
- **`lrepl/`** — the semantics lab: a Lisp-like live-image REPL
  (terminal, SLIME/swank, comint) where the engine behaviors were
  discovered first. Whole-world generations, `:dis`, payload-version
  demos. Warm line latency ~0.3 s.
  **Run it:** `python3 lrepl/lrepl.py` (`--demo` for the scripted tour;
  `lrepl/swank.py 4005` for SLIME).

```
lrepl> fun double(n: Int): Int = n + n
lrepl> fun scale(n: Int): Int = double(double(n))
lrepl> scale(5)
20
lrepl> fun double(n: Int): Int = n * 3
redefined fun double — live slot swapped; existing callers now run the new body
lrepl> scale(5)
45
lrepl> :dis double        ← wasm disassembly of the current body, on the fly
```

## Docs map

- `docs/DESIGN.md` — the design: the wasm-GC substrate, the
  generation/CL-image model, slots, cells and payload versions, the
  browser delta loop, the Compose layer (what remains on the product
  path), the daemon, frontends, and the hard-won failure modes &
  invariants.
- `docs/ROADMAP.md` — what works today (present tense) and what is
  next, in order: the function-granularity manifest, top-level state
  holders across their own redefinition, interface `is`, the Compose
  integration, migration quality + generation GC, upstreaming.

## Layout

| path | what |
|---|---|
| `browser/` | the product loop: `app/src/*.kt` (the six-file demo), `build.mjs` (two-step compile + registry ferry), `glue.mjs` (instance chain, sweep, migration), `dev.mjs` (dev server), `test.mjs` (the gate), `index.html` (the demo page) |
| `lrepl/` | the lab: `lrepl.py` (driver), `daemon.mjs` (the image: one instance per generation, funcref registry, mini-WASI, captured stdout), `CompileServer.kt` (resident JVM), `swank.py`/`swank_test.py`, `registry_test.py` (the registry gate) |
| `survival/` | the validation experiments: type unification, cross-module dispatch, version subtyping, cell-encoding parity and cost, hand-written-vs-compiler migration (each `driver*.mjs` reproduces one result) |
| `docs/` | the two documents above |

Build artifacts (`*.klib`, `*.wasm`, `server.jar`, `build/`, IC caches)
are generated on demand and gitignored. After `./gradlew dist`, delete
`server.jar` and the IC caches (the compile server embeds the compiler;
a warm cache mixes encodings).

## The one compiler patch outside the flag

`compiler/fir/checkers/checkers.wasm/src/.../FirWasmImportAnnotationChecker.kt`
relaxes `isTypeSupportedInWasmInterop` to allow any type except Char for
`@WasmImport`/`@WasmExport` (the restriction assumed a JS host; a host
supplying *wasm funcrefs* is fully type-checked by the engine at
instantiation). An upstreamable version gates this behind an opt-in
flag — ROADMAP §7.
