@file:OptIn(FreezingIsDeprecated::class, kotlin.native.runtime.NativeRuntimeApi::class, ObsoleteWorkersApi::class)

import kotlinx.cinterop.*
import kotlin.native.concurrent.*
import kotlin.test.*
import objcTests.*

@Test fun testObjCWeakRef() {
    val deallocListener = DeallocListener()
    assertFalse(deallocListener.deallocated)

    // [deallocListener.deallocExecutorIsNil()] calls deallocExecutor getter, which retains the result and either
    // puts it to autoreleasepool or releases it immediately (Obj-C ARC optimization). It seems to depend on the platform.
    // Wrap the call to autoreleasepool to ensure the object will be released:
    autoreleasepool {
        testObjCWeakRef0(deallocListener)
    }

    kotlin.native.runtime.GC.collect()
    assertTrue(deallocListener.deallocated)
    assertTrue(deallocListener.deallocExecutorIsNil())
}

private fun testObjCWeakRef0(deallocListener: DeallocListener) = withWorker {
    assertTrue(deallocListener.deallocExecutorIsNil())

    val obj = object : DeallocExecutor() {}
    deallocListener.deallocExecutor = obj
    obj.deallocListener = deallocListener

    assertFalse(deallocListener.deallocExecutorIsNil())

//    TODO: can't actually test, Obj-C runtime doesn't expect _tryRetain throwing an exception.
//    runInWorker {
//        assertFailsWith<IncorrectDereferenceException> {
//            deallocListener.deallocExecutorIsNil()
//        }
//    }

    obj.freeze()

    runInWorker {
        // [deallocListener.deallocExecutorIsNil()] calls deallocExecutor getter, which retains [obj] and either
        // puts it to autoreleasepool or releases it immediately (Obj-C ARC optimization).
        // Wrap the call to autoreleasepool to ensure [obj] will be released:
        autoreleasepool {
            assertFalse(deallocListener.deallocExecutorIsNil())
        }
        // Process release of Kotlin reference to [obj] in any case:
        kotlin.native.runtime.GC.collect()
    }
}
