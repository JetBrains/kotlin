package org.jetbrains.litmuskt.tests

import org.jetbrains.litmuskt.LitmusTestContainer
import org.jetbrains.litmuskt.autooutcomes.LitmusIIOutcome
import org.jetbrains.litmuskt.autooutcomes.LitmusIOutcome
import org.jetbrains.litmuskt.autooutcomes.accept
import org.jetbrains.litmuskt.autooutcomes.interesting
import org.jetbrains.litmuskt.litmusTest
import kotlin.concurrent.Volatile

@LitmusTestContainer
object MessagePassing {

    val Plain = litmusTest({
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

    val VolatileAnnotated = litmusTest({
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

    val RaceFree = litmusTest({
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

    val MissingVolatile = litmusTest({
        object : LitmusIOutcome() {
            var x = 0
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
            interesting(0)
        }
    }

}
