package codegen.arithmetic.division

import kotlin.test.*

@Test
fun divisionByZero() {
    assertFailsWith(ArithmeticException::class, { 5 / 0 })
    assertFailsWith(ArithmeticException::class, { 5 % 0 })
    assertEquals(1, 5 / try { 0 / 0; 1 } catch (e: ArithmeticException) { 5 })
    assertEquals(Double.NaN, 0.0 / 0.0)
}
