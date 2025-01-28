// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-73508
// FULL_JDK
// RENDER_DIAGNOSTICS_FULL_TEXT

import kotlinx.atomicfu.*

fun test() {
    val a = <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>atomic<Int>(127)<!>
    a.compareAndSet(127, 128) // true
    a.compareAndSet(128, 7777) // false

    val aa: <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicRef<Int><!>
    aa = a
}

typealias AtomicfuAtomicReference<T> = AtomicRef<T>

fun testTypealiased() {
    val aa: <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicfuAtomicReference<Int><!>
    aa = <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>atomic<Int>(127)<!>
}

fun testArray(a: <!ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY!>AtomicArray<Int><!>) {}
