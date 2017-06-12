// EXPECTED_REACHABLE_NODES: 501
package foo

class UserException() : RuntimeException()

fun bar(e: Exception): String {
    var s: String = ""
    var exceptionObject: Exception? = null

    try {
        throw e
    }
    catch (e: UserException) {
        s = "UserException"
        exceptionObject = e
    }
    catch (e: IllegalArgumentException) {
        s = "IllegalArgumentException"
        exceptionObject = e
    }
    catch (f: IllegalStateException) {
        s = "IllegalStateException"
        exceptionObject = f
    }
    catch (e: Exception) {
        s = "Exception"
        exceptionObject = e
    }

    assertEquals(e, exceptionObject, "e == exceptionObject")
    return s
}

fun box(): String {

    assertEquals("UserException", bar(UserException()))
    assertEquals("IllegalArgumentException", bar(IllegalArgumentException()))
    assertEquals("IllegalStateException", bar(IllegalStateException()))
    assertEquals("Exception", bar(Exception()))

    return "OK"
}
