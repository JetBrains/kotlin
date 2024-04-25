// OUTPUT_DATA_FILE: worker10.out
// DISABLE_NATIVE: gcType=NOOP
// FREE_COMPILER_ARGS: -opt-in=kotlin.experimental.ExperimentalNativeApi,kotlin.native.runtime.NativeRuntimeApi,kotlinx.cinterop.ExperimentalForeignApi

import kotlin.test.*
import kotlin.concurrent.AtomicReference
import kotlin.concurrent.AtomicInt
import kotlin.concurrent.*
import kotlin.native.concurrent.*
import kotlin.native.ref.WeakReference
import kotlinx.cinterop.StableRef

class Data(val x: Int)

val topInt = 1
val topString = "string"
var topStringVar = "string"
val topSharedStringWithGetter: String
        get() = "top"
val topData = Data(42)
val topSharedData = Data(43)

@Test fun runTest1() {
    val worker = Worker.start()

    assertEquals(1, topInt)
    assertEquals("string", topString)
    assertEquals(42, topData.x)
    assertEquals(43, topSharedData.x)
    assertEquals("top", topSharedStringWithGetter)

    worker.execute(TransferMode.SAFE, { -> }, {
        it -> topInt == 1
    }).consume {
        result -> assertEquals(true, result)
    }

    worker.execute(TransferMode.SAFE, { -> }, {
        it -> topString == "string"
    }).consume {
        result -> assertEquals(true, result)
    }

    worker.execute(TransferMode.SAFE, { -> }, {
        it -> topStringVar == "string"
    }).consume {
        result -> assertTrue(result)
    }

    worker.execute(TransferMode.SAFE, { -> }, {
        it -> topSharedStringWithGetter == "top"
    }).consume {
        result -> assertTrue(result)
    }

    worker.execute(TransferMode.SAFE, { -> }, {
        it -> topData.x == 42
    }).consume {
        result -> assertTrue(result)
    }

    worker.execute(TransferMode.SAFE, { -> }, {
        it -> try {
            topSharedData.x == 43
        } catch (e: Throwable) {
            false
        }
    }).consume {
        result -> assertTrue(result)
    }

    worker.requestTermination().result
    println("OK")
}

val atomicRef = AtomicReference<Any?>(Any())
val stableRef = StableRef.create(Any())
val semaphore = AtomicInt(0)

@Test fun runTest2() {
    semaphore.value = 0
    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { null }) {
        val value = atomicRef.value
        semaphore.incrementAndGet()
        while (semaphore.value != 2) {}
        println(value.toString() != "")
    }
    while (semaphore.value != 1) {}
    atomicRef.value = null
    kotlin.native.runtime.GC.collect()
    semaphore.incrementAndGet()
    future.result
    worker.requestTermination().result
}

@Test fun runTest3() {
    semaphore.value = 0
    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { null }) {
        val value = stableRef.get()
        semaphore.incrementAndGet()
        while (semaphore.value != 2) {}
        println(value.toString() != "")
    }
    while (semaphore.value != 1) {}
    stableRef.dispose()
    kotlin.native.runtime.GC.collect()
    semaphore.incrementAndGet()
    future.result
    worker.requestTermination().result
}

@NoInline
fun <T: Any> ensureWeakIs(weak: WeakReference<T>, expected: T?) {
    assertEquals(expected, weak.get())
}

val stableHolder1 = StableRef.create(("hello" to "world"))

@Test fun runTest4() {
    val worker = Worker.start()
    semaphore.value = 0
    val future = worker.execute(TransferMode.SAFE, { WeakReference(stableHolder1.get()) }) {
        ensureWeakIs(it, "hello" to "world")
        semaphore.incrementAndGet()
        while (semaphore.value != 2) {}
        kotlin.native.runtime.GC.collect()
        ensureWeakIs(it, null)
    }
    while (semaphore.value != 1) {}
    stableHolder1.dispose()
    kotlin.native.runtime.GC.collect()
    semaphore.incrementAndGet()
    future.result
    worker.requestTermination().result
}

val stableHolder2 = StableRef.create(("hello" to "world"))

@Test fun runTest5() {
    val worker = Worker.start()
    semaphore.value = 0
    val future = worker.execute(TransferMode.SAFE, { WeakReference(stableHolder2.get()) }) {
        val value = it.get()
        semaphore.incrementAndGet()
        while (semaphore.value != 2) {}
        kotlin.native.runtime.GC.collect()
        assertEquals("hello" to "world", value)
    }
    while (semaphore.value != 1) {}
    stableHolder2.dispose()
    kotlin.native.runtime.GC.collect()
    semaphore.incrementAndGet()
    future.result
    worker.requestTermination().result
}

val atomicRef2 = AtomicReference<Any?>(Any())
@Test fun runTest6() {
    semaphore.value = 0
    val worker = Worker.start()
    val future = worker.execute(TransferMode.SAFE, { null }) {
        val value = atomicRef2.compareAndExchange(null, null)
        semaphore.incrementAndGet()
        while (semaphore.value != 2) {}
        assertEquals(true, value.toString() != "")
    }
    while (semaphore.value != 1) {}
    atomicRef2.value = null
    kotlin.native.runtime.GC.collect()
    semaphore.incrementAndGet()
    future.result
    worker.requestTermination().result
}
