/**
 * This is a header comment that should be prepended to the addition.kt file.
 */

// FILE: addition.kt
import kotlin.test.*

@Test
fun addition() {
    assertEquals(42, 40 + 2)
}

// FILE: multiplication.kt
import kotlin.test.*

@Test
fun multiplication () {
    assertEquals(42, 21 * 2)
}

// MODULE: b(default)
// FILE: subtraction.kt
import kotlin.test.*

@Test
fun subtraction () {
    assertEquals(42, 50 - 8)
}

// MODULE: c(default)(default)
// FILE: division.kt
import kotlin.test.*

@Test
fun division () {
    assertEquals(42, 126 / 3)
}
