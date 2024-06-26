package org.jetbrains.litmuskt.tests

import org.jetbrains.litmuskt.LitmusOutcomeType
import org.jetbrains.litmuskt.LitmusTestContainer
import org.jetbrains.litmuskt.autooutcomes.LitmusIIIIOutcome
import org.jetbrains.litmuskt.autooutcomes.forbid
import org.jetbrains.litmuskt.autooutcomes.interesting
import org.jetbrains.litmuskt.litmusTest
import kotlin.concurrent.Volatile

@LitmusTestContainer
object IndependentReadsOfIndependentWrites {

    val Plain = litmusTest({
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
            default(LitmusOutcomeType.ACCEPTED)
        }
    }

    // because of Java, tests cannot be named "Volatile"
    val VolatileAnnotated = litmusTest({
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

}
