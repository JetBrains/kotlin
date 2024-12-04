private fun testClassA(): Int {
    val a = ClassA()
    a.someVar = 0
    return a.someVar!!
}

fun test(): Int {
    return testClassA()
}
