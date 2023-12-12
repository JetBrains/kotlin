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
    val i: Int? = 1234
    requireNotNull(i)
    assertEquals(i, 1234)

    return "OK"
}
