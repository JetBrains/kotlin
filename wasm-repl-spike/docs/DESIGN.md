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
  values; the host supplies *wasm funcrefs* (older modules' exports)
  where `@WasmImport` declarations expect them, and V8 type-checks every
  supplied funcref against the declared (canonically-unified) signature at
  instantiation. Incompatible redefinitions are not representable — they
  fail loudly.

## 2. The submission model

Every submission becomes one wasm module:

```kotlin
@file:OptIn(kotlin.wasm.ExperimentalWasmInterop::class)
<import lines harvested from submitted code>
<frozen text of every class declared so far>          // identical text ⇒ identical rec groups
@WasmImport("host", "fib") external fun fib(n: Int): Int   // prior bindings as imports
@WasmImport("host", "p__get") external fun p__get(): Pt
…<the submission's new code, references rewritten>…
```

Compiled (klib step, then `-Xinclude` link), instantiated, initialized
— and that is the whole story: LOAD IS APPLY. The module's init
resolves its bindings' fdefinition cells BY NAME against the image's
table and installs its impls (§15), so the newest definition is live the
moment loading finishes. Nothing is re-run and nothing is swept; the
module is neither the state owner (the image's tables are — §15)
nor the identity owner (the shared cell is). Cost today: ~0.3 s and
25–50 KB per line over the once-per-dist base (§7).

## 3. The CL correspondence, image level

In Common Lisp the compiler lives *in the image*: `compile` links into
mutable function cells (`setf symbol-function`), classes redefine in
place behind stable wrappers, and nothing is chained because
loading code never mints a new runtime. V8 has no such instruction: new
code must arrive as a new `WebAssembly.Instance`, and a wasm global
cannot move between instances — the bindings and objects a module
defines live *in* it. So a submission is one `compile-file` + `load`,
made visible only because wasm makes the loader's unit explicit; the
chain of instances *behaves* as one image.

The row-by-row correspondence is §6's CL table. At the image level:
fdefinition cells (§5, §15) for mutable function cells, the class
registry (§6) for `find-class`, object adoption (§7) for `defvar`,
and the compile daemon (§10) for `compile`-in-the-image.

Won't fully converge: the compiler is external (a resident JVM — the CL
analog is an out-of-image cross-compiler), and the image is physically a
chain of instances whose boundaries are the engine's type-police. Deltas
shrink the chain toward "one base + the last patch" (§7).

## 4. Bindings and their lowering

| kind | how it lives in the image |
|---|---|
| `class` | frozen text re-declared by every module; identity via rec-group unification (§6) |
| `fun` | cell-backed (§5): the cell-reading entry is the ABI for all callers |
| `val`/`var` | real top-level property in its defining module + exported `name__get` (`name__set` for var) accessors; later references are rewritten to accessor calls |
| `object` | rewritten to `class Foo__obj { … }` + a single shared instance treated like a val |

- **Vals are lazy** (`WasmPropertyLazyInitLowering`): an initializer can
  first run inside a *later* module — any WASI it uses must run
  against the *defining* module's memory (§9). **Val redefinition**
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
  policy and plumbing — cells, the binding registry, module assembly.
  Still missing: resolution (Analysis API would kill the i32 ambiguity)
  and overload support.

## 5. Function bindings — fdefinition cells (true redefinition)

Both loops run **`-Xwasm-hot-reload`**, and every reloadable function
gets the fdefn-cell encoding (the full story: §15; the essentials):

```wat
(type $fdefn (struct (field (mut funcref))))                       ;; the fdefinition object
(global $__hot_cell$f (mut (ref null $fdefn)) (struct.new $fdefn (ref.func $fImpl)))
(func $__hot_entry$f (type $funType) args… global.get $cell struct.get $fdefn $fn ref.cast $funType call_ref)
```

The entry is registered under the function's real reference key, so every
call site, vtable/itable entry and function reference resolves to it and
reads the CELL per call — never the funcref. The cell belongs to the
IMAGE: the stdlib's fdefinition table owns it, and each module's cell
global is a stand-in resolved by name at load (the fasl loader's patched
fdefn addresses — SBCL's model). A redefinition is one `struct.set` into
the shared cell, visible to every caller, present and future; there are no
setters, no sweeps and no writers, and first definition and redefinition
are the same instruction sequence (LOAD IS APPLY).

- **Compatible redefinition**: everyone funnels through the one cell;
  all callers see the new body immediately. Vtable entries reference the
  entry, so a method-body swap updates already-existing instances.
- **No install-time type police** — SBCL's contract: the fdefinition is
  type-erased, an incompatible redefinition installs, and callers trap
  at their own per-call `ref.cast` with per-binding isolation (gated:
  browser v7/v16).
- **Julia semantics for definitions** (Kotlin has overloading; CL's
  one-function-per-name model is wrong here): a name is a generic
  function; each (name, signature) is a *method* with its own cell. A
  **new signature ADDS a method** (old callers untouched — overload
  resolution picks at compile time); an **existing signature REPLACES**
  it. The binding name is the exact string
  `watName#<sig-suffix>` — collisions are impossible.
- Generation GC is native: a donor module collects once no cell holds
  its funcrefs (`survival/fdefn` probe-2).
- Per-call cost: the prior encodings' indirection plus one `struct.get`
  and one `ref.cast` (the trap-at-call contract's price).
- Constructors and inline functions are excluded; callable-reference
  and merged-suspend-lambda classes stay plain to protect link-time
  deduplication (§15's scope rules).

`:dis NAME` shows the raw entry (`local.get…; global.get $__hot_cell$…;
struct.get…; ref.cast…; call_ref`).

## 6. Classes: cells, layouts, and interpreted migration

Under `-Xwasm-hot-reload` every non-interface class gets the **cell
encoding**: the class struct becomes a *cell* — `{vtable, itable, rtti,
payload}` — whose shape never changes; fields live in a per-class
**payload** struct behind the mutable `payload` field:

```wat
(type $kotlin.Any$payload (sub (struct (field $_hashCode (mut i32)) (field $$layout (ref null $HotLayout)))))
(type $Pt (sub $kotlin.Any (struct …vtable/itable/rtti…
         (field (mut (ref null $kotlin.Any$payload))))))      ; the cell: stable forever
(type $Pt$payload (sub $kotlin.Any$payload
         (struct (field $_hashCode (mut i32)) (field $$layout (ref null $HotLayout)) …fields…)))
```

- The payload field is typed with the ROOT payload type — kotlin.Any's,
  `{_hashCode, $layout}` — because mutable fields are invariant in wasm
  GC subtyping, so every cell in the hierarchy declares the identical
  field type. Every payload RESTATES the root's two fields (the wasm text
  format requires a subtype's field list to begin with its parent's), so
  the layout pointer is readable through the root type from any
  instance — one static `struct.get` (SBCL's wrapper, with the
  indirection removed: class identity and slots fused).
- The vtable field is root-typed (Any's vtable) and mutable too — the
  same invariance trick — so the cell type is stable across METHOD-SET
  changes as well. Field and method changes are both cell-internal
  events; casts, `is` and every method signature stay stable.

**Field access** — the fast-path test is the TYPE SYSTEM ITSELF:
`ref.test (ref $payload<current>)` on the instance's payload. Same-shape
instances read inline at full speed; an addition-migrated payload still
subtypes (its class's new layout chains onto the superclass's), so
surviving-slot reads keep the fast path. A genuine shape change
(deletion/retype mints a SIBLING payload — wasm subtyping only extends
field lists) fails the test and routes through the image. The `$layout`
field points at the layout object the payload was shaped by —
pointer-identity comparable, and how the generic machinery reads the
instance's own shape.

**The LAYOUT** — the class's current definition (the registry's
`find-class` entry), an ordinary stdlib Kotlin class (`HotLayout`: the class's CURRENT vtable and itable
— the dispatch half the stdlib stepper installs — plus the `allocate`
closure and a slot vector of
`HotSlot{nameTag, typeTag, reader, writer}`): the *tags* are the slot's
name hash and its type tag — the CLASS IDENTITY the backend already
uses everywhere (the content hash of the class's reference key, the same
id the RTTI carries as klassId; every slot type is a class, primitives
included, so Boolean and Byte are distinct despite sharing the i8 payload
kind) plus a nullability bit, so tag-equal means cast-compatible by
construction: the tag names the very class the writer's unbox cast
targets. The *reader/writer/allocate* are funcrefs the DEFINING module
compiled — per-slot closures with ONE erased signature each
(`(anyref) -> anyref` reader, `(anyref, anyref)` writer, `() -> anyref`
allocate; the concrete casts live inside, the fdefn entry's trick).
Every load registers its classes' layouts into the stdlib's table
(`kotlin.wasm.internal.hotLayoutsRegister/AddSlot`, ONE map classId →
current layout: registering IS setting current) and stores the returned
layout object into the class's module-local layout global, which
same-module allocations stamp; a module that only INSTANTIATES a
foreign class (a stdlib value-class box, KClassImpl) has no global
and asks the image for the class's CURRENT layout — so EVERY
instance names its shape and none depends on a "who touches its
fields" convention. Load is apply, like the fdefinition cells; the table
pins the defining modules.

**The generic machinery** — stdlib Kotlin (`hotLayoutsUpdateInstance`,
`hotLayoutsSlotValue`, `hotLayoutsSetSlotValue` in
`kotlin.wasm.internal`), built on the small wasm primitives (§16): it
touches only the ROOT types (the cell, the payload) and the layout
objects — all slot knowledge lives in the
defining module's closures, so migration is DISPATCH (one `call_ref` per
accessor kind), not interpretation:

- `updateInstance(cell, classId, toLayout)` — allocate a fresh payload
  of the target layout (type defaults baked; the initform refinement is
  a follow-up), copy the live `_hashCode` and every same-named
  same-TAGGED slot through reader→writer — a retyped slot is NOT copied
  (the copy would trap the writer's cast mid-migration and lose the
  whole instance; it takes its default, everything else still migrates)
  — then swap the cell's payload **in place** — every holder (globals,
  arrays, the host) sees the migrated instance; identity never changes.
- `slotValue(cell, nameTag, typeTag)` / `setSlotValue` — the OLD-code
  arms: derive the instance's CONCRETE class from its RTTI klassId (the
  same content hash the table keys on — inherited-field access through a
  supertype stays sound), update-to-current, then read or write the slot
  BY NAME+TYPE through the current layout — SBCL's `slot-value`. A name
  that is gone (or whose type changed) **throws** a catchable
  exception — the honest unbound-slot: the field is deleted, not
  shadowed.

What each edit kind does (the engine judges everything; the loops do no
bookkeeping at all — loading applies):

| edit | mechanism | old instances | old code |
|---|---|---|---|
| **body-only** | byte-identical rec groups → same runtime type; the load installs the new bodies into the shared cells | run new bodies via vtables | run new bodies via cells |
| **add fields** | a new-shape payload (one per compilation, keyed by the class); allocation news it; the defining module registers the new layout | migrate lazily on first new-code field access (update-instance) — identity preserved, surviving values kept, added slots read their type default | read surviving slots by name through the image (slower, same values) |
| **delete fields / change types** | sibling payload again — wasm subtyping only extends field lists | migrate (same-named same-typed slots copied; the rest defaults) | touching the changed slot **throws** at slot-value (catchable) — the fields are gone, not shadowed |
| **back to a previous shape** | the payload type re-declares the old shape — the engine's content canonicalization makes it the SAME canonical type the old code expects | migrate (update-instance to the re-registered layout) — same state, no trap | work as when the shape was first current |
| **add an OPEN method** | vtable struct grows, but it's behind the root-typed mutable field — no signature changes; virtual dispatch lowers to test → (cast + read slot) \| (call the stdlib `hotVtableUpdate(cell)`, retry), which derives the class from the RTTI klassId and installs the CURRENT vtable + itable from the class registry | migrate their vtable on next dispatch — the new method is callable on instances born before it existed; overrides still win | keep working: a grown vtable is a SUBTYPE of the old one |
| **add an interface default method** | same stepper (the itable installs with the vtable) — retroactive interfaces reach old instances | migrate on next interface dispatch | keep working |
| **gain an interface supertype** | nothing at the type level (interfaces erase from every signature) — the class's RTTI id list and itable slots are runtime DATA; `is` and interface dispatch install the current RTTI + itable via the stepper and retry | migrate on first `is`/interface dispatch — a pre-existing instance answers `is I` true and dispatches after one migration | keep working |
| **method signature change, class-supertype gain, finality flip** | real shape changes: the cell SUBTYPING CHAIN is part of every signature mentioning the class. Precision: the redefinition module itself loads (the new chain is self-consistent) — the rejection fires at the NEXT module importing a stranded binding (its old-typed funcref fails the engine's import check); that rejection IS the change detector | remain the old type, usable by old-version code | can't touch them; `is` agrees |

**The CL correspondence, per mechanism** — CLOS/AMOP class redefinition
rebuilt on wasm GC types:

| here | Common Lisp |
|---|---|
| the cell `{vtable, itable, rtti, payload}`, shape never changes, identity preserved | SBCL's wrapper split: redefinition swaps the layout behind a stable instance, never the instance |
| the payload — the swappable shape struct the cell carries, its `$layout` field stamped at birth | PCL's instance body: wrapper pointer + slot vector; the cell+payload split is the price of wasm's fixed GC object types |
| the LAYOUT object — the class's current definition: dispatch, slots and construction fused, pointer identity | SBCL's WRAPPER with the indirection removed: class identity and layout in one object |
| payload LAYOUTS — one per class per compile, keyed by the class | redefinition mints a new layout; live instances keep the obsolete one until updated |
| no shape naming at all — the payload type key is the class; identical shapes unify via the engine's content canonicalization | a redefinition is an event, not content; the update always runs and copies by slot name — "healing" is just a full copy |
| allocation news the current layout | `make-instance` always news the current layout |
| the LAYOUT: the class's current vtable, itable, slot descriptors and accessor closures, registered in the image's registry at load | SBCL's wrapper+layout pair, fused; `find-class` is a table lookup |
| update-instance: allocate-with-defaults, copy same-named same-TAGGED slots through the layouts' closures, swap in place, one shot | `update-instance-for-redefined-class`: obsolete instances update on **first access**, never eagerly |
| slot-value / (setf slot-value): update-to-current, then a BY-NAME read/write; a deleted slot throws a catchable exception | obsolete code touching a discarded slot errors; no shadow reads |
| `hotVtableUpdate`: test → miss → install current vtable + itable + RTTI → retry | method addition invalidates the generic function's discriminating function; the next call recomputes it |

The one deliberate divergence: CL layouts add and drop slots freely (the
AMOP update passes both lists); wasm subtyping can only extend field
lists — so deletions mint siblings instead of subtypes, and the trap
(not a shadowed read) is the old-code story for them. Everything else
is the AMOP protocol with the wasm type system standing in for the
layout registry — and the AMOP's *discarded/local/added slot lists*
that SBCL computes at redefinition time are here computed at RUNTIME
from the two registered layouts, so the compiler never sees any
previous state at all.

**Where the machinery lives** — the split of §16: the TABLES and the
generic machinery (update-instance's whole copy loop, slot-value's
scan) are stdlib KOTLIN, built on the backend's small primitives
(payload access, the layout read, the RTTI class id, the dispatch
cells, call_ref over the erased accessor signatures); only the accessor closures and the
init that names the module's own declarations are link-emitted
(payload-level access is below Kotlin's type system). A compile consumes no
previous state: wiping any cache changes nothing but compile speed
(`lrepl/registry_test.py` gates two shape bumps across cache wipes).
Pinning: the table holds the defining module's funcrefs, so the image
retains every module that still has live instances — the same
retention shape as the fdefinition cells. Known gap, accepted: the type
defaults of migration are NOT the class's initializers yet (the
initform refinement bakes them into the allocate closure — a listed
follow-up). Function-typed slots are NOT a gap: funcrefs cannot cross
the erased channel (the func and any hierarchies are disjoint), so the
value rides inside an ordinary stdlib box — the reader boxes it, the
writer unboxes it, and migration copies it like any other slot
(`--demo cells`' funcref beat).

- **Interface `is` still lies** (the one upstreamable inconsistency):
  class `is` is structural, but interface `is` is RTTI-based and
  `interfaceId` is a stable NAME hash — after an interface shape change an
  old instance answers `is NewInterface` → true while `as` traps with
  `illegal cast` (`survival/driver5/6.mjs`; fix — fold the member set into
  the id — is on the ROADMAP).
- **Cells are mode-global**: a patch module must be built with the same
  flag — cell-built and plain-built class types do not unify.
  Function-only body donation still works cross-mode.

Inspectable from the REPL: `:dis ClassName` (cell struct, payload
layout, vtable type, the layout's reader/writer/allocate closures);
`--demo cells` (addition, in-place migration on first access with the
added slot defaulted, back-shape heal, deletion trap, RETYPE — the
retyped slot defaults while the rest migrates and old code traps at
the getter's fdefn cast — and the funcref beat — a function-typed
slot copied through the stdlib box, read back by old code); `--demo
classes` / `--demo hierarchy` (body swaps, the interface-`is` lie,
default-method swaps, constructor changes, object redefinition keeping
singleton state, engine-rejected shape changes); `--demo main`
(functions, Julia overloads, stdlib patching).

## 7. The delta loop — the browser dev loop

The browser loop (`browser/`) is the product shape: the delta IS the
compilation unit. The stdlib BASE is a separate wasm, emitted once per
fresh build dir by the same hot link (the stdlib klib ships IR, not
fragments — the cold link re-lowers it once with the flag, a uniformly
cell-built world; ~2,900 cells in the base) and instantiated once per
session — and it IS the image: its stdlib owns both of the image's
tables (§15). Every edit compiles a FILE-GRANULAR delta: in a hot+multimodule
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
| function (incl. property accessors) | import the owner's entry as `__fn$<tag>` — no cell, no machinery; the owner stays authoritative. Works because every caller — static calls, vtable/itable entries — resolves through the owner's cell-reading ENTRY, never the impl, so a later redefinition still reaches delta callers through the shared cell. **THE invariant the design rests on.** | define: cell + entry + impl; the delta's init resolves the cell against the image's table and installs |
| top-level val/var backing global | never re-emitted — the global lives in its defining instance; state preserved by construction (CL semantics) | a NEW binding: new global initialized by the delta's `_initialize`; old accessors keep the old value reachable |
| class | types re-declared (wasm has no type imports; canonical rec groups unify them); `__vt$`/`__it$`/`__rt$` globals and constructors imported; instances unchanged | define + the class's CURRENT layout (payload struct, readers/writers/allocate) as in §6; vtable growth handled by the stdlib stepper |
| inline function | hash-tracked by the IC: a changed inline function dirties all its callers | callers are dirty by propagation |

The vtable stepper is STDLIB KOTLIN (`hotVtableUpdate`), like the rest
of the generic machinery: it looks the class up in the registry (the
same classId → layout map) and installs the
registered vtable + itable — old instances' dispatch keeps working
from any module. Reactor init ordering is preserved: the glue
controls instantiate → init → push (load is apply), and a patch whose
instantiation traps never inits.

Facts the glue is built on (all covered by the `browser/test.mjs` gate,
v1–v16):

- **The glue holds an instance CHAIN, not one instance** — a delta
  defines only its dirty files, so an export lives in the instance that
  defined its file last. `liveView` (glue.mjs) resolves names
  newest-instance-first. There is no sweep: loading applied the patch,
  and every caller reaches the new bodies through the image's cells.
- **Dead exports**: when the engine rejects a patch's instantiation (a
  class-cell shape change), that patch never inits and never joins the
  chain — the previous live version keeps running; the live view marks
  the failed patch's exports dead so newest-first resolution cannot
  reach half-applied state.
- **The registry is load-is-apply**: each loaded delta's init
  registers its classes' layouts into the base's registry, REPLACING
  each class's previous entry (newest definition live, exactly like
  the fdefinition cells) — and that is the WHOLE previous-state
  story: no dump, no injection, no compile-time history anywhere.
  Superseded layout objects stay reachable only while old
  instances' `$layout` pointers hold them — retention is native GC.
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
  import `__hot_adopt$<object>#<hash>`; the `#<hash>` is the one naming
  piece stable across the klib round trip AND edits of the declaring
  file (adoption is exactly the dirty-file case; full identity tags are
  not stable across edits). The glue eagerly captures the previous live
  instance's VALUE at patch load — the guarded store is lazy and can
  fire after the holder's body has been redefined, so capturing the
  entry's behavior would read the new, still-null global. A null (cold
  boot, brand-new object) means normal init. The engine type-checks
  the captured value against the declared cell type at the JS boundary
  when wasm receives it: an incompatible holder change is engine-gated
  twice — the patch's instantiation is rejected like any class-cell
  shape change (old code keeps running), and any live path that drives
  the new code's holder access traps at the adoption boundary. A field
  added to an adopted holder migrates via update-instance on first
  access, new fields getting type defaults (initforms are a listed
  quality item). The REPL answers null (its accessor machinery is its
  own state sharing — new-binding semantics unchanged there);
  top-level vals/vars keep defparameter semantics (annotation opt-in
  remains).

Measured (six-file demo app, honest numbers): the emitted function set
is exactly the dirty files' (11 cell resolutions for a body-only
model.kt edit vs 21 whole-module); a cross-file INLINE edit dirties the
caller's file through the IC and the delta re-emits both. Byte size
barely moves at demo scale (33–52 KB deltas vs a ~40 KB full patch) —
types are
re-declared by design, so the win is the clean files' BODIES, which
must be proven at real-module scale. Warm edit-to-patch ~0.3–0.6 s
(before the resident server, base-emission skip and deltas: ~3.1 s).

**Remaining compiler work — function granularity** (the per-function
manifest): PARKED — the design, its state and its one open gate beat
are ROADMAP §1.

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
  backend, so cells apply to its output like any other function — body
  swaps work like any other. The new part is notification: after a patch
  applies, the glue calls a generated `__hot_invalidate()` that mutates the
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
  collide with the hot-reload callable-reference merging; hot mode
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
- **The immediate blocker** (hit attempting the first compose app):
  linking the shipped Compose runtime klibs (2.0–2.3 era) crashes the
  compiler in `getInlineClassBackingField` — the value class's backing
  property does not survive klib deserialization (`MultiValueMap` is
  the first casualty; an upstream bug, independent of the hot-reload
  encoding). Repro: `compose/repro/`. Two unblock paths: fix the
  deserializer (upstreamable), or build the runtime klibs from source
  with this compiler (CMP clone at `~/sz-compose`, `ad506cafef` — the
  sz-harness precedent).

Two hosts, one engine — the loop is identical except for the host
mechanics: wasm-js (browser glue in the page; exceptions readable via
`WebAssembly.JSTag`; no WASI) and wasm-wasi (the REPL daemon; the
exception tag must be shared/exported). The engine — canonical rec
groups, cells, payload layouts, the migration machinery — is
target-agnostic V8 machinery, and the loops are one architecture now.

The full target loop: editor save → IC recompile of the dirty module
(the backend emits the delta wasm under the flag — the compile needs NO
previous state; the image resolves everything at load) → browser glue
instantiates the delta against the base + live chain (V8 type-checks
every import; load applies, and migration runs lazily through the
image's layouts) → `__hot_invalidate()` touches the global
snapshot → recomposition; on rejection, overlay with the engine's
verdict, old code keeps running.

Deliberately REPL-only: the PSI rewrite frontend (the product's Gradle
task knows what changed), mini-WASI shims, `:dis`/binding-table UX, the
module-per-line model. Nothing load-bearing depends on them; the
REPL stays the semantics lab where engine behaviors get discovered first.

## 9. The runtime (daemon.mjs, REPL only)

One persistent Node process owns the image — the same base+delta
architecture as the browser loop:

- **The resident base** — the FULL stdlib, linked once from the stdlib
  klib itself (`Image._ensure_base`: `-Xinclude=<stdlib klib>` + `-main
  noCall` + hot + multimodule + `-Xwasm-IC-generate-unchanged-modules`),
  instantiated once per image; its stdlib owns both of the image's
  tables. A `reset` drops it: the tables die with the base, so a fresh
  image is a fresh table. Why the FULL stdlib and not the app's
  reference closure: a REPL can reference ANY stdlib function in ANY
  later line — the delta links emit only a pruned stdlib slice as a
  byproduct, and a delta importing outside that slice fails to link.
  The base is stamped against the klib's mtime (a dist rebuild
  re-links it) and is dist-keyed, not image-keyed. The
  `-Xwasm-IC-generate-unchanged-modules` flag is load-bearing: with a
  warm IC cache the link otherwise skips emitting the base entirely
  (the emission skip keys on the IC cache's module info, not the
  output file).
- **The instance chain** — one `WebAssembly.Instance` per app
  delta, instantiated against `<kotlin>` = the base's exports plus the
  driver's `host` funcrefs (previous bindings' accessors), retained
  forever (their objects and exports may be referenced by any later
  module).
- **Mini-WASI** — hand-implemented `fd_write` (captured per-call stdout;
  the *only* result channel — Kotlin `String` is an opaque GC object on
  every target, §13), `random_get`, clock, environ stubs; any other WASI
  import stubbed with `() => 0`. One closure, bound to the BASE's
  memory — only the base imports WASI functions; deltas reach I/O
  through its stdlib.
- **Reactor init** — hot modules export `_initialize` and declare no
  start section, so init runs only when the daemon calls it; loading
  applies.
- **No state channel at all**: compiles consume no previous state —
  the image's layouts table resolves migration at load, so the daemon
  has no dump command and the driver injects nothing.

The image side never reads the filesystem: the wasm travels base64 over
the wire — the CL `compile`-not-`compile-file` shape. On disk only
compiler/toolchain I/O remains: `build/image.kt` (the compiler reads
file paths; doubles as slime's M-. target), `build/image.klib` (the
two-step CLI's intermediate — the `-Xinclude` link step is the only wasm
producer), `build/gen{N}.wasm` (debug dump for `:dis`),
`build/base-full.wasm` (the base, re-linked only when the dist changes),
`build/ic-cache/` (speed only), `server.jar` (the shim + parse JVMs —
the compiler itself lives in the Kotlin compile daemon).

## 10. Latency

Warm per-line ~0.2–0.6 s: compiles go through the official Kotlin
compile daemon — the same resident service Gradle drives, whose
`CompileServiceImpl` already dispatches `TargetPlatform.WASM` — fronted
by a protocol shim (`CompileServer.kt` compile mode: the `\x01`/RESULT
line protocol both drivers speak; falls back to fresh CLI invocations if
the shim or the daemon dies), `-Xcache-directory` IC (warm link ~0.3 s),
instantiation + call (tens of ms). The daemon is keyed by the dist's
jars (`CompilerId`): it MD5-watches them, so an in-place `./gradlew
dist` makes it restart itself — no stale-compiler footgun — and it
outlives the shim (pinned client-alive file), so a restarted loop
resumes on a warm compiler JVM.

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

## 13. Failure modes & invariants

Lab-specific list; the cross-loop rules (dist-rebuild reset, IC
config-hash coupling, per-image work dirs) live in the ROADMAP's
operational notes.

- klib `-Xinclude` resolution breaks under `/tmp` on macOS (symlink).
- Raw wasm exports hand Kotlin `String` to JS as an **opaque GC object
  on BOTH targets** — the js-string builtins carry literals and interop
  boundaries, not return values (a gate bug proved this the hard way).
  Anything the host must read goes through the print channel: capture
  `fd_write` on wasm-wasi, wrap `console.log` on wasm-js. Explicit
  `JsString` interop at exported boundaries is the app-level escape.
  A *null* String prints as nothing — concatenate instead of printing
  raw nullable strings.
- Top-level vals are lazy ⇒ initializers can fire inside other
  modules ⇒ per-instance WASI memory.
- Kotlin/Wasm wasi modules declare `(memory 0)` — 0 pages; the scoped
  allocator grows it via the `wasm_memory_grow` intrinsic on first use.
- Wasm exception tags are **module-local** ⇒ JS cannot extract Kotlin
  exception payloads; the driver's throwers `println` their explanation
  before throwing (fix: export the tag — ROADMAP).
- `f!!(x)` on a *nullable* function type infers `R?` — function cells
  are non-null.

## 14. Validation anchors (`survival/` — local scratch, not committed)

The `survival/` wat labs prove the engine behaviors by hand; they are
UNTRACKED local files (superseded labs preserved on the
`wasm-hot-reload-table-era` branch). The gates re-prove the compiled
versions per change; the anchors still quoted:

| experiment | result |
|---|---|
| cross-module typed value flow; virtual dispatch; `is`; growing type sections; changed shape (negative control); user-typed funcrefs via JS | ✅ unifies / dispatches into creator's body / ✅ / identity robust / engine rejects at JS boundary / full round trip |
| cell encoding cost (`bench_cells.kt`, 20M accesses) | ~2.5 ns/access; the fast path is a ref.test + inline read for every access |
| browser gate (`browser/test.mjs` v1–v16): body swaps with state preserved, field migration add+delete, hierarchy dispatch, additive + OPEN methods hot on v1-born instances, supertype gain engine-rejected, ghost instances trapping honest `illegal cast`, generics/vararg/extension hot, cross-file inline, held-lambda re-binding, itable defaults on live instances, object/companion defvar | ✅ the delta loop's full semantics beat list |

## 15. The fdefinition-cell encoding

The binding state of the image is SBCL's fdefinition model translated
to wasm GC:

- **The cell**: one canonical STANDALONE struct type
  `fdefn = (struct (field (mut funcref)))` — the fdefinition object.
  Cells are ordinary GC heap objects: they cross instance boundaries,
  JS can hold them, and a `(setf fdefinition)` is one `struct.set`.
  Standalone, NOT in a rec group: a rec group is the canonicalization
  unit, so standalone types declared elsewhere never unify with a
  grouped member.
- **The image**: the stdlib IS the image — two top-level runtime
  functions (`kotlin.wasm.internal.hotFdefnsRegister` /
  `hotFdefnsLookup` over a private `HashMap`) live in the RESIDENT base
  instance, and every module's resolve reaches them through the
  ordinary cross-module import channel, on BOTH targets. The base is
  SBCL's core: library code owns the binding state, and the host is
  not the runtime at all (it only wires adoptions). `(room: inspect)`
  is literally iterating the table's keys.
- **Call sites hold the CELL** (a per-module global), never the funcref
  — the fasl loader's patched fdefn addresses. The entry is
  `global.get cell; struct.get fn; ref.cast $sig; call_ref`.
- **The module init IS the fasl loader**: for each binding,
  `resolve(name, impl)` looks the cell up BY NAME against the running
  image (registering it on first definition), installs this module's
  impl, and patches the module's cell global. LOAD IS APPLY: first
  definition and redefinition are the same instruction sequence, and
  the newest definition in the image is live the moment loading
  finishes. No sweep. No writers.
- **GENESIS — cells are born live**: a defining module's cell global
  INITIALIZES to a locally-minted fdefn holding its own impl
  (`struct.new $fdefn (ref.func $impl)`), not null; the init's resolve
  then overwrites it with the image's shared cell. The core carries
  its fdefns already resolved, and fasl load re-points them at the
  image's — which is why the load path itself is cell-routed: the
  runtime machinery AND the JS interop string adapters are
  hot-reloadable like everything else, a freshly instantiated module
  dereferences live cells from the first instant, and
  bootstrap-ordering hazards cannot occur. The base's `_initialize`
  must still run — that is what registers its cells into the image's
  table.
- **The init order is pool-first**: `_fieldInitialize` runs BEFORE the
  binding-resolution loop (the string pool's addresses/lengths array
  starts null and only `_fieldInitialize` populates it; the wasi name
  literals read it). Genesis makes the window sound: entries serve
  live cells before any resolve runs, so initializers calling own
  bindings in that window run the same bodies the resolution is about
  to install.
- **Names are exact strings** `watName#<sig-suffix>` (the suffix stays:
  `IdSignature.toString()` is not injective), passed to the resolve as
  real kotlin.Strings through the target's own literal channel — the
  imported JS-string constants on wasm-js, the string pool on
  wasm-wasi. No custom section exists anywhere.
- **No install-time type police** — SBCL's own contract: the
  fdefinition is type-erased; a wrong-signature redefinition installs
  and callers trap at their own per-call `ref.cast`, with per-binding
  isolation.
- **Generation GC is native**: a donor module collects once no cell
  holds its funcrefs; cells do not pin their defining module.

The reloadable scope (see `isHotReloadableFunction`): program code AND
the library — except constructors, inline functions, the linear-memory
interop layer (`kotlin.wasm.unsafe` — a redefinition installed from
another instance would dereference raw pointers in the wrong memory),
and the callable-reference /
merged-suspend-lambda classes that link-time deduplication owns.
Sharing library bodies is what makes the stdlib-override story work:
one write into the shared cell reaches every caller.

## 16. The emitted-vs-stdlib split

Where the manual IR lives and why. The principle: **everything that is
STATE is stdlib Kotlin; what is emitted is ABI or below the type
system.**

Already ordinary library code (hotReload.kt): both tables
(fdefinitions, the class registry), the layout objects themselves,
and the WHOLE generic machinery — update-instance's copy loop,
slot-value's scan, the dispatch stepper — as plain Kotlin over the
backend's small primitives (thirteen today). The image's entire state store and semantics
live in readable Kotlin; nothing is dumped or injected anywhere.

Still emitted (≈300 lines of IR-builder code), in three families:

- **The ABI** (irreducible by construction): the cell-reading ENTRY —
  `global.get cell; struct.get fn; ref.cast $sig; call_ref` — is what
  every call site, vtable and funcref resolves to; it is per-signature
  and registered under the function's real key, and its body IS the
  per-call budget. Same for the genesis cell-global init (a `ref.func`
  of the module's OWN impl — Kotlin can't name a module-local raw
  funcref) and the per-binding globals.
- **Below the type system, tier 1 — the PRIMITIVES** (declared
  `implementedAsIntrinsic` in hotReload.kt, lowered in the backend):
  `wasmGet/SetPayload`, `wasmGetPayloadLayout` (the instance's own
  layout — one root-typed struct.get), `wasmGet/SetPayloadHash`,
  `wasmGetRttiKlassId`, `wasmGetCellVtable`/`wasmInstallDispatch`, and
  `wasmCallSlot{Reader,Writer,Allocate}` (the
  `wasm_call_ref` mechanism: push args, narrow the funcref to the
  erased accessor signature — the cast IS the type check — one static
  call_ref). Upstreamable as general reftypes intrinsics; each is ~10
  lines of lowering, and the non-hot image lowers them to
  `unreachable` (dead code there, so the plain stdlib build stays
  valid). NB the existing `wasm_call_ref` intrinsic cannot replace
  the three call shapes from SOURCE: its declared signature takes
  only the function, the argument list is patched in by IR lowerings
  after the frontend, and `funcref`/`typedfuncref` are not `Function`
  — these three ARE its source-callable rendering.
- **Below the type system, tier 2 — the LAST raw struct**: the fdefn
  cell's TYPE declaration plus the two primitives over it
  (`wasmFdefnNew/SetFn`), so the resolve is
  ordinary Kotlin. The cell stays a hand-emitted struct ON PURPOSE: it is the ONE measured hot path
  (every call: `global.get; struct.get fn; ref.cast; call_ref`), and an
  ordinary class field would add a payload load + shape check to every
  call — the layout objects took the Kotlin-class form only because
  their access is cold (migration).
- **Per-module orchestration — genuinely irreducible**: the master
  init (per-binding name literals + impl ref.funcs + per-class
  registry registrations) and the per-slot reader/writer/allocate
  closures (payload-level struct access + boxing, below Kotlin's
  type system). Each names THIS module's own declarations or performs
  raw payload access — neither can be library code by definition.

**The upstream asks** (each kills a specific emitted piece): a
Kotlin-visible PLAIN STRUCT declaration (no class cell — kills the
fdefn struct's type), and a source-callable `callRef` with reified
shapes (kills the three call primitives — `wasm_call_ref`'s patched
varargs are IR-lowering-only by design). With both, the emitted side
shrinks to the entries + the init + the closures, and the
struct-access primitives themselves are upstream candidates of the
same generality `wasm_call_ref` already has.
