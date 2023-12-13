import kotlin.test.*

fun box(): String {
    assertEquals("STR", nullableString("str"))

    assertEquals("", nullableString(null))

    return "OK"
}

private fun nullableString(string: String?): String = if (string.isNullOrBlank()) "" else "STR"