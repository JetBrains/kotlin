/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.collections

import kotlin.random.Random
import kotlin.test.*

// TODO: Check which parts are already tested in libraries/stdlib/test/collections/IterableTests.kt
class ArrayListNativeTest {
    @Test fun basic() {
        val a = ArrayList<String>()
        assertTrue(a.isEmpty())
        assertEquals(0, a.size)

        assertTrue(a.add("1"))
        assertTrue(a.add("2"))
        assertTrue(a.add("3"))
        assertFalse(a.isEmpty())
        assertEquals(3, a.size)
        assertEquals("1", a[0])
        assertEquals("2", a[1])
        assertEquals("3", a[2])

        a[0] = "11"
        assertEquals("11", a[0])

        assertEquals("11", a.removeAt(0))
        assertEquals(2, a.size)
        assertEquals("2", a[0])
        assertEquals("3", a[1])

        a.add(1, "22")
        assertEquals(3, a.size)
        assertEquals("2", a[0])
        assertEquals("22", a[1])
        assertEquals("3", a[2])

        a.clear()
        assertTrue(a.isEmpty())
        assertEquals(0, a.size)
    }

    @Test fun iterator() {
        val a = ArrayList(listOf("1", "2", "3"))
        val it = a.iterator()
        assertTrue(it.hasNext())
        assertEquals("1", it.next())
        assertTrue(it.hasNext())
        assertEquals("2", it.next())
        assertTrue(it.hasNext())
        assertEquals("3", it.next())
        assertFalse(it.hasNext())
    }

    @Test fun containsAll() {
        val a = ArrayList(listOf("1", "2", "3", "4", "5"))
        assertFalse(a.containsAll(listOf("6", "7", "8")))
        assertFalse(a.containsAll(listOf("5", "6", "7")))
        assertFalse(a.containsAll(listOf("4", "5", "6")))
        assertTrue(a.containsAll(listOf("3", "4", "5")))
        assertTrue(a.containsAll(listOf("2", "3", "4")))
    }

    @Test fun remove() {
        val a = ArrayList(listOf("1", "2", "3"))
        assertTrue(a.remove("2"))
        assertEquals(2, a.size)
        assertEquals("1", a[0])
        assertEquals("3", a[1])
        assertFalse(a.remove("2"))
        assertEquals(2, a.size)
        assertEquals("1", a[0])
        assertEquals("3", a[1])
    }

    @Test fun removeAll() {
        val a = ArrayList(listOf("1", "2", "3", "4", "5", "1"))
        assertFalse(a.removeAll(listOf("6", "7", "8")))
        assertEquals(listOf("1", "2", "3", "4", "5", "1"), a)
        assertTrue(a.removeAll(listOf("5", "3", "1")))
        assertEquals(listOf("2", "4"), a)
    }

    @Test fun retainAll() {
        val a = ArrayList(listOf("1", "2", "3", "4", "5"))
        assertFalse(a.retainAll(listOf("1", "2", "3", "4", "5")))
        assertEquals(listOf("1", "2", "3", "4", "5"), a)
        assertTrue(a.retainAll(listOf("5", "3", "1")))
        assertEquals(listOf("1", "3", "5"), a)
    }

    @Test fun equals() {
        val a = ArrayList(listOf("1", "2", "3"))
        assertTrue(a == listOf("1", "2", "3"))
        assertFalse(a == listOf("2", "3", "1")) // order matters
        assertFalse(a == listOf("1", "2", "4"))
        assertFalse(a == listOf("1", "2"))
    }

    @Test fun hashCodeIdentity() {
        val a = ArrayList(listOf("1", "2", "3"))
        assertEquals(listOf("1", "2", "3").hashCode(), a.hashCode())
    }

    @Test fun toStringIdentity() {
        val a = ArrayList(listOf("1", "2", "3"))
        assertEquals(listOf("1", "2", "3").toString(), a.toString())
    }

    @Test fun subList() {
        val a0 = ArrayList(listOf("0", "1", "2", "3", "4"))
        val a = a0.subList(1, 4)
        assertEquals(3, a.size)
        assertEquals("1", a[0])
        assertEquals("2", a[1])
        assertEquals("3", a[2])
        assertTrue(a == listOf("1", "2", "3"))
        assertTrue(a.hashCode() == listOf("1", "2", "3").hashCode())
        assertTrue(a.toString() == listOf("1", "2", "3").toString())
    }

    @Test fun resize() {
        val a = ArrayList<String>()
        val n = 10000
        for (i in 1..n)
            assertTrue(a.add(i.toString()))
        assertEquals(n, a.size)
        for (i in 1..n)
            assertEquals(i.toString(), a[i - 1])
        a.trimToSize()
        assertEquals(n, a.size)
        for (i in 1..n)
            assertEquals(i.toString(), a[i - 1])
    }

    @Test fun subListContains() {
        val a = ArrayList(listOf("1", "2", "3", "4"))
        val s = a.subList(1, 3)
        assertTrue(a.contains("1"))
        assertFalse(s.contains("1"))
        assertTrue(a.contains("2"))
        assertTrue(s.contains("2"))
        assertTrue(a.contains("3"))
        assertTrue(s.contains("3"))
        assertTrue(a.contains("4"))
        assertFalse(s.contains("4"))
    }

    @Test fun subListIndexOf() {
        val a = ArrayList(listOf("1", "2", "3", "4", "1"))
        val s = a.subList(1, 3)
        assertEquals(0, a.indexOf("1"))
        assertEquals(-1, s.indexOf("1"))
        assertEquals(1, a.indexOf("2"))
        assertEquals(0, s.indexOf("2"))
        assertEquals(2, a.indexOf("3"))
        assertEquals(1, s.indexOf("3"))
        assertEquals(3, a.indexOf("4"))
        assertEquals(-1, s.indexOf("4"))
    }

    @Test fun subListLastIndexOf() {
        val a = ArrayList(listOf("1", "2", "3", "4", "1"))
        val s = a.subList(1, 3)
        assertEquals(4, a.lastIndexOf("1"))
        assertEquals(-1, s.lastIndexOf("1"))
        assertEquals(1, a.lastIndexOf("2"))
        assertEquals(0, s.lastIndexOf("2"))
        assertEquals(2, a.lastIndexOf("3"))
        assertEquals(1, s.lastIndexOf("3"))
        assertEquals(3, a.lastIndexOf("4"))
        assertEquals(-1, s.lastIndexOf("4"))
    }

    @Test fun subListClear() {
        val a = ArrayList(listOf("1", "2", "3", "4"))
        val s = a.subList(1, 3)
        assertEquals(listOf("2", "3"), s)

        s.clear()
        assertEquals(listOf<String>(), s)
        assertEquals(listOf("1", "4"), a)
    }

    @Test fun subListSubListClear() {
        val a = ArrayList(listOf("1", "2", "3", "4", "5", "6"))
        val s = a.subList(1, 5)
        val q = s.subList(1, 3)
        assertEquals(listOf("2", "3", "4", "5"), s)
        assertEquals(listOf("3", "4"), q)

        q.clear()
        assertEquals(listOf<String>(), q)
        assertEquals(listOf("2", "5"), s)
        assertEquals(listOf("1", "2", "5", "6"), a)
    }

    @Test fun subListAdd() {
        val a = ArrayList(listOf("1", "2", "3", "4"))
        val s = a.subList(1, 3)
        assertEquals(listOf("2", "3"), s)

        assertTrue(s.add("5"))
        assertEquals(listOf("2", "3", "5"), s)
        assertEquals(listOf("1", "2", "3", "5", "4"), a)
    }

    @Test fun subListSubListAdd() {
        val a = ArrayList(listOf("1", "2", "3", "4", "5", "6"))
        val s = a.subList(1, 5)
        val q = s.subList(1, 3)
        assertEquals(listOf("2", "3", "4", "5"), s)
        assertEquals(listOf("3", "4"), q)

        assertTrue(q.add("7"))
        assertEquals(listOf("3", "4", "7"), q)
        assertEquals(listOf("2", "3", "4", "7", "5"), s)
        assertEquals(listOf("1", "2", "3", "4", "7", "5", "6"), a)
    }

    @Test fun subListAddAll() {
        val a = ArrayList(listOf("1", "2", "3", "4"))
        val s = a.subList(1, 3)
        assertEquals(listOf("2", "3"), s)

        assertTrue(s.addAll(listOf("5", "6")))
        assertEquals(listOf("2", "3", "5", "6"), s)
        assertEquals(listOf("1", "2", "3", "5", "6", "4"), a)
    }

    @Test fun subListSubListAddAll() {
        val a = ArrayList(listOf("1", "2", "3", "4", "5", "6"))
        val s = a.subList(1, 5)
        val q = s.subList(1, 3)
        assertEquals(listOf("2", "3", "4", "5"), s)
        assertEquals(listOf("3", "4"), q)

        assertTrue(q.addAll(listOf("7", "8")))
        assertEquals(listOf("3", "4", "7", "8"), q)
        assertEquals(listOf("2", "3", "4", "7", "8", "5"), s)
        assertEquals(listOf("1", "2", "3", "4", "7", "8", "5", "6"), a)
    }

    @Test fun subListRemoveAt() {
        val a = ArrayList(listOf("1", "2", "3", "4", "5"))
        val s = a.subList(1, 4)
        assertEquals(listOf("2", "3", "4"), s)

        assertEquals("3", s.removeAt(1))
        assertEquals(listOf("2", "4"), s)
        assertEquals(listOf("1", "2", "4", "5"), a)
    }

    @Test fun subListSubListRemoveAt() {
        val a = ArrayList(listOf("1", "2", "3", "4", "5", "6", "7"))
        val s = a.subList(1, 6)
        val q = s.subList(1, 4)
        assertEquals(listOf("2", "3", "4", "5", "6"), s)
        assertEquals(listOf("3", "4", "5"), q)

        assertEquals("4", q.removeAt(1))
        assertEquals(listOf("3", "5"), q)
        assertEquals(listOf("2", "3", "5", "6"), s)
        assertEquals(listOf("1", "2", "3", "5", "6", "7"), a)
    }

    @Test fun subListRemoveAll() {
        val a = ArrayList(listOf("1", "2", "3", "3", "4", "5"))
        val s = a.subList(1, 5)
        assertEquals(listOf("2", "3", "3", "4"), s)

        assertTrue(s.removeAll(listOf("3", "5")))
        assertEquals(listOf("2", "4"), s)
        assertEquals(listOf("1", "2", "4", "5"), a)
    }

    @Test fun subListSubListRemoveAll() {
        val a = ArrayList(listOf("1", "2", "3", "4", "5", "6", "7"))
        val s = a.subList(1, 6)
        val q = s.subList(1, 4)
        assertEquals(listOf("2", "3", "4", "5", "6"), s)
        assertEquals(listOf("3", "4", "5"), q)

        assertTrue(q.removeAll(listOf("4", "6")))
        assertEquals(listOf("3", "5"), q)
        assertEquals(listOf("2", "3", "5", "6"), s)
        assertEquals(listOf("1", "2", "3", "5", "6", "7"), a)
    }

    @Test fun subListRetainAll() {
        val a = ArrayList(listOf("1", "2", "3", "4", "5"))
        val s = a.subList(1, 4)
        assertEquals(listOf("2", "3", "4"), s)

        assertTrue(s.retainAll(listOf("5", "3")))
        assertEquals(listOf("3"), s)
        assertEquals(listOf("1", "3", "5"), a)
    }

    @Test fun subListSubListRetainAll() {
        val a = ArrayList(listOf("1", "2", "3", "4", "5", "6", "7"))
        val s = a.subList(1, 6)
        val q = s.subList(1, 4)
        assertEquals(listOf("2", "3", "4", "5", "6"), s)
        assertEquals(listOf("3", "4", "5"), q)

        assertTrue(q.retainAll(listOf("5", "3")))
        assertEquals(listOf("3", "5"), q)
        assertEquals(listOf("2", "3", "5", "6"), s)
        assertEquals(listOf("1", "2", "3", "5", "6", "7"), a)
    }

    @Test fun iteratorRemove() {
        val a = ArrayList(listOf("1", "2", "3", "4", "5"))
        val it = a.iterator()
        while (it.hasNext())
            if (it.next()[0].toInt() % 2 == 0)
                it.remove()
        assertEquals(listOf("1", "3", "5"), a)
    }

    @Test fun iteratorAdd() {
        val a = ArrayList(listOf("1", "2", "3", "4", "5"))
        val it = a.listIterator()
        while (it.hasNext()) {
            val next = it.next()
            if (next[0].toInt() % 2 == 0)
                it.add("-" + next)
        }
        assertEquals(listOf("1", "2", "-2", "3", "4", "-4", "5"), a)
    }

    @Test fun iteratorNext() {
        val a = arrayListOf("1", "2", "3", "4", "5")
        val it = a.listIterator()
        assertFailsWith<NoSuchElementException> {
            while (true) {
                it.next()
            }
        }
    }

    @Test fun iteratorPrevious() {
        val a = arrayListOf("1", "2", "3", "4", "5")
        val it = a.listIterator()
        it.next()
        assertFailsWith<NoSuchElementException> {
            while (true) {
                it.previous()
            }
        }
    }

    @Test fun factory() {
        val nonConstStr = Random.nextInt().toString()
        val list = arrayListOf(nonConstStr, "b", "c")
        assertEquals(3, list.size)
        assertEquals(nonConstStr, list[0])
        assertEquals("b", list[1])
        assertEquals("c", list[2])

        list.add("d")
        assertEquals(4, list.size)
        assertEquals(nonConstStr, list[0])
        assertEquals("b", list[1])
        assertEquals("c", list[2])
        assertEquals("d", list[3])
    }
}