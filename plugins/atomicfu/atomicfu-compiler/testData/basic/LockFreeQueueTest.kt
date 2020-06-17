import kotlinx.atomicfu.*
import kotlin.test.*

class LockFreeQueueTest {
    fun testBasic() {
        val q = LockFreeQueue()
        check(q.dequeue() == -1)
        q.enqueue(42)
        check(q.dequeue() == 42)
        check(q.dequeue() == -1)
        q.enqueue(1)
        q.enqueue(2)
        check(q.dequeue() == 1)
        check(q.dequeue() == 2)
        check(q.dequeue() == -1)
    }
}

// MS-queue
public class LockFreeQueue {
    private val head = atomic(Node(0))
    private val tail = atomic(head.value)

    private class Node(val value: Int) {
        val next = atomic<Node?>(null)
    }

    public fun enqueue(value: Int) {
        val node = Node(value)
        tail.loop { curTail ->
            val curNext = curTail.next.value
            if (curNext != null) {
                tail.compareAndSet(curTail, curNext)
                return@loop
            }
            if (curTail.next.compareAndSet(null, node)) {
                tail.compareAndSet(curTail, node)
                return
            }
        }
    }

    public fun dequeue(): Int {
        head.loop { curHead ->
            val next = curHead.next.value ?: return -1
            if (head.compareAndSet(curHead, next)) return next.value
        }
    }
}

fun box(): String {
    val testClass = LockFreeQueueTest()
    testClass.testBasic()
    return "OK"
}