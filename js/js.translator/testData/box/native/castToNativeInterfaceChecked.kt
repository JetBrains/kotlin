external interface I {
    fun foo(): String
}

fun createObject(): Any? = null

fun box(): String {
    try {
        (createObject() as I).foo()
        return "fail: exception not thrown"
    }
    // It used to be a ClassCastException, however, on JVM for null it's  NullPointerException.
    // So, to have parity in the common code, it throws a NullPointerException for null value.
    // catch (e: ClassCastException) {
    catch (e: NullPointerException) {
        return "OK"
    }
}
