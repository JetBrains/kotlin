import kotlin.test.*
import test.*

@Test fun runTest() {
    assertTrue(isBaseClass(Class()))
    assertNotNull(asBaseClass(Class()))

    assertFalse(isBaseInterface(Class()))
    assertNull(asBaseInterface(Class()))
}
