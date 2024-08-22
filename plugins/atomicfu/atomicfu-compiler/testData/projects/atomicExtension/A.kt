package bar

import kotlinx.atomicfu.*

internal inline fun AtomicInt.inlineFoo(v: Int) {
    value = v
}
