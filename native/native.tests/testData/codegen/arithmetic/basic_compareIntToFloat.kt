import kotlin.test.*

fun box(): String {
    assertEquals(1, 0.compareTo(-0.0f))
    assertEquals(0, 0.compareTo(+0.0f))

    return "OK"
}
