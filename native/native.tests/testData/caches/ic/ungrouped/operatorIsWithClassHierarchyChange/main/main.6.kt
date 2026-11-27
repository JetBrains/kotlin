import kotlin.test.*
import test.*

@Test fun runTest() {
    assertFalse(isBaseClass(Class()))
    assertNull(asBaseClass(Class()))

    assertTrue(isBaseInterface(Class()))
    assertNotNull(asBaseInterface(Class()))
}
