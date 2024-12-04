interface TestInterface {
    @JsName("testName")
    open fun testFunction(): String = "TestInterface"
    open fun testFunction(x: String): String = "TestInterface: ${x}"
}

interface TestInterfaceA : TestInterface {
    override fun testFunction(): String = "TestInterfaceA"
    override fun testFunction(x: String): String = "TestInterfaceA: ${x}"
}

class TestClassA : TestInterfaceA {
    override fun testFunction(): String = "TestClassA"
    override fun testFunction(x: String): String = "TestClassA: ${x}"
}

fun testTestInterface1(x: TestInterface) = x.testFunction()
fun testTestInterface2(x: TestInterface) = x.testFunction("OK")

fun testTestInterfaceA1(x: TestInterfaceA) = x.testFunction()
fun testTestInterfaceA2(x: TestInterfaceA) = x.testFunction("OK")

fun testTestClassA1(x: TestClassA) = x.testFunction()
fun testTestClassA2(x: TestClassA) = x.testFunction("OK")

fun box(): String {
    assertEquals("TestInterface", testTestInterface1(object : TestInterface {}))
    assertEquals("TestInterface: OK", testTestInterface2(object : TestInterface {}))

    assertEquals("TestInterfaceA", testTestInterface1(object : TestInterfaceA {}))
    assertEquals("TestInterfaceA: OK", testTestInterface2(object : TestInterfaceA {}))
    assertEquals("TestInterfaceA", testTestInterfaceA1(object : TestInterfaceA {}))
    assertEquals("TestInterfaceA: OK", testTestInterfaceA2(object : TestInterfaceA {}))

    assertEquals("TestClassA", testTestInterface1(TestClassA()))
    assertEquals("TestClassA: OK", testTestInterface2(TestClassA()))
    assertEquals("TestClassA", testTestInterfaceA1(TestClassA()))
    assertEquals("TestClassA: OK", testTestInterfaceA2(TestClassA()))
    assertEquals("TestClassA", testTestClassA1(TestClassA()))
    assertEquals("TestClassA: OK", testTestClassA2(TestClassA()))

    return "OK"
}
