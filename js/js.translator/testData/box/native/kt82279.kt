// ISSUE: KT-82279
// DONT_TARGET_EXACT_BACKEND: WASM_JS
// WASM_MUTE_REASON: UNSUPPORTED_JS_INTEROP
// IGNORE_BACKEND: JS_IR_ES6

// FILE: main.kt
external abstract class Processor {
    abstract fun process(): String
}

external open class ProcessorReference(klass: JsClass<out Processor>)

external fun process(reference: ProcessorReference): String

class MyProcessor: Processor() {
    override fun process(): String = "OK"

    companion object : ProcessorReference(MyProcessor::class.js)
}

fun box(): String {
    return process(MyProcessor)
}

// FILE: native.js
function Processor() {}

function ProcessorReference(ctor) {
    this.ctor = ctor;
}

function process(reference) {
    return new reference.ctor().process();
}
