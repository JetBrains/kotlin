import kotlin.native.concurrent.*
import kotlin.native.internal.GC
import kotlin.native.Platform
import kotlin.test.*

fun test1() {
    val a = AtomicReference<Any?>(null)
    val b = AtomicReference<Any?>(null)
    a.value = b
    b.value = a
}

class Holder(var other: Any?)

fun test2() {
    val array = arrayOf(AtomicReference<Any?>(null), AtomicReference<Any?>(null))
    val obj1 = Holder(array).freeze()
    array[0].value = obj1
}

fun test3() {
    val a1 = FreezableAtomicReference<Any?>(null)
    val head = Holder(null)
    var current = head
    repeat(30) {
        val next = Holder(null)
        current.other = next
        current = next
    }
    a1.value = head
    current.other = a1
    current.freeze()
}


fun makeIt(): Holder {
    val atomic = AtomicReference<Holder?>(null)
    val holder = Holder(atomic).freeze()
    atomic.value = holder
    return holder
}


fun test4() {
    val holder = makeIt()
    // To clean rc count coming from rememberNewContainer().
    kotlin.native.internal.GC.collect()
    // Request cyclic collection.
    kotlin.native.internal.GC.collectCyclic()
    // Ensure we processed delayed release.
    repeat(10) {
        // Wait a bit and process queue.
        Worker.current.park(10)
        Worker.current.processQueue()
        kotlin.native.internal.GC.collect()
    }
    val value = @Suppress("UNCHECKED_CAST") (holder.other as? AtomicReference<Holder?>?)
    assertTrue(value != null)
    assertTrue(value.value == holder)
}

fun createRef(): AtomicReference<Any?> {
    val atomic1 = AtomicReference<Any?>(null)
    val atomic2 = AtomicReference<Any?>(null)
    atomic1.value = atomic2
    atomic2.value = atomic1
    return atomic1
}

class Holder2(var value: AtomicReference<Any?>) {
    fun switch() {
        value = value.value as AtomicReference<Any?>
    }
}

fun createHolder2() = Holder2(createRef())

fun test5() {
    val holder = createHolder2()
    kotlin.native.internal.GC.collect()
    kotlin.native.internal.GC.collectCyclic()
    Worker.current.park(100 * 1000)
    holder.switch()
    kotlin.native.internal.GC.collect()
    Worker.current.park(100 * 1000)
    withWorker {
        executeAfter(0L, {
            kotlin.native.internal.GC.collect()
        }.freeze())
    }
    Worker.current.park(1000)
    assertTrue(holder.value.value != null)
}

fun test6() {
    val atomic = AtomicReference<Any?>(null)
    atomic.value = Pair(atomic, Holder(atomic)).freeze()
}

fun createRoot(): AtomicReference<Any?> {
    val ref1 = AtomicReference<Any?>(null)
    val ref2 = AtomicReference<Any?>(null)

    ref1.value = Holder(ref2).freeze()
    ref2.value = Any().freeze()

    return ref1
}

fun test7() {
    val ref1 = createRoot()
    kotlin.native.internal.GC.collect()

    kotlin.native.internal.GC.collectCyclic()
    Worker.current.park(500 * 1000L)

    withWorker {
        executeAfter(0L, {}.freeze())
        Worker.current.park(500 * 1000L)

        val node = ref1.value as Holder
        val ref2 = node.other as AtomicReference<Any?>
        assertTrue(ref2.value != null)
    }
}

fun array(size: Int) = Array<Any?>(size, { null })

fun test8() {
    val ref = AtomicReference<Any?>(null)
    val obj1 = array(2)
    val obj2 = array(1)
    val obj3 = array(2)

    obj1[0] = obj2
    obj1[1] = obj3

    obj2[0] = obj3

    obj3[0] = obj2
    obj3[1] = ref

    ref.value = obj1.freeze()
}

fun createNode1(): Holder {
    val ref = AtomicReference<Any?>(null)
    val node2 = Holder(ref)
    val node1 = Holder(node2)
    ref.value = node1.freeze()

    return node1
}

fun getNode2(): Holder {
    val node1 = createNode1()
    GC.collect()

    return node1.other as Holder
}

fun test9() {
    withWorker {
        val node2 = getNode2()
        executeAfter(10 * 1000L, { GC.collectCyclic() }.freeze())

        GC.collect()

        Worker.current.park(50 * 1000L)

        execute(TransferMode.SAFE, {}, {}).result

        val ref = node2.other as AtomicReference<Any?>
        assertTrue(ref.value != null)
    }
}

fun main() {
    Platform.isMemoryLeakCheckerActive = true
    kotlin.native.internal.GC.cyclicCollectorEnabled = true
    test1()
    test2()
    test3()
    test4()
    repeat(10) {
        test5()
    }
    test6()
    test7()
    test8()
    test9()
}
