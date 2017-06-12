// EXPECTED_REACHABLE_NODES: 906
// KT-5192 JS compiler fails to generate correct code for List implementation
package foo


class stdlib_emptyListClass : List<Any> by ArrayList<Any>() {}

fun box(): String {

    assertTrue(stdlib_emptyListClass() is List<*>, "stdlib_emptyListClass() is List<*> #1")
    assertTrue((stdlib_emptyListClass() as Any) is List<*>, "stdlib_emptyListClass() is List<*> #2")
    assertTrue((stdlib_emptyListClass() as Any) !is ArrayList<*>, "stdlib_emptyListClass() !is ArrayList<*>")

    assertTrue(stdlib_emptyListClass().isEmpty(), "stdlib_emptyListClass().isEmpty()")
    assertTrue(stdlib_emptyListClass().size == 0, "stdlib_emptyListClass().size == 0")

    return "OK"
}

