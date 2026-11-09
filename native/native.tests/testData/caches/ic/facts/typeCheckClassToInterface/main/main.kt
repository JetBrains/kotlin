import kotlin.test.*
import test.*

@Test
fun runTest() {
    assertTrue(isX(makeY()))
    assertFalse(isX(Any()))
    assertNotNull(asX(makeY()))
    assertNull(asX(Any()))
}
