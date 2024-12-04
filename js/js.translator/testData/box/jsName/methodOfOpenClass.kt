@JsExport
open class TestOpenClass {
    @JsName("testName")
    open fun testFunction(): String = "TestOpenClass"
    open fun testFunction(x: String): String = "TestOpenClass: ${x}"
}

open class TestOpenClassA : TestOpenClass() {
    override fun testFunction(): String = "TestOpenClassA"
    override fun testFunction(x: String): String = "TestOpenClassA: ${x}"
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
    assertEquals("TestOpenClass", testTestOpenClass1(TestOpenClass()))
    assertEquals("TestOpenClass: OK", testTestOpenClass2(TestOpenClass()))

    assertEquals("TestOpenClassA", testTestOpenClass1(TestOpenClassA()))
    assertEquals("TestOpenClassA: OK", testTestOpenClass2(TestOpenClassA()))
    assertEquals("TestOpenClassA", testTestOpenClassA1(TestOpenClassA()))
    assertEquals("TestOpenClassA: OK", testTestOpenClassA2(TestOpenClassA()))

    assertEquals("TestClassA", testTestOpenClass1(TestClassA()))
    assertEquals("TestClassA: OK", testTestOpenClass2(TestClassA()))
    assertEquals("TestClassA", testTestOpenClassA1(TestClassA()))
    assertEquals("TestClassA: OK", testTestOpenClassA2(TestClassA()))
    assertEquals("TestClassA", testTestClassA1(TestClassA()))
    assertEquals("TestClassA: OK", testTestClassA2(TestClassA()))

    return "OK"
}
