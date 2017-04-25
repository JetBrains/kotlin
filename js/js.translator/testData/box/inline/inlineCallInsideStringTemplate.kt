// EXPECTED_REACHABLE_NODES: 491
package foo

inline fun foo(): Any? = "foo()"

fun box(): String {
    assertEquals("foo()", "${foo()}")
    assertEquals("aaa foo() bb", "aaa ${foo()} bb")
    return "OK"
}
