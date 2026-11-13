// Importing module: gets provider-module functions via @WasmImport with
// USER-CLASS types in the signature. If this compiles, source-level
// cross-module linking works without binary patching of import sections.

import kotlin.wasm.WasmImport

class Q(val v: Int) {
    open fun doubled(): Int = v * 2
}

@OptIn(kotlin.wasm.ExperimentalWasmInterop::class)
@WasmImport("host", "makeQ")
external fun makeQ(v: Int): Q

@OptIn(kotlin.wasm.ExperimentalWasmInterop::class)
@WasmImport("host", "readQ")
external fun readQ(q: Q): Int

@OptIn(kotlin.wasm.ExperimentalWasmInterop::class)
@WasmImport("host", "isQ")
external fun isQ(x: Any?): Boolean

fun evalHostRoundTrip(): Int {
    val q = makeQ(7)
    return readQ(q)          // calls imported fn with a locally-created Q
}

fun evalVirtualDispatch(): Int {
    val q = makeQ(7)
    return q.doubled()       // virtual dispatch on object created here, method compiled there
}

fun evalIsCheck(): Boolean = isQ(makeQ(3))

// Statement-thunk pattern for the REPL listener:
fun listener(): String = "readQ=" + readQ(makeQ(7)) + " virtual=" + evalVirtualDispatch() + " isQ=" + evalIsCheck()
