package org.jetbrains.litmuskt.tests

import org.jetbrains.litmuskt.LitmusTestContainer
import org.jetbrains.litmuskt.autooutcomes.LitmusZZOutcome
import org.jetbrains.litmuskt.autooutcomes.accept
import org.jetbrains.litmuskt.autooutcomes.interesting
import org.jetbrains.litmuskt.litmusTest

//@LitmusTestContainer
//@OptIn(ObsoleteNativeApi::class)
//object WordTearingNative {
//
//    val Bitset = litmusTest({
//        object : LitmusZZOutcome() {
//            val bs = BitSet()
//        }
//    }) {
//        thread {
//            bs.set(0)
//        }
//        thread {
//            bs.set(1)
//        }
//        outcome {
//            r1 = bs[0]
//            r2 = bs[1]
//            this
//        }
//        spec {
//            accept(true, true)
//            interesting(true, false)
//            interesting(false, true)
//        }
//    }
//}
