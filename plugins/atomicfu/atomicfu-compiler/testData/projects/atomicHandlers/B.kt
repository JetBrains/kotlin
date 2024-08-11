package bar

import kotlinx.atomicfu.*

internal class B {
    //var size = 0

//    init {
//        size = topLevel_i.value
//    }

    internal fun b() {
        //check(size == 34)
        //val aClass = A()
        //if (aClass.i.compareAndSet(0, 133))
            //check(aClass.i.value == 133)
//        aClass.b.getAndSet(false)
//        aClass.l.getAndSet(0L)
//        aClass.r.getAndSet("bbb")
//        aClass.intArr[0].getAndSet(0)
//        aClass.boolArr[0].getAndSet(false)
//        aClass.longArr[0].getAndSet(0L)
//        aClass.refArr[0].getAndSet("bbb")

        topLevel_i.compareAndSet(0, 133)
//        topLevel_b.getAndSet(false)
//        topLevel_l.getAndSet(0L)
//        topLevel_r.getAndSet("bbb")
//        topLevel_intArr[0].getAndSet(0)
//        topLevel_boolArr[0].getAndSet(false)
//        topLevel_longArr[0].getAndSet(0L)
//        topLevel_refArr[0].getAndSet("bbb")

        println("End")
    }
}