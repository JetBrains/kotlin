import kotlin.test.*

fun box(): String {
    val i: Int? = 1234
    requireNotNull(i)
    assertEquals(i, 1234)

    return "OK"
}
