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
    val s: S = P()

    require(s is P)
    assertEquals(s.str, "P")

    return "OK"
}
