@file:OptIn(FreezingIsDeprecated::class, ObsoleteWorkersApi::class)

import kotlinx.cinterop.*
import kotlin.native.concurrent.*
import kotlin.test.*
import objcTests.*

private class NSObjectImpl : NSObject() {
    var x = 111
}

// Also see counterpart interop/objc/illegal_sharing.kt
@Test fun testSharing() = withWorker {
    val obj = NSObjectImpl()
    val array = nsArrayOf(obj)

    assertFalse(obj.isFrozen)

    obj.x = 222

    if (Platform.isFreezingEnabled) {
        obj.freeze()
        assertTrue(obj.isFrozen)
        runInWorker {
            val obj1 = array.objectAtIndex(0) as NSObjectImpl
            assertFailsWith<InvalidMutabilityException> {
                obj1.x = 333
            }
        }
        assertEquals(222, obj.x)
    }

    // TODO: test [obj release] etc.
}
