// TARGET_BACKEND: JS_IR
// ONLY_IR_DCE
// CHECK_OPTIMIZED_JS

object Global { var x = 10 }

// FUNCTION_HAS_EFFECTS: function write WRITE
fun write() { Global.x = 15 }
// FUNCTION_HAS_EFFECTS: function read READ
fun read() = Global.x
// FUNCTION_HAS_EFFECTS: function pure PURE
fun pure() {}

// FUNCTION_HAS_EFFECTS: function fWrite WRITE
// FUNCTION_HAS_EFFECTS: function gWrite WRITE
// FUNCTION_HAS_EFFECTS: function hWrite WRITE
fun fWrite(n: Int) { gWrite(n); write() }
fun gWrite(n: Int) { hWrite(n - 1); }
fun hWrite(n: Int) { if (n > 0) fWrite(n - 1) }

// FUNCTION_HAS_EFFECTS: function fRead READ
// FUNCTION_HAS_EFFECTS: function gRead READ
// FUNCTION_HAS_EFFECTS: function hRead READ
fun fRead(n: Int) { gRead(n); read() }
fun gRead(n: Int) { hRead(n - 1); }
fun hRead(n: Int) { if (n > 0) fRead(n - 1) }

// FUNCTION_HAS_EFFECTS: function fPure PURE
// FUNCTION_HAS_EFFECTS: function gPure PURE
// FUNCTION_HAS_EFFECTS: function hPure PURE
fun fPure(n: Int) { gPure(n); pure() }
fun gPure(n: Int) { hPure(n - 1); }
fun hPure(n: Int) { if (n > 0) fPure(n - 1) }

fun box(): String {
    fWrite(5)
    fRead(5)
    fPure(5)
    return "OK"
}
