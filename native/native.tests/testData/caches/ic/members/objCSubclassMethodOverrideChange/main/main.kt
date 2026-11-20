import kotlin.test.*
import test.*

@Test
fun runTest() {
    val obj = KotlinSubclassOfNSObject()
    assertEquals("KotlinSubclassOfNSObject", viaDescription(obj))
    assertNotEquals(42u, viaHash(obj))
}
