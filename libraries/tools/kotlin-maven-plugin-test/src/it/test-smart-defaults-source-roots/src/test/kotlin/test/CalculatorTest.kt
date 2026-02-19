package test

import org.junit.Test
import org.junit.Assert.assertEquals

class CalculatorTest {
    @Test
    fun testAdd() {
        val calculator = Calculator()
        assertEquals(5, calculator.add(2, 3))
    }

    @Test
    fun testSubtract() {
        val calculator = Calculator()
        assertEquals(1, calculator.subtract(3, 2))
    }
}
