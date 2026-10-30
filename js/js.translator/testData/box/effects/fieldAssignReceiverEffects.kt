// TARGET_BACKEND: JS_IR
// ONLY_IR_DCE
// CHECK_OPTIMIZED_JS
// ENABLE_UNUSED_PROPERTY_DCE
// RUN_PLAIN_BOX_FUNCTION
// INFER_MAIN_MODULE

// MODULE: receiver_effects
// FILE: lib.kt

external fun record(value: String): Int
class Cell { var unused = 0 }
val cell = Cell()
fun nextCell(): Cell { record("R"); return cell }
fun rhs(): Int { record("V"); return 1 }
@JsExport fun runPure() { nextCell().unused = 1 }
@JsExport fun runWrite() { nextCell().unused = rhs() }

// FILE: test.js

var trace = "";
function record(value) { trace += value; return 0; }
function box() {
    var api = this["receiver_effects"];
    trace = ""; api.runPure(); var pure = trace;
    trace = ""; api.runWrite(); var write = trace;
    if (pure !== "R" || write !== "RV")
        return "Receiver traces: pure=" + pure + ", write=" + write;
    return "OK";
}
