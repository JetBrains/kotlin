private fun testClassA(): Int {
    val a = ClassA()
    a.someVar = 0
    return a.someVar!!
}

private fun testClassB(): Int {
    val b = ClassB()
    b.someVar = b.x
    return b.someVar!!
}

fun test(): Int {
    val b = testClassB()
    val a = testClassA()
    return b + a
}
