package bar.p1

import kotlinx.atomicfu.*
import bar.A

internal class B {
    private val a = A()

    fun foo() {
        a.i.compareAndSet(7, 7777)
        a.l.compareAndSet(7L, 7777L)
        a.b.compareAndSet(true, false)
        a.s.compareAndSet("aaa", "bbb")

        a.intArr[0].compareAndSet(0, 89)
        a.longArr[0].compareAndSet(0L, 89L)
        a.booleanArr[0].compareAndSet(true, false)
        a.refArr[0].compareAndSet(null, "aaaaaa")
    }
}

