// EA-56241
package foo

fun Int.foo(a: Int) = this + a

val bar = { Int.(a: Int) -> this * a }

fun test(op: Int.(Int) -> Int) = 3 op 20

fun box(): String {
    val op = { Int.(a: Int) -> this / a }

    assertEquals(41, 34 foo 7)
    assertEquals(28, 4 bar 7)
    assertEquals(-17, test { this - it })
    assertEquals(7, 49 op 7)

    return "OK"
}
