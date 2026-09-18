import kotlin.test.*
import test.*

@Test
fun runTest() {
    val foo = Foo()
    assertEquals(43, bar(foo))
    assertEquals(43, viaChild(Child()))
    assertEquals(43, viaInterfaceChild(InterfaceChild()))
    assertEquals(43, viaInterface(InterfaceChild()))
}
