import messaging.*
import kotlinx.cinterop.*
import kotlin.test.*

fun main(args: Array<String>) {
    autoreleasepool {
        primitives()
        aggregates()
    }
}

private fun primitives() {
    assertEquals(3.14f, PrimitiveTestSubject.floatFn())
    assertEquals(3.14, PrimitiveTestSubject.doubleFn())
    assertEquals(42, PrimitiveTestSubject.intFn())
    assertEquals(vectorOf(2f, 4f, 5f, 8f), PrimitiveTestSubject.simdFn())
}

private fun aggregates() {
    AggregateTestSubject.singleFloatFn().useContents {
        assertEquals(3.14f, f)
    }
    AggregateTestSubject.simplePackedFn().useContents {
        assertEquals('0'.toByte(), f1)
        assertEquals(111, f2)
    }
    AggregateTestSubject.evenSmallerPackedFn().useContents {
        assertEquals('x'.toByte(), x)
        assertEquals(169, y)
        assertEquals('z'.toByte(), z)
    }
    AggregateTestSubject.homogeneousBigFn().useContents {
        assertEquals(1.0f, f1)
        assertEquals(2.0f, f2)
        assertEquals(3.0f, f3)
        assertEquals(4.0f, f4)
        assertEquals(5.0f, f5)
        assertEquals(6.0f, f6)
        assertEquals(7.0f, f7)
        assertEquals(8.0f, f8)
    }
    AggregateTestSubject.homogeneousSmallFn().useContents {
        assertEquals(1.0f, f1)
        assertEquals(2.0f, f2)
        assertEquals(3.0f, f3)
        assertEquals(4.0f, f4)
    }
    AggregateTestSubject.simd_quatfFn().useContents {
        assertEquals(vectorOf(1f, 4f, 9f, 25f), vector)
    }
    AggregateTestSubject.geterogeneousSmallFn().useContents {
        assertEquals(1, s1)
        assertEquals(vectorOf(1f, 4f, 9f, 25f), v2)
        assertEquals(3f, f3)
        assertEquals(4, i4)
    }
}

