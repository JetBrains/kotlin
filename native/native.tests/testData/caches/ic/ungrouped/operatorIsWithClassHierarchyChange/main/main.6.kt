import kotlin.test.*
import test.*

@Test fun runTest() {
    assertFalse(isBaseClass(Class()))
    assertTrue(isBaseInterface(Class()))
}
