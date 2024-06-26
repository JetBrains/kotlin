package org.jetbrains.litmuskt.tests

import org.jetbrains.litmuskt.LitmusOutcomeType
import org.jetbrains.litmuskt.LitmusTestContainer
import org.jetbrains.litmuskt.autooutcomes.LitmusIIIOutcome
import org.jetbrains.litmuskt.autooutcomes.LitmusIIOutcome
import org.jetbrains.litmuskt.autooutcomes.accept
import org.jetbrains.litmuskt.autooutcomes.interesting
import org.jetbrains.litmuskt.litmusTest

@LitmusTestContainer
object Coherence {

    val Plain = litmusTest({
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

    val CSE = litmusTest({
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

}
