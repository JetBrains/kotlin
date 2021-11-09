// FILE: addition.kt
package samples.regular_multifile_with_explicit_packages

import kotlin.test.*

@Test
fun addition() {
    assertEquals(42, 40 + 2)
}

// FILE: multiplication.kt
package samples.regular_multifile_with_explicit_packages.a

import kotlin.test.*

@Test
fun multiplication () {
    assertEquals(42, 21 * 2)
}

// FILE: subtraction.kt
package samples.regular_multifile_with_explicit_packages.b

import kotlin.test.*

@Test
fun subtraction () {
    assertEquals(42, 50 - 8)
}

// FILE: division.kt
package samples.regular_multifile_with_explicit_packages.a.b

import kotlin.test.*

@Test
fun division () {
    assertEquals(42, 126 / 3)
}
