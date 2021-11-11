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

// MODULE: c(b)
// FILE: division.kt
import kotlin.test.*

@Test
fun division () {
    assertEquals(42, 126 / 3)
}

// MODULE: d(default)
// FILE: division2.kt
import kotlin.test.*

@Test
fun division2 () {
    assertEquals(42, 126 / 3)
}

// MODULE: e(d)
// FILE: division3.kt
import kotlin.test.*

@Test
fun division3 () {
    assertEquals(42, 126 / 3)
}
