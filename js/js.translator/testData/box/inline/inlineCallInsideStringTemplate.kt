// EXPECTED_REACHABLE_NODES: 1282
package foo

inline fun foo(): Any? = "foo()"

// CHECK_BREAKS_COUNT: function=box count=0 TARGET_BACKENDS=JS_IR
// CHECK_LABELS_COUNT: function=box name=$l$block count=0 TARGET_BACKENDS=JS_IR
fun box(): String {
    assertEquals("foo()", "${foo()}")
    assertEquals("aaa foo() bb", "aaa ${foo()} bb")
    return "OK"
}
