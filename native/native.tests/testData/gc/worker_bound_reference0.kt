// DISABLE_NATIVE: gcType=NOOP
// FREE_COMPILER_ARGS: -opt-in=kotlin.experimental.ExperimentalNativeApi,kotlin.native.runtime.NativeRuntimeApi

import kotlin.test.*

import kotlin.concurrent.AtomicInt
import kotlin.concurrent.AtomicReference
import kotlin.native.concurrent.*
import kotlin.native.*
import kotlin.native.ref.WeakReference
import kotlin.native.runtime.GC

class A(var a: Int)

@Suppress("DEPRECATION_ERROR") // Freezing API
fun getOwnerAndWeaks(initial: Int): Triple<AtomicReference<WorkerBoundReference<A>?>, WeakReference<WorkerBoundReference<A>>, WeakReference<A>> {
    val ref = WorkerBoundReference(A(initial))
    val refOwner: AtomicReference<WorkerBoundReference<A>?> = AtomicReference(ref)
    val refWeak = WeakReference(ref)
    val refValueWeak = WeakReference(ref.value)

    return Triple(refOwner, refWeak, refValueWeak)
}

@Test
fun testCollect() {
    val (refOwner, refWeak, refValueWeak) = getOwnerAndWeaks(3)

    refOwner.value = null
    GC.collect()

    // Last reference to WorkerBoundReference is gone, so it and it's referent are destroyed.
    assertNull(refWeak.value)
    assertNull(refValueWeak.value)
}

@Suppress("DEPRECATION_ERROR") // Freezing API
fun getOwnerAndWeaksFrozen(initial: Int): Triple<AtomicReference<WorkerBoundReference<A>?>, WeakReference<WorkerBoundReference<A>>, WeakReference<A>> {
    val ref = WorkerBoundReference(A(initial)).freeze()
    val refOwner: AtomicReference<WorkerBoundReference<A>?> = AtomicReference(ref)
    val refWeak = WeakReference(ref)
    val refValueWeak = WeakReference(ref.value)

    return Triple(refOwner, refWeak, refValueWeak)
}

@Test
fun testCollectFrozen() {
    val (refOwner, refWeak, refValueWeak) = getOwnerAndWeaksFrozen(3)

    refOwner.value = null
    // This runs the finalizer on the WorkerBoundReference<A>, which schedules removing A from the root set
    GC.collect()
    // This actually frees A
    GC.collect()

    // Last reference to WorkerBoundReference is gone, so it and it's referent are destroyed.
    assertNull(refWeak.value)
    assertNull(refValueWeak.value)
}

fun collectInWorkerFrozen(worker: Worker, semaphore: AtomicInt): Pair<WeakReference<A>, Future<Unit>> {
    val (refOwner, _, refValueWeak) = getOwnerAndWeaksFrozen(3)

    val future = worker.execute(TransferMode.SAFE, { Pair(refOwner, semaphore) }) { (refOwner, semaphore) ->
        semaphore.incrementAndGet()
        while (semaphore.value < 2) {
        }

        refOwner.value = null
        GC.collect()
    }

    while (semaphore.value < 1) {
    }
    // At this point worker is spinning on semaphore. refOwner still contains reference to
    // WorkerBoundReference, so referent is kept alive.
    GC.collect()
    assertNotNull(refValueWeak.value)

    return Pair(refValueWeak, future)
}

@Test
fun testCollectInWorkerFrozen() {
    val semaphore: AtomicInt = AtomicInt(0)

    val worker = Worker.start()

    val (refValueWeak, future) = collectInWorkerFrozen(worker, semaphore)
    semaphore.incrementAndGet()
    future.result

    // At this point WorkerBoundReference no longer has a reference, so it's referent is destroyed.
    GC.collect()
    assertNull(refValueWeak.value)

    worker.requestTermination().result
}

@Suppress("DEPRECATION_ERROR") // Freezing API
fun doNotCollectInWorkerFrozen(worker: Worker, semaphore: AtomicInt): Future<WorkerBoundReference<A>> {
    val ref = WorkerBoundReference(A(3)).freeze()

    return worker.execute(TransferMode.SAFE, { Pair(ref, semaphore) }) { (ref, semaphore) ->
        semaphore.incrementAndGet()
        while (semaphore.value < 2) {
        }

        GC.collect()
        ref
    }
}

@Test
fun testDoNotCollectInWorkerFrozen() {
    val semaphore: AtomicInt = AtomicInt(0)

    val worker = Worker.start()

    val future = doNotCollectInWorkerFrozen(worker, semaphore)
    while (semaphore.value < 1) {
    }
    GC.collect()
    semaphore.incrementAndGet()

    val value = future.result
    assertEquals(3, value.value.a)
    worker.requestTermination().result
}

@Suppress("DEPRECATION_ERROR") // Freezing API
class B1 {
    lateinit var b2: WorkerBoundReference<B2>
}

@Suppress("DEPRECATION_ERROR") // Freezing API
data class B2(val b1: WorkerBoundReference<B1>)

@Suppress("DEPRECATION_ERROR") // Freezing API
fun createCyclicGarbage(): Triple<AtomicReference<WorkerBoundReference<B1>?>, WeakReference<B1>, WeakReference<B2>> {
    val ref1 = WorkerBoundReference(B1())
    val ref1Owner: AtomicReference<WorkerBoundReference<B1>?> = AtomicReference(ref1)
    val ref1Weak = WeakReference(ref1.value)

    val ref2 = WorkerBoundReference(B2(ref1))
    val ref2Weak = WeakReference(ref2.value)

    ref1.value.b2 = ref2

    return Triple(ref1Owner, ref1Weak, ref2Weak)
}

@Test
fun collectCyclicGarbage() {
    val (ref1Owner, ref1Weak, ref2Weak) = createCyclicGarbage()

    ref1Owner.value = null
    GC.collect()

    assertNull(ref1Weak.value)
    assertNull(ref2Weak.value)
}

@Suppress("DEPRECATION_ERROR") // Freezing API
class C1 {
    lateinit var c2: AtomicReference<WorkerBoundReference<C2>?>

    fun dispose() {
        c2.value = null
    }
}

@Suppress("DEPRECATION_ERROR") // Freezing API
data class C2(val c1: AtomicReference<WorkerBoundReference<C1>>)

@Suppress("DEPRECATION_ERROR") // Freezing API
fun createCyclicGarbageWithAtomicsFrozen(): Triple<AtomicReference<WorkerBoundReference<C1>?>, WeakReference<C1>, WeakReference<C2>> {
    val ref1 = WorkerBoundReference(C1()).freeze()
    val ref1Weak = WeakReference(ref1.value)

    val ref2 = WorkerBoundReference(C2(AtomicReference(ref1))).freeze()
    val ref2Weak = WeakReference(ref2.value)

    ref1.value.c2 = AtomicReference(ref2)

    return Triple(AtomicReference(ref1), ref1Weak, ref2Weak)
}

@Suppress("DEPRECATION_ERROR") // Freezing API
fun dispose(refOwner: AtomicReference<WorkerBoundReference<C1>?>) {
    refOwner.value!!.value.dispose()
    refOwner.value = null
}

@Test
fun collectCyclicGarbageWithAtomicsFrozen() {
    val (ref1Owner, ref1Weak, ref2Weak) = createCyclicGarbageWithAtomicsFrozen()

    dispose(ref1Owner)
    // Finalizes WorkerBoundReference<C2> and schedules C2 removal from the root set
    GC.collect()
    // Frees C2, finalizes WorkerBoundReference<C1> and schedules C1 removal from the root set
    GC.collect()
    // Frees C1
    GC.collect()

    assertNull(ref1Weak.value)
    assertNull(ref2Weak.value)
}

@Suppress("DEPRECATION_ERROR") // Freezing API
fun createCrossThreadCyclicGarbageWithAtomicsFrozen(
        worker: Worker
): Triple<AtomicReference<WorkerBoundReference<C1>?>, WeakReference<C1>, WeakReference<C2>> {
    val ref1 = WorkerBoundReference(C1()).freeze()
    val ref1Weak = WeakReference(ref1.value)

    val future = worker.execute(TransferMode.SAFE, { ref1 }) { ref1 ->
        val ref2 = WorkerBoundReference(C2(AtomicReference(ref1))).freeze()
        Pair(ref2, WeakReference(ref2.value))
    }
    val (ref2, ref2Weak) = future.result

    ref1.value.c2 = AtomicReference(ref2)

    return Triple(AtomicReference(ref1), ref1Weak, ref2Weak)
}

@Test
fun collectCrossThreadCyclicGarbageWithAtomicsFrozen() {
    val worker = Worker.start()

    val (ref1Owner, ref1Weak, ref2Weak) = createCrossThreadCyclicGarbageWithAtomicsFrozen(worker)

    dispose(ref1Owner)
    // This marks C2 as gone on the main thread
    GC.collect()
    // This cleans up all the references from the worker thread and destroys C2, but C1 is still alive.
    worker.execute(TransferMode.SAFE, {}) { GC.collect() }.result
    // And this finally destroys C1
    GC.collect()

    assertNull(ref1Weak.value)
    assertNull(ref2Weak.value)

    worker.requestTermination().result
}