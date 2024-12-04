import kotlinx.cinterop.*
import kotlin.test.*
import objcTests.*

@Test
fun testKT53373() {
    val kt53373Class = objc_lookUpClass("KT53373")
    assertNotNull(kt53373Class)
    assertEquals("KT53373", class_getName(kt53373Class)?.toKString())
}
