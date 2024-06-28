// FUNCTION: org.junit.jupiter.api.Assertions.assertTrue
// WITH_JUNIT5

import java.util.function.Supplier
import org.junit.jupiter.api.Assertions.assertTrue

fun box() = expectThrowableMessage {
    val supplier = Supplier { "Message:" }
    assertTrue(1 != 1, supplier)
}
