package test.collections

import kotlin.test.*
import java.util.*
import org.junit.Test

class SetJsTest {
    val data: Set<String> = createTestMutableSet()
    val empty: Set<String> = hashSet<String>()

    Test fun size() {
        assertEquals(2, data.size())
        assertEquals(0, empty.size())
    }

    Test fun isEmpty() {
        assertFalse(data.isEmpty())
        assertTrue(empty.isEmpty())
    }

    Test fun contains() {
        assertTrue(data.contains("foo"))
        assertTrue(data.contains("bar"))
        assertFalse(data.contains("baz"))
        assertFalse(data.contains(1))
        assertFalse(empty.contains("foo"))
        assertFalse(empty.contains("bar"))
        assertFalse(empty.contains("baz"))
        assertFalse(empty.contains(1))
    }

    Test fun iterator() {
        var result = ""
        for (e in data) {
            result += e
        }

        assertTrue(result == "foobar" || result == "barfoo")
    }

    Test fun containsAll() {
        assertTrue(data.containsAll(arrayList("foo", "bar")))
        assertTrue(data.containsAll(arrayList<String>()))
        assertFalse(data.containsAll(arrayList("foo", "bar", "baz")))
        assertFalse(data.containsAll(arrayList("baz")))
    }

    Test fun add() {
        val data = createTestMutableSet()
        assertTrue(data.add("baz"))
        assertEquals(3, data.size())
        assertFalse(data.add("baz"))
        assertEquals(3, data.size())
        assertTrue(data.containsAll(arrayList("foo", "bar", "baz")))
    }

    Test fun remove() {
        val data = createTestMutableSet()
        assertTrue(data.remove("foo"))
        assertEquals(1, data.size())
        assertFalse(data.remove("foo"))
        assertEquals(1, data.size())
        assertTrue(data.contains("bar"))
    }

    Test fun addAll() {
        val data = createTestMutableSet()
        assertTrue(data.addAll(arrayList("foo", "bar", "baz", "boo")))
        assertEquals(4, data.size())
        assertFalse(data.addAll(arrayList("foo", "bar", "baz", "boo")))
        assertEquals(4, data.size())
        assertTrue(data.containsAll(arrayList("foo", "bar", "baz", "boo")))
    }

    Test fun removeAll() {
        val data = createTestMutableSet()
        assertFalse(data.removeAll(arrayList("baz")))
        assertTrue(data.containsAll(arrayList("foo", "bar")))
        assertEquals(2, data.size())
        assertTrue(data.removeAll(arrayList("foo")))
        assertTrue(data.contains("bar"))
        assertEquals(1, data.size())
        assertTrue(data.removeAll(arrayList("foo", "bar")))
        assertEquals(0, data.size())

        val data2 = createTestMutableSet()
        assertFalse(data.removeAll(arrayList("foo", "bar", "baz")))
        assertTrue(data.isEmpty())
    }

    Test fun retainAll() {
        val data1 = createTestMutableSet()
        assertTrue(data1.retainAll(arrayList("baz")))
        assertTrue(data1.isEmpty())

        val data2 = createTestMutableSet()
        assertTrue(data2.retainAll(arrayList("foo")))
        assertTrue(data2.contains("foo"))
        assertEquals(1, data2.size())
    }

    Test fun clear() {
        val data = createTestMutableSet()
        data.clear()
        assertTrue(data.isEmpty())

        data.clear()
        assertTrue(data.isEmpty())
    }

    //Helpers
    fun createTestMutableSet(): MutableSet<String> = hashSet("foo", "bar")
}
