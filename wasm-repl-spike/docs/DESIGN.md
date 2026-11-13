# lrepl — design

A Lisp-like live-image REPL for Kotlin/Wasm: definitions are compiled into
a running WebAssembly image, redefinitions are visible to already-compiled
callers, and existing objects follow their class forward. No Gradle
anywhere in the loop. The product destination is Compose hot reload in the
browser (§8).

## 1. Why wasm GC makes this possible

Three engine/toolchain properties do the heavy lifting (all validated in
`survival/`):

- **Type identity is content-addressed and stable across modules.**
  ir2wasm emits deterministic rec groups (sorted by stable `cityHash128`
  signatures, cross-module-stable encodings); V8 canonicalizes
  structurally identical rec groups into the *same* runtime types, so two
  independently compiled modules declaring the same class with the same
  text get unified types — objects from one are accepted by the other's
  typed signatures. Identity survives *growth* (a module with ten extra
  unrelated classes still unifies shared types); what breaks it is a
  changed *shape* — and the engine then rejects the mistyped value at the
  boundary, which is the signature-checking we want for free.
- **Dispatch is object-borne.** Every object stores its vtable (field 0);
  virtual calls load it and `call_ref`. The vtable belongs to the
  object's *creating* module, so new code calling old objects dispatches
  into whichever module defined their class — position-independent by
  construction.
- **The host owns the linking.** Instantiation imports are plain JS
  values; the host supplies *wasm funcrefs* (older generations' exports)
  where `@WasmImport` declarations expect them, and V8 type-checks every
  supplied funcref against the declared (canonically-unified) signature at
  instantiation. Incompatible redefinitions are not representable — they
  fail loudly.

## 2. The generation model

Every submission becomes one wasm module ("generation"):

```kotlin
@file:OptIn(kotlin.wasm.ExperimentalWasmInterop::class)
<import lines harvested from submitted code>
<frozen text of every class declared so far>          // identical text ⇒ identical rec groups
@WasmImport("host", "fib") external fun fib(n: Int): Int   // prior bindings as imports
@WasmImport("host", "p__get") external fun p__get(): Pt
…<the submission's new code, references rewritten>…
```

Compiled (klib step, then `-Xinclude` link), instantiated with
`{host: <funcrefs from older generations>}`, registered. Nothing ever
re-runs: a generation is the **slot owner** — a function's mutable slot
global and its exported setter live in its *defining* generation forever;
a redefinition installs the new entry into that setter. It is *not* the
history holder (that's the image registry, §6) nor the identity/state
owner (identity is the cell; state is reachable from anywhere, including
JS). Cost today: ~0.5 s and ~0.5–1 MB per line, stdlib re-linked every
time — the numbers the delta loop attacks (§7).

## 3. The CL correspondence, image level

In Common Lisp the compiler lives *in the image*: `compile` links into
mutable function cells (`setf symbol-function`), classes redefine in
place behind stable wrappers, and there are no "generations" because
loading code never mints a new runtime. V8 has no such instruction: new
code must arrive as a new `WebAssembly.Instance`, and a wasm global
cannot move between instances — the slots and objects a generation
defines live *in* it. So a generation is one `compile-file` + `load`,
made visible only because wasm makes the loader's unit explicit; the
chain of generations *behaves* as one image.

| CL | here |
|---|---|
| mutable function cells | slot globals: every call reads the mutable funcref (§5) |
| redefinable classes, SBCL wrappers + lazy update | cells + payload versions + lazy in-place migration (§6) |
| `defvar` keeps a bound value cell (initform is cold-boot-only) | object holders adopt the previous live instance at init (§7); top-level vals remain `defparameter` pending an opt-in |
| class registry lives in the image (`find-class`) | the image registry: version history in the module's custom section, ferried by the host (§6) |
| `compile` in the image, no fs | resident compile server + wasm bytes over a pipe (§10) |

Won't fully converge: the compiler is external (a resident JVM — the CL
analog is an out-of-image cross-compiler), and the image is physically a
chain of instances whose boundaries are the engine's type-police. Deltas
shrink the chain toward "one base + the last patch" (§7).

## 4. Bindings and their lowering

| kind | how it lives in the image |
|---|---|
| `class` | frozen text re-declared per generation; identity via rec-group unification (§6) |
| `fun` | slot-backed (§5); the slot-reading entry is the ABI for all callers |
| `val`/`var` | real top-level property in its defining generation + exported `name__get` (`name__set` for var) accessors; later references are rewritten to accessor calls |
| `object` | rewritten to `class Foo__obj { … }` + a single shared instance treated like a val |

- **Vals are lazy** (`WasmPropertyLazyInitLowering`): an initializer can
  first run inside a *later* generation — any WASI it uses must run
  against the *defining* generation's memory (§9). **Val redefinition**
  registers a new binding; the old object stays reachable through the old
  accessor (Lisp-image semantics). Var state is shared via get/set.
- **Type recovery**: an un-annotated val's type is read back from the
  accessor's wasm signature (`(ref null $Pt)` → `Pt`); i32 is ambiguous
  (Boolean/UInt/Char collapse) — annotate.
- **The frontend lives in the compile server, not the driver.** Each
  submission goes to a second resident JVM (`CompileServer.kt` `parse`
  mode — real Kotlin PSI; the IntelliJ Application is a JVM-wide
  singleton) with the binding table; that server parses *and rewrites*
  (references to val/obj bindings → accessor calls, var assignments →
  setter calls, locals shadow bindings — offset-precise on PSI ranges so
  strings/comments are untouched), returning final texts plus the exact
  bindings each piece uses. Protocol and rewriting rules are documented
  at the top of `CompileServer.kt`. The driver (`lrepl.py`) owns only
  policy and plumbing — slots, the binding registry, module assembly.
  Still missing: resolution (Analysis API would kill the i32 ambiguity)
  and overload support.

## 5. Function slots — true redefinition (compiler-generated)

The REPL runs on **`-Xwasm-hot-reload`**: ir2wasm emits, per reloadable
function,

```wat
(global $__slot$f (mut (ref null $funType)) (ref.func $fImpl))
(func $entry    (type $funType) args… global.get $__slot$f call_ref)  ;; the real symbol
(func $__hot_set$f (param (ref null $funType)) global.set $__slot$f)  ;; exported
```

The entry is registered under the function's real reference key, so every
call site, vtable/itable entry and function reference resolves to it and
reads the slot per call — raw typed funcrefs, no boxing. The driver only
book-keeps which export names each generation owns (reference-key hash
suffix for overload disambiguation) and installs the new generation's
entry into the ORIGINAL generation's setter (the daemon's `swapDirect`).

- **Compatible redefinition**: everyone funnels through the one slot;
  all callers see the new body immediately. Vtable entries reference the
  entry, so a method-body swap updates already-existing instances
  (gated in `survival/driver7.mjs`).
- **Julia semantics for definitions** (Kotlin has overloading; CL's
  one-function-per-name model is wrong here): a name is a generic
  function; each (name, signature) is a *method* with its own slot. A
  **new signature ADDS a method** (old callers untouched — overload
  resolution picks at compile time); an **existing signature REPLACES**
  it (live slot swap). The engine's setter type check is the judge of
  signature identity.
- Measured cost (`survival/bench_direct.kt`, fib(34)): plain 15.9 ms vs
  slotted 22.0 ms — ~0.27 ns/call.

`:dis NAME` shows the raw entry (`local.get…; global.get $__slot$…; call_ref`).

## 6. Classes: cells, payload versions, and live migration

Under `-Xwasm-hot-reload` every non-interface class gets the **cell
encoding**: the class struct becomes a *cell* — `{vtable, itable, rtti,
payload}` — whose shape never changes; fields live in per-class
**payload** structs behind the mutable `payload` field:

```wat
(type $kotlin.Any$payload (sub (struct (field (mut i32)))))            ; _hashCode
(type $Pt (sub $AnyCell (struct …vtable/itable/rtti…
         (field (mut (ref null $kotlin.Any$payload))))))                ; the cell: stable forever
(type $Pt$payload (sub $kotlin.Any$payload (struct …fields…)))          ; v1
```

- The payload field is typed with the ROOT payload type (`payload<Any>`):
  mutable fields are invariant in wasm GC subtyping, so every cell in the
  hierarchy declares the identical field type, and every payload version
  and subclass payload (all start with `_hashCode`) is a
  `payload<Any>` subtype.
- Payload structs subtype the superclass payload (flat field indices
  match `ClassMetadata.fields`) and are always *open* so versions chain:
  `payload<$key>` is v1, `payload<$key>.#vN` the later versions —
  synthetic signatures derived from the class reference key, so every
  generation re-emits the history byte-identically and
  cross-generation canonicalization keeps old instances, old code and
  new code in one type universe.
- The vtable field is root-typed (Any's vtable) and mutable too — the
  same invariance trick — so the cell type is stable across METHOD-SET
  changes as well: an added open method grows the vtable struct (a new
  type) but no wasm signature mentioning the class changes. Field and
  method changes are both cell-internal events; casts, `is` and every
  method signature stay stable.

What each edit kind does (the engine judges everything; the driver only
does nominal matching and swap orchestration):

| edit | mechanism | old instances | old code |
|---|---|---|---|
| **body-only** | byte-identical rec groups → same runtime type; driver sweeps the method slots | run new bodies via vtables | run new bodies via slots |
| **add fields** | new payload version `(sub $anchor)` (largest prefix-compatible ancestor); allocation news it; field access goes through the per-class stepper `__hot_payload$<Class>` (ref.test fast path, else migrateFrom: copy surviving fields by name, default new ones, swap the cell's payload in place) | migrate lazily on first new-code field access — identity preserved, surviving values kept | keep reading surviving fields of migrated instances (upcast) |
| **delete fields / change types** | new payload is a *sibling* (wasm subtyping extends field lists, never shrinks) | migrate (doomed fields read out, then dropped) | touching the changed field **traps at its version cast** — the invalidation trampoline: the fields are gone, not shadowed |
| **back to a previous shape** | that version IS current again; no new version, no migration | later-version holders pass the cast as an upcast — the trap *heals* | work as when the shape was first current |
| **add an OPEN method** | vtable struct grows, but it's behind the root-typed mutable field — no signature changes; virtual dispatch lowers to test → (cast + read slot) \| (call the universal `__hot_vtable_update(cell)`, retry), which reads the RTTI klassId and installs the CURRENT vtable + itable | migrate their vtable on next dispatch — the new method is callable on instances born before it existed; overrides still win | keep working: a grown vtable is a SUBTYPE of the old one |
| **add an interface default method** | same stepper (the itable installs with the vtable) — retroactive interfaces reach old instances | migrate on next interface dispatch | keep working |
| **method signature change, class-supertype gain, finality flip** | real shape changes: the cell SUBTYPING CHAIN is part of every signature mentioning the class — the engine rejects the swap; that rejection IS the change detector. Bindings typed at the old version are reported stranded | remain the old type, usable by old-version code | can't touch them; `is` agrees |

Migration is one-shot and in-place (payload for fields, vtable for
methods): the cell is never reallocated, so every reference — globals,
other fields, arrays, JS-held values — sees the new world. Old code never
migrates *up*: it reads shared prefix fields / prefix vtable slots
(additions — old payload and vtable types are supertypes of the new ones)
or fails its own version cast (deletions/type changes) — honest,
engine-enforced, no half-states.

**The CL correspondence, per mechanism** — CLOS/AMOP class redefinition
rebuilt on wasm GC types:

| here | Common Lisp |
|---|---|
| the cell `{vtable, itable, rtti, payload}`, shape never changes, identity preserved | SBCL's wrapper split: redefinition swaps the layout behind a stable instance, never the instance |
| payload versions, one per redefinition, chained by subtyping | redefinition mints a new layout; live instances keep the obsolete one until updated |
| version *counter* per class | a redefinition is an ordered **event** in image history — the number names the event, not the content |
| allocation news the current version | `make-instance` always news the current layout |
| the stepper: ref.test fast path, else migrateFrom — copy surviving fields by name, default the new ones, swap in place | `update-instance-for-redefined-class`: obsolete instances update on **first access**, never eagerly; the update gets the added/discarded slot lists |
| back-shape redefinition: that version IS current again, the trap heals | redefining back to a previous shape makes that layout current; the update becomes a no-op |
| old code traps at its version cast — fields are gone, not shadowed | obsolete code touching a discarded slot errors; no shadow reads |
| `__hot_vtable_update`: test → miss → install current vtable → retry | method addition invalidates the generic function's discriminating function; the next call recomputes it |

The one deliberate divergence: CL layouts add and drop slots freely (the
AMOP update passes both lists); wasm GC subtyping can only extend field
lists — hence deletions mint *sibling* versions instead of subtypes, and
the trap (not a shadowed read) is the old-code story for them. Everything
else is the AMOP protocol with the wasm type system standing in for the
layout registry.

**Where the previous payload versions come from** — the **image
registry**: every hot-mode module embeds its COMPLETE payload-version
table as a `hot-reload-registry` wasm custom section (each class's
current version plus every historical version of bumped classes —
completeness is load-bearing: a class's FIRST bump needs the previous v1
shape, which an unbumped class's current entry provides), serialized
with the klib serializer's types-fragment codec for byte-identical
re-emission. The registry is a named custom section — static module
data, readable identically by any host (browser, Node, file tooling)
with no interop and no runtime footprint. The host reads it from the
live newest module (`WebAssembly.Module.customSections` in a browser,
plain file bytes in the REPL) and ferries it into the next compilation
as one hidden source declaration (`__hotRegistry { const val REGISTRY
= "<base64>" }`) parsed by the backend before lowering. The image is the
previous-state store: wiping the IC cache loses nothing — the cache
serves compilation speed only (`lrepl/registry_test.py` gates first and
second bumps across cache wipes). The registry is append-only and global
per image, not per file — a class that moves files keeps its history —
and dies with the image (`:reset` coherence). The version decision is a
pure function of the class's current fields and the previous versions,
computed identically by type generation, field-access codegen and
allocation, so multi-file modules agree without ordering. Roughly 100
bytes per class.

- **Interface `is` still lies** (the one upstreamable inconsistency):
  class `is` is structural, but interface `is` is RTTI-based and
  `interfaceId` is a stable NAME hash — after an interface shape change an
  old instance answers `is NewInterface` → true while `as` traps with
  `illegal cast` (`survival/driver5/6.mjs`; fix — fold the member set into
  the id — is on the ROADMAP).
- **Cells are mode-global**: a patch module must be built with the same
  flag — cell-built and plain-built class types do not unify.
  Function-only body donation still works cross-mode.

Inspectable from the REPL: `:dis ClassName` (cell struct, payload version
structs, vtable type, steppers); `--demo cells` (addition, in-place
migration on first access, back-shape heal, deletion trap); `--demo
classes` / `--demo hierarchy` (body swaps, the interface-`is` lie,
default-method swaps, constructor changes, object redefinition keeping
singleton state, engine-rejected shape changes); `--demo main`
(functions, Julia overloads, stdlib patching).

## 7. The delta loop — the browser dev loop

The browser loop (`browser/`) is the product shape: the delta IS the
compilation unit. The stdlib BASE is a separate wasm, emitted once per
fresh build dir by the same hot link (the stdlib klib ships IR, not
fragments — the cold link re-lowers it once with the flag, a uniformly
cell-built world; ~2,400 slots in the base) and instantiated once per
session. Every edit compiles a FILE-GRANULAR delta: in a hot+multimodule
link, a non-stdlib artifact's files partition by the IC's `isModified()` —
modified files' fragments are defined, clean files' declarations are
imported from the live patch chain under the artifact's own module name
(the multimodule `__fn$<tag>`/`__vt$`/`__rt$` convention is the ABI; the
glue maps that name to the live instances). No extra flag: the
whole-module loop is the all-files-dirty case, and a cold cache emits
the full patch by construction.

Per reloadable declaration, clean vs dirty:

| declaration | clean | dirty / new |
|---|---|---|
| function (incl. property accessors) | import the owner's entry as `__fn$<tag>` — no slot, no setter; the owner stays authoritative. Works because every caller — static calls, vtable/itable entries — resolves through the owner's slot-reading ENTRY, never the impl, so a later redefinition still reaches delta callers via the sweep. **THE invariant the design rests on.** | define: slot + entry + impl + setter; the glue routes the setter to the owning instance |
| top-level val/var backing global | never re-emitted — the global lives in its defining instance; state preserved by construction (CL semantics) | a NEW binding: new global initialized by the delta's `_initialize`; old accessors keep the old value reachable |
| class | types re-declared (wasm has no type imports; canonical rec groups unify them); `__vt$`/`__it$`/`__rt$` globals and constructors imported; instances unchanged | define + payload version + steppers as in §6; vtable growth handled by the existing stepper |
| inline function | hash-tracked by the IC: a changed inline function dirties all its callers | callers are dirty by propagation |

The universal `__hot_vtable_update` stepper is generated from the full
`linkerData.hotReloadableClasses` set and stays module-wide — old
instances' dispatch must keep working from any generation. Reactor init
ordering is preserved: the glue controls instantiate → init → sweep →
invalidate, and a rejected patch never inits.

Facts the glue is built on (all covered by the `browser/test.mjs` gate,
v1–v16):

- **The glue holds an instance CHAIN, not one instance** — a delta
  defines only its dirty files, so an export lives in the instance that
  defined its file last. `liveView` (glue.mjs) resolves names
  newest-instance-first; `applyPatch` sweeps EVERY live instance's
  setters — a redefined function's slot may live in any earlier
  generation.
- **Dead exports**: when the engine rejects a patch's entry at an
  owner's setter (a cell-chain shape change), that export is dead code
  for the live world; the live view marks it dead and keeps resolving
  the previous live version — newest-first resolution alone would let
  the rejected new body win the lookup.
- **The registry merges the previous module's table**: a delta's own
  gcTypes hold only its dirty files' classes, so the emitted
  `hot-reload-registry` merges the previous table in (own entries win) —
  otherwise clean files' payload history silently dies and their next
  edit breaks the version chain.
- **Dependency projections close over SUPERTYPES** (two link bugs the
  stress gate found): versioned payload references are recorded like
  the unversioned one, and projections close over supertype chains — a
  class's payload subtypes its SUPERCLASS's payload (Int's subtypes
  Number's, across stdlib *files*), and a grown vtable subtypes the
  previous one. Per-file closure in `makeProjection`; artifact-wide in
  `compileArtifactMultimodule`, the only site that sees the whole
  dependency universe.
- **A fresh image chain needs a FULL first patch**: the IC cache's
  "unchanged" is file-content truth, not image truth — the cache
  outlives a dev-server restart, the instance chain does not, so a
  never-edited file would arrive as an import against a chain that
  doesn't exist. The loop wipes the app's IC entries on a new chain
  (cheap; the stdlib's stay).
- **Object state holders are defvar**: an `object` (or companion)
  redefined by its own dirty file ADOPTS the previous live instance
  instead of re-running its initializer — CL `defvar` / JVM `<clinit>`
  semantics, reconstructed because a fresh module's value cell starts
  empty. The compiler emits the `boundp` check as a synthesized host
  import `__hot_adopt$<object>#<hash>`; the `#<hash>` is the slot
  sweep's disambiguator — the one naming piece stable across the klib
  round trip AND edits of the declaring file (adoption is exactly the
  dirty-file case; full identity tags are not stable across edits). The
  glue eagerly captures the previous live instance's VALUE at patch
  load, pre-sweep — the guarded store is lazy and can fire after the
  sweep has rewired the previous getter's slot, so capturing the
  entry's behavior would read the new, still-null global. A null (cold
  boot, brand-new object) means normal init. The engine type-checks
  the captured value against the declared cell type at the JS boundary
  when wasm receives it: an incompatible holder change is engine-gated
  twice — the sweep rejects the new shape's swaps like any cell-chain
  change (old code keeps running), and any live path that drives the
  new code's holder access traps at the adoption boundary. A field
  added to an adopted holder migrates via the payload stepper on first
  access, new fields getting type defaults (initforms are a listed
  quality item). The REPL answers null (its accessor machinery is its
  own state sharing — new-binding semantics unchanged there);
  top-level vals/vars keep defparameter semantics (annotation opt-in
  remains).

Measured (six-file demo app, honest numbers): the emitted function set
is exactly the dirty files' (11 slot installs for a body-only model.kt
edit vs 21 whole-module); a cross-file INLINE edit dirties the caller's
file through the IC and the delta re-emits both. Byte size barely moves
at demo scale (23–50 KB deltas vs a ~40 KB full patch) — types are
re-declared by design, so the win is the clean files' BODIES, which
must be proven at real-module scale. Warm edit-to-patch ~0.3–0.6 s
(before the resident server, base-emission skip and deltas: ~3.1 s).

**Remaining compiler work — function granularity**: unchanged functions
in a dirty file should import instead of re-emitting. The blocker is
that tags are identity-addressed (`cityHash64(referenceKey)` — stable
across edits, so imports are deterministic, but blind to body changes);
a per-function CONTENT hash over the post-lowering IR, riding the
registry round-trip as a manifest, would refine the dirty decision
within a file. The JS IC's inline-function hashing is the precedent and
already provides the caller-propagation inline needs.

## 8. The Compose layer — the remaining product work

The engine swaps *code*; Compose additionally needs the recomposition
machinery to accept it. The bar, which every reference system agrees on
(Vite HMR, React Fast Refresh — signature changes force a remount;
Flutter — tree rebuilt over preserved state; Compose Live Edit —
slot-table state via positional keys; CL/Smalltalk — everything, by
construction): edit-to-screen under a second from real file saves,
state survives, failures honest and in-place with the app running the
old code, inside the dev environment the user is already in.

- **Composables are functions**: the Compose plugin runs before the wasm
  backend, so slots apply to its output like any other function — body
  swaps work like any other. The new part is notification: after a slot
  sweep the glue calls a generated `__hot_invalidate()` that mutates the
  global snapshot state (the Recomposer's invalidation entry point), so
  changed scopes recompose. No Compose runtime changes needed for body
  edits.
- **Group keys are the hard problem** (Live Edit's hard problem on the
  JVM too): `remember {}` values live in the slot table keyed by group
  keys the Compose compiler derives from source structure; a body edit
  shifts positional keys and remembered state maps to wrong slots. The
  fix is Compose-compiler-team territory: reload-stable keys
  (content/structure-derived, not offset-derived) or a key-remap table
  shipped with the patch so the runtime re-keys the surviving slot
  table on swap. The one place the wasm backend can't solve it alone.
- **Lambda merging**: Compose's own lambda deduplication must not
  collide with the hot-reload callable-reference merging; slot mode
  already excludes merged-suspend-lambda classes for exactly this kind
  of reason, and the same exclusion applies to Compose-merged classes.
- **ViewModels and custom state holders** are plain classes — cells +
  payload migration already preserve them across field changes (the
  part Flutter and HMR do worst, done by the engine here).
- **Top-level state holders**: objects/companions are defvar — state
  survives the holder's own redefinition via init-time adoption (§7),
  and added fields migrate; top-level vals/vars await their opt-in
  annotation (ROADMAP §2).
- **Failure UX**: engine-rejected swaps surface in a Vite-style
  full-screen browser overlay with the engine's verdict; the app keeps
  running old code because nothing was swapped. Deleted-field traps on
  migrated instances become first-class overlay errors via the catchable
  invalidation exception (ROADMAP §5) naming class, field and held
  version.

Two hosts, one engine — the loop is identical except for the host
mechanics: wasm-js (browser glue in the page; registry via
`WebAssembly.Module.customSections`; exception payloads already readable
via `WebAssembly.JSTag`; no WASI) and wasm-wasi (the REPL daemon; the
same registry bytes read from the `.wasm` file; the exception tag must be
shared/exported). The engine — canonical rec groups, slots, cells,
payload versions, steppers — is target-agnostic V8 machinery.

The full target loop: editor save → IC recompile of the dirty module
(the backend emits the delta wasm + registry under the flag) → dev
server reads the live image's registry and feeds the compile (the image
is the previous-state store) → browser glue instantiates the delta with
imports resolved from the live instances (V8 type-checks every funcref),
sweeps slots, steppers migrate lazily → `__hot_invalidate()` touches the
global snapshot → recomposition; on rejection, overlay with the engine's
verdict, old code keeps running.

Deliberately REPL-only: the PSI rewrite frontend (the product's Gradle
task knows what changed), mini-WASI shims, `:dis`/binding-table UX, the
per-line generation model, the start-section binary patch. Nothing
load-bearing depends on them; the REPL stays the semantics lab where
engine behaviors get discovered first.

## 9. The runtime (daemon.mjs, REPL only)

One persistent Node process owns the image:

- **Registry of generations** — one `WebAssembly.Instance` per
  generation, retained forever (its objects and exports may be
  referenced by any later generation).
- **`host` imports** — resolved from the registry at instantiation:
  binding name → (owning generation, export name) → funcref.
- **Mini-WASI** — hand-implemented `fd_write` (captured per-call stdout;
  the *only* result channel — raw wasm exports hand Kotlin `String` to
  JS as an opaque GC struct), `random_get`, clock, environ stubs; any
  other WASI import stubbed with `() => 0`.
- **Per-instance WASI closures** — each instance's WASI functions bind
  to *that instance's* memory via a mutable ref filled right after
  instantiation; a shared memory ref silently corrupts the wrong
  generation (lazy initializers make cross-generation WASI calls
  routine).
- **Reactor init** — hot modules export `_initialize` and declare no
  start section, so init runs only when the daemon calls it, after
  memory capture.
- **`swap`** — hands an opaque function object (new impl, generation N)
  to a setter (slot owner, generation K) without either value crossing
  the JSON boundary.

The image side never reads the filesystem: the driver patches the bytes
in memory (start section → exported `__lrepl_init`) and keeps the newest
bytes in memory for the registry ferry — the CL `compile`-not-
`compile-file` shape. On disk only compiler/toolchain I/O remains:
`build/image.kt` (the compiler reads file paths; doubles as slime's M-.
target), `build/image.klib` (the two-step CLI's intermediate — the
`-Xinclude` link step is the only wasm producer), `build/gen{N}.wasm`
(debug dump for `:dis`), `build/ic-cache/` (speed only), `server.jar`
(the resident JVMs).

## 10. Latency

Warm per-line ~0.2–0.6 s: resident-JVM compile server (warm klib ~0.1 s,
falls back to fresh CLI invocations if it dies), `-Xcache-directory` IC
(warm link ~0.3 s), instantiation + call (tens of ms).

## 11. Frontends

- **terminal** — `python3 lrepl.py` / `--demo`.
- **swank/SLIME** — `python3 swank.py [port]`; `M-x slime-connect`.
  connection-info, `create-repl`, `listener-eval` (C-x C-e),
  `compile-string-for-emacs` (C-c C-c — whatever region slime's Lisp
  heuristics grab, the server re-submits the whole top-level declaration
  containing point, so C-c C-c Just Works inside class bodies),
  completions and `describe-symbol` from the live binding registry,
  find-definitions (M-. pins the defining file/offset in
  `build/image.kt`), autodoc, SLDB popups for runtime traps (the image
  stays usable inside the debugger; uncaught Kotlin exceptions are
  honestly labeled unreadable — module-local tags, §13).
- **stdio/comint** — `lrepl.py --stdio` behind `make-comint`; what
  kotlin-mode's `kotlin-send-region`/`-buffer`/`-block` talk to.
  Submissions split on blank lines; `import` lines harvested;
  `package`/`@file:` ignored.
- **browser (wasm-js)** — `browser/dev.mjs` serves a page running the
  loop in the product host (§7; `browser/README.md`; Node gate
  `browser/test.mjs` v1–v16). The harness for the Compose integration —
  engine behaviors were discovered in the REPL; they must hold up in the
  browser.

Swank and stdio are separate processes ⇒ separate images.

## 12. The one frontend-unrelated compiler patch

`FirWasmImportAnnotationChecker.kt` relaxes `isTypeSupportedInWasmInterop`
from "primitives only" to "anything except Char" (shared by `@WasmImport`
and `@WasmExport`): the restriction assumed a JS host that cannot
construct user-typed GC values, but a host supplying *wasm funcrefs* is
fully type-checked by the engine. An upstreamable version should gate
this behind an opt-in flag.

## 13. Failure modes & invariants (hard-won)

Lab-specific list; the cross-loop rules (dist-rebuild reset, IC
config-hash coupling, per-image work dirs) live in the ROADMAP's
operational notes.

- klib `-Xinclude` resolution breaks under `/tmp` on macOS (symlink).
- Raw wasm exports never convert Kotlin `String` ↔ JS strings **on the
  wasi target** — print inside wasm, capture `fd_write`. (On wasm-js,
  `String` wraps `JsString` — exports are directly readable; the stdout
  constraint is wasi-only.) A *null* String prints as nothing —
  concatenate instead of printing raw nullable strings.
- Top-level vals are lazy ⇒ initializers can fire inside other
  generations ⇒ per-instance WASI memory.
- Kotlin/Wasm wasi modules declare `(memory 0)` — 0 pages; the scoped
  allocator grows it via the `wasm_memory_grow` intrinsic on first use.
- Wasm exception tags are **module-local** ⇒ JS cannot extract Kotlin
  exception payloads; the driver's throwers `println` their explanation
  before throwing (fix: export the tag — ROADMAP).
- `f!!(x)` on a *nullable* function type infers `R?` — slots are
  non-null.

## 14. Validation anchors (`survival/`)

| experiment | result |
|---|---|
| cross-module typed value flow; virtual dispatch; `is`; growing type sections; changed shape (negative control); user-typed funcrefs via JS | ✅ unifies / dispatches into creator's body / ✅ / identity robust / engine rejects at JS boundary / full round trip |
| slot machinery (`driver7.mjs`): function swap, live-instance method swap, setter type check | ✅ regression gate, rebuilt per compiler change |
| version placement rule: old group byte-identical, new version in its OWN rec group (`subtyping/driver8.mjs`) | ✅ same group breaks the chain |
| add/delete/migrate semantics, hand-written (`cells/driver9.mjs`) | ✅ the compiler-automated behavior matches 1:1 |
| cell encoding cost (`bench_cells.kt`, 20M accesses) | ~2.5 ns/access; steppers add a call only for classes with versions |
| browser gate (`browser/test.mjs` v1–v16): body swaps with state preserved, field migration add+delete, hierarchy dispatch, additive + OPEN methods hot on v1-born instances, supertype gain engine-rejected, ghost instances trapping honest `illegal cast`, generics/vararg/extension hot, cross-file inline, held-lambda re-binding, itable defaults on live instances, object/companion defvar (state survives own redefinition + field addition; incompatible holder rejected whole) | ✅ the delta loop's full semantics beat list |

Each `.kt` pair + `driver*.mjs` reproduces one result.
