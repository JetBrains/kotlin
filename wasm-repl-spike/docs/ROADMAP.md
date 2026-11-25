# Roadmap — what works, and what to do next

The product context is **Compose hot reload in the browser** (wasm-js);
nothing load-bearing may depend on how the wasi REPL is wired; the
browser is the product host. The design — engine, delta loop, Compose
layer, measurements — lives in `docs/DESIGN.md`.

## The production target

A KGP dev task that watches sources → IC-compiles the changed module
klib → emits a delta wasm built with `-Xwasm-hot-reload` → the dev
server pushes it → the browser loader instantiates it against the
resident base and live chain → the delta's init resolves its bindings
against the image and the app updates with state intact.
Everything the prototype validated is the spec for that loop; the
REPL's remaining hacks are stand-ins for what the compiler/loader owns:

| prototype mechanism (stand-in) | production replacement |
|---|---|
| one module per line | patch modules — SHIPPED (browser loop): file-granular deltas over the IC's dirty set; the per-function manifest (§1 below) remains |
| REPL's `host` funcref imports | the multimodule `__fn$`/`__vt$`/`__rt$` channel — SHIPPED in both loops |
| PSI frontend in the compile server | REPL-only; the dev task knows what changed |
| checker relaxation (any-type `@WasmImport/@WasmExport`) | upstream, flag-gated |

## What works today

The engine and both loops are green on the full gate set (the
operational notes below name them). Each pointer's DESIGN section owns
the mechanism; this list owns the status.

- **Functions** (DESIGN §5, §15): fdefinition cells — a redefinition
  is one `struct.set` into the shared cell (load is apply); a different
  signature is a new overload (Julia semantics); an incompatible
  redefinition installs and callers trap at their own per-call cast.
  Defining a function whose simple name matches an already-resolved
  stdlib import REDEFINES the builtin natively — the driver emits the
  declaration under the builtin's package (-Xallow-kotlin-package), so
  its binding key equals the stdlib's and the load installs into the
  EXISTING cell: already-compiled callers flip with no driver bridge.
  Constructors and inline functions are excluded.
- **Classes** (DESIGN §6): cells + payload layouts — one shape per
  class per compile, no compile-time history anywhere; the `$layout`
  pointer and the class registry (register IS set-current); the type
  system as the fast-path check; update-instance + slot-value as the
  image's generic machinery (retyped slots default, deleted slots
  throw the catchable unbound-slot, function-typed slots ride the
  stdlib box). Cell-chain changes are engine-rejected — coexistence
  for genuinely new types.
- **Method additions hot** (DESIGN §6): the stdlib stepper installs
  the class's current vtable + itable + RTTI from the registry;
  interface default bodies swap on live instances, and a class GAINING
  an interface flips `is` and dispatch on pre-existing instances after
  one lazy migration.
- **The registry is the whole previous-state story** (DESIGN §6, §16):
  no dump, no injection, no custom section — wiping the IC cache loses
  nothing (`registry_test.py`).
- **The browser loop** (DESIGN §7): file-granular deltas over the IC's
  dirty set against a hot stdlib base; the glue keeps the instance
  chain (newest-first; engine-rejected patches never join, old code
  keeps running). Gated `browser/test.mjs` v1–v16.
- **Language features, stress-verified**: generics/varargs/extension
  receivers swap hot; cross-file inline propagates through the IC; a
  held top-level lambda re-binds; default bodies swap live.
- **Objects are defvar** (DESIGN §7): redefinition adopts the previous
  live instance; added fields migrate; incompatible holders are
  engine-gated twice. Top-level vals/vars remain defparameter (the
  opt-in is §2 below).
- **The REPL** (DESIGN §9–§11): the same base+delta architecture — one
  resident stdlib base owning both tables, 25–50 KB deltas per line —
  plus `:dis` and the swank/stdio frontends.

## Latency

| loop | warm per-edit | cold boot |
|---|---|---|
| REPL (25–50 KB deltas over the base) | ~0.3 s | ~3 s (IC cache warming) |
| browser (delta loop) | ~0.3–0.6 s | ~3 s (first full patch) |

Both loops compile through the official Kotlin compile daemon — the
same resident service Gradle drives, which already dispatches the wasm
target — fronted by a protocol shim (no JVM spawn per edit; the daemon
outlives any one loop run). The browser link skips the byte-identical
stdlib base after first emission, and delta emission ships only the
dirty files. The remaining
lever is the per-function manifest (below) — and at real module scale,
where clean files' bodies dominate, the size/latency win is much
larger than the demo's (whose bytes are mostly fixed machinery:
re-declared types, cell/payload structs, string pool).

## What to do next

### 1. Function-granularity manifest + real-scale proof — PARKED

The sound design is fully built and parked on branch
`wasm-hot-reload-manifest-wip` (commit 789dcb8f196f; design summary in
its message): rows of the `hot-reload-indices` section carry a third
column (cityHash64 of the binding's rendered wasm body), the skip
decision runs at LINK time over all linked fragments (emit-then-drop —
a body's hash must include the CONTENT of non-reloadable module-local
callees: a constructor keeps its stable signature while a field edit
changes the shape it constructs), with cycle-poisoning and
content-rendered pool/global dependencies. One gate beat (browser v3:
fresh-instance initializer after a field addition) still fails for an
undetermined reason — the delta provably re-emits the factory and
constructor, and the trace is correct on paper; it was one diagnosis
from green when parked.

Why parked: the cost curve (~+800 lines, correctness subtleties
surfacing per gate beat) outweighed a payoff that is deferred — the
win only materializes at real-Compose-module scale (the Compose
runtime now links in both arms — see §10 — so nothing upstream
blocks a real-scale delta);
demo-scale deltas are already 38–49 KB, under the 50 KB gate. Resume
when real-scale need returns; everything learned is in the branch.

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

### 5. Migration quality + module GC

- initforms for new fields (type defaults today);
- unbound-slot diagnostics: the deleted-slot throw is catchable
  already, but its message names raw tags — name the class, field and
  layout; and the retyped-GETTER case is a raw fdefn-cast RuntimeError
  (the getter's wasm signature changed), not a typed exception;
- incompatible-signature degradation (sketch, not built): generate a
  same-signature thrower per function and let the host install it into
  the old cell, so stale call sites raise a catchable exception naming
  the old signature;
- layout pruning is DONE by construction: the registry keeps ONE entry
  per class (register IS set-current), and superseded layout objects
  live only while old instances' `$layout` pointers hold them —
  native GC;
- **module GC**: the fdefn encoding makes it native — a donor module
  collects once no cell holds its funcrefs (the local `survival/` labs'
  probes; cells do not pin their defining module). The remaining cap is
  the V8 cross-instance `call_ref` retention (the gc_probe lab, local
  scratch, preserved on the `wasm-hot-reload-table-era` branch) —
  upstream; file it. The
  instance chains themselves are host-retained by design (export
  resolution); pruning them is a glue-side policy question.

### 6. Exception tag export (upstreamable)

Tags are module-local, so Kotlin exception payloads are invisible from
JS. Export the tag; the host can `WebAssembly.Exception.getArg` the
Throwable and print it properly.

### 7. Upstreaming

The checker relaxation (flag-gated), the exception tag export, and
long-term hot-reload mode + the cell/payload/fdefn encoding as
experimental modes (the multimodule `__fn$/__vt$/__rt$` machinery is
precedent).

### 8. Foreign-binding entries — kill the host funcref channel — PARKED

The last driver→runtime move: a delta referencing an older
module's binding emits `@WasmImport("host", …)` and the driver
supplies the old instance's exported funcref at instantiation
(daemon.mjs's `hostFns` loop, glue.mjs's host proxy, lrepl's
`gen_import_lines`/`find_export` — ~80 lines across both loops). The
image already holds every binding by exact name, so the compiler
could instead emit a FOREIGN-BINDING ENTRY — the standard cell-reading
trampoline plus a MUTABLE cell global the init populates from
`hotFdefnsLookup(name)` (a stdlib function that exists); no host
import, no funcref over the JSON boundary. Val accessors and foreign
constructor references ride the same pattern; type recovery stays (it
types the source, not the import).

THE SEMANTIC TRADE (why parked): today an incompatible old-reference
signature rejects the whole patch AT INSTANTIATION (clean browser
overlay, "nothing swapped"); with cell routing the patch loads and
the failure moves to the per-call `ref.cast` — per-binding, catchable,
exactly SBCL's type-erased fdefinition contract, but a different
failure mode than the gates assert. Class-cell shape rejections are
unaffected (they die at instantiation for type reasons regardless).
Parked pending that call; the passenger candidate found in the same
audit: lrepl's `reset()` still wipes the IC cache on table-era "baked
indices" grounds — with nothing baked anymore, the wipe (and the slow
`:reset`) may be unnecessary.

### 9. String-free emitted init (hash the binding key) — PARKED

After the move wrapper's death, the binding NAME pushed into each
resolve call is the last string in emitted hot code — the only
wasi-vs-js divergence left (JS-string import constants vs the string
pool `_fieldInitialize` populates, which is why the resolve loop must
follow it). Hashing the fdefn table's key (cityHash64, the classId
and slot-name-tag channel) would make the emitted init string-free:
resolve could run FIRST, the two targets' inits would be literally
identical, and GENESIS would be deletable (cell globals init to null;
the resolve stores the shared cell before any initializer runs) —
one minted fdefn per binding per module gone.

PARKED on the identity trade: the table's keys are exact strings
today — collision-free by construction — and the user does not want
a content hash as binding identity (the classId and slot-tag hashes
were accepted for NAMING and copy-guarding, not as the fdefinition
table's key; a collision there shares a cell silently, and the keys'
readability — `(room: inspect)` — dies too). Any future unification
must find a string-preserving channel instead (an early-available
stdlib string interner, or a differently-shaped ordering), not
hashing. The audit fact that makes this worth revisiting: the resolve
BuiltIn calls are direct remaps, not cell-routed — resolve-first has
no bootstrap circle.

### 10. Plain third-party klibs under the hot link — RETRACTED

Originally diagnosed as a resolver gap (a missing
`hot.reload.payload/payload<$kotlin/Enum>` key when linking the
plain kotlin-test klib); on re-verification the exception was an
IC artifact of a mis-shaped CLI command (hot multimodule links
REQUIRE `-Xcache-directory`, and the link's `-ir-output-name` must
differ from the klib step's). With a well-formed command the PLAIN
kotlin-test klib links and runs identically in hot and plain arms
(eight kotlin.test-heavy box tests re-verified both ways), and the
runtime side needed only a runner fix (library modules keep their
real artifact name, `kotlin-<name>.wasm` — the app module alone is
`_<name>_.wasm`). Plain third-party klibs are FINE in hot links:
nothing blocks production apps on this front.

The same class of misdiagnosis briefly became "the Compose runtime
klibs don't deserialize on our dist" (every Maven
`runtime-wasm-js` 1.6–1.10 failing with `Inline class has no field:
MultiValueMap`). RETRACTED: the compose runtime's `depends` list is
resolved by klib `unique_name`, and the CLI's flat `-libraries` has
no transitive resolution — one missing dep (`androidx.collection:
collection` vs the differently-named `collection-internal`, or
browser/annotation-internal) makes the partial-linkage machinery
stub the unresolved module's classes WITHOUT the backing property,
and the wasm backend then dies on the stub. With the FULL closure
(`org.jetbrains.compose.runtime:runtime-wasm-js:1.9.3` even
REDIRECTS to androidx 1.9.4; its pins: coroutines 1.8.1, browser
0.3, atomicfu 0.27.0, androidx.collection 1.5.0) every version
links and runs on our dist in BOTH arms — including the originally
"broken" 1.8.0. The deserializer was never at fault. (A KGP-style
`-Xinclude=<classesDir>` link is the sanity check: gradle builds
the same klibs fine.)

**The Compose macrobenchmark, milestone-1 harness (runtime +
coroutines, headless composition driven from JS; wasm-js, node
25, 3-run medians).** Plain monolith: 4,824,239 B raw / 1,059,704 B
gz; recomposition (`bumpRemembered`) 189 ns, invalidation
(`__hot_invalidate`) 127 ns. Hot image (10 module instances, base =
hot stdlib): 13,229,688 B raw / 2,968,707 B gz = 2.7×/2.8×;
recomposition 965 ns = **5.1×**, invalidation 811 ns = **6.4×**.
The gap dwarfs the fib bench's 1.5–1.6× because this workload is
cross-module state traffic: every `mutableStateOf` read/write and
every runtime call is a fdefn-cell hop across module instances, and
state fields run the layout-checked slot-value path — the encoding
overhead compounds per access, not per call site. The raw-size gap
is partly "no DCE in the hot image" (each library module ships its
whole library), not encoding overhead alone.

## Operational notes

- After `./gradlew dist`: delete the IC caches AND the cached stdlib
  BASE wasm files (browser: `app/build/kotlin-kotlin-stdlib.*`), and
  wipe the WHOLE `lrepl/build` — stale `gen*.klib`/`image.klib`
  fragments from the previous compiler deserialization-trap with
  `IllegalStateException: UnexpectedEnd` on the first compile (the
  lrepl base-full stamp re-links automatically). The base is
  byte-identical across edits but NOT across compiler changes — a
  stale base links with confusing type-canonicalization errors or
  traps in init. `server.jar` no longer needs deleting — the shim
  doesn't embed the compiler; the daemon is keyed on the dist's jars
  (`CompilerId` + an MD5 classpath watcher) and restarts itself when
  they change in place. Rebuild server.jar only when the shim source or
  the daemon-client API changes. Stray daemons: they die on the 2h idle
  timer, or kill them with `java -cp
  dist/kotlinc/lib/kotlin-daemon-client.jar
  org.jetbrains.kotlin.daemon.client.KotlinCompilerClient --stop`.
- The IC cache root is keyed by a config hash covering the link step's
  `-ir-output-name` — keep link output names stable or every edit pays
  a cold cache (speed only: no state lives anywhere but the image).
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
- Before gating, verify the dist jar actually contains the change:
  grep the new symbol names in `BackendWasmSymbols.class` inside
  `kotlin-compiler.jar` (Kotlin property access does NOT put a symbol
  name in the CALLER's class file — grepping BodyGenerator.class for
  `wasmSymbols` members finds nothing even in green builds), and never
  read a build's success from a `cmd | grep; echo rc=$?` — that prints
  GREP's exit code, not the build's.
- Regression gates per compiler change: `browser/test.mjs` (the full
  v1–v16 delta-loop gate — patches, migration, dispatch, rejections,
  ghosts, language features), `lrepl/registry_test.py` (the
  image-resolves-everything acid test: class bumps across IC-cache
  wipes), `--demo main` (functions, overloads, the stdlib override),
  `--demo cells` (layouts, migration, retype, funcref slots),
  `swank_test.py` (frontend).
  Zero `Traceback`/`AttributeError` in every log — exit 0 alone is not
  green (`--demo` tolerates per-line errors; the `:dis` probe on an
  inline stdlib function prints an informational error by design).
