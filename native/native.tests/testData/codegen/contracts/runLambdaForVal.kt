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
    val x: Int
    run {
        x = 42
    }
    assertEquals(x, 42)

    return "OK"
}
