# GMS — Generational Mark & Sweep (Kotlin/Native)

Design notes for the `gms` garbage collector under `kotlin-native/runtime/src/gc/gms/`.
This document describes what gms *is*, how a collection runs, and — because gms was
deliberately modelled on it — how each piece relates to **JavaScriptCore's Riptide**
collector. Read it before touching the barrier, the remembered set, the collection
policy, or the sweep.

> Scope: this is the *logical* design. Exact invariants live in the source comments,
> which are authoritative. File references are given so the two stay in sync.

---

## 1. One-paragraph summary

gms is a **non-moving, generational, concurrent, parallel mark-and-sweep** collector.
It is *not* a copying/semispace collector: there is no "eden space" you allocate into
and evacuate from. Generations are **logical**, tracked by a single sticky *old* bit
per object (the "sticky mark bit" scheme). A minor (**Eden**) collection traces and
sweeps only the young generation plus the *old→young* edges recorded by the write
barrier; a major (**Full**) collection re-traces and sweeps the whole heap and rebuilds
the old generation from scratch. gms is built *on top of* the existing CMS concurrent
mark-and-sweep machinery (`gc/cms`): it reuses the `ParallelProcessor`, the SATB
deletion barrier, and mark-termination detection, and layers the generational scheme
over them.

This is the same fundamental architecture as **JSC/Riptide**: non-moving, sticky-mark-bit
generational, concurrent+parallel. The comparison table in §11 maps each mechanism.

---

## 2. Design lineage: why it looks like JSC

Riptide (JavaScriptCore's GC) established a practical recipe for a **non-moving**
generational concurrent collector, which is exactly the constraint K/N is under —
object identity and raw interior pointers into the heap must stay stable, so evacuation
is off the table. The pieces gms borrows, and where they live:

| JSC/Riptide idea | gms realization |
|---|---|
| Sticky mark bits instead of a copied eden | `GC::ObjectData` *old* bit (`ObjectData.hpp`) |
| Eden vs Full driven by heap-growth ratio | `choosePolicyScope` / `fullGrowthTriggerPercent` (`GmsCollectionPolicy.*`) |
| Generational write barrier that filters on the **source** object's age | `shouldRememberOldToYoung` (`Barriers.cpp`) |
| Validate a remembered slot's owning object before reading the slot | `HeapLayoutSnapshot::containerOf` (`alloc/`), drain-time |
| Remembered-set overflow → fall back to a full collection | fixed-capacity SSB → `requestFullCollection` (`RememberedSet.hpp`) |
| Concurrent barrier during the mark window | inherited CMS SATB barrier plus Eden insertion barrier (`Barriers.cpp` slow path) |

The source comments repeatedly cite "JSC/Riptide semantics" at the exact points where
gms makes the same call. The important *divergences* — where K/N could not copy JSC
directly — are called out in §7 and §8, and they are the source of most of the
subtlety (and of the hardening history in §12).

---

## 3. The per-object header (`ObjectData.hpp`)

The GC header is a **single atomic pointer-sized word** — it does not grow over CMS.
The word is partitioned:

```
bit 1 (kOldBit=2)  : generational "old" (sticky) flag — orthogonal to everything else
the rest           : CMS mark/mark-stack linkage, verbatim:
    0              : white  (unmarked, not on the mark stack)
    1 (kNoQueueMark): marked, terminal (not linked into the mark stack)
    real ObjectData*: marked, and linked into the intrusive mark stack (points at successor)
```

This packing is safe because real `ObjectData*` successors are ≥8-aligned (low 3 bits
clear) and the mark-terminal sentinel uses only bit 0, leaving bit 1 free for *old*.
The invariant that makes it work: every mark/link CAS carries the object's *current*
old bit through unchanged, and the old bit is written **only during sweep** (single
writer, no concurrent marking), so folding it into a mark-phase store races with
nothing.

The `(marked, old)` pair encodes generation between collections:

| state | meaning |
|---|---|
| `(0,0)` | young, live-if-reachable — traced & swept every collection |
| `(0,1)` | old survivor — implicitly live in Eden, re-traced only in Full |

Two scope-aware liveness decisions live here and are called from the sweep:

- `edenSweepKeep()` — old survivors kept untouched; young+marked → keep and **promote**
  (`setOld`); unmarked young → dead.
- `fullSweepKeep()` — marked → keep and promote; else dead (including dead *old*
  objects, which Eden never reclaims).

**vs JSC:** JSC stores generation in per-`MarkedBlock` bitmaps (mark bits + a
`newlyAllocated` bitmap) plus a per-cell `CellState`. gms folds both the mark state and
the generation into the object's own 8-byte word. Same *sticky mark bit* idea; gms's
representation is per-object and lock-free rather than per-block side tables.

---

## 4. Generations and collection scopes

- **Young** = allocated since it was last kept by a collection (old bit clear).
- **Old** = survived at least one collection (old bit set, sticky until a Full).
- **Eden (minor)**: `mark::edenCollection() == true`. Marking treats old objects as
  implicitly-live **terminals** — `MarkTraits::tryEnqueue`/`tryMark` refuse to trace an
  old object (`ConcurrentMark.hpp`). So the mark closure reaches only young objects and
  whatever is fed in through the remembered set. Sweep keeps all old objects, reclaims
  unmarked young, promotes marked young. Dead *old* objects survive as floating garbage.
- **Full (major)**: everything is traced from roots and swept; the remembered set is
  discarded (rediscovered from roots); dead old objects are finally reclaimed. Equivalent
  to a CMS collection with promotion bookkeeping.

`CollectionScope` selection: `GmsCollectionPolicy::choosePolicyScope()` (§9). The scope
requested at schedule time can still be **upgraded to Full** at STW start by a pending
forced-Full request (`GmsGCTraits::onCollectionStart` consumes it unconditionally so it
can't leak into a later cycle). Scheduling and scope claiming are atomic in `GCState`:
once the GC thread claims epoch N and its scope, a concurrent Full request is scheduled
as epoch N+1 rather than racing to mutate the already-claimed Eden cycle.

---

## 5. The collection cycle (`MainGCThread.hpp` + `ConcurrentMark.cpp`)

A single collection, orchestrated by `MainGCThread::PerformCollection`:

1. **Setup + STW.** `mark_.setupBeforeSTW`, then `stopTheWorld`.
2. **Scope decision.** `GCTraits::onCollectionStart(scope)`:
   - consume the forced-Full request → decide Eden vs Full → `setEdenCollection`.
   - `setCollectionInProgress(true)` — opens the *collection window* (see §6).
   - return the **actual** scope after a possible Eden→Full upgrade.
3. **Mark, STW portion** (`markInSTW`, world stopped):
   - collect every mutator's root set;
   - **Eden**: `seedRememberedSets` — drain each thread's SSB into the mark closure as
     extra roots (§7). **Full**: `clearRememberedSets`.
   - `enableBarriers` (switch to the SATB/concurrent barriers) and `resumeTheWorld`.
4. **Mark, concurrent portion.** Build the mark closure concurrently with mutators,
   using the CMS SATB deletion barrier and the weak-ref read barrier. Mutators flush
   local mark queues into the parallel processor; termination is detected via
   `batchesEverShared`. If concurrent termination fails after N attempts, finish under a
   short STW.
5. **Weak processing.** `processWeaks<GmsProcessWeaksTraits>` — Eden-aware liveness
   (old objects count as live even with no mark this cycle).
6. **`prepareForGC`**, then (concurrent sweep) `resumeTheWorld`.
7. **Sweep.** `allocator_.sweep` — scope-aware per §10, runs concurrently with mutators.
8. **Finish.** `onCollectionFinish(actualScope, keptBytes)`:
   - feed live bytes into the policy/stats using the actual scope, not the stale request;
   - `setCollectionInProgress(false)` — closes the collection window (all promotions have
     settled because `Heap::Sweep` drains every page and joins concurrent sweepers first);
   - `setEdenCollection(false)`.
   `GCHandle::finished(actualScope)` uses the same actual scope, so an Eden request that was
   upgraded to Full still gets Full statistics and the Full marked==kept debug assertion.

**vs JSC:** structurally the same shape — a short STW to scan roots, then a concurrent
parallel mark to fixpoint with a barrier maintaining the tricolor invariant, then sweep.
JSC sweeps **lazily** (per-block, driven by allocation); gms sweeps eagerly but
**concurrently**, and has the Eden clean-old-page skip (§10) as its analogue of "don't
touch blocks you don't need to."

---

## 6. Write barriers (`Barriers.cpp`)

`beforeHeapRefUpdate(owner, ref, value, loadAtomic)` is the single write-barrier seam.
It serves **two independent roles**, gated by two independent flags:

### 6a. Generational remembered-set barrier (`generationalActive`, always on)

Records the overwritten slot into the current thread's SSB for a **candidate
old→young edge**. When the heap owner is known and ages are stable this is exact;
otherwise it is a conservative young-value superset that the STW drain validates by
resolving the slot's container. The stable-owner filter is
`shouldRememberOldToYoung(owner, value)`:

- `owner == nullptr` (container unknown: static/global slots, old runtime helper paths,
  and C++ helpers such as `mm::RefField::accessor()`) → fall back to the **value's** age:
  record iff the value is young. An old→young edge always stores a young value, so this
  still captures every real edge; old-value writes are dropped. Generated instance and
  array stores, including instance volatile/atomic reference intrinsics, pass a real
  owner and avoid this fallback.
- `owner` not on the heap (stack/arena/permanent/global) → never remember (reached via
  roots every collection).
- `owner` young → never remember (a young owner is itself traced by Eden; its outgoing
  edges are followed directly). **This is the filter that matters** — young→young field
  stores are the dominant churn (every constructor store); recording them is what
  overflowed the SSB and forced a Full every cycle.
- `owner` old → record, unless `value` is already old (old→old is irrelevant to Eden).

**The collection window caveat.** While `collectionInProgress` is true, the concurrent
sweep is promoting young survivors to old one object at a time, so an owner's old bit is
*transiently unstable*: a store into a soon-to-be-promoted owner could be misread as
young and drop a real old→young edge. During the window the barrier records by the
**value's** age instead (over-records transiently); the STW drain re-filters by
container age once promotions have settled. Owner *heap-ness* is stable across a
collection, so both paths still require a heap-or-unknown owner.

> **Do not** hint `generationalActive()` as unlikely. It is true for essentially the whole
> process lifetime (set once at GC construction). An earlier
> `__builtin_expect(generationalActive(), false)` inverted the branch prediction and put
> remembering on the cold path for the hottest mutator store. (Finding #2.)

### 6b. SATB / concurrent-mark barrier (`barriersPhase`, on only during a collection)

Inherited from CMS. During `kMarkClosure`: a **deletion (SATB) barrier** enqueues the
overwritten referent, plus an **Eden insertion barrier** — during a concurrent Eden mark
a new referent may become reachable only through an old (untraced) object, so the new
value is marked directly. During `kWeakProcessing`: the weak read barrier hides
unmarked (dead) referents.

**vs JSC:** JSC's `WriteBarrier(from)` is likewise dual-purpose (generational + the
concurrent Dijkstra barrier). The crucial API difference: **JSC's barrier receives the
source cell `from`** and its filter is a single cell-state check on that source; it then
remembers the *object*. gms's barrier does **not** always have the owner: generated
instance/array stores, including instance volatile/atomic reference intrinsics, pass it;
static/global stores, legacy runtime helpers, and C++ helper paths can still be ownerless.
That is why gms needs both a barrier-time source-age filter *and* a drain-time container
resolution — see §7. Threading the owner through the volatile/atomic intrinsics
(Finding #3) closed the largest ownerless hot path.

---

## 7. The remembered set (`RememberedSet.hpp`, drained in `ConcurrentMark.cpp`)

A **per-thread sequential store buffer (SSB)**: a fixed-capacity array (`kCapacity =
1<<16` = 64K slots = 512 KiB/thread) allocated lazily on the first remembered store.
Threads that never produce an old→young edge therefore do not pay the 512 KiB buffer
cost. After that first slow-path allocation, the barrier `record()`s **slot addresses**
(`ObjHeader**`) without further allocation and needs no synchronization (appended only
by its owner, drained only under STW).

**Slot-level, not object-level.** This is the key divergence from JSC. JSC's barrier has
the source cell, so it remembers the *object* and, at collection time, recovers the
container of any pointer by masking to the `MarkedBlock` boundary (`ptr & ~(blockSize-1)`)
— O(1), no scan. gms now gets the owner for generated instance/array stores and filters
on source age there, but it still remembers the *slot* rather than the source object.
That means the Eden drain must revalidate the slot's container, both for ownerless paths
and for the collection-window over-recording described above:

`ConcurrentMark::seedRememberedSets` (Eden only, STW):
1. If no thread recorded anything, return before building the snapshot (a large heap with
   an empty SSB must not pay to sort every page range).
2. If any recorded entry is conservative, build a `HeapLayoutSnapshot` (sorted page
   ranges). Exact stable-owner entries skip this cost.
3. For each recorded slot:
   - exact entries: read the slot directly. Their owner was known old and stable when the
     slot was recorded, and Eden does not reclaim old objects.
   - conservative entries: **resolve the container first** via `containerOf` (a pure
     address-range lookup that never dereferences the slot), and enqueue the referent
     **only if the container is a genuinely old, live heap object**. Young/dead-young/
     reclaimed/unresolvable containers are skipped *without reading `*slot`*.

Resolving the container **before** dereferencing is a hard correctness requirement, not
an optimization: a slot can go stale (the collection-window barrier records slots inside
young owners; a dead-young owner is swept and its cell reused, leaving the slot pointing
at unrelated memory). Reading `*slot` first would load garbage and CAS a wild mark word —
the `afd34b6` crash class (§12).

**Overflow/thread exit → Full.** When the SSB fills, `record()` returns false and the
caller calls `requestFullCollection()`; the next collection becomes Full (which needs no
remembered set) and the buffer resets. The same fallback is used when a thread
unregisters with a non-empty SSB: the per-thread buffer dies with `ThreadData`, so
`BarriersThreadData::onThreadUnregistration` requests a Full collection before clearing
it. Classic "remembered-set unavailable → full GC" fallback. JSC grows its remembered
set instead; gms trades a growable set for a fixed buffer plus a guaranteed-correct
fallback.

**vs JSC, summarized:** JSC filters at the barrier on source-cell state and remembers
objects, recovered by masking. gms filters at the barrier when the source owner is
available and stable, falls back to value-age recording for ownerless/window cases, and
then revalidates slot containers at the drain through a page-range snapshot. The
`RememberedSet.hpp` comment explicitly names JSC's page-aligned masking as the intended
future hot-path optimization (M7) that would replace the snapshot lookup — a performance
refinement, not a correctness change.

---

## 8. Interior-pointer resolution & the allocator dependency

`containerOf` only works on the **custom allocator** (`FixedBlockPage`/`NextFitPage`/
`SingleObjectPage`), which lays out pages so a raw interior pointer can be resolved to
its object, and rejects pointers into **free** cells (the free-list guard — Finding #4;
without it a freed cell's free-list link word can spoof `isOld()`).

The legacy/std allocator cannot resolve interior pointers
(`heapLayoutResolvesInteriorPointers() == false`). With no way to detect a stale slot,
`*slot` would be an unguarded read of possibly-freed memory. So **production never runs
Eden on a non-resolving backend**: `choosePolicyScope` forces Full-only there (Full needs
no drain). Only a test that pins the scope over a controlled graph runs Eden on such a
backend (`setForcedScopeForTests`).

**vs JSC:** JSC's `MarkedSpace` is *always* block-aligned, so interior-pointer recovery
by masking is universal and free — JSC never has a "resolving vs non-resolving backend"
split. gms's split is a direct consequence of supporting more than one allocator.

---

## 9. Collection policy (`GmsCollectionPolicy.*`)

Run a **Full** to establish a baseline of live "old" bytes, then run **Eden** collections
until the live heap grows past that baseline by `fullGrowthTriggerPercent` (default
**33%**), then Full again. Growth comes from promoted survivors and from floating garbage
Eden can't reclaim, so this bounds floating garbage while keeping Full collections
infrequent.

- Baseline = kept bytes after the last Full; `live` = kept bytes after the last
  collection of any scope. `trigger = base + base*percent/100`. `live > trigger` → Full.
- `base == 0` (no Full yet) → Full, to establish the baseline.
- Non-resolving allocator → Full (§8).
- The percent is a runtime knob (`setFullGrowthTriggerPercent`, surfaced via `GC.kt`),
  floored at 1 (0 would force a Full every cycle).
- Explicit full-heap entry points (`GC.collect` and exported `PerformFullGC`) call
  `GC::requestFullCollection()` before scheduling, so their contract remains a Full
  collection even when the policy would otherwise choose Eden.
- Test support is process-global too: `resetForTests()` resets baseline/counters/trigger
  and clears the forced-scope override so one test's pinned Eden/Full choice cannot leak
  into the next.

**vs JSC:** this deliberately mirrors JSC's `minEdenToOldGenerationRatio` (~1/3) — the
same "let the young heap grow to a fraction of old before paying for a major" heuristic,
expressed as a growth percentage over the post-Full baseline.

---

## 10. Sweep (`gc::tryResetMark`, `sweepSkipsCleanOldPages`, page `Sweep` templates)

The allocator's sweep calls `gc::tryResetMark(objectData)` per object → `edenSweepKeep`
or `fullSweepKeep` (§3). Kept objects normalize to `(marked=0, old=1)`.

`gc::isMarked` is **Eden-aware**: during an Eden collection an old survivor reports as
live even though it carries no mark this cycle (critical for the legacy allocator's
object/ExtraObjectData sweep and for weak refs / Obj-C peers / monitors / Cleaners).

**Clean-old-page skip** (`sweepSkipsCleanOldPages()` true during Eden): a page holding
only old survivors that was not allocated into since the last collection has an identical
outcome to its previous sweep, so Eden skips it entirely. The page `Sweep` templates
thread kept-object statistics through the sweep scope so the skip can re-report cached
kept totals (this is what the test double `FakeGCSweepScope` in
`CustomAllocatorTestSupport.hpp` must model). The skip is additionally gated by
`SweepTraits::kCanSkipCleanOldPages`: object pages may use it; extra-object pages must
always be swept. A Full always visits every page.

**vs JSC:** JSC sweeps lazily per block at allocation time and skips blocks it doesn't
need; the clean-old-page skip is gms's eager-but-concurrent analogue.

---

## 11. Side-by-side: gms vs JSC/Riptide

| Dimension | JSC / Riptide | gms (Kotlin/Native) |
|---|---|---|
| Moving? | No (mark-sweep, segregated `MarkedSpace`) | No (mark-sweep, custom/legacy allocator) |
| Generational scheme | Sticky mark bits | Sticky *old* bit (packed in GC header word) |
| Generation storage | Per-`MarkedBlock` bitmaps + per-cell `CellState` | 1 bit in the object's 8-byte atomic header word |
| Concurrency | Concurrent + parallel mark | Concurrent + parallel mark (reuses CMS `ParallelProcessor`) |
| Mark invariant during concurrency | Dijkstra insertion barrier | SATB deletion barrier + Eden insertion barrier (from CMS) |
| Generational barrier filter | Source **cell state** at barrier time | Source **age** at barrier time (owner) **+** container age at drain time |
| Remembered set unit | Objects (source cells) | **Slots** (`ObjHeader**`), lazy per-thread SSB |
| Container recovery | Mask to block boundary — O(1), universal | `HeapLayoutSnapshot` page-range lookup — O(log pages), custom allocator only |
| Owner available at barrier? | Always (`WriteBarrier(from)`) | Generated instance/array stores, including instance volatile/atomic; not static/global/runtime-helper paths → hybrid filter |
| Remembered-set overflow | Grow the set | Fixed 64K/thread, or thread exit with buffered slots → force next collection Full |
| Eden vs Full trigger | `minEdenToOldGenerationRatio` (~1/3) | `fullGrowthTriggerPercent` (default 33%) over post-Full baseline |
| Sweep | Lazy, per-block, allocation-driven | Eager, concurrent, scope-aware; Eden clean-old-page skip |
| Dead old-gen objects | Floating garbage until major | Floating garbage until Full |
| Backend variance | One space, always block-aligned | Custom (resolves interior ptrs) vs legacy/std (Full-only) |

---

## 12. Divergences that bit us (hardening history)

gms's central divergence from JSC — **slot-level remembering with drain-time container
recovery, because the owner isn't always at the barrier** — is the root of most of the
subtlety. The `kn-gms-m7-hardening` work fixed a crash and five adjacent issues:

- **The crash (`afd34b6`).** Under `-opt` (escape analysis on), the RS drain dereferenced
  a **stale** recorded slot (`*slot`) *before* validating its container, marking a wild
  pointer whose tag bits passed `heap()` → a `tryMark` CAS onto read-only `.text`. Fix:
  resolve the container first (`containerOf` never dereferences the slot) and read `*slot`
  only for a genuinely-old live container. JSC never hits this because its remembered set
  holds objects, not raw slots.
- **#1 legacy allocator.** `containerOf` returns null there → the drain's `*slot` read was
  unguarded. Fix: Full-only on non-resolving backends (§8).
- **#2 inverted barrier hint** (§6a).
- **#3 owner not threaded through volatile/atomic ref stores** → `owner==nullptr` →
  value-age fallback only → SSB flood → forced Full every cycle. Fix: a codegen change
  threading the real owner (receiver / array / null-for-static) through new
  `*VolatileHeapRefWithOwner` runtime entry points, so the exact source-age filter
  applies. This is precisely the information JSC's `WriteBarrier(from)` always has.
- **#4 free-cell guard** in `FixedBlockPage::objectContainingInteriorPointer` (§8).
- **#5/#6** drain cleanups (empty-SSB early-out before snapshot; hoisted per-slot branch).

The later correctness pass fixed the remaining places where Full/Eden semantics could be
lost outside the drain itself:

- **Thread exit with buffered slots.** A detaching mutator used to destroy its SSB with
  recorded old→young edges still inside it. Since the next Eden collection would no
  longer see those edges, `onThreadUnregistration` now requests a Full fallback before
  the per-thread buffer is cleared (§7).
- **Scheduled Full lost to scope claiming.** The GC thread now claims an epoch and its
  requested scope together in `GCState`; a Full request racing with an already-claimed
  Eden epoch becomes the next epoch instead of being overwritten (§4).
- **Actual scope propagation.** `GmsGCTraits::onCollectionStart` returns the actual scope
  after forced-Full upgrade, and `MainGCThread` uses it for policy stats and
  `GCHandle::finished`, preserving Full accounting and assertions (§5).
- **Full entry points and test reset.** `PerformFullGC` now requests Full just like
  `GC.collect`, and `resetForTests()` clears the forced-scope override and forced-Full
  test latch (§9).
- **Clean-old-page trait guard.** `NextFitPage::Sweep` now uses the same
  `kCanSkipCleanOldPages` gate as `FixedBlockPage::Sweep`, keeping extra-object pages
  out of the Eden clean-old-page shortcut (§10).

Regression tests: `GmsGenerationalTest.AtomicStoreThreadsOwnerThroughRememberedSetFilter`
(the #3 runtime seam) and `CustomAllocatorTest.FixedBlockPageInteriorPointerRejectsFreeCells`
(the #4 free-cell guard), plus GMS regressions for claimed-scope Full scheduling,
`PerformFullGC`, test-policy reset, and thread-exit SSB fallback. See the memory note
`gms-ea-remembered-set-crash`.

---

## 13. Future work (from the source TODOs)

- **Page-aligned container recovery (M7).** Make the container recoverable by masking
  (`slot & ~(SIZE-1)`), JSC/MarkedBlock-style, replacing the drain-time snapshot lookup.
  Performance refinement, not correctness (`RememberedSet.hpp`).
- **Growable / object-level remembered set** to remove the fixed 64K cap and the
  overflow→Full fallback entirely.
- **Sweep without a pause** (`ConcurrentMark.hpp` step 7 TODO).

### Lazy-sweep migration: investigated, NOT pursued (prototype reverted)

JSC's throughput comes from a **co-designed package** — versioned side-bitmap marks + bump-only
allocation + bulk `stopAllocating` + **lazy per-block sweep on demand** — so steady-state work is
∝ allocation, not ∝ live-heap (it never walks the heap to sweep;
`LocalAllocator::tryAllocateIn → block->sweep(&freeList)`). Adopting it in gms was scoped as a phased
migration (A: per-page marking version + mark-time live-byte accounting; B: mark-time-driven policy
trigger; C: incremental-sweep primitive + background sweeper; D: RS-drain working-set pre-sweep;
E: versioned `isLive` + drop the eager sweep; F: footprint knob).

**Conclusion: this is not a "fix the blockers" migration — it requires re-architecting gms's allocator
*and* marker to match JSC's foundations, so it was NOT pursued.** Two independent representational
facts block it, both confirmed by prototyping the early phases:

1. **Per-object mark word (R4).** Lazy sweep leaves stale mark bits set on unswept pages and relies on
   a per-page **version** to invalidate them wholesale (a stale `MarkedBlock` reads all-clear in O(1)).
   gms's marks live *in the object's 8-byte word*, which also serves as the intrusive mark-stack link.
   A page mixes dead objects (stale bits still set) and live to-be-marked ones; stamping it "current
   version" would resurrect the dead. Fixing this means moving marks to **per-page side bitmaps + a
   non-intrusive mark stack** (touching the shared CMS `ParallelProcessor`) — i.e. reverting R4. The
   only alternative, an O(page) mark-clearing walk on first-mark, is the very sweep cost lazy sweep
   exists to avoid and is race-prone under parallel marking.
2. **Variable page sizes.** A uniform O(1) object→page (needed on the mark/RS hot path) is a single
   mask only if pages are uniform-sized. gms's are not (`FixedBlockPage` 128 KiB, `NextFitPage` 256 KiB,
   `SingleObjectPage` variable), so masking cannot compose across kinds; a safe uniform resolver needs
   **uniform-size blocks + a large-object registry** (JSC `BlockDirectory`/`PreciseAllocation`) — an
   allocator redesign. (Aligned-page groundwork was prototyped and reverted; per-kind masking works but
   does not compose into a uniform resolver.)

Each of those is a multi-month redesign, verifiable on x86_64 but with **arm64 validation unavailable
in this environment** (no toolchain/emulator). Given that **gms already implements the lazy-sweep
benefit its architecture permits — the clean-old-page skip (§10)** avoids the sweep walk for all-old
pages during Eden — and that no profile has shown sweep wall-time to dominate (run that profile first),
the migration was **not implemented**. Prototype code for phases A–C and the M7 alignment groundwork
was written and verified green on x86_64, then **reverted**; this section is the durable record. The
standalone per-page-version bolt-on was separately measured and rejected (below).

#### Why a per-page marking version does NOT help *as a standalone bolt-on*

The migration above adopts the version as part of the whole package. Adding *only* the version to
gms's existing eager sweep was evaluated separately and rejected. The reason it does not transfer:

- JSC keeps mark bits in a **side bitmap** the version lets it avoid. gms's R4 packing put the
  mark/link component *and* the sticky old bit **inside the per-object word** (§3), which is
  already loaded at both mark (`MarkTraits::tryMark`/`tryEnqueue`) and sweep
  (`gc::tryResetMark`). There is no side table to skip, so a per-page version cannot speed up
  the mark hot path or the per-object sweep walk.
- The one page-level fact gms exploits — the Eden **clean-old-page skip** (§10) — is already
  O(1) per page via two booleans (`allocatedSinceSweep_`, `lastSweepSkippable_`). Re-expressing
  it as a version compare is strictly *more* work (two `uint32` reads + a global-version read
  per allocation, vs a byte store).
- O(1) Full de-promotion is **unsound**: pages are mixed-age, so `isOld()` is genuinely
  per-object; a per-page old epoch cannot encode it, and a per-object epoch would break the
  8-byte word (§3). `fullSweepKeep` already folds `setOld()` into a walk that visits every
  survivor anyway, so the theoretical saving is one `fetch_or` per already-touched object.

An isolated microbenchmark (guard + per-allocation stamp, with skip/walk counts asserted equal
between the boolean and version variants) confirmed the analysis: the version variant is
**~4–6% slower**, never faster, driven by the per-allocation global-version read. A version
compare also proved **more error-prone** than the boolean — the naive "clean since last sweep"
predicate `lastAllocVersion != lastSweptVersion` over-skips freshly-allocated pages (an ordering
predicate is required), and, like JSC, it is not wrap-safe without a periodic hard reset. Net:
negative performance, added complexity, added correctness surface. The real JSC-parity lever
remains **M7 page-aligned container recovery** above, which is orthogonal to marking versions.

---

## File map

| Concern | File |
|---|---|
| Per-object header, sticky old bit, sweep-keep decisions | `cpp/ObjectData.hpp` |
| Write barriers (generational + SATB), source-age filter | `cpp/Barriers.{hpp,cpp}` |
| Remembered set (SSB) | `cpp/RememberedSet.hpp` |
| Concurrent/parallel mark, RS drain, Eden terminality | `cpp/ConcurrentMark.{hpp,cpp}` |
| Eden/Full policy, heap-growth trigger, stats | `cpp/GmsCollectionPolicy.{hpp,cpp}` |
| Scope orchestration hooks (collection window) | `cpp/GmsGCTraits.hpp` |
| GC entry points, `isMarked`, `tryResetMark`, sweep skip | `cpp/GCImpl.{hpp,cpp}` |
| Epoch/scope scheduling state | `../common/cpp/GCState.hpp` |
| Collection driver loop | `../common/cpp/MainGCThread.hpp` |
| Thread registration/unregistration hook | `../../mm/cpp/ThreadRegistry.cpp` |
| Interior-pointer resolution, backend split | `../../alloc/**/AllocatorImpl.cpp`, `Allocator.hpp` |
