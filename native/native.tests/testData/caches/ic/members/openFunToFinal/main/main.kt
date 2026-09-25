import kotlin.test.*
import test.*

@Test
fun runTest() {
    val foo = Foo()
    assertEquals(42, bar(foo))
    assertEquals(42, viaChild(Child()))
    assertEquals(42, viaInterfaceChild(InterfaceChild()))
    assertEquals(42, viaInterface(InterfaceChild()))
}
