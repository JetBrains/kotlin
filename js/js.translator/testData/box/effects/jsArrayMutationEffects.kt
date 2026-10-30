// TARGET_BACKEND: JS_IR
// ONLY_IR_DCE
// CHECK_OPTIMIZED_JS
// ENABLE_UNUSED_PROPERTY_DCE
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE

// MODULE: array_set_effects
// FILE: lib.kt

external val sharedArray: IntArray

fun mutate(array: IntArray): Int {
    array[0] = 7
    return 0
}

class Sink(array: IntArray) {
    val unused = mutate(array)
}

@JsExport
fun createSink(): Any = Sink(sharedArray)

// FILE: test.js

var sharedArray = new Int32Array(1);

function box() {
    sharedArray[0] = 0;
    const sink = this["array_set_effects"].createSink();
    if (sharedArray[0] !== 7)
        return "Array mutation was lost: " + sharedArray[0];
    return "OK";
}
