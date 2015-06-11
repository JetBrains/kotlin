package foo

// CHECK_NOT_CALLED: test

trait A

class AImpl: A

inline
fun test<reified T>(x: Any?): T = x as T

fun box(): String {
    var a: A = AImpl()
    assertEquals(a, test<A>(a), "a = AImpl()")
    a = object : A {}
    assertEquals(a, test<A>(a), "a = object : A{}")
    failsClassCast("test(null)") { test<A>(null) }
    failsClassCast("test(object{})") { test<A>(object{}) }

    return "OK"
}