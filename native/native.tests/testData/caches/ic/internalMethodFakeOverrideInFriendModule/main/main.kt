import kotlin.test.*

class MyDemo : Demo()

@Test
fun testMyDemo() {
    val myDemo = MyDemo()

    assertEquals(myDemo.demoFun(), 5)
    assertEquals(myDemo.demoVal, 6)
    assertEquals(myDemo.demoValGet, 7)
    assertEquals(myDemo.demoVarSetGet, 8)
    myDemo.demoVarSetGet = -9
    assertEquals(myDemo.demoVarSetGet, -9)
    assertEquals(DemoSetterCalls, 1)
    assertEquals(DemoGetterCalls, 2)
}