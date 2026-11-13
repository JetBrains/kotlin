// The browser-loop gate, run in Node (same V8 as the browser; the glue
// uses only browser APIs). One held Model instance + one held hierarchy
// pair, across six versions:
//   v1: create instances, bump twice                       -> "counter: 2"
//   v2: body-only edit of render() — slot swap             -> "[v2] counter: 2"
//   v3: add a field and use it — payload version + MIGRATE  -> "[v3] counter: 2 note=null"
//   v4: delete the field again — migrate again, field gone   -> "[v4] counter: 2"
//   v5: base-class body change — override still wins        -> Rex: …hums… / woof
//   v6: add a method to Model — ENGINE REJECTS the shape
//       change at the setters; old code keeps running
// The held instances' state must survive v2–v4; the hierarchy pair must
// show correct dispatch through the swap; the rejected patch must leave
// the running behavior unchanged.
// v9–v16 are the language-feature stress beats: generics/vararg/
// extension bodies (slotted), cross-file inline propagation through the
// IC, held-lambda re-binding (CL semantics), interface default-method
// bodies on live instances (itables), and object/companion redefinition
// — DEFVAR semantics: top-level singleton state survives the object's
// own redefinition (adopted at init) and a field addition (payload
// migration), while an incompatible holder change is engine-rejected
// like any cell-chain change, old code running.
// The app is MULTI-FILE (app/src/model.kt + app/src/zoo.kt): every beat
// edits one file — the Model beats touch model.kt, the zoo beats touch
// zoo.kt (the file granularity the delta link will key on).

import { execFileSync } from 'node:child_process'
import path from 'node:path'
import { compileNext, baseBytes, SRC_NAMES } from './build.mjs'
import { loadBase, liveView, applyDelta, readRegistry } from './glue.mjs'

// v1 is the COMMITTED app — the on-disk src files are the demo playground
// and may carry live edits from the last session
const HERE = path.dirname(new URL(import.meta.url).pathname)
const APP = {}
for (const name of SRC_NAMES) {
    APP[name] = execFileSync('git', ['-C', path.join(HERE, '..', '..'), 'show',
        `HEAD:wasm-repl-spike/browser/app/src/${name}`], { encoding: 'utf-8' })
}

/** sources with one `from`→`to` replacement applied to `file` */
function edit(sources, file, from, to) {
    const text = sources[file]
    if (!text.includes(from)) fail(`${file}: edit anchor not found: ${JSON.stringify(from.slice(0, 60))}`)
    return { ...sources, [file]: text.replace(from, to) }
}

function fail(msg) {
    console.error('FAIL:', msg)
    process.exit(1)
}

function render(m) { return live.exports.renderModel(m) }
let live = null

// v1 — boot: the stdlib BASE once (cold code), then the first patch
const r1 = await compileNext(APP)
if (r1.error) fail('v1 compile: ' + r1.error)
console.log('v1 patch:', r1.bytes.length, 'bytes (base:', baseBytes().length, 'bytes, once)')
const base = await loadBase(baseBytes())
live = liveView([])
await applyDelta(live, r1.bytes, base)
const model = live.exports.createModel('counter')
const zoo = [['base', 'Rex'], ['dog', 'Rex'], ['cat', 'Tom'], ['cow', 'Belle'], ['duck', 'Donald']]
    .map(([k, n]) => live.exports.make(k, n))
live.exports.bumpModel(model)
live.exports.bumpModel(model)
if (live.exports.applyTransform(5) !== 10) fail('v1 applyTransform: ' + live.exports.applyTransform(5))
if (live.exports.applyAll(1) !== 20) fail('v1 applyAll: ' + live.exports.applyAll(1))
if (live.exports.useAdder(10, 5) !== 31) fail('v1 useAdder: ' + live.exports.useAdder(10, 5))
if (live.exports.useTwice(21) !== 42) fail('v1 useTwice: ' + live.exports.useTwice(21))
const flat = live.exports.makeGreeter('flat')
const loud = live.exports.makeGreeter('loud')
if (live.exports.greet(flat, 'rex') !== 'hi rex') fail('v1 flat greeter: ' + live.exports.greet(flat, 'rex'))
if (live.exports.greet(loud, 'rex') !== 'HEY rex') fail('v1 loud greeter: ' + live.exports.greet(loud, 'rex'))
if (live.exports.hitCounter() !== 1 || live.exports.hitCounter() !== 2) fail('v1 counter: ' + live.exports.hitCounter())
const tag = live.exports.makeTagged('a')
if (live.exports.useTagged(tag) !== 'a#1') fail('v1 tagged: ' + live.exports.useTagged(tag))
if (live.exports.tagsMade() !== 1) fail('v1 tagsMade: ' + live.exports.tagsMade())
if (live.exports.pickInt(true, 3, 9) !== 3) fail('v1 pick: ' + live.exports.pickInt(true, 3, 9))
if (live.exports.sumAll(1, 2, 3) !== 6) fail('v1 sum: ' + live.exports.sumAll(1, 2, 3))
if (live.exports.shout('hey') !== 'hey!') fail('v1 shout: ' + live.exports.shout('hey'))
let s = render(model)
console.log('v1:', s)
if (s !== 'counter: 2') fail(`v1 render: ${s}`)
const chorus1 = zoo.map((a) => live.exports.intro(a))
console.log('v1 zoo:', chorus1.join(' | '))
if (chorus1.join('|') !== 'Rex says …|Rex says woof|Tom says meow|Belle says …|Donald says quack') {
    fail('v1 zoo: ' + chorus1.join(' | '))
}

// v2 — body-only edit of model.kt's render()
const src2 = edit(APP, 'model.kt',
                  'fun render(): String = label + ": " + count',
                  'fun render(): String = "[v2] " + label + ": " + count')
const r2 = await compileNext(src2)
if (r2.error) fail('v2 compile: ' + r2.error)
let sweep = await applyDelta(live, r2.bytes, base)
console.log('v2: swapped', sweep.swapped.length, 'slots, rejected', sweep.rejected.length, '—', r2.bytes.length, 'B delta')
if (sweep.rejected.length > 0) fail('v2 must be a clean swap')
s = render(model)
console.log('v2:', s)
if (s !== '[v2] counter: 2') fail(`v2 render: ${s} (state must survive)`)

// v3 — field addition: payload version, held instance MIGRATES
const src3 = edit(edit(src2, 'model.kt',
    'class Model(var count: Int, val label: String) {',
    'class Model(var count: Int, val label: String) {\n    var note: String = "none"'),
    'model.kt',
    'fun render(): String = "[v2] " + label + ": " + count',
    'fun render(): String = "[v3] " + label + ": " + count + " [" + note + "]"')
const r3 = await compileNext(src3)
if (r3.error) fail('v3 compile: ' + r3.error)
const reg = readRegistry(r3.bytes)
if (!reg || reg.length === 0) fail('v3 has no registry section')
sweep = await applyDelta(live, r3.bytes, base)
console.log('v3: swapped', sweep.swapped.length, 'slots; registry', reg.length, 'bytes —', r3.bytes.length, 'B delta')
s = render(model)
console.log('v3:', s)
if (s !== '[v3] counter: 2 [null]') fail(`v3 render: ${s} (migration must preserve count)`)
const fresh3 = live.exports.createModel('fresh')
if (render(fresh3) !== '[v3] fresh: 0 [none]') fail('v3 fresh initializer: ' + render(fresh3))

// v4 — field deletion: migrate again, the field is simply gone
const src4 = edit(edit(src3, 'model.kt',
    '\n    var note: String = "none"', ''),
    'model.kt',
    'fun render(): String = "[v3] " + label + ": " + count + " [" + note + "]"',
    'fun render(): String = "[v4] " + label + ": " + count')
const r4 = await compileNext(src4)
if (r4.error) fail('v4 compile: ' + r4.error)
sweep = await applyDelta(live, r4.bytes, base)
console.log('v4: swapped', sweep.swapped.length, 'slots —', r4.bytes.length, 'B delta')
s = render(model)
console.log('v4:', s)
if (s !== '[v4] counter: 2') fail(`v4 render: ${s} (deletion must preserve surviving fields)`)

// v5 — base-class body change: the whole zoo speaks through the swap;
// the base instance and the Cow (no override) run the NEW body, the
// overrides still win
const src5 = edit(src4, 'zoo.kt',
    'open fun speak(): String = "…"', 'open fun speak(): String = "…hums…"')
const r5 = await compileNext(src5)
if (r5.error) fail('v5 compile: ' + r5.error)
sweep = await applyDelta(live, r5.bytes, base)
console.log('v5: swapped', sweep.swapped.length, 'slots —', r5.bytes.length, 'B delta')
const chorus5 = zoo.map((a) => live.exports.intro(a))
console.log('v5 zoo:', chorus5.join(' | '))
if (chorus5[0] !== 'Rex says …hums…') fail('v5 base body: ' + chorus5[0])
if (chorus5[3] !== 'Belle says …hums…') fail('v5 cow (no override): ' + chorus5[3])
if (chorus5[1] !== 'Rex says woof' || chorus5[2] !== 'Tom says meow' || chorus5[4] !== 'Donald says quack') {
    fail('v5 overrides must still win: ' + chorus5.join(' | '))
}

// v6a — add a NON-VIRTUAL method to final Model: NOT a shape change (it
// never enters the vtable; the cell type is untouched) — the swap must be
// ACCEPTED, and the new method rides along as a fresh export
const src6a = edit(src5, 'model.kt',
    'class Model(var count: Int, val label: String) {',
    'class Model(var count: Int, val label: String) {\n    fun reset() { count = 0 }')
const r6a = await compileNext(src6a)
if (r6a.error) fail('v6a compile: ' + r6a.error)
sweep = await applyDelta(live, r6a.bytes, base)
console.log('v6a: swapped', sweep.swapped.length, ', rejected', sweep.rejected.length, '—', r6a.bytes.length, 'B delta')
if (sweep.rejected.length > 0) fail('v6a non-virtual addition must swap cleanly')
s = render(model)
if (s !== '[v4] counter: 2') fail(`v6a behavior must be unchanged: ${s}`)
console.log('v6a: additive method accepted (Julia semantics) —', s)

// v6 — STAGE 3: add an OPEN method to Animal and USE it in intro.
// The vtable struct grows (a new type) but the vtable is now behind the
// mutable root-typed cell field, so no wasm signature changed — the
// engine ACCEPTS the swap, and the OLD instances migrate their vtable
// on the next dispatch: the whole chorus speaks the NEW method, with
// overrides still winning.
const src6 = edit(edit(src6a, 'zoo.kt',
    'open fun speak(): String = "…hums…"',
    'open fun speak(): String = "…hums…"\n\n    open fun pet(): String = "pet"'),
    'zoo.kt',
    'fun intro(): String = name + " says " + speak()',
    'fun intro(): String = name + " says " + speak() + " (" + pet() + ")"')
const r6 = await compileNext(src6)
if (r6.error) fail('v6 compile: ' + r6.error)
sweep = await applyDelta(live, r6.bytes, base)
console.log('v6: swapped', sweep.swapped.length, ', rejected', sweep.rejected.length, '—', r6.bytes.length, 'B delta')
if (sweep.rejected.length > 0) {
    fail('v6 method addition must be ACCEPTED (vtable indirection): ' +
         [...new Set(sweep.rejected.map((x) => x.name))].join(', '))
}
const chorus6 = zoo.map((a) => live.exports.intro(a))
console.log('v6 zoo:', chorus6.join(' | '))
const expected6 = [
    'Rex says …hums… (pet)',   // base: new speak + new pet, vtable-migrated
    'Rex says woof (pet)',     // Dog: override STILL WINS, pet dispatched after migration
    'Tom says meow (pet)',
    'Belle says …hums… (pet)', // Cow (no override): new speak + pet
    'Donald says quack (pet)',
]
for (let i = 0; i < zoo.length; i++) {
    if (chorus6[i] !== expected6[i]) {
        fail(`v6 chorus[${i}]: '${chorus6[i]}' != '${expected6[i]}' (old instances must dispatch the NEW method)`)
    }
}
console.log('v6: old instances vtable-migrated; overrides still win')

// v7 — a REAL shape change remains engine-rejected: give Model a CLASS
// supertype — the cell subtyping chain is part of every wasm signature
// mentioning Model, so the swapped funcrefs fail the setter type check
const src7 = edit(src6, 'model.kt',
    'class Model(var count: Int, val label: String) {',
    'open class Counted { fun kind() = "counted" }\n\nclass Model(var count: Int, val label: String) : Counted() {')
const r7 = await compileNext(src7)
if (r7.error) fail('v7 compile: ' + r7.error)
sweep = await applyDelta(live, r7.bytes, base)
console.log('v7: swapped', sweep.swapped.length, ', rejected', sweep.rejected.length, '—', r7.bytes.length, 'B delta')
if (sweep.rejected.length === 0) fail('v7 cell-chain change must be rejected by the engine')
const rejected7 = [...new Set(sweep.rejected.map((x) => x.name))]
if (!rejected7.some((n) => n.startsWith('createModel') || n.startsWith('bumpModel') || n.startsWith('renderModel'))) {
    fail('v7 must reject Model-touching slots, got: ' + rejected7.join(', '))
}
// the OLD code keeps running for the rejected slots: behavior unchanged
s = render(model)
if (s !== '[v4] counter: 2') fail(`v7 Model behavior must be unchanged: ${s}`)
const chorus7 = zoo.map((a) => live.exports.intro(a))
if (chorus7[1] !== 'Rex says woof (pet)') fail('v7 old zoo code must keep running: ' + chorus7[1])
console.log('v7: engine rejected the cell-chain change; old code still running —', chorus7[1])

// v8 — the GHOST INSTANCE: the demo-7d trap, gated (it was a raw
// `unreachable` in the stepper's fall-through before the fix). Add a Pig
// and hold one; then save a Pig-less source while STILL holding the pig;
// then grow Animal's vtable AGAIN (a second open method) — the pig's
// vtable fails the dispatch test, the universal stepper runs, and the
// pig's klassId matches no arm (its class is gone from the module). The
// fall-through must return the instance's CURRENT vtable so the
// dispatch's own cast is the gate: the ghost pig traps the design's
// honest "illegal cast" while the whole rest of the zoo migrates.
const src8a = edit(edit(src7, 'zoo.kt',
    'class Cow(name: String) : Animal(name)',
    'class Cow(name: String) : Animal(name)\n\nclass Pig(name: String) : Animal(name) {\n    override fun speak(): String = "oink"\n}'),
    'zoo.kt',
    '"duck" -> Duck(name.toString())',
    '"duck" -> Duck(name.toString())\n    "pig" -> Pig(name.toString())')
const r8a = await compileNext(src8a)
if (r8a.error) fail('v8a compile: ' + r8a.error)
sweep = await applyDelta(live, r8a.bytes, base)
const pig = live.exports.make('pig', 'Porky')
const pigIntro = live.exports.intro(pig)
console.log('v8a pig born:', pigIntro, '—', r8a.bytes.length, 'B delta')
if (pigIntro !== 'Porky says oink (pet)') fail('v8a pig: ' + pigIntro)

// v8b — the presenter saves a Pig-less source; the page still HOLDS the
// pig (its vtable still matches, so it keeps dispatching — no stepper)
const r8b = await compileNext(src7)
if (r8b.error) fail('v8b compile: ' + r8b.error)
sweep = await applyDelta(live, r8b.bytes, base)
const pigIntro8b = live.exports.intro(pig)
console.log('v8b ghost pig still speaks (class gone, vtable unchanged):', pigIntro8b, '—', r8b.bytes.length, 'B delta')
if (pigIntro8b !== 'Porky says oink (pet)') fail('v8b ghost pig: ' + pigIntro8b)

// v8c — grow Animal's vtable again: the ghost pig's old vtable fails the
// dispatch test → stepper → klassId matches no arm → CURRENT vtable →
// the cast is the gate → honest trap; every OTHER animal migrates
const src8c = edit(edit(src7, 'zoo.kt',
    'open fun pet(): String = "pet"',
    'open fun pet(): String = "pet"\n\n    open fun pat(): String = "pat"'),
    'zoo.kt',
    'fun intro(): String = name + " says " + speak() + " (" + pet() + ")"',
    'fun intro(): String = name + " says " + speak() + " (" + pet() + "/" + pat() + ")"')
const r8c = await compileNext(src8c)
if (r8c.error) fail('v8c compile: ' + r8c.error)
sweep = await applyDelta(live, r8c.bytes, base)
let ghostTrap = null
try {
    live.exports.intro(pig)
} catch (e) {
    ghostTrap = e
}
if (!ghostTrap) fail('v8c ghost pig must trap (its class is gone and the vtable grew)')
if (/unreachable/i.test(ghostTrap.message)) {
    fail('v8c ghost pig hit the RAW stepper fall-through trap: ' + ghostTrap.message)
}
if (!/cast/i.test(ghostTrap.message)) {
    fail('v8c ghost pig trap should be the honest illegal cast: ' + ghostTrap.message)
}
console.log('v8c ghost pig trapped honestly:', ghostTrap.message, '—', r8c.bytes.length, 'B delta')
const chorus8 = zoo.map((a) => live.exports.intro(a))
console.log('v8c zoo:', chorus8.join(' | '))
const expected8 = [
    'Rex says …hums… (pet/pat)',
    'Rex says woof (pet/pat)',
    'Tom says meow (pet/pat)',
    'Belle says …hums… (pet/pat)',
    'Donald says quack (pet/pat)',
]
for (let i = 0; i < zoo.length; i++) {
    if (chorus8[i] !== expected8[i]) {
        fail(`v8c chorus[${i}]: '${chorus8[i]}' != '${expected8[i]}' (the live zoo must migrate past the ghost)`)
    }
}
console.log('v8: ghost instance traps illegal cast (not unreachable); the rest of the zoo migrates')

// v9 — generics, vararg, extension bodies in toolbox.kt — all slotted,
// all must swap hot; and the OBJECT's state must be untouched by this
// unrelated-file delta (its global stays with its owner by construction)
const src9 = edit(edit(edit(src8c, 'toolbox.kt',
    'fun <T> pick(first: Boolean, a: T, b: T): T = if (first) a else b',
    'fun <T> pick(first: Boolean, a: T, b: T): T = if (first) b else a'),
    'toolbox.kt',
    'fun String.shout(): String = this + "!"',
    'fun String.shout(): String = this + "!!"'),
    'toolbox.kt',
    'fun sum(vararg xs: Int): Int {\n    var total = 0\n    for (x in xs) total = total + x\n    return total\n}',
    'fun sum(vararg xs: Int): Int {\n    var total = 0\n    for (x in xs) total = total + x\n    return total + 1000\n}')
const r9 = await compileNext(src9)
if (r9.error) fail('v9 compile: ' + r9.error)
sweep = await applyDelta(live, r9.bytes, base)
// stateful calls captured ONCE — the counter increments per call, so a
// log line plus an assertion would double-count (the first version of
// this beat failed itself, not the compiler)
const counter9 = live.exports.hitCounter()
console.log('v9:', 'pick', live.exports.pickInt(true, 3, 9), '| sum', live.exports.sumAll(1, 2, 3),
    '| shout', live.exports.shout('hey'), '| counter', counter9, '—', r9.bytes.length, 'B delta')
if (live.exports.pickInt(true, 3, 9) !== 9) fail('v9 generic body must swap hot: ' + live.exports.pickInt(true, 3, 9))
if (live.exports.sumAll(1, 2, 3) !== 1006) fail('v9 vararg body must swap hot: ' + live.exports.sumAll(1, 2, 3))
if (live.exports.shout('hey') !== 'hey!!') fail('v9 extension body must swap hot: ' + live.exports.shout('hey'))
if (counter9 !== 3) fail('v9 object state must survive unrelated-file edits: ' + counter9)

// v10 — a plain slotted body edit in lambdas.kt
const src10 = edit(src9, 'lambdas.kt',
    'fun useTwice(n: Int): Int = twice(n)',
    'fun useTwice(n: Int): Int = twice(n) + 1')
const r10 = await compileNext(src10)
if (r10.error) fail('v10 compile: ' + r10.error)
sweep = await applyDelta(live, r10.bytes, base)
if (live.exports.useTwice(21) !== 43) fail('v10 slotted body: ' + live.exports.useTwice(21))
console.log('v10: useTwice', live.exports.useTwice(21), '—', r10.bytes.length, 'B delta')

// v11 — CROSS-FILE INLINE: edit toolbox.kt's inline `twice` body; its
// caller useTwice lives in lambdas.kt — the IC's inline hashing must
// dirty the caller's file so the delta re-emits it with the new body
// inlined. If the caller is not dirtied, its slot keeps the OLD inlined
// body and the assertion fails — the propagation test.
const src11 = edit(src10, 'toolbox.kt',
    'inline fun twice(n: Int): Int = n * 2',
    'inline fun twice(n: Int): Int = n * 3')
const r11 = await compileNext(src11)
if (r11.error) fail('v11 compile: ' + r11.error)
sweep = await applyDelta(live, r11.bytes, base)
console.log('v11: useTwice', live.exports.useTwice(21), '—', r11.bytes.length, 'B delta')
if (live.exports.useTwice(21) !== 64) fail('v11 inline caller must be dirtied and re-emitted: ' + live.exports.useTwice(21))

// v12 — HELD LAMBDA, redefined: editing the val's initializer is a NEW
// BINDING (CL semantics) — the initializer runs fresh at the delta's
// _initialize, so the new body serves; the old binding's instance keeps
// running for old callers. The unchanged sibling val must keep its value.
const src12 = edit(src11, 'lambdas.kt',
    'val transform: (Int) -> Int = { it * 2 }',
    'val transform: (Int) -> Int = { it * 3 }')
const r12 = await compileNext(src12)
if (r12.error) fail('v12 compile: ' + r12.error)
sweep = await applyDelta(live, r12.bytes, base)
console.log('v12: applyTransform(5)', live.exports.applyTransform(5),
    '| applyAll(1)', live.exports.applyAll(1), '—', r12.bytes.length, 'B delta')
if (live.exports.applyTransform(5) !== 15) fail('v12 new lambda binding must serve: ' + live.exports.applyTransform(5))
if (live.exports.applyAll(1) !== 20) fail('v12 unchanged sibling val must keep its value: ' + live.exports.applyAll(1))

// v13 — INTERFACE DEFAULT-METHOD bodies on LIVE instances (shapes.kt):
// itable entries read slots, so a held v1-born FlatGreeter must run the
// NEW default bodies while the LoudGreeter's override still wins
const src13 = edit(edit(src12, 'shapes.kt',
    'fun prefix(): String = "hi"',
    'fun prefix(): String = "hola"'),
    'shapes.kt',
    'fun greet(name: String): String = prefix() + " " + name',
    'fun greet(name: String): String = prefix() + ", " + name')
const r13 = await compileNext(src13)
if (r13.error) fail('v13 compile: ' + r13.error)
sweep = await applyDelta(live, r13.bytes, base)
console.log('v13: flat', live.exports.greet(flat, 'rex'), '| loud', live.exports.greet(loud, 'rex'),
    '—', r13.bytes.length, 'B delta')
if (live.exports.greet(flat, 'rex') !== 'hola, rex') fail('v13 held instance must run the new default bodies: ' + live.exports.greet(flat, 'rex'))
if (live.exports.greet(loud, 'rex') !== 'HEY, rex') fail('v13 override must still win: ' + live.exports.greet(loud, 'rex'))

// v14 — the OBJECT's OWN file is dirty (state.kt), but object state
// holders have DEFVAR semantics: the delta's _initialize ADOPTS the
// previous live instance (the __hot_adopt$ host import; no previous
// binding answers null, so cold boots still construct) instead of
// re-running the initializer. Top-level singleton state SURVIVES the
// object's own redefinition (hits: 3 -> 4), the companion likewise
// (made: 1), and the held Tagged INSTANCE's fields survive (cells) —
// the Compose state-holder story.
const src14 = edit(src13, 'state.kt',
    'fun hit(): Int {\n        hits = hits + 1\n        return hits\n    }',
    'fun hit(): Int {\n        hits = hits + 1\n        return hits + 100\n    }')
const r14 = await compileNext(src14)
if (r14.error) fail('v14 compile: ' + r14.error)
sweep = await applyDelta(live, r14.bytes, base)
// stateful calls captured ONCE (see v9)
const hit14 = live.exports.hitCounter()
const tag14 = live.exports.useTagged(tag)
const made14 = live.exports.tagsMade()
console.log('v14: hitCounter', hit14, '| useTagged(tag)', tag14,
    '| tagsMade', made14, '—', r14.bytes.length, 'B delta')
if (hit14 !== 104) fail('v14 object state must survive its own redefinition (defvar): ' + hit14)
if (tag14 !== 'a#2') fail('v14 held instance state must survive: ' + tag14)
if (made14 !== 1) fail('v14 companion state must survive its own redefinition (defvar): ' + made14)

// v15 — the real Compose case: ADD A FIELD to the state-holder object.
// The adopted instance holds the previous payload version; the per-class
// stepper migrates it in place on the first new-code field access —
// surviving state keeps its values, and the new field migrates with the
// TYPE default (String -> null): initializers are cold-boot only, the
// initforms gap is a listed ROADMAP quality item (the v3 beat's note=null
// is the same behavior on plain instances).
const src15 = edit(edit(src14, 'state.kt',
    'object Counter {\n    var hits: Int = 0',
    'object Counter {\n    var hits: Int = 0\n    var tag: String = "none"'),
    'state.kt',
    '@WasmExport("tagsMade")',
    '@WasmExport("counterTag")\nfun counterTag(): String = Counter.tag\n\n@WasmExport("tagsMade")')
const r15 = await compileNext(src15)
if (r15.error) fail('v15 compile: ' + r15.error)
sweep = await applyDelta(live, r15.bytes, base)
const ctag15 = live.exports.counterTag()
const hit15 = live.exports.hitCounter()
console.log('v15: counterTag', ctag15, '| hitCounter', hit15, '—', r15.bytes.length, 'B delta')
if (ctag15 !== null) fail('v15 new field must migrate with the TYPE default (initforms are a follow-up): ' + ctag15)
if (hit15 !== 105) fail('v15 object state survives a field addition (migration): ' + hit15)

// v16 — an INCOMPATIBLE holder change (gain a class supertype): the
// shape-carrying swaps (signatures mentioning the Counter cell — hit,
// getInstance, static_init, accessors) are engine-rejected like any
// cell-chain change (v7). But a signature-COMPATIBLE function whose body
// merely touches the holder (hitCounter: () -> Int) is accepted by the
// sweep, serves the new body, and traps LOUDLY at the first holder
// access: the adoption boundary check (the old instance handed to the
// new cell type), surfaced by the static-init machinery as a catchable
// ExceptionInInitializerError. Everything not touching the holder
// keeps running — and the companion, whose shape is untouched, even
// adopts across this rejected patch.
const src16 = edit(src15, 'state.kt',
    'object Counter {',
    'open class CounterBase\n\nobject Counter : CounterBase() {')
const r16 = await compileNext(src16)
if (r16.error) fail('v16 compile: ' + r16.error)
const result16 = await applyDelta(live, r16.bytes, base)
console.log('v16: incompatible holder —', result16.rejected.length, 'swaps engine-rejected —',
    r16.bytes.length, 'B delta')
if (result16.rejected.length === 0) fail('v16 the cell-chain holder change must be engine-rejected')
let trapped = false
try { live.exports.hitCounter() } catch { trapped = true }
if (!trapped) fail('v16 touching the incompatible holder must trap at the adoption boundary (ExceptionInInitializerError)')
if (live.exports.tagsMade() !== 1) fail('v16 code not touching the holder must keep running: ' + live.exports.tagsMade())

console.log('BROWSER LOOP PASSED — swaps, migration add+delete, zoo dispatch, additive methods, METHOD ADDITIONS hot (vtable migration), engine-rejected cell-chain change, GHOST instances trap honestly, LANGUAGE-FEATURE STRESS (generics/vararg/extension hot, cross-file inline propagation, held-lambda re-binding, itable defaults on live instances), OBJECT DEFVAR (state survives its own redefinition + field addition via adoption+migration; incompatible holder: shape-carrying swaps rejected, first touch traps ExceptionInInitializerError, the rest keeps running)')

process.exit(0)
