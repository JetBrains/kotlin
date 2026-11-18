// The hot-reload glue — browser-shaped, browser-only APIs (WebAssembly,
// fetch). The dev server and the Node gate use the same module.
//
// The loop: the stdlib BASE loads once (loadBase) and IS the image (its
// stdlib owns the fdefinition table). Every edit is a FILE-GRANULAR
// delta wasm — its dirty files defined, its clean files' declarations
// imported from the live patch chain under the app's own module name,
// everything else imported from the base through the multimodule
// `__fn$`/`__vt$`/`__rt$` convention. Per edit the glue does ONE call,
// applyDelta: instantiate the delta against base + chain (loadPatch)
// and push — LOAD IS APPLY, so that is the whole story: the delta's
// init resolves its bindings' cells by name against the image's table
// and installs its impls; the newest definition is live the moment
// loading finishes. The PAGE holds live objects (wasm GC objects are
// JS-referenceable) and calls through the live view; every call reads
// its binding's cell, and class redefinitions ride along — the new
// bodies' payload steppers migrate held instances lazily, in place.

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

const BASE_WASM_OPTIONS = { builtins: ['js-string'], importedStringConstants: "'" }
const PATCH_WASM_OPTIONS = { builtins: [''], importedStringConstants: "'" }

/** The stdlib base instance — cold code, once per session. Its
 *  `_initialize` MUST run: that is LOAD IS APPLY for the base itself —
 *  where it registers its cells into the image's table (the stdlib
 *  runtime). */
export async function loadBase(wasmBytes) {
    const module = new WebAssembly.Module(wasmBytes, BASE_WASM_OPTIONS)
    const imports = Object.assign({}, await baseImportObject(), {
        // the base is the chain's cold root: every object-adoption import
        // answers "no previous binding" — normal cold initialization
        host: new Proxy({}, { get: () => () => null }),
    })
    const instance = new WebAssembly.Instance(module, imports)
    if (typeof instance.exports._initialize === 'function') {
        instance.exports._initialize()
    }
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
 * last, and since imports bind to the owner's CELL-READING entry, later
 * redefinitions still reach the delta's callers through the shared
 * cell). */
export async function loadPatch(wasmBytes, base, prevExports = null, live = null) {
    const module = new WebAssembly.Module(wasmBytes, PATCH_WASM_OPTIONS)
    const stringConstants = new Proxy({}, { get(_, prop) { return prop } })
    const importObject = {
        js_code: {},
        intrinsics: { tag: WebAssembly.JSTag },
        "'": stringConstants,
        // the delta's resolve calls the image's fdefinition table THROUGH
        // this channel — the base instance owns it
        '<kotlin>': base.exports,
        host: {},
    }
    // Object-state adoption (defvar semantics for `object` holders): the
    // delta's initializers import the previous binding's getter from
    // "host" as `__hot_adopt$<object>#<hash>` — the compiler emits the
    // boundp check because a fresh module's value cell starts empty. The
    // `#<hash>` suffix is stable across the klib round trip AND edits of
    // the declaring file (adoption is exactly the dirty-file case), so
    // resolve by it: find the newest live instance exporting a
    // `__hot_entry$…#<hash>` — the getter's cell-reading entry, which
    // still serves the previous body and returns the previous value
    // cell's contents. Null — normal cold init — when no previous
    // binding exists (first patch, brand-new object). The engine
    // type-checks the returned instance against the import's declared
    // cell type at this boundary, so an incompatible holder change
    // traps here, at init.
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
            // Resolve EAGERLY, before instantiation: the delta's object
            // initialization is LAZY, so the adopt probe can fire
            // arbitrarily late — after the holder's body has been
            // redefined (which would read the new, still-null global).
            // Capturing the VALUE here — the previous `symbol-value` of
            // the holder — is correct whenever the probe fires. An
            // unresolved or unreadable previous binding adopts null:
            // normal cold init. The engine still type-checks the value
            // at the JS boundary when the wasm import call receives it.
            let adopted = null
            if (typeof getter === 'function') {
                try { adopted = getter() } catch { /* unreadable: cold init */ }
            }
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

/** The one call the loop makes per edit: instantiate the patch against
 *  the base + the live chain's merged exports (a delta's clean-file
 *  imports resolve there) and push it onto the chain. An empty chain
 *  means the first, full patch. */
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
        // instantiation or INIT failed — an adopted holder whose cell
        // type no longer unifies with the previous live instance traps
        // there. Nothing was pushed: the live image keeps running the
        // old code.
        return { applied: [], added: [], rejected: [{ name: '<patch>', error: String(e) }], error: String(e) }
    }
    // LOAD IS APPLY: the init already resolved every binding's cell
    // against the image's table and installed this patch's impls (the
    // newest definition is live). An incompatible redefinition installs
    // too — the fdefinition is type-erased, exactly SBCL's — and callers
    // trap at their own per-call cast (rejected is always empty).
    const boundCount = Object.keys(instance.exports).filter((n) => n.startsWith('__hot_entry$')).length
    const added = Object.keys(instance.exports).filter(
        (n) => !n.startsWith('__') && !live.instances.some((i) => n in i.exports)
    )
    live.instances.push(instance)
    return { applied: new Array(boundCount).fill(''), added, rejected: [] }
}
