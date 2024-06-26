package org.jetbrains.litmuskt.tests

import org.jetbrains.litmuskt.autooutcomes.LitmusIOutcome
import org.jetbrains.litmuskt.LitmusTestContainer
import org.jetbrains.litmuskt.autooutcomes.accept
import org.jetbrains.litmuskt.litmusTest
import kotlin.concurrent.Volatile

@LitmusTestContainer
object UnsafePublication {

    private data class IntHolder(val x: Int = 0)

    val Plain = litmusTest({
        object : LitmusIOutcome() {
            var h: IntHolder? = null
        }
    }) {
        thread {
            h = IntHolder()
        }
        thread {
            r1 = h?.x ?: -1
        }
        spec {
            accept(0)
            accept(-1)
        }
    }

    val VolatileAnnotated = litmusTest({
        object : LitmusIOutcome() {
            @Volatile
            var h: IntHolder? = null
        }
    }) {
        thread {
            h = IntHolder()
        }
        thread {
            r1 = h?.x ?: -1
        }
        spec {
            accept(0)
            accept(-1)
        }
    }

    val PlainWithConstructor = litmusTest({
        object : LitmusIOutcome() {
            var h: IntHolder? = null
        }
    }) {
        thread {
            h = IntHolder(x = 1)
        }
        thread {
            r1 = h?.x ?: -1
        }
        spec {
            accept(1)
            accept(-1)
        }
    }

    val PlainArray = litmusTest({
        object : LitmusIOutcome() {
            var arr: Array<Int>? = null
        }
    }) {
        thread {
            arr = Array(10) { 0 }
        }
        thread {
            r1 = arr?.get(0) ?: -1
        }
        spec {
            accept(0)
            accept(-1)
        }
    }

    private class RefHolder(val ref: IntHolder)

    val Reference = litmusTest({
        object : LitmusIOutcome() {
            var h: RefHolder? = null
        }
    }) {
        thread {
            val ref = IntHolder(x = 1)
            h = RefHolder(ref)
        }
        thread {
            val t = h
            r1 = t?.ref?.x ?: -1
        }
        spec {
            accept(1)
            accept(-1)
        }
    }

    private class LeakingIntHolderContext {
        var ih: LeakingIntHolder? = null

        inner class LeakingIntHolder {
            val x: Int = 1

            init {
                ih = this
            }
        }
    }

    val PlainWithLeakingConstructor = litmusTest({
        object : LitmusIOutcome() {
            var ctx = LeakingIntHolderContext()
        }
    }) {
        thread {
            ctx.LeakingIntHolder()
        }
        thread {
            r1 = ctx.ih?.x ?: -1
        }
        spec {
            accept(1)
            accept(0)
            accept(-1)
        }
    }

}
