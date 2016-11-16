package test.properties.delegation

import org.junit.Test
import kotlin.test.*
import kotlin.properties.*

class NotNullVarTest() {
    @Test fun doTest() {
        NotNullVarTestGeneric("a", "b").doTest()
    }
}

private class NotNullVarTestGeneric<T : Any>(val a1: String, val b1: T) {
    var a: String by Delegates.notNull()
    var b by Delegates.notNull<T>()

    public fun doTest() {
        a = a1
        b = b1
        assertTrue(a == "a", "fail: a should be a, but was $a")
        assertTrue(b == "b", "fail: b should be b, but was $b")
    }
}

class ObservablePropertyTest {
    var result = false

    var b: Int by Delegates.observable(1, { property, old, new ->
        assertEquals("b", property.name)
        result = true
        assertEquals(new, b, "New value has already been set")
    })

    @Test fun doTest() {
        b = 4
        assertTrue(b == 4, "fail: b != 4")
        assertTrue(result, "fail: result should be true")
    }
}

class A(val p: Boolean)

class VetoablePropertyTest {
    var result = false
    var b: A by Delegates.vetoable(A(true), { property, old, new ->
        assertEquals("b", property.name)
        assertEquals(old, b, "New value hasn't been set yet")
        result = new.p == true;
        result
    })

    @Test fun doTest() {
        val firstValue = A(true)
        b = firstValue
        assertTrue(b == firstValue, "fail1: b should be firstValue = A(true)")
        assertTrue(result, "fail2: result should be true")
        b = A(false)
        assertTrue(b == firstValue, "fail3: b should be firstValue = A(true)")
        assertFalse(result, "fail4: result should be false")
    }
}
