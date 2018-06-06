// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1112
package foo

fun catchSomeExceptions(e: Exception) {

    try {
        throw e
    }
    catch (e: NullPointerException) { }
    catch(e: IllegalArgumentException) { }

    fail("should not reach this point")
}

fun box(): String {

    try {
        catchSomeExceptions(RuntimeException())
    } catch(e: RuntimeException) {
        return "OK"
    }

    return "Not OK"
}