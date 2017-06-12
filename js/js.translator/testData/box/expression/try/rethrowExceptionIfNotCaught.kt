// EXPECTED_REACHABLE_NODES: 495
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