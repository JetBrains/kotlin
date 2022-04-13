// DONT_TARGET_EXACT_BACKEND: JS
// KJS_WITH_FULL_RUNTIME

fun demo(): Unit {}

fun testFunctionUsage() {
    val u1 = demo()
    val d1: dynamic = u1
    assertTrue(d1 == u1)
    assertEquals(u1.toString(), "kotlin.Unit")
}

fun testFunctionCall() {
    fun test(u1: Unit): Boolean {
        val d1: dynamic = u1
        assertEquals(u1.toString(), "kotlin.Unit")
        return d1 == u1
    }
    assertTrue(test(demo()))
}

fun testObjectField() {
    val o = object { val u1 = demo() }
    val d1: dynamic = o.u1
    assertTrue(d1 == o.u1)
    assertEquals(o.u1.toString(), "kotlin.Unit")
}

fun testClassField() {
    class Test(val u1: Unit)
    val o = Test(demo())
    val d1: dynamic = o.u1
    assertTrue(d1 == o.u1)
    assertEquals(o.u1.toString(), "kotlin.Unit")
}

fun testContainer() {
    val l = listOf<Unit>(demo())
    val d1: dynamic = l.last()
    assertTrue(d1 == l.last())
    assertEquals(l.last().toString(), "kotlin.Unit")
}

fun box(): String {
    testFunctionUsage()
    testFunctionCall()
    testObjectField()
    testClassField()
    testContainer()
    return "OK"
}
