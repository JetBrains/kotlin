// EXPECTED_REACHABLE_NODES: 1294
package foo

// CHECK_NOT_CALLED: test

interface A

class AImpl: A

inline
fun <reified T> test(x: Any?): T = x as T

fun box(): String {
    var a: A = AImpl()
    assertEquals(a, test<A>(a), "a = AImpl()")
    a = object : A {}
    assertEquals(a, test<A>(a), "a = object : A{}")
    failsClassCast("test(null)") { test<A>(null) }
    failsClassCast("test(object{})") { test<A>(object{}) }

    return "OK"
}
