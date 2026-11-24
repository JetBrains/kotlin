main.1.ktimport kotlin.test.*
import test.*

@Test fun runTest() {
    assertFalse(isBaseClass(Class()))
    assertFalse(isBaseInterface(Class()))
}
