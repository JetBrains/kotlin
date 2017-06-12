// EXPECTED_REACHABLE_NODES: 502
package foo

fun box(): String {

    var s: String = ""

    try {
        throw Exception("Exception")
    } catch (e: Throwable) {
        s = "Throwable:" + e.message!!
    }
    assertEquals("Throwable:Exception", s)

    s = ""
    try {
        throw Exception("Exception")
    } catch (e: Exception) {
        s = "Exception:" + e.message!!
    }
    assertEquals("Exception:Exception", s)

    s = ""
    try {
        throw RuntimeException("RuntimeException")
    } catch (e: Exception) {
        s = "Exception:" + e.message!!
    }
    assertEquals("Exception:RuntimeException", s)

    s = ""
    try {
        throw NullPointerException("NullPointerException")
    } catch (e: Exception) {
        s = "Exception:" + e.message!!
    }
    assertEquals("Exception:NullPointerException", s)

    s = ""
    try {
        throw IndexOutOfBoundsException("IndexOutOfBoundsException")
    } catch (e: NullPointerException) {
        s = "NullPointerException:" + e.message!!
    } catch (e: RuntimeException) {
        s = "RuntimeException:" + e.message!!
    } catch (e: Exception) {
        s = "Exception:" + e.message!!
    }
    assertEquals("RuntimeException:IndexOutOfBoundsException", s)

    try {
        throw RuntimeException()
    } catch (e: Exception) {
        assertEquals(null, e.message)
    }

   return "OK"
}