// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1281
external class TypeError(message: String?, fileName: String? = definedExternally, lineNumber: Int? = definedExternally) : Throwable

fun box(): String {
    try {
        js("null.foo()")
        return "fail: expected exception not thrown"
    }
    catch (e: TypeError) {
        return "OK"
    }
}