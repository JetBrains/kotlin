@JsExport
abstract class TestOpenClass {
    @JsName("testName")
    abstract fun testFunction(): String
    abstract fun testFunction(x: String): String
}

abstract class TestOpenClassA : TestOpenClass() {
    override abstract fun testFunction(): String
    override abstract fun testFunction(x: String): String
}

class TestClassA : TestOpenClassA() {
    override fun testFunction(): String = "TestClassA"
    override fun testFunction(x: String): String = "TestClassA: ${x}"
}

fun testTestOpenClass1(x: TestOpenClass) = x.testFunction()
fun testTestOpenClass2(x: TestOpenClass) = x.testFunction("OK")

fun testTestOpenClassA1(x: TestOpenClassA) = x.testFunction()
fun testTestOpenClassA2(x: TestOpenClassA) = x.testFunction("OK")

fun testTestClassA1(x: TestClassA) = x.testFunction()
fun testTestClassA2(x: TestClassA) = x.testFunction("OK")

fun box(): String {
    assertEquals("TestClassA", testTestOpenClass1(TestClassA()))
    assertEquals("TestClassA: OK", testTestOpenClass2(TestClassA()))
    assertEquals("TestClassA", testTestOpenClassA1(TestClassA()))
    assertEquals("TestClassA: OK", testTestOpenClassA2(TestClassA()))
    assertEquals("TestClassA", testTestClassA1(TestClassA()))
    assertEquals("TestClassA: OK", testTestClassA2(TestClassA()))

    return "OK"
}
