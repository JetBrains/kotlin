import kotlin.test.*
import objcTests.*
@Test
fun testKT35544() {
    val exception = assertFailsWith<TypeCastException> {
        123 as NSString
    }
    assertEquals("class kotlin.Int cannot be cast to class objcTests.NSString", exception.message)
}
