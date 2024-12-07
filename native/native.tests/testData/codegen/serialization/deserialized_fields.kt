import kotlin.test.*

class CustomThrowable(val extraInfo: String): Throwable("Some message", null)

fun box(): String {
    val custom = CustomThrowable("Extra info")
    assertEquals(custom.extraInfo, "Extra info")
    assertEquals(custom.message, "Some message")
    assertEquals(custom.cause, null)
    return "OK"
}