// ISSUE: KT-88819
// IGNORE_BACKEND: WASM_JS

external interface I {
    fun foo(): String
}

fun createObject(): Any? = null

fun box(): String {
    try {
        (createObject() as I).foo()
        return "fail: exception not thrown"
    }
    catch (e: NullPointerException) {
        return "OK"
    }
}
