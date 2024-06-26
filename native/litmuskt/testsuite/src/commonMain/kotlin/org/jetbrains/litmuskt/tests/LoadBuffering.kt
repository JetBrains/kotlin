package org.jetbrains.litmuskt.tests

import org.jetbrains.litmuskt.LitmusTestContainer
import org.jetbrains.litmuskt.autooutcomes.LitmusIIOutcome
import org.jetbrains.litmuskt.autooutcomes.accept
import org.jetbrains.litmuskt.autooutcomes.interesting
import org.jetbrains.litmuskt.litmusTest
import kotlin.concurrent.Volatile

@LitmusTestContainer
object LoadBuffering {

    val NoOutOfThinAirValues = litmusTest({
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

    val Plain = litmusTest({
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

    val VolatileAnnotated = litmusTest({
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

    val PlainWithFakeDependencies = litmusTest({
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

}
