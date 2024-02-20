package komem.litmus.tests

import komem.litmus.*
import kotlin.concurrent.Volatile

data class IntHolder(val x: Int)

class IntHolderCtor {
    val x = 1
}

val ATOM: LitmusTest<*> = litmusTest({
    object : LitmusIOutcome() {
        var x = 0
    }
}) {
    thread {
        x = -1 // signed 0xFFFFFFFF
    }
    thread {
        r1 = x
    }
    spec {
        accept(0)
        accept(-1)
    }
}

val SB: LitmusTest<*> = litmusTest({
    object : LitmusIIOutcome() {
        var x = 0
        var y = 0
    }
}) {
    thread {
        x = 1
        r1 = y
    }
    thread {
        y = 1
        r2 = x
    }
    // no need for explicit outcome{}
    spec {
        accept(0, 1)
        accept(1, 0)
        accept(1, 1)
        interesting(0, 0)
    }
}

val SBVolatile: LitmusTest<*> = litmusTest({
    object : LitmusIIOutcome() {
        @Volatile
        var x = 0

        @Volatile
        var y = 0
    }
}) {
    thread {
        x = 1
        r1 = y
    }
    thread {
        y = 1
        r2 = x
    }
    // no need for explicit outcome{}
    spec {
        accept(0, 1)
        accept(1, 0)
        accept(1, 1)
    }
}

val MP: LitmusTest<*> = litmusTest({
    object : LitmusIIOutcome() {
        var x = 0
        var y = 0
    }
}) {
    thread {
        x = 1
        y = 1
    }
    thread {
        r1 = y
        r2 = x
    }
    spec {
        accept(0, 0)
        accept(0, 1)
        accept(1, 1)
        interesting(1, 0)
    }
}

val MPVolatile: LitmusTest<*> = litmusTest({
    object : LitmusIIOutcome() {
        @Volatile
        var x = 0

        @Volatile
        var y = 0
    }
}) {
    thread {
        x = 1
        y = 1
    }
    thread {
        r1 = y
        r2 = x
    }
    spec {
        accept(0, 0)
        accept(0, 1)
        accept(1, 1)
    }
}

val MP_DRF: LitmusTest<*> = litmusTest({
    object : LitmusIOutcome() {
        var x = 0

        @Volatile
        var y = 0
    }
}) {
    thread {
        x = 1
        y = 1
    }
    thread {
        r1 = if (y != 0) x else -1
    }
    spec {
        accept(1)
        accept(-1)
    }
}

val CoRR: LitmusTest<*> = litmusTest({
    object : LitmusIIOutcome() {
        var x = 0
    }
}) {
    thread {
        x = 1
    }
    thread {
        r1 = x
        r2 = x
    }
    spec {
        accept(0, 0)
        accept(0, 1)
        accept(1, 1)
        interesting(1, 0)
    }
}

val CoRR_CSE: LitmusTest<*> = litmusTest({
    data class Holder(var x: Int)
    object : LitmusIIIOutcome() {
        val holder1 = Holder(0)
        val holder2 = holder1
    }
}) {
    thread {
        holder1.x = 1
    }
    thread {
        val h1 = holder1
        val h2 = holder2
        r1 = h1.x
        r2 = h2.x
        r3 = h1.x
    }
    spec {
        interesting(1, 0, 0)
        interesting(1, 1, 0)
        default(LitmusOutcomeType.ACCEPTED)
    }
}

val IRIW: LitmusTest<*> = litmusTest({
    object : LitmusIIIIOutcome() {
        var x = 0
        var y = 0
    }
}) {
    thread {
        x = 1
    }
    thread {
        y = 1
    }
    thread {
        r1 = x
        r2 = y
    }
    thread {
        r3 = y
        r4 = x
    }
    spec {
        interesting(1, 0, 1, 0)
        interesting(0, 1, 0, 1)
        default(LitmusOutcomeType.ACCEPTED)
    }
}

val IRIWVolatile: LitmusTest<*> = litmusTest({
    object : LitmusIIIIOutcome() {
        @Volatile
        var x = 0

        @Volatile
        var y = 0
    }
}) {
    thread {
        x = 1
    }
    thread {
        y = 1
    }
    thread {
        r1 = x
        r2 = y
    }
    thread {
        r3 = y
        r4 = x
    }
    spec {
        forbid(1, 0, 1, 0)
        default(LitmusOutcomeType.ACCEPTED)
    }
}

val UPUB: LitmusTest<*> = litmusTest({
    object : LitmusIOutcome() {
        var h: IntHolder? = null
    }
}) {
    thread {
        h = IntHolder(0)
    }
    thread {
        r1 = h?.x ?: -1
    }
    spec {
        accept(0)
        accept(-1)
    }
}

val UPUBCtor: LitmusTest<*> = litmusTest({
    object : LitmusIOutcome() {
        var h: IntHolderCtor? = null
    }
}) {
    thread {
        h = IntHolderCtor()
    }
    thread {
        r1 = h?.x ?: -1
    }
    spec {
        accept(1)
        accept(-1)
    }
}

val LB_DEPS_OOTA: LitmusTest<*> = litmusTest({
    object : LitmusIIOutcome() {
        var x = 0
        var y = 0
    }
}) {
    thread {
        r1 = x
        y = r1
    }
    thread {
        r2 = y
        x = r2
    }
    spec {
        accept(0, 0)
    }
}

val LB: LitmusTest<*> = litmusTest({
    object : LitmusIIOutcome() {
        var x = 0
        var y = 0
    }
}) {
    thread {
        r1 = x
        y = 1
    }
    thread {
        r2 = y
        x = 1
    }
    spec {
        accept(0, 0)
        accept(1, 0)
        accept(0, 1)
        interesting(1, 1)
    }
}

val LBVolatile: LitmusTest<*> = litmusTest({
    object : LitmusIIOutcome() {
        @Volatile
        var x = 0

        @Volatile
        var y = 0
    }
}) {
    thread {
        r1 = x
        y = 1
    }
    thread {
        r2 = y
        x = 1
    }
    spec {
        accept(0, 0)
        accept(1, 0)
        accept(0, 1)
    }
}

val LBFakeDEPS: LitmusTest<*> = litmusTest({
    object : LitmusIIOutcome() {
        var x = 0
        var y = 0
    }
}) {
    thread {
        r1 = x
        y = 1 + r1 * 0
    }
    thread {
        r2 = y
        x = r2
    }
    spec {
        accept(0, 0)
        accept(0, 1)
    }
}
