// FILE: main.kt
package samples.regular_multifile_with_explicit_packages

import kotlin.test.*

@Test
fun addition() {
    assertEquals(42, 40 + 2)
}

@Test
fun multiplication () {
    assertEquals(42, 21 * 2)
}

@Test
fun subtraction () {
    assertEquals(42, 50 - 8)
}

@Test
fun division () {
    assertEquals(42, 126 / 3)
}

// FILE: a.kt
package samples.regular_multifile_with_explicit_packages.a

import kotlin.test.*

@Test
fun addition() {
    assertEquals(42, 40 + 2)
}

@Test
fun multiplication () {
    assertEquals(42, 21 * 2)
}

@Test
fun subtraction () {
    assertEquals(42, 50 - 8)
}

@Test
fun division () {
    assertEquals(42, 126 / 3)
}

// FILE: b.kt
package samples.regular_multifile_with_explicit_packages.b

import kotlin.test.*

@Test
fun addition() {
    assertEquals(42, 40 + 2)
}

@Test
fun multiplication () {
    assertEquals(42, 21 * 2)
}

@Test
fun subtraction () {
    assertEquals(42, 50 - 8)
}

@Test
fun division () {
    assertEquals(42, 126 / 3)
}

// FILE: a_b.kt
package samples.regular_multifile_with_explicit_packages.a.b

import kotlin.test.*

@Test
fun addition() {
    assertEquals(42, 40 + 2)
}

@Test
fun multiplication () {
    assertEquals(42, 21 * 2)
}

@Test
fun subtraction () {
    assertEquals(42, 50 - 8)
}

@Test
fun division () {
    assertEquals(42, 126 / 3)
}
