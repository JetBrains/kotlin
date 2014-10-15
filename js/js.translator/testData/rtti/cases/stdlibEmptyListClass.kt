// KT-5192 JS compiler fails to generate correct code for List implementation
package foo

import java.util.ArrayList

class stdlib_emptyListClass : List<Any> by ArrayList<Any>() {}

fun box(): String {

    assertTrue(stdlib_emptyListClass() is List<*>, "stdlib_emptyListClass() is List<*> #1")
    assertTrue((stdlib_emptyListClass(): Any) is List<*>, "stdlib_emptyListClass() is List<*> #2")
    assertTrue((stdlib_emptyListClass(): Any) !is ArrayList<*>, "stdlib_emptyListClass() !is ArrayList<*>")

    assertTrue(stdlib_emptyListClass().isEmpty(), "stdlib_emptyListClass().isEmpty()")
    assertTrue(stdlib_emptyListClass().size() == 0, "stdlib_emptyListClass().size() == 0")

    return "OK"
}

