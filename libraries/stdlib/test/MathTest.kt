package test.collections

import kotlin.math.*
import java.math.BigInteger
import java.math.BigDecimal

import kotlin.test.*
import org.junit.Test as test

class MathTest {
    test fun testBigInteger() {
        val a = BigInteger("2")
        val b = BigInteger("3")

        assertEquals(BigInteger("5"), a + b)
        assertEquals(BigInteger("-1"), a - b)
        assertEquals(BigInteger("6"), a * b)
        assertEquals(BigInteger("0"), a / b)
        assertEquals(BigInteger("-2"), -a)
        assertEquals(BigInteger("1"), -a % b)
        assertEquals(BigInteger("-2"), -a remainder b)
    }

    test fun testBigDecimal() {
        val a = BigDecimal("2")
        val b = BigDecimal("3")

        assertEquals(BigDecimal("5"), a + b)
        assertEquals(BigDecimal("-1"), a - b)
        assertEquals(BigDecimal("6"), a * b)
        assertEquals(BigDecimal("2"), BigDecimal("4") / a)
        assertEquals(BigDecimal("-2"), -a)
        assertEquals(BigDecimal("-2"), -a % b)
    }
}

fun main(args: Array<String>) {
    MathTest().testBigInteger()
    MathTest().testBigDecimal()
}
