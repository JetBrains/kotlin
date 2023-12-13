import kotlin.test.*

fun box(): String {
    val x: Int
    run {
        x = 42
    }
    assertEquals(x, 42)

    return "OK"
}
