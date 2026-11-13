@file:OptIn(kotlin.wasm.ExperimentalWasmInterop::class)

// The demo app for the browser hot-reload loop, file 2 of 2: the zoo —
// the dispatch story. Cow has NO override (speaks the base body); Dog,
// Cat and Duck override. Present from v1, because gaining a supertype
// later is itself a shape change the engine would reject. The demo
// beats add a Pig here (beat 6) and the open-method additions (7d) —
// vtable migration on live instances.
import kotlin.wasm.WasmExport
import kotlin.js.JsString
import kotlin.js.toJsString

open class Animal(val name: String) {
    open fun speak(): String = "…"

    fun intro(): String = name + " says " + speak()
}

class Dog(name: String) : Animal(name) {
    override fun speak(): String = "woof"
}

class Cat(name: String) : Animal(name) {
    override fun speak(): String = "meow"
}

class Cow(name: String) : Animal(name)

class Duck(name: String) : Animal(name) {
    override fun speak(): String = "quack"
}

// Beat 6 adds a Pig class here and a "pig" case below — additive, hot.
@WasmExport("make")
fun make(kind: JsString, name: JsString): Animal = when (kind.toString()) {
    "base" -> Animal(name.toString())
    "dog" -> Dog(name.toString())
    "cat" -> Cat(name.toString())
    "cow" -> Cow(name.toString())
    "duck" -> Duck(name.toString())
    else -> throw IllegalArgumentException("unknown kind: " + kind.toString())
}

@WasmExport("intro")
fun intro(a: Animal): JsString = a.intro().toJsString()
