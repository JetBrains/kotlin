// TARGET_BACKEND: JS_IR
// ONLY_IR_DCE
// CHECK_OPTIMIZED_JS
// ENABLE_UNUSED_PROPERTY_DCE

// FUNCTION_HAS_EFFECTS: function=write WRITE
fun write() {
    throw Exception("a")
}

fun box(): String {
    try {
        write()
    } catch (e: Exception) {
        return "OK"
    }
    return "no exception thrown"
}
