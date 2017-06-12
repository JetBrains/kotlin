// EXPECTED_REACHABLE_NODES: 511
package foo

fun foo(i: Int): String = "foo" + i
public fun foo(): Int = 4
public fun boo(): Int = 23
fun boo(i: Int): String = "boo" + i

interface T {
    public fun foo(): Int
    public fun boo(): Int
}

public class A : T {
    fun foo(i: Int): String = "A.foo" + i
    override fun foo(): Int = 42
    override fun boo(): Int = 2
    fun boo(i: Int): String = "A.boo" + i
}

//Testing

fun test(testName: String, ff: Any, fb: Any) {
    val f = ff.toString()
    val b = fb.toString().replaceAll("boo", "foo")

    if (f != b) throw Exception("FAILED on ${testName}:\n f = \"$f\"\n b = \"$b\"")
}

fun box(): String {
    val a = A()

    test("a.foo()", { a.foo() }, { a.boo() })
    test("a.foo(Int)", { a.foo(1) }, { a.boo(1) })

    assertEquals("foo3", foo(3))
    assertEquals(4, foo())
    assertEquals(23, boo())
    assertEquals("boo6", boo(6))

    assertEquals("A.foo3", a.foo(3))
    assertEquals(42, a.foo())
    assertEquals(2, a.boo())
    assertEquals("A.boo35", a.boo(35))

    return "OK"
}
