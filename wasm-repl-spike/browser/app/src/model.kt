@file:OptIn(kotlin.wasm.ExperimentalWasmInterop::class)

// The demo app for the browser hot-reload loop, file 1 of 2: the Model
// counter (the state story — swaps preserve it, field changes migrate
// it). File granularity for the loop's deltas: an edit HERE recompiles
// this file's slots; the zoo file rides along imported. Edit this file
// while dev.mjs serves it: every save recompiles and hot-swaps into the
// live page. Objects are created ONCE by the page and held by the GLUE
// (JS can hold wasm GC objects) — their state survives every edit.
import kotlin.wasm.WasmExport
import kotlin.js.JsString
import kotlin.js.toJsString

class Model(var count: Int, val label: String) {
    fun bump(): Int {
        count = count + 1
        return count
    }

    fun render(): String = label + ": " + count
}

@WasmExport("createModel")
fun createModel(label: JsString): Model = Model(0, label.toString())

@WasmExport("bumpModel")
fun bumpModel(m: Model): Int = m.bump()

@WasmExport("renderModel")
fun renderModel(m: Model): JsString = m.render().toJsString()
