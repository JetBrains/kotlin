// EA-56241
package foo

class A(val v: Int)

val times = { A.(a: Int) -> this.v * a }

fun test(div: A.(Int) -> Int) = A(20) / 4

fun box(): String {
    val compareTo = { A.(a: A) -> this.v - a.v }

    assertEquals(28, A(4) * 7)
    assertEquals(5, test { this.v / it })
    assertEquals(false, A(49) <= A(7))
    assertEquals(true, A(5) > A(1))

    return "OK"
}
