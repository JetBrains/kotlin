// EXPECTED_REACHABLE_NODES: 493
package foo

fun bar(e: Exception): String {
    var s: String = ""
    var exceptionObject: Exception? = null

    try {
        throw e
    }
    catch (e1: IllegalArgumentException) {
        s = "IllegalArgumentException"
        exceptionObject = e1
    }
    catch (e2: Exception) {
        s = "Exception"
        exceptionObject = e
    }

    assertEquals(e, exceptionObject, "e == exceptionObject")
    return s
}

fun box(): String {

    assertEquals("IllegalArgumentException", bar(IllegalArgumentException()))
    assertEquals("Exception", bar(Exception()))

    return "OK"
}
