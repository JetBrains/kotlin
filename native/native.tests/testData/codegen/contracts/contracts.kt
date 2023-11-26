import kotlin.test.*
import kotlin.contracts.*

open class S
class P(val str: String = "P") : S()

@OptIn(kotlin.contracts.ExperimentalContracts::class)
fun check(actual: Boolean) {
    contract { returns() implies actual }
    assertTrue(actual)
}

fun testContractForCast() {
    val s: S = P()

    check(s is P)
    assertEquals(s.str, "P")
}

fun testRequire() {
    val s: S = P()

    require(s is P)
    assertEquals(s.str, "P")
}

fun testNonNullSmartCast() {
    val i: Int? = 1234
    requireNotNull(i)
    assertEquals(i, 1234)
}

fun testRunLambdaForVal() {
    val x: Int
    run {
        x = 42
    }
    assertEquals(x, 42)
}

fun testIsNullString() {
    assertEquals("STR", nullableString("str"))

    assertEquals("", nullableString(null))
}

private fun nullableString(string: String?): String = if (string.isNullOrBlank()) "" else "STR"

fun box(): String {
    testContractForCast()
    testRequire()
    testIsNullString()
    testNonNullSmartCast()
    testRunLambdaForVal()

    return "OK"
}
