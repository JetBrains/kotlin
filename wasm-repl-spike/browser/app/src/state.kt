@file:OptIn(kotlin.wasm.ExperimentalWasmInterop::class)

// Objects and companions — the top-level-state story. An object's
// INSTANCE is a backing global in its defining file: while this file is
// CLEAN the global stays with its owner and state survives every other
// edit by construction. When THIS file is dirty the object re-defines —
// with DEFVAR semantics: the delta's _initialize adopts the previous
// live instance instead of re-running the initializer, so `hits`
// survives the object's own redefinition (the state-holder story real
// UIs need); a field addition migrates the adopted instance in place.
import kotlin.wasm.WasmExport
import kotlin.js.JsString
import kotlin.js.toJsString

object Counter {
    var hits: Int = 0

    fun hit(): Int {
        hits = hits + 1
        return hits
    }
}

class Tagged(val label: String) {
    var uses: Int = 0

    companion object {
        var made: Int = 0
    }

    fun use(): String {
        uses = uses + 1
        return label + "#" + uses
    }
}

@WasmExport("hitCounter")
fun hitCounter(): Int = Counter.hit()

@WasmExport("makeTagged")
fun makeTagged(label: JsString): Tagged {
    Tagged.made = Tagged.made + 1
    return Tagged(label.toString())
}

@WasmExport("useTagged")
fun useTagged(t: Tagged): JsString = t.use().toJsString()

@WasmExport("tagsMade")
fun tagsMade(): Int = Tagged.made
