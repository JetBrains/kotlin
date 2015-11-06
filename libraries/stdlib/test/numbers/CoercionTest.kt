package numbers

import org.junit.Test
import org.junit.Test as test
import kotlin.test.*

class CoercionTest {

    fun usage() {
        val n = 1
        // infix usage
        val n1 = n coerceAtLeast  2
        // function usage
        val n2 = n.coerceAtLeast(2)

        // infix with range
        val n3 = n coerceIn 2..5
    }

    @Test
    fun coercionsInt() {
        expect(5) { 5.coerceAtLeast(1) }
        expect(5) { 1.coerceAtLeast(5) }
        expect(1) { 5.coerceAtMost(1) }
        expect(1) { 1.coerceAtMost(5) }

        for (value in 0..10) {
            expect(value) { value.coerceIn(null, null) }
            val min = 2
            val max = 5
            val range = min..max
            expect(value.coerceAtLeast(min)) { value.coerceIn(min, null) }
            expect(value.coerceAtMost(max)) { value.coerceIn(null, max) }
            expect(value.coerceAtLeast(min).coerceAtMost(max)) { value.coerceIn(min, max) }
            expect(value.coerceAtMost(max).coerceAtLeast(min)) { value coerceIn range }
            assertTrue((value coerceIn range) in range)
        }

        assertFails { 1.coerceIn(1, 0) }
        assertFails { 1.coerceIn(1..0) }
    }

    @Test
    fun coercionsLong() {
        expect(5L) { 5L.coerceAtLeast(1L) }
        expect(5L) { 1L.coerceAtLeast(5L) }
        expect(1L) { 5L.coerceAtMost(1L) }
        expect(1L) { 1L.coerceAtMost(5L) }

        for (value in 0L..10L) {
            expect(value) { value.coerceIn(null, null) }
            val min = 2L
            val max = 5L
            val range = min..max
            expect(value.coerceAtLeast(min)) { value.coerceIn(min, null) }
            expect(value.coerceAtMost(max)) { value.coerceIn(null, max) }
            expect(value.coerceAtLeast(min).coerceAtMost(max)) { value.coerceIn(min, max) }
            expect(value.coerceAtMost(max).coerceAtLeast(min)) { value coerceIn range }
            assertTrue((value coerceIn range) in range)
        }

        assertFails { 1L.coerceIn(1L, 0L) }
        assertFails { 1L.coerceIn(1L..0L) }

    }

    @Test
    @Suppress("DEPRECATION_ERROR")
    fun coercionsDouble() {
        expect(5.0) { 5.0.coerceAtLeast(1.0) }
        expect(5.0) { 1.0.coerceAtLeast(5.0) }
        expect(1.0) { 5.0.coerceAtMost(1.0) }
        expect(1.0) { 1.0.coerceAtMost(5.0) }

        for (value in (0..10).map { it.toDouble() }) {
            expect(value) { value.coerceIn(null, null) }
            val min = 2.0
            val max = 5.0
            val range = min..max
            expect(value.coerceAtLeast(min)) { value.coerceIn(min, null) }
            expect(value.coerceAtMost(max)) { value.coerceIn(null, max) }
            expect(value.coerceAtLeast(min).coerceAtMost(max)) { value.coerceIn(min, max) }
            expect(value.coerceAtMost(max).coerceAtLeast(min)) { value coerceIn range }
            assertTrue((value coerceIn range) in range)
        }

        assertFails { 1.0.coerceIn(1.0, 0.0) }
        assertFails { 1.0.coerceIn(1.0..0.0) }
    }

    @Test
    @Suppress("DEPRECATION_ERROR")
    fun coercionsComparable() {
        val v = 0..10 map { ComparableNumber(it) }

        expect(5) { v[5].coerceAtLeast(v[1]).value }
        expect(5) { v[1].coerceAtLeast(v[5]).value }
        expect(v[5]) { v[5].coerceAtLeast(ComparableNumber(5)) }

        expect(1) { v[5].coerceAtMost(v[1]).value }
        expect(1) { v[1].coerceAtMost(v[5]).value }
        expect(v[1]) { v[1].coerceAtMost(ComparableNumber(1)) }

        for (value in v) {
            expect(value) { value.coerceIn(null, null) }
            val min = v[2]
            val max = v[5]
            val range = min..max
            expect(value.coerceAtLeast(min)) { value.coerceIn(min, null) }
            expect(value.coerceAtMost(max)) { value.coerceIn(null, max) }
            expect(value.coerceAtLeast(min).coerceAtMost(max)) { value.coerceIn(min, max) }
            expect(value.coerceAtMost(max).coerceAtLeast(min)) { value coerceIn range }
            assertTrue((value coerceIn range) in range)
        }

        assertFails { v[1].coerceIn(v[1], v[0]) }
        assertFails { v[1].coerceIn(v[1]..v[0]) }
    }
}

private class ComparableNumber(public val value: Int) : Comparable<ComparableNumber> {
    override fun compareTo(other: ComparableNumber): Int = this.value - other.value
    override fun toString(): String = "CV$value"
}