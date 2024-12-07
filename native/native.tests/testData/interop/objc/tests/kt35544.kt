import kotlin.test.*
import objcTests.*
@Test
fun testKT35544() {
    val exception = assertFailsWith<ClassCastException> {
        123 as NSString
    }
    assertEquals("class kotlin.Int cannot be cast to class objcTests.NSString", exception.message)
}

val bundle35544: Any = NSBundle()
@Test
fun testKT35544runtime() {
    val exception = assertFailsWith<ClassCastException> {
        bundle35544 as NSString
    }
    assertEquals("class NSBundle cannot be cast to class objcTests.NSString", exception.message)
}
