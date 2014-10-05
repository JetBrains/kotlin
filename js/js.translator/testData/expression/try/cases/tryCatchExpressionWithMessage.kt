package foo

fun box(): String {

    var s: String = ""

    try {
        throw Exception("Exception")
    } catch (e: Throwable) {
        s = "Throwable:" + e.getMessage()!!
    }
    assertEquals("Throwable:Exception", s)

    s = ""
    try {
        throw Exception("Exception")
    } catch (e: Exception) {
        s = "Exception:" + e.getMessage()!!
    }
    assertEquals("Exception:Exception", s)

    s = ""
    try {
        throw RuntimeException("RuntimeException")
    } catch (e: Exception) {
        s = "Exception:" + e.getMessage()!!
    }
    assertEquals("Exception:RuntimeException", s)

    s = ""
    try {
        throw NullPointerException("NullPointerException")
    } catch (e: Exception) {
        s = "Exception:" + e.getMessage()!!
    }
    assertEquals("Exception:NullPointerException", s)

    s = ""
    try {
        throw IndexOutOfBoundsException("IndexOutOfBoundsException")
    } catch (e: NullPointerException) {
        s = "NullPointerException:" + e.getMessage()!!
    } catch (e: RuntimeException) {
        s = "RuntimeException:" + e.getMessage()!!
    } catch (e: Exception) {
        s = "Exception:" + e.getMessage()!!
    }
    assertEquals("RuntimeException:IndexOutOfBoundsException", s)

    try {
        throw RuntimeException()
    } catch (e: Exception) {
        assertEquals(null, e.getMessage())
    }

   return "OK"
}