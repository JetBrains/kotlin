// TARGET_BACKEND: JS_IR
// ONLY_IR_DCE
// CHECK_OPTIMIZED_JS
// ENABLE_UNUSED_PROPERTY_DCE

// FUNCTION_HAS_EFFECTS: function=noCatch WRITE
fun noCatch() {
    throw Exception("a")
}

// FUNCTION_HAS_EFFECTS: function=catchAll WRITE
fun catchAll() {
    try {
        throw Exception("must be caught")
    } catch (e: Throwable) {}
}

fun box(): String {
    catchAll()
    try {
        noCatch()
    } catch (e: Exception) {
        return "OK"
    }
    return "no exception thrown"
}
