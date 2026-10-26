// TARGET_BACKEND: JS_IR
// ONLY_IR_DCE
// CHECK_OPTIMIZED_JS
// ENABLE_UNUSED_PROPERTY_DCE
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE

// MODULE: probe_inherited_call
// FILE: lib.kt

external fun record()

open class Base {
    open fun touch(): Int { record(); return 0 }
}

class Leaf : Base()

fun operate(value: Leaf): Int = value.touch()

class Sink(value: Leaf) {
    val unused = operate(value)
}

@JsExport
fun createSink(): Any = Sink(Leaf())

// FILE: test.js

var hits = 0;
function record() { hits++; }

function box() {
    const sink = this["probe_inherited_call"].createSink();
    if (hits !== 1) return "Inherited call count: " + hits;
    return "OK";
}
