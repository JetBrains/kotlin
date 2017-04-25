// EXPECTED_REACHABLE_NODES: 502
package foo

interface A

class AImpl : A {}

inline
fun <reified T> test(x: Any?): T? = x as T?

fun box(): String {
    var a: A? = AImpl()
    assertEquals(a, test<A>(a), "a = AImpl()")
    a = object : A {}
    assertEquals(a, test<A>(a), "a = object : A{}")
    assertEquals(null, test<A>(null), "test(null)")
    failsClassCast("test(object{})") { test<A>(object{}) }

    return "OK"
}
