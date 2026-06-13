import kotlin.test.*
import test2.*

@Test
fun runTest() {
    assertEquals(1, paramUse())
    assertEquals(1, resultUse())
    assertEquals(1, defaultValueUse())
    assertEquals(3, trailingUse())
    assertEquals("Text", genericUse())
    assertEquals(1, genericAddParamUse())
}
