fun box(): String {
    val result = foo() + foo()
    if (result == "aa")
        return "OK"
    return "FAIL: $result"
}

fun foo(): String {
    val array = arrayOf("a", "b")
    val result = array[0]
    array[0] = "42"
    return result
}