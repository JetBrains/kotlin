import kotlin.test.*
import test.*

@Test
fun runTest() {
    val obj = KotlinSubclassOfNSObject()
    assertNotEquals("KotlinSubclassOfNSObject", viaDescription(obj))
    assertEquals(42u, viaHash(obj))
}
