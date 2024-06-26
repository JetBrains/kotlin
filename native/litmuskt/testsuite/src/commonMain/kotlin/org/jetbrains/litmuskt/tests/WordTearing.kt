package org.jetbrains.litmuskt.tests

import org.jetbrains.litmuskt.LitmusTestContainer
import org.jetbrains.litmuskt.autooutcomes.LitmusIIIOutcome
import org.jetbrains.litmuskt.autooutcomes.LitmusZZOutcome
import org.jetbrains.litmuskt.autooutcomes.accept
import org.jetbrains.litmuskt.litmusTest

@LitmusTestContainer
object WordTearing {

    val Array = litmusTest({
        object : LitmusZZOutcome() {
            val arr = BooleanArray(2)
        }
    }) {
        thread {
            arr[0] = true
        }
        thread {
            arr[1] = true
        }
        outcome {
            r1 = arr[0]
            r2 = arr[1]
            this
        }
        spec {
            accept(true, true)
        }
    }

    // source: https://github.com/openjdk/jcstress/blob/master/tests-custom/src/main/java/org/openjdk/jcstress/tests/tearing/ArrayInterleaveTest.java
    val ArrayInterleave = litmusTest({
        object : LitmusIIIOutcome() {
            val arr = ByteArray(256)
        }
    }) {
        thread {
            for (i in arr.indices step 2) arr[i] = 1
        }
        thread {
            for (i in 1..<arr.size step 2) arr[i] = 2
        }
        outcome {
            for (byte in arr) when (byte) {
                0.toByte() -> r1++
                1.toByte() -> r2++
                2.toByte() -> r3++
            }
            this
        }
        spec {
            accept(0, 128, 128)
        }
    }
}
