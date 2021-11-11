// KIND: STANDALONE

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

// FILE: subtraction.kt
import kotlin.test.*

@Test
fun subtraction () {
    assertEquals(42, 50 - 8)
}

// FILE: division.kt
import kotlin.test.*

@Test
fun division () {
    assertEquals(42, 126 / 3)
}
