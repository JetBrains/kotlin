import kotlin.test.*
import test.*

@Test fun runTest() {
    assertFalse(isBaseClass(Class()))
    assertNull(asBaseClass(Class()))

    assertFalse(isBaseInterface(Class()))
    assertNull(asBaseInterface(Class()))
}
