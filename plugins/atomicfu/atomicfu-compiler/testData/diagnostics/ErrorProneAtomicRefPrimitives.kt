// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-73508
// FULL_JDK

import kotlinx.atomicfu.*

fun test() {
    val a = atomic<Int>(127)
    a.compareAndSet(127, 128) // true
    a.compareAndSet(128, 7777) // false

    val aa: AtomicRef<Int>
    aa = a
}

typealias AtomicfuAtomicReference<T> = AtomicRef<T>

fun testTypealiased() {
    val aa: AtomicfuAtomicReference<Int>
    aa = atomic<Int>(127)
}
