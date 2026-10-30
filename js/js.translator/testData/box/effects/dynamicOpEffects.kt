// TARGET_BACKEND: JS_IR
// ONLY_IR_DCE
// CHECK_OPTIMIZED_JS
// ENABLE_UNUSED_PROPERTY_DCE
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE

// MODULE: probe_dynamic_write
// FILE: lib.kt

fun operate(value: dynamic): Int {
    value.field = 7
    return 0
}

class Sink(value: dynamic) {
    val unused = operate(value)
}

@JsExport
fun createSink(value: dynamic): Any = Sink(value)

// FILE: test.js

var input = {field: 0};

function box() {
    const sink = this["probe_dynamic_write"].createSink(input);
    if (input.field !== 7) return "Dynamic write was lost: " + input.field;
    return "OK";
}
