import kotlin.test.*

fun box(): String {
    val str = ""
    assertEquals(0, str.length)
    assertEquals("", str.toString())
    return "OK"
}
