// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1109
external interface I {
    fun foo(): String
}
fun createObject(): Any? = null

fun box(): String {
    val result = (createObject() as? I)?.foo()
    return if (result == null) "OK" else "fail"
}