import kotlin.test.*
import test.*

@Test fun runTest() {
    assertTrue(isBaseClass(Class()))
    assertFalse(isBaseInterface(Class()))
}
