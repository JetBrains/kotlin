import kotlin.test.*

@Test
fun foo() {
    println("This is a statement that should go to main.kt in the default module")
}

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
