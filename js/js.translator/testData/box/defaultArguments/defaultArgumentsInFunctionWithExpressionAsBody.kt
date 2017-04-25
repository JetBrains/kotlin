// EXPECTED_REACHABLE_NODES: 494
// KT-6037: KT-6037 Javascript default function arguments fill code generated in wrong order on method without "return keyword"
package foo

inline fun <T> id(x: T) = x

fun test(arg: Int = 10) = id(arg)

fun foo(value: String = "K") = "O" + try { value } catch(e: Exception) { "..." }

fun box(): String {

    assertEquals(10, test())
    assertEquals(100, test(100))
    assertEquals("OK", foo())

    return "OK"
}