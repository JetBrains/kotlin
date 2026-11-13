// The hot-reload glue — browser-shaped, browser-only APIs (WebAssembly,
// fetch). The dev server and the Node gate use the same module.
//
// The loop: the stdlib BASE loads once (loadBase); every edit is a
// FILE-GRANULAR delta wasm — its dirty files defined, its clean files'
// declarations imported from the live patch chain under the app's own
// module name, everything else imported from the base through the
// multimodule `__fn$`/`__vt$`/`__rt$` convention. The glue holds the
// instance CHAIN (liveView) and per edit does ONE call, applyDelta:
// instantiate the delta against base + chain (loadPatch), sweep every
// live instance's `__hot_set$X` setters with the delta's `__hot_entry$X`
// funcrefs (the engine type-checks each install; rejections are
// collected, not thrown — that patch's export of the name is marked
// dead and the previous live version keeps running), then push. The
// PAGE holds live objects (wasm GC objects are JS-referenceable) and
// calls through the live view; every call funnels through the mutable
// slots, and class redefinitions ride along — the new bodies' payload
// steppers migrate held instances lazily, in place.

// the base's generated import object (wasm-js intrinsics + js-string
// builtins) lives in app/build next to the base wasm — imported lazily
// so the first compile can create it
let baseImportObjectPromise = null

function baseImportObject() {
    if (baseImportObjectPromise === null) {
        baseImportObjectPromise =
            import('./app/build/kotlin-kotlin-stdlib.import-object.mjs').then((m) => m.importObject)
    }
    return baseImportObjectPromise
}

export const BASE_WASM_OPTIONS = { builtins: ['js-string'], importedStringConstants: "'" }
export const PATCH_WASM_OPTIONS = { builtins: [''], importedStringConstants: "'" }

/** The stdlib base instance — cold code, once per session. */
export async function loadBase(wasmBytes) {
    const module = new WebAssembly.Module(wasmBytes, BASE_WASM_OPTIONS)
    // the base is the image chain's cold root: every object-adoption
    // import answers "no previous binding" — normal cold initialization
    const imports = Object.assign({}, await baseImportObject(), {
        host: new Proxy({}, { get: () => () => null }),
    })
    const instance = new WebAssembly.Instance(module, imports)
    return instance
}

/** An app patch, instantiated against the LIVE base and — for DELTA
 * patches — the live patch chain. The import object is the multimodule
 * convention: `<kotlin>` = the base instance's exports (the
 * `__fn$`/`__vt$`/`__rt$` service imports), the exception tag
 * (WebAssembly.JSTag — same one the base uses), and the imported string
 * constants proxy. A delta defines only its dirty files; its clean
 * files' declarations are imports under the artifact's OWN module name,
 * which the compiler discovers here from the import section — they
 * resolve to the previous instances' merged exports (newest-first: an
 * unchanged function resolves to the instance that defined its file
 * last, and since imports bind to the owner's SLOT-READING entry, later
 * redefinitions still reach the delta's callers through the sweep). */
export async function loadPatch(wasmBytes, base, prevExports = null, live = null) {
    const module = new WebAssembly.Module(wasmBytes, PATCH_WASM_OPTIONS)
    const stringConstants = new Proxy({}, { get(_, prop) { return prop } })
    const importObject = {
        js_code: {},
        intrinsics: { tag: WebAssembly.JSTag },
        "'": stringConstants,
        '<kotlin>': base.exports,
    }
    // Object-state adoption (defvar semantics for `object` holders): the
    // delta's initializers import the previous binding's getter from
    // "host" as `__hot_adopt$<object>#<hash>` — the compiler emits the
    // boundp check because a fresh module's value cell starts empty. The
    // `#<hash>` suffix is the one naming piece stable across the klib
    // round trip AND edits of the declaring file (adoption is exactly
    // the dirty-file case; the slot sweep's disambiguator rides on the
    // same property), so resolve by it: find the newest live instance
    // exporting a `__hot_entry$…#<hash>` — the getter's slot-reading
    // entry, which at init (pre-sweep) still serves the previous body and
    // returns the previous value cell's contents. Null — normal cold
    // init — when no previous binding exists (first patch, brand-new
    // object). The engine type-checks the returned instance against the
    // import's declared cell type at this boundary, so an incompatible
    // holder change traps here, at init.
    const provided = new Set(['<kotlin>', 'js_code', 'intrinsics', "'", 'host'])
    for (const imp of WebAssembly.Module.imports(module)) {
        if (imp.module === 'host' && imp.name.startsWith('__hot_adopt$')) {
            const suffix = imp.name.slice(imp.name.lastIndexOf('#') + 1)
            let getter = null
            if (live !== null) {
                outer: for (let i = live.instances.length - 1; i >= 0; i--) {
                    for (const n of Object.keys(live.instances[i].exports)) {
                        if (!n.startsWith('__hot_entry$') || !n.endsWith('#' + suffix)) continue
                        const dead = live.deadExports.get(n)
                        if (dead !== undefined && dead.has(i)) continue
                        getter = live.instances[i].exports[n]
                        break outer
                    }
                }
            }
            // Resolve EAGERLY, before instantiation and before the sweep:
            // the delta's object initialization is LAZY, so the adopt probe
            // can fire arbitrarily late — after the sweep has rewired the
            // previous entry's slot to the new body (which would read the
            // new, still-null global). Capturing the VALUE here — the
            // pre-sweep `symbol-value` of the holder — is correct whenever
            // the probe fires. An unresolved or unreadable previous binding
            // adopts null: normal cold init. The engine still type-checks
            // the value at the JS boundary when the wasm import call
            // receives it.
            let adopted = null
            if (typeof getter === 'function') {
                try { adopted = getter() } catch { /* unreadable: cold init */ }
            }
            importObject.host ??= {}
            importObject.host[imp.name] = () => adopted
        } else if (prevExports !== null && !provided.has(imp.module)) {
            importObject[imp.module] ??= prevExports
        }
    }
    const instance = new WebAssembly.Instance(module, importObject)
    // hot modules are REACTOR-STYLE (compiler-owned): no start section —
    // run the val/field initializers explicitly, after instantiation has
    // resolved the base imports
    if (typeof instance.exports._initialize === 'function') {
        instance.exports._initialize()
    }
    return instance
}

/** The LIVE view over the patch-instance chain: `exports` resolves each
 * name newest-instance-first, SKIPPING dead definitions: when the engine
 * rejects a patch's entry for a slot at an owner's setter (a class SHAPE
 * change — the new cell type can't serve old instances), that patch's
 * export of the name is dead code and the previous live version keeps
 * running — the loop's "old code keeps running" contract. Delta emission
 * made the chain the norm — a delta defines only its dirty files, so an
 * export lives in the instance whose file was defined last (a
 * redefinition wins by being newer; a clean file's export keeps resolving
 * to its owner). */
export function liveView(instances) {
    /** export name -> Set(instance index) whose definition is dead */
    const deadExports = new Map()
    return {
        instances,
        deadExports,
        exports: new Proxy({}, {
            get(_, name) {
                if (typeof name !== 'string') return undefined
                const dead = deadExports.get(name)
                for (let i = instances.length - 1; i >= 0; i--) {
                    if (dead !== undefined && dead.has(i)) continue
                    const v = instances[i].exports[name]
                    if (v !== undefined) return v
                }
                return undefined
            },
        }),
    }
}

// The image registry: the patch's complete payload-version table, read
// straight from the binary (no instantiation needed).
export function readRegistry(wasmBytes) {
    const module = new WebAssembly.Module(wasmBytes, PATCH_WASM_OPTIONS)
    const sections = WebAssembly.Module.customSections(module, 'hot-reload-registry')
    return sections.length > 0 ? new Uint8Array(sections[0]) : null
}

// Sweep every LIVE instance's slot setters with the patch's entries (a
// liveView — see liveView). Every install is engine-judged (V8
// type-checks the funcref at the setter): a rejection means the new
// body's wasm signature differs from the slot's — the honest signal of a
// class SHAPE change (a changed cell/vtable type flows into every
// signature mentioning the class). Rejections are collected, not
// thrown: everything the engine accepts is swapped, the old code keeps
// running for the rest — the same policy as the REPL.
//
// With deltas the sweep must cover EVERY instance in the chain, not just
// the latest: a redefined function's slot lives in the instance that
// defined its file LAST — which may be any earlier generation — and
// every reader, of any generation, dispatches through its owner's slot.
// The REPL's per-generation sweep, transplanted.
//
// Returns { swapped, added, rejected: [{name, error}] }.
export function applyPatch(live, patch) {
    const patchExports = patch.exports
    const swapped = []
    const rejected = []
    for (const instance of live.instances) {
        const liveExports = instance.exports
        for (const name of Object.keys(liveExports)) {
            if (!name.startsWith('__hot_set$')) continue
            const entryName = '__hot_entry$' + name.slice('__hot_set$'.length)
            const entry = patchExports[entryName]
            if (typeof entry !== 'function') continue
            try {
                liveExports[name](entry) // the engine type-checks the funcref here
                swapped.push(name.slice('__hot_set$'.length).split('#')[0])
            } catch (e) {
                rejected.push({ name: name.slice('__hot_set$'.length).split('#')[0], error: String(e) })
            }
        }
    }
    // wasm instance exports are FROZEN — brand-new functions can't be
    // attached; they ride the patch instance instead (the liveView's
    // newest-first exports reach them). The multimodule service convention
    // (`__fn$`/`__vt$`/`__rt$`) is ABI plumbing, not user-visible
    // exports — filtered out.
    const added = Object.keys(patchExports).filter(
        (n) => !n.startsWith('__') && !live.instances.some((i) => n in i.exports)
    )
    return { swapped, added, rejected }
}

/** The one call the loop makes per edit: instantiate the patch against
 *  the base + the live chain's merged exports (a delta's clean-file
 *  imports resolve there), sweep every live instance's setters with its
 *  entries, then push it onto the chain (after the sweep — its own
 *  slots are pre-initialized; only the PREVIOUS instances need
 *  installing). An empty chain means the first, full patch. */
export async function applyDelta(live, wasmBytes, base) {
    let instance
    try {
        instance = await loadPatch(
            wasmBytes,
            base,
            live.instances.length > 0 ? live.exports : null,
            live.instances.length > 0 ? live : null,
        )
    } catch (e) {
        // instantiation or INIT failed — an adopted holder whose cell type
        // no longer unifies with the previous live instance traps there.
        // Nothing was swept and nothing was pushed: the live image keeps
        // running the old code — the same contract as a rejected swap.
        return { swapped: [], added: [], rejected: [{ name: '<patch>', error: String(e) }], error: String(e) }
    }
    const result = applyPatch(live, instance)
    // an engine-rejected entry means this patch's export of that name is
    // DEAD code for the live world (the new cell type can't serve old
    // instances) — mark it so the live view keeps resolving the previous
    // live version ("old code keeps running")
    const idx = live.instances.length
    for (const name of new Set(result.rejected.map((r) => r.name))) {
        let dead = live.deadExports.get(name)
        if (dead === undefined) {
            dead = new Set()
            live.deadExports.set(name, dead)
        }
        dead.add(idx)
    }
    live.instances.push(instance)
    return result
}
