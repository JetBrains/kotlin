@file:kotlin.jvm.JvmVersion
package test.numbers

import java.math.BigInteger
import java.math.BigDecimal

import kotlin.test.*
import org.junit.Test

class MathTest {
    @Test fun testBigInteger() {
        val a = BigInteger("2")
        val b = BigInteger("3")

        assertEquals(BigInteger("5"), a + b)
        assertEquals(BigInteger("-1"), a - b)
        assertEquals(BigInteger("6"), a * b)
        assertEquals(BigInteger("0"), a / b)
        assertEquals(BigInteger("-2"), -a)
        assertEquals(BigInteger("-2"), -a % b)
        assertEquals(BigInteger("1"), (-a).mod(b))
        assertEquals(BigInteger("-2"), (-a).remainder(b))

        assertEquals(BigInteger("3"), a.inc())
        assertEquals(BigInteger("1"), a.dec())
        assertEquals(BigInteger("-3"), a.inv())
        assertEquals(BigInteger("2"), a and b)
        assertEquals(BigInteger("3"), a or b)
        assertEquals(BigInteger("1"), a xor b)
        assertEquals(BigInteger("4"), a shl 1)
        assertEquals(BigInteger("1"), a shr 1)
        assertEquals(BigInteger("-4"), -a shl 1)
        assertEquals(BigInteger("-1"), -a shr 1)

        assertEquals(BigInteger("2"), "2".toBigInteger())
        assertEquals(BigInteger("-3"), "-3".toBigInteger())
        assertEquals(BigInteger("2"),  2.toBigInteger())
        assertEquals(BigInteger("-3"), -3L.toBigInteger())

        assertEquals(BigDecimal("2"), a.toBigDecimal())
    }

    @Test fun testBigDecimal() {
        val a = BigDecimal("2")
        val b = BigDecimal("3")

        assertEquals(BigDecimal("5"), a + b)
        assertEquals(BigDecimal("-1"), a - b)
        assertEquals(BigDecimal("6"), a * b)
        assertEquals(BigDecimal("2"), BigDecimal("4") / a)
        assertEquals(BigDecimal("-2"), -a)
        assertEquals(BigDecimal("-2"), -a % b)
        assertEquals(BigDecimal("-2"), (-a).mod(b))
        assertEquals(BigDecimal("-2"), (-a).rem(b))

        assertEquals(BigDecimal("3"), a.inc())
        assertEquals(BigDecimal("1"), a.dec())

        assertEquals(BigDecimal("2.5"), "2.5".toBigDecimal())
        assertEquals(BigDecimal("-3"), "-3".toBigDecimal())
        assertEquals(BigDecimal("2"),  2.toBigDecimal())
        assertEquals(BigDecimal("-3"), -3L.toBigDecimal())
        assertEquals(BigDecimal("2.0"), 2f.toBigDecimal())
        assertEquals(BigDecimal("0.5"),  0.5.toBigDecimal())
    }

    @Test fun mutatingBigNumbers() {
        var a = 2.toBigInteger()
        var b = "1.5".toBigDecimal()

        a++
        b++

        assertEquals(BigInteger("3"), a)
        assertEquals(BigDecimal("2.5"), b)

        --a
        --b

        assertEquals(BigInteger("2"), a)
        assertEquals(BigDecimal("1.5"), b)
    }
}

