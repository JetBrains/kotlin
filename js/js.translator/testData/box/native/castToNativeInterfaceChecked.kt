// EXPECTED_REACHABLE_NODES: 1281
external interface I {
    fun foo(): String
}

fun createObject(): Any? = null

fun box(): String {
    try {
        (createObject() as I).foo()
        return "fail: exception not thrown"
    }
    catch (e: ClassCastException) {
        return "OK"
    }
}