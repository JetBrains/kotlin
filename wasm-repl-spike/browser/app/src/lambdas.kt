@file:OptIn(kotlin.wasm.ExperimentalWasmInterop::class)

// Lambdas and closures — the Function machinery. A lambda COMPILES to a
// class instance, and lambda classes are excluded from slotting (they go
// through link-time dedup), so a lambda's body is hot only through its
// ENCLOSING slotted function. A lambda HELD in a top-level val is a
// binding like any other: redefining the val (editing this file)
// creates a NEW binding whose initializer runs fresh — the held
// instance of the old binding keeps running the old body for old
// callers. That is the reason Compose needs __hot_invalidate to
// recompose (fresh lambdas observe the new source) rather than any
// in-place patching of lambda objects.
//
// `useTwice` calls the INLINE function `twice` from toolbox.kt — the
// cross-file inline test: editing an inline body must dirty every
// caller's file (the IC's inline hashing exists for exactly this), so
// the caller re-emits with the new body inlined.
import kotlin.wasm.WasmExport
import kotlin.js.JsString
import kotlin.js.toJsString

val transform: (Int) -> Int = { it * 2 }

val transforms: List<(Int) -> Int> = listOf({ it + 1 }, { it * 10 })

@WasmExport("applyTransform")
fun applyTransform(n: Int): Int = transform(n)

@WasmExport("applyAll")
fun applyAll(n: Int): Int = transforms.fold(n) { acc, f -> f(acc) }

fun makeAdder(by: Int): (Int) -> Int = { it + by }

@WasmExport("useAdder")
fun useAdder(base: Int, by: Int): Int {
    val add = makeAdder(by)
    return add(base) + add(base + 1)
}

@WasmExport("useTwice")
fun useTwice(n: Int): Int = twice(n)
