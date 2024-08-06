package bar

import kotlinx.atomicfu.*

internal class B {
    internal fun b() {
        val aClass = A()
        if (aClass.a.compareAndSet(0, 133))
            check(aClass.a.value == 133)
        println("End")
    }
}