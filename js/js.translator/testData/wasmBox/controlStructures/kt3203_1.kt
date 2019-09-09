fun testIf(): String {
    val condition = true
    val result = if (condition) {
        val hello: String? = "hello"
        if (hello == null) {
            false
        }
        else {
            true
        }
    }
    else true
    if (!result) return "result is false"
    return "OK"
}

fun box(): String {
    return testIf()
}
