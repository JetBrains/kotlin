import kotlinx.cinterop.*
import kotlin.test.*
import objcTests.*

@Test
fun testTryRetainGC() {
    kotlin.native.internal.GC.collect()
    val weakRefHolder = WeakRefHolder()
    createGarbageNSObjects(weakRefHolder)
    weakRefHolder.obj = object : NSObject() {}
    // Loading weak ref takes a lock. If K/N runtime runs GC while the lock is taken,
    // then it releases garbage objects and thus Obj-C runtime might take a recursive lock
    // and abort in _os_unfair_lock_recursive_abort.
    weakRefHolder.loadManyTimes()
}

private fun createGarbageNSObjects(weakRefHolder: WeakRefHolder) {
    autoreleasepool {
        repeat(100) {
            // Assigning the object to a weak reference so Obj-C would take a lock when deallocating it:
            weakRefHolder.obj = NSObject()
        }
        weakRefHolder.obj = null
    }
}