package bar

import kotlinx.atomicfu.*

internal class A {
    internal val i: AtomicInt = atomic(7)
    internal val l: AtomicLong = atomic(7L)
    internal val b: AtomicBoolean = atomic(true)
    internal val s: AtomicRef<String> = atomic("aaa")

    internal val intArr = AtomicIntArray(10)
    internal val longArr = AtomicLongArray(10)
    internal val booleanArr = AtomicBooleanArray(10)
    internal val refArr = atomicArrayOfNulls<String?>(10)
}
