// Hot-reload gate experiment — module A, compiled WITH -Xwasm-hot-reload.
//
// The compiler (this branch) automatically emits per function:
//   __slot$fib           mutable funcref global, init = ref.func of the body
//   __hot_entry$fib      slot-reading entry, registered under the real symbol
//   __hot_set$fib        exported setter taking (ref null $funType)
// so the host can swap bodies at runtime — including method bodies for
// ALREADY-EXISTING instances (vtable entries reference the entry).
//
// We export the helper thunks with @WasmExport to drive the experiment
// from JS (the relaxed checker allows user-typed signatures).

import kotlin.wasm.WasmExport

fun fib(n: Int): Int = if (n < 2) n else fib(n - 1) + fib(n - 2)

class Counter(var n: Int) {
    fun value(): Int = n
}

@OptIn(kotlin.wasm.ExperimentalWasmInterop::class)
@WasmExport
fun makeCounter(): Counter = Counter(6)

@OptIn(kotlin.wasm.ExperimentalWasmInterop::class)
@WasmExport
fun readCounter(c: Counter): Int = c.value()

@OptIn(kotlin.wasm.ExperimentalWasmInterop::class)
@WasmExport
fun fibViaSlot(n: Int): Int = fib(n)

// Catchable-deprecation gate: calls fib through its slot inside a try/catch,
// demonstrating that the compiler-generated deprecation twin raises a real,
// catchable Kotlin IllegalStateException (shared exception tag) with a
// message naming the old signature.
@OptIn(kotlin.wasm.ExperimentalWasmInterop::class)
@WasmExport
fun fibCatching() {
    // println so the result is visible from JS (Kotlin String is opaque there)
    try { println("ok: " + fib(5)) }
    catch (e: IllegalStateException) { println("caught ${e::class.simpleName}: " + (e.message ?: "<no message>")) }
}
