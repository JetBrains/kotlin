// EXPECTED_REACHABLE_NODES: 1294
package foo

interface A

class AImpl : A {}

fun test(x: Any?): A? = x as A?

fun box(): String {
    var a: A? = AImpl()
    assertEquals(a, test(a), "a = AImpl()")
    a = object : A {}
    assertEquals(a, test(a), "a = object : A{}")
    assertEquals(null, test(null), "test(null)")
    failsClassCast("test(object{})") { test(object{}) }

    return "OK"
}