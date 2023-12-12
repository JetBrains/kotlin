import kotlin.test.*
import kotlin.contracts.*

open class S
class P(val str: String = "P") : S()

@OptIn(kotlin.contracts.ExperimentalContracts::class)
fun check(actual: Boolean) {
    contract { returns() implies actual }
    assertTrue(actual)
}

fun box(): String {
    assertEquals("STR", nullableString("str"))

    assertEquals("", nullableString(null))

    return "OK"
}

private fun nullableString(string: String?): String = if (string.isNullOrBlank()) "" else "STR"