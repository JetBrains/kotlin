/**
 * This is a header comment that should be prepended to the addition.kt file.
 */

// MODULE: a
// FILE: addition.kt
import kotlin.test.*

@Test
fun addition() {
    assertEquals(42, 40 + 2)
}

// MODULE: m(a)
// FILE: multiplication.kt
import kotlin.test.*

@Test
fun multiplication () {
    assertEquals(42, 21 * 2)
}

// MODULE: s(m)()
// FILE: subtraction.kt
import kotlin.test.*

@Test
fun subtraction () {
    assertEquals(42, 50 - 8)
}

// MODULE: d(s)(s)
// FILE: division.kt
import kotlin.test.*

@Test
fun division () {
    assertEquals(42, 126 / 3)
}
