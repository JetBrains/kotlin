import kotlin.native.ref.*
import kotlinx.cinterop.*
import kotlin.test.*
import objcTests.*

@Test fun testWeakRefs() {
    testWeakReference({ createNSObject()!! })

    createAndAbandonWeakRef(NSObject())

    testWeakReference({ NSArray.arrayWithArray(listOf(42)) as NSArray })
}

private fun testWeakReference(block: () -> NSObject) {
    val ref = autoreleasepool {
        createAndTestWeakReference(block)
    }

    kotlin.native.internal.GC.collect()

    assertNull(ref.get())
}

private fun createAndTestWeakReference(block: () -> NSObject): WeakReference<NSObject> {
    val ref = createWeakReference(block)
    assertNotNull(ref.get())
    assertEquals(ref.get()!!.hash(), ref.get()!!.hash())
    return ref
}

private fun createWeakReference(block: () -> NSObject) = WeakReference(block())

private fun createAndAbandonWeakRef(obj: NSObject) {
    WeakReference(obj)
}