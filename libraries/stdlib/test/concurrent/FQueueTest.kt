package test.concurrent

import kotlin.concurrent.*
import kotlin.test.assertTrue
import java.util.LinkedList
import java.util.Random
import kotlin.test.*

import org.junit.Test as test

class FQueueTest() {
    test fun testEmpty() {
        val empty = FunctionalQueue<Int>()
        assertTrue(empty.empty)
    }

    test fun testNonEmpty() {
        val empty = FunctionalQueue<Int>()
        assertTrue(!(empty.add(1)).empty)
    }

    test fun testRandomAccess() {
        var functionalQueue = FunctionalQueue<Int>()
        var queue = LinkedList<Int>()
        val rand = Random()
        rand.setSeed(239)
        for (i in 1..10) {
            if (rand.nextInt(100) < 80 && !queue.empty) {
                val temp = functionalQueue.removeFirst()
                assertEquals(temp.first, queue.removeFirst())
                functionalQueue = temp.second
            } else {
                val element = rand.nextInt()
                queue.addLast(element)
                functionalQueue = functionalQueue.add(element)
            }
        }
    }

    test fun testSizeAndEmpty() {
        var queue = FunctionalQueue<Int>()
        var size = 0
        for (i in 1..100) {
            assertEquals(size, queue.size)
            assertEquals(size == 0, queue.empty)
            queue = queue.add(i)
            size++
        }
        val rand = Random()
        rand.setSeed(24)
        for (i in 1..100) {
            assertEquals(size, queue.size)
            assertEquals(size == 0, queue.empty)
            if (rand.nextInt(100) < 80) {
                queue = queue.add(i)
                size++
            } else {
                queue = queue.removeFirst().second
                size--
            }
        }
        while (!queue.empty) {
            queue = queue.removeFirst().second
            size--
            assertEquals(size, queue.size)
            assertEquals(size == 0, queue.empty)
        }
    }

    test fun testManyAddAndOneRemove() {
        var queue = FunctionalQueue<Int>()
        for (i in 1..10000) {
            queue = queue.add(i)
        }
        for (i in 1..10000) {
            queue.removeFirst()
        }
    }
}
