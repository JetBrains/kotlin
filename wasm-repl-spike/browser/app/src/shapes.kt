@file:OptIn(kotlin.wasm.ExperimentalWasmInterop::class)

// Interfaces with DEFAULT methods — the itable story on LIVE instances.
// Default bodies are slotted like everything else and itable entries
// read slots, so a default-body edit must reach instances created BEFORE
// the edit. Only body edits are exercised: an interface member-SET
// change is the known interfaceId inconsistency (ROADMAP §2) — `is`
// would lie while `as` traps — deliberately out of scope here.
import kotlin.wasm.WasmExport
import kotlin.js.JsString
import kotlin.js.toJsString

interface Greeter {
    fun prefix(): String = "hi"

    fun greet(name: String): String = prefix() + " " + name
}

class LoudGreeter : Greeter {
    override fun prefix(): String = "HEY"
}

class FlatGreeter : Greeter

@WasmExport("makeGreeter")
fun makeGreeter(kind: JsString): Greeter =
    if (kind.toString() == "loud") LoudGreeter() else FlatGreeter()

@WasmExport("greetWith")
fun greetWith(kind: JsString, name: JsString): JsString {
    val g: Greeter = makeGreeter(kind)
    return g.greet(name.toString()).toJsString()
}

@WasmExport("greet")
fun greet(g: Greeter, name: JsString): JsString = g.greet(name.toString()).toJsString()
