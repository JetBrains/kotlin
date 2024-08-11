package bar

import kotlinx.atomicfu.*

internal val topLevel_i = atomic(0)
//internal val topLevel_b = atomic(true)
//internal val topLevel_l = atomic(100000000000L)
//internal val topLevel_r = atomic<String>("aaaa")
//
//internal val topLevel_intArr = AtomicIntArray(10)
//internal val topLevel_boolArr = AtomicBooleanArray(10)
//internal val topLevel_longArr = AtomicLongArray(10)
//internal val topLevel_refArr = atomicArrayOfNulls<String?>(10)

//internal class A {
    //internal val i = atomic(0)
//    internal val b = atomic(true)
//    internal val l = atomic(100000000000L)
//    internal val r = atomic<String>("aaaa")
//
//    internal val intArr = AtomicIntArray(10)
//    internal val boolArr = AtomicBooleanArray(10)
//    internal val longArr = AtomicLongArray(10)
//    internal val refArr = atomicArrayOfNulls<String?>(10)

    // todo private val accessed from inline fun
//}