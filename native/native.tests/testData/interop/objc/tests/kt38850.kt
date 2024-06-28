import kotlinx.cinterop.*
import kotlin.test.*
import objcTests.*

enum class ColorRef(val color: KT38850Color){
    Black(KT38850Color.blackColor()),
}

@Test
fun testKT38850() {
    assertEquals("black", ColorRef.Black.color.name)
}
