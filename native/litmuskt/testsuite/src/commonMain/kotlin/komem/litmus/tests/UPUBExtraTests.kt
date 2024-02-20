package komem.litmus.tests

import komem.litmus.LitmusTest
import komem.litmus.litmusTest
import kotlin.concurrent.Volatile

val UPUBVolatile: LitmusTest<*> = litmusTest({
    object {
        @Volatile
        var h: IntHolder? = null
        var o = 0
    }
}) {
    thread {
        h = IntHolder(0)
    }
    thread {
        o = h?.x ?: -1
    }
    outcome { o }
    spec {
        accept(0)
        accept(-1)
    }
}

val UPUBArray: LitmusTest<*> = litmusTest({
    object {
        var arr: Array<Int>? = null
        var o = 0
    }
}) {
    thread {
        arr = Array(10) { 0 }
    }
    thread {
        o = arr?.get(0) ?: -1
    }
    outcome { o }
    spec {
        accept(0)
        accept(-1)
    }
}

private class UPUBRefInner(val x: Int)
private class UPUBRefHolder(val ref: UPUBRefInner)

val UPUBRef: LitmusTest<*> = litmusTest({
    object {
        var h: UPUBRefHolder? = null
        var o = 0
    }
}) {
    thread {
        val ref = UPUBRefInner(1)
        h = UPUBRefHolder(ref)
    }
    thread {
        val t = h
        o = t?.ref?.x ?: -1
    }
    outcome { o }
    spec {
        accept(1)
        accept(-1)
    }
}

private class UPUBIntHolderInnerLeaking {
    var ih: InnerHolder? = null

    inner class InnerHolder {
        val x: Int

        init {
            x = 1
            ih = this
        }
    }
}

val UBUBCtorLeaking: LitmusTest<*> = litmusTest({
    object {
        var h = UPUBIntHolderInnerLeaking()
        var o = 0
    }
}) {
    thread {
        h.InnerHolder()
    }
    thread {
        o = h.ih?.x ?: -1
    }
    outcome { o }
    spec {
        accept(1)
        accept(0)
        accept(-1)
    }
}
