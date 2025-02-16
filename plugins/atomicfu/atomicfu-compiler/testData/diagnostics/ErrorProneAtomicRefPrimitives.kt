// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-73508
// FULL_JDK
// RENDER_DIAGNOSTICS_FULL_TEXT

import kotlinx.atomicfu.*

fun test() {
    val a = atomic<Int>(127)
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>a.compareAndSet(127, 128)<!> // true
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>a.compareAndSet(128, 7777)<!> // false

    val aa: AtomicRef<Int>
    aa = a
}

typealias AtomicfuAtomicReference<T> = AtomicRef<T>

fun testTypealiased() {
    val aa: AtomicfuAtomicReference<Int>
    aa = atomic<Int>(127)
    <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>aa.compareAndSet(127, 128)<!>
}

fun testArray(a: AtomicArray<Int>) {
    a[0].compareAndSet(1, 2) // A call on `AtomicRef<Int?>`
}
