import utils.*

fun test(x: Int): Int = apply(x) { it * 2 }

fun box(): String {
    assertEquals(6, test(3))

    return "OK"
}