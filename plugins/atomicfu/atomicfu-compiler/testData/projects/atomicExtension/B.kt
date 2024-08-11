package bar

import kotlinx.atomicfu.*

class B {
    private val a = atomic(0)

    fun foo() {
        a.inlineFoo(56)
        topLevel_intArr[0].getAndSet(56)
        check(a.value == 56)
        simpleInt = 67
        println("AAA")
    }
}