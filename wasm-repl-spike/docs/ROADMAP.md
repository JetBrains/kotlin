# Roadmap — what works, and what to do next

The product context is **Compose hot reload in the browser** (wasm-js);
nothing load-bearing may depend on how the wasi REPL is wired; the
browser is the product host. The design — engine, delta loop, Compose
layer, measurements — lives in `docs/DESIGN.md`.

## The production target

A KGP dev task that watches sources → IC-compiles the changed module
klib → emits a delta wasm built with `-Xwasm-hot-reload` → the dev
server pushes it → the browser loader instantiates it against the live
chain and sweeps the slots → the app updates with state intact.
Everything the prototype validated is the spec for that loop; the
REPL's remaining hacks are stand-ins for what the compiler/loader owns:

| prototype mechanism (stand-in) | production replacement |
|---|---|
| module-per-line generations | patch modules — SHIPPED (browser loop): file-granular deltas over the IC's dirty set; the per-function manifest (§1 below) remains |
| JS daemon registry + `swap` | the dev-mode loader in the JS glue (instance chain, owner sweep) |
| PSI frontend in the compile server | REPL-only; the dev task knows what changed |
| checker relaxation (any-type `@WasmImport/@WasmExport`) | upstream, flag-gated |

## What works today

- **Functions**: `-Xwasm-hot-reload` gives every reloadable function a
  mutable typed-funcref slot, a slot-reading entry under its real key
  (all call sites, vtables and funcrefs dispatch through it) and an
  exported setter the engine type-checks. Redefinitions swap the slot;
  a different signature is a new overload (Julia semantics).
  Constructors and inline functions are excluded; callable-reference
  and merged-suspend-lambda classes are excluded to protect link-time
  dedup. Measured cost ~0.27 ns/call.
- **Classes**: stable-cell encoding — the class struct is a cell
  {vtable, itable, rtti, payload}; field changes mint payload VERSIONS
  chained against the class's history and existing instances MIGRATE
  lazily in place (CL defclass semantics; back-to-old-shape redefines
  heal with no migration). Cell-chain changes (gaining a class
  supertype, finality flips) are engine-rejected — coexistence is the
  honest semantics for a genuinely new type.
- **Method additions are hot**: the vtable is behind the same mutable
  root-typed indirection with a compiler-generated universal stepper;
  existing instances dispatch a newly added open method after a
  one-shot vtable migration, overrides still win, and old code keeps
  working (a grown vtable subtypes the old). Interface default-method
  BODY changes reach live instances the same way (itable entries read
  slots) — gated.
- **The image registry**: every hot module embeds its COMPLETE
  payload-version table as a `hot-reload-registry` custom section
  (deltas merge the previous module's table to stay complete); the
  host ferries it back as one hidden source declaration. It is the
  compiler's ONLY previous-state input — wiping the IC cache loses
  nothing (registry_test.py gates both bumps across wipes).
- **The browser loop** (`browser/`, wasm-js, multimodule patch
  modules): a page-shaped dev loop with no WASI. The stdlib base loads
  once — and it is HOT: the same link that builds the patch re-lowers
  the stdlib once with the flag (its klib ships IR, not wasm), so base
  and app share ONE encoding — every class cell-built, every function
  slotted (~2.4k stdlib slots in the base, emitted once and cached; no
  cell/plain boundary to bridge). Every edit pushes a delta that defines only the IC-modified
  files, importing clean files' declarations from the live patch chain
  under the artifact's own module name (no flag, no manifest — the
  dirty set is the IC's per-file fingerprint comparison). The glue
  keeps the instance chain (newest-first export resolution, skipping
  dead definitions — an engine-rejected entry leaves the previous live
  version running), sweeps every owner instance's setters, and forces
  a FULL first patch for a fresh chain (the IC cache's "unchanged" is
  file truth, not image truth). Gated (`browser/test.mjs`, v1–v16):
  state through swaps, field migration add+delete, hierarchy dispatch
  with overrides winning, additive methods, engine-rejected shape
  change with old code running, ghost instances trapping the honest
  "illegal cast", and the language-feature stress beats.
- **Language features, stress-verified** (the six-file demo): generics,
  varargs and extension receivers are slotted and swap hot; cross-file
  INLINE edits propagate (the IC's inline hashing dirties every
  caller's file, so the delta re-emits them with the new body
  inlined); a held top-level lambda re-binds on redefinition (new
  binding, fresh initializer — CL semantics; the sibling val keeps its
  value); interface default bodies swap on LIVE instances.
- **Object state holders are defvar** (the Compose state-holder
  story): an `object` (or companion) redefined by its own dirty file
  ADOPTS the previous live instance instead of re-running its
  initializer (the guarded store fires on the delta's first access of
  the holder) — the compiler emits a `__hot_adopt$<object>#<hash>` host
  import as the `boundp` check; the glue
  eagerly captures the previous live instance's value at patch load
  (pre-sweep; a null — normal cold init — when no previous binding
  exists) and the engine type-checks it against the cell type at the JS
  boundary when wasm receives it, so an incompatible holder change is
  engine-gated twice: the sweep rejects the new shape's swaps like any
  cell-chain change (old code keeps running), and any live path that
  drives the new code's holder access traps at the adoption boundary. Added fields migrate via the payload steppers. Top-level
  vals/vars are still defparameter (opt-in remains). Gated: state
  survives redefinition AND field addition; incompatible holders are
  rejected. The REPL keeps new-binding semantics (its accessor
  machinery is its own state sharing — the daemon answers null).
- **The REPL** (`lrepl/`): the semantics lab where the engine behaviors
  were discovered first — whole-world generations, slots + cells +
  payload versions, `:dis`, swank frontends. ~0.3 s per line warm.

## Latency (measured 2026-11-12)

| loop | warm per-edit | cold boot |
|---|---|---|
| REPL (whole-world) | ~0.3 s | ~3 s (IC cache warming) |
| browser (delta loop) | ~0.3–0.6 s | ~3 s (first full patch) |

Both loops run on the resident CompileServer (no JVM spawn per edit);
the browser link skips the byte-identical stdlib base after first
emission, and delta emission ships only the dirty files. The remaining
lever is the per-function manifest (below) — and at real module scale,
where clean files' bodies dominate, the size/latency win is much
larger than the demo's (whose bytes are mostly fixed machinery:
re-declared types, cell/payload structs, string pool, registry).

## What to do next

### 1. Function-granularity manifest + real-scale proof

File granularity is shipped. What remains: a per-function content-hash
manifest (post-lowering IR hash riding the registry round-trip) so
unchanged functions in a DIRTY file import instead of re-emitting; the
JS IC's inline-function hashing is the precedent and the
caller-propagation mechanism inline needs. Then prove the size/latency
win on a module with real bulk. Gate: a redefinition costs a
function-sized delta (<50 KB, <150 ms).

### 2. Top-level val/var opt-in (defvar for non-object holders)

Objects and companions are defvar (see above). Top-level vals/vars are
still defparameter — every redefinition re-runs the initializer (safe
default: derived values recompute). The opt-in is a dev-mode-only
annotation marking a val/var as a state holder, so its backing global
adopts like an object's instance field. On the Compose critical path
only insofar as apps use top-level vals as holders.

### 3. Interface `is` version-aware (upstreamable)

Interface `is` is RTTI-based on a stable NAME hash, so after a member-
set change an old instance answers `is I` true while `as` traps. Fold
the member set into `interfaceId`. driver5/driver6 are the anchors.

### 4. Compose integration

`__hot_invalidate` + reload-stable group keys with the Compose compiler
team; the error overlay. Everything the browser loop proves carries
over (DESIGN.md §8).

### 5. Migration quality + generation GC (now load-bearing)

- initforms for new fields (type defaults today);
- catchable `ObsoleteInstanceException` instead of the raw illegal-cast
  trampoline, naming class, field, held version;
- incompatible-signature degradation (sketch, not built): generate a
  same-signature thrower per function and let the host install it into
  the old slot, so stale call sites raise a catchable exception naming
  the old signature;
- version pruning for long sessions;
- **generation GC**: one instance per edit is retained; prune
  instances whose slots are all swept away and whose globals are
  unreferenced (track handouts with JS WeakRefs).

### 6. Exception tag export (upstreamable)

Tags are module-local, so Kotlin exception payloads are invisible from
JS. Export the tag; the host can `WebAssembly.Exception.getArg` the
Throwable and print it properly.

### 7. Upstreaming

The checker relaxation (flag-gated), the exception tag export, and
long-term slot mode + the cell/payload encoding as experimental modes
(the multimodule `__fn$/__vt$/__rt$` machinery is precedent).

## Operational notes

- After `./gradlew dist`: delete `server.jar` and the IC cache (the
  compile server embeds the compiler; a warm cache mixes encodings).
- The IC cache root is keyed by a config hash covering the link step's
  `-ir-output-name` — keep link output names stable or every edit pays
  a cold cache (speed only: payload history lives in the registry).
- A fresh image chain (new page, restarted dev server) needs a FULL
  first patch — the loop forces it (`forceFullPatch`); the production
  dev task owns this contract.
- Observed ONCE, never reproduced (3 clean runs + the deterministic
  gate): a warm-cache multimodule link crashed with
  `kotlin.createString is not found in fragments`. If it recurs,
  capture the ic-cache dir before wiping — it smells like a
  stdlib-fragment/cache interaction the whole-world path never hits.
- Concurrent images: `LREPL_WORK=<dir>` per session; cell-built and
  plain-built modules cannot share class types.
- Regression gates per compiler change: `browser/test.mjs` (the full
  v1–v16 delta-loop gate — patches, migration, dispatch, rejections,
  ghosts, language features), `lrepl/registry_test.py` (image
  registry), `--demo cells` (versions + migration),
  `survival/driver7.mjs` (slots), `swank_test.py` (frontend).
