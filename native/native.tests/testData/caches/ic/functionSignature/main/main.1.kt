import kotlin.test.*
import test2.*

@Test
fun runTest() {
    assertEquals(2, paramUse())
    assertEquals(2, resultUse())
    assertEquals(2, defaultValueUse())
    assertEquals(5, trailingUse())
    assertEquals(listOf("Text"), genericUse())
    assertEquals(1, genericAddParamUse())
}
