package foo

inline fun foo(action: (x: Int, y: Int) -> Int): Int = action(1, 2)

val bar = foo { x, y -> js("x + y") }

fun box(): String {
    assertEquals(3, bar)

    return "OK"
}
