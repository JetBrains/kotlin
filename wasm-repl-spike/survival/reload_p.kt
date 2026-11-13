// Hot-reload gate experiment — patch module P, supplying replacement BODIES
// as exported funcrefs, installed into module A's compiler-generated slot
// setters. Exports under the flag are the slot-reading ENTRIES (they read
// P's own slot globals, initialized to P's bodies — one extra indirection,
// same behavior). Their wasm function types must canonically match module
// A's slot types:
//   fib replacement:        (i32) -> i32
//   Counter.value method:   (ref null $Counter) -> i32   — dispatch receiver
//   is the first parameter, so a top-level function taking Counter has the
//   SAME wasm type as the method and can be installed in the method's slot.
// Counter is declared identically to reload_a.kt so rec groups unify.
// NOTE: since the stable-cell encoding, P must be built WITH the flag too —
// cell-built A no longer unifies class types with plain-built modules
// (function-only donation still works cross-mode).

import kotlin.wasm.WasmExport

class Counter(var n: Int) {
    fun value(): Int = n
}

@OptIn(kotlin.wasm.ExperimentalWasmInterop::class)
@WasmExport("fibv2")
fun fibv2(n: Int): Int = if (n < 2) 100 + n else fibv2(n - 1) + fibv2(n - 2)

@OptIn(kotlin.wasm.ExperimentalWasmInterop::class)
@WasmExport("valuev2")
fun valuev2(c: Counter): Int = c.n * 1000
