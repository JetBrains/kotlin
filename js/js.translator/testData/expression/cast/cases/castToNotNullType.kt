package foo

class A

fun checkCastNullableToNotNull(): Boolean {
    val a = null
    try {
        val s = a as A
    }
    catch (e: Exception) {
        return true
    }
    return false
}

fun checkCastNotNullToNotNull(): Boolean {
    val a = A()
    var s = a as A
    return s == a
}

fun box(): String {
    if (!checkCastNullableToNotNull()) return "Failed when try cast Nullable to NotNull"
    if (!checkCastNotNullToNotNull()) return "Failed when try cast NotNull to NotNull"
    return "OK"
}
