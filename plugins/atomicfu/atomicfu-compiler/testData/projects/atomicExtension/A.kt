package bar

import kotlinx.atomicfu.*

internal val topLevel_intArr = AtomicIntArray(10)

internal var simpleInt = 5657

internal inline fun AtomicInt.inlineFoo(v: Int) {
    value = v
}
