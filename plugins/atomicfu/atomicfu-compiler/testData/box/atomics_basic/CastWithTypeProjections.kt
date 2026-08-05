// ISSUE: KT-77692

import kotlinx.atomicfu.*
import kotlin.test.*

private val ref: AtomicRef<String?> = atomic(null)
private val refArray: AtomicArray<String?> = atomicArrayOfNulls(1)

fun test() {
    assertEquals(null, (ref as AtomicRef<*>).value as Any?)
    assertTrue((ref as AtomicRef<in String?>).compareAndSet(null, "projected"))
    assertEquals("projected", (ref as AtomicRef<out String>).value)

    assertEquals(null, (refArray as AtomicArray<*>)[0].value)
    assertTrue((refArray as AtomicArray<in String?>)[0].compareAndSet(null, "projected"))
    assertEquals("projected", (refArray as AtomicArray<out String>)[0].value)

    assertEquals("projected", (((ref as AtomicRef<*>) as AtomicRef<String?>) as AtomicRef<Any?>).value)
    assertEquals("projected", (((refArray as AtomicArray<*>) as AtomicArray<String?>) as AtomicArray<Any?>)[0].value)

    assertEquals("projected",
        ((((refArray as AtomicArray<*>) as AtomicArray<Any?>)[0] as AtomicRef<in String?>) as AtomicRef<*>).value
    )
}

fun box(): String {
    test()
    return "OK"
}
