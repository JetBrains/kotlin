import kotlinx.atomicfu.*
import kotlin.test.*

class LockFreeStackTest {
    fun testClear() {
        val s = LockFreeStack<String>()
        assertTrue(s.isEmpty())
        s.pushLoop("A")
        assertTrue(!s.isEmpty())
        s.clear()
        assertTrue(s.isEmpty())
    }

    fun testPushPopLoop() {
        val s = LockFreeStack<String>()
        assertTrue(s.isEmpty())
        s.pushLoop("A")
        assertTrue(!s.isEmpty())
        assertEquals("A", s.popLoop())
        assertTrue(s.isEmpty())
    }

    fun testPushPopUpdate() {
        val s = LockFreeStack<String>()
        assertTrue(s.isEmpty())
        s.pushUpdate("A")
        assertTrue(!s.isEmpty())
        assertEquals("A", s.popUpdate())
        assertTrue(s.isEmpty())
    }
}

class LockFreeStack<T> {
    private val top = atomic<Node<T>?>(null)

    private class Node<T>(val value: T, val next: Node<T>?)

    fun isEmpty() = top.value == null

    fun clear() { top.value = null }

    fun pushLoop(value: T) {
        top.loop { cur ->
            val upd = Node(value, cur)
            if (top.compareAndSet(cur, upd)) return
        }
    }

    fun popLoop(): T? {
        top.loop { cur ->
            if (cur == null) return null
            if (top.compareAndSet(cur, cur.next)) return cur.value
        }
    }

    fun pushUpdate(value: T) {
        top.update { cur -> Node(value, cur) }
    }

    fun popUpdate(): T? =
        top.getAndUpdate { cur -> cur?.next } ?.value
}

fun box(): String {
    val testClass = LockFreeStackTest()
    testClass.testClear()
    testClass.testPushPopLoop()
    testClass.testPushPopUpdate()
    return "OK"
}