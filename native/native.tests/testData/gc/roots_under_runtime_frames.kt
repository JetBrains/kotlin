// DISABLE_NATIVE: gcType=NOOP
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.runtime.NativeRuntimeApi,kotlin.experimental.ExperimentalNativeApi,kotlin.native.concurrent.ObsoleteWorkersApi

// The runtime calls Kotlin code while the thread stays in the Runnable state: static initializers
// of objects and of files, and the work queue of a worker that the user processes by hand. So a
// Kotlin frame K calls runtime code R, and R calls Kotlin code K'. A collection inside K' must
// still find the roots of K, which lies below the runtime frame.
//
// In every test the object under test lives only in the frame of the test, it is used after the
// call and a weak reference to it is checked after the call.

import kotlin.concurrent.Volatile
import kotlin.native.NoInline
import kotlin.native.concurrent.ThreadLocal
import kotlin.native.concurrent.Worker
import kotlin.native.ref.WeakReference
import kotlin.native.runtime.GC
import kotlin.test.*

class Holder(val data: IntArray) {
    fun sum() = data.sum()
}

@NoInline
private fun newHolder() = Holder(IntArray(16) { it })

// The creation of a weak reference is itself such a call, so the holder is pinned by a global
// while the weak reference is created.
@Volatile
private var pin: Any? = null

@NoInline
private fun weakOf(holder: Holder): WeakReference<Holder> {
    pin = holder
    val weak = WeakReference(holder)
    pin = null
    return weak
}

object LazyObject {
    val value: Int = run { GC.collect(); 1 }
}

@ThreadLocal
object LazyThreadLocalObject {
    val value: Int = run { GC.collect(); 2 }
}

// A property of a file is initialized at the first access to the file.
private val fileLevelValue: Int = run { GC.collect(); 3 }

@Test
fun objectInitializerThatCollects() {
    val holder = newHolder()
    val weak = weakOf(holder)

    val value = LazyObject.value

    assertEquals(1, value)
    assertNotNull(weak.value)
    assertEquals(120, holder.sum())
}

@Test
fun threadLocalObjectInitializerThatCollects() {
    val holder = newHolder()
    val weak = weakOf(holder)

    val value = LazyThreadLocalObject.value

    assertEquals(2, value)
    assertNotNull(weak.value)
    assertEquals(120, holder.sum())
}

@Test
fun fileInitializerThatCollects() {
    val holder = newHolder()
    val weak = weakOf(holder)

    val value = fileLevelValue

    assertEquals(3, value)
    assertNotNull(weak.value)
    assertEquals(120, holder.sum())
}

@Test
fun jobProcessedByProcessQueue() {
    val worker = Worker.current
    val holder = newHolder()
    val weak = weakOf(holder)
    var ran = false
    worker.executeAfter(0L) {
        GC.collect()
        ran = true
    }

    worker.processQueue()

    assertTrue(ran)
    assertNotNull(weak.value)
    assertEquals(120, holder.sum())
}

@Test
fun jobProcessedByPark() {
    val worker = Worker.current
    val holder = newHolder()
    val weak = weakOf(holder)
    var ran = false
    worker.executeAfter(0L) {
        GC.collect()
        ran = true
    }

    worker.park(0L, true)

    assertTrue(ran)
    assertNotNull(weak.value)
    assertEquals(120, holder.sum())
}

// The argument of the program is held by `main`, which is below the initializer in the stack.
@Test
fun nestedInitializers() {
    val holder = newHolder()
    val weak = weakOf(holder)

    val value = Outer.value

    assertEquals(11, value)
    assertNotNull(weak.value)
    assertEquals(120, holder.sum())
}

object Inner {
    val value: Int = run { GC.collect(); 1 }
}

object Outer {
    val value: Int = run {
        val local = newHolder()
        val weak = weakOf(local)
        val inner = Inner.value
        GC.collect()
        check(weak.value != null)
        10 + inner + local.sum() - 120
    }
}
