@file:OptIn(kotlin.wasm.ExperimentalWasmInterop::class)

// The "everything else" sweep: generics, inline, vararg, extension.
// `twice` is INLINE — its callers (useTwice in lambdas.kt) inline the
// body at compile time, so editing it must dirty every caller's file
// through the IC's inline hashing. `pick`/`sum`/`shout` are slotted
// (generics, varargs and extension receivers are all hot at the
// ir2wasm level) — body edits must swap in place.
import kotlin.wasm.WasmExport
import kotlin.js.JsString
import kotlin.js.toJsString

fun <T> pick(first: Boolean, a: T, b: T): T = if (first) a else b

inline fun twice(n: Int): Int = n * 2

fun sum(vararg xs: Int): Int {
    var total = 0
    for (x in xs) total = total + x
    return total
}

fun String.shout(): String = this + "!"

@WasmExport("pickInt")
fun pickInt(first: Boolean, a: Int, b: Int): Int = pick(first, a, b)

@WasmExport("sumAll")
fun sumAll(a: Int, b: Int, c: Int): Int = sum(a, b, c)

@WasmExport("shout")
fun shout(s: JsString): JsString = s.toString().shout().toJsString()
