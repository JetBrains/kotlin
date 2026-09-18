import kotlin.test.*
import test.*

@Test
fun runTest() {
    val foo = Foo()
    assertEquals(100, bar(foo))
    assertEquals(100, viaChild(Child()))
    assertEquals(100, viaInterfaceChild(InterfaceChild()))
    assertEquals(100, viaInterface(InterfaceChild()))
}
