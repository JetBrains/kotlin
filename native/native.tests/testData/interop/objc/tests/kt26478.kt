import kotlin.test.*
import objcTests.*

class KT26478

@Test
fun testKT26478compiletime() {
    val exception = assertFailsWith<ClassCastException> {
        NSBundle() as KT26478
    }
    assertEquals("class NSBundle cannot be cast to class KT26478", exception.message)
}

val bundle26478:Any = NSBundle()
@Test
fun testKT26478runtime() {
    val exception = assertFailsWith<ClassCastException> {
        bundle26478 as KT26478
    }
    assertEquals("class NSBundle cannot be cast to class KT26478", exception.message)
}
