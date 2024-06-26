package org.jetbrains.litmuskt.tests

import org.jetbrains.litmuskt.LitmusOutcomeType
import org.jetbrains.litmuskt.LitmusTestContainer
import org.jetbrains.litmuskt.autooutcomes.LitmusIOutcome
import org.jetbrains.litmuskt.autooutcomes.LitmusLOutcome
import org.jetbrains.litmuskt.autooutcomes.accept
import org.jetbrains.litmuskt.litmusTest
import kotlin.concurrent.Volatile

@LitmusTestContainer
object Atomicity {

    val Int = litmusTest({
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

    val Long = litmusTest({
        object : LitmusLOutcome() {
            var x = 0L
        }
    }) {
        thread {
            x = -1 // signed 0xFFFFFFFF_FFFFFFFF
        }
        thread {
            r1 = x
        }
        spec {
            accept(0)
            accept(-1)
            default(LitmusOutcomeType.INTERESTING)
        }
    }

    val LongVolatile = litmusTest({
        object : LitmusLOutcome() {
            @Volatile
            var x = 0L
        }
    }) {
        thread {
            x = -1 // signed 0xFFFFFFFF_FFFFFFFF
        }
        thread {
            r1 = x
        }
        spec {
            accept(0)
            accept(-1)
        }
    }
}
