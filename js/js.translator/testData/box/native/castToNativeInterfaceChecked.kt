// EXPECTED_REACHABLE_NODES: 489
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