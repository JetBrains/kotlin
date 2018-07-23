/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package samples.misc

import samples.Sample
import samples.assertPrints

class Repeat {

    @Sample
    fun repeatAnyTimes() {
        var sum10Times = 0
        repeat(10) {
            sum10Times += it
        }
        assertPrints(sum10Times, "45")

        var sumZeroTimes = 0
        repeat(0) {
            sumZeroTimes += it
        }
        assertPrints(sumZeroTimes, "0")

        var sumMinusTimes = 0
        repeat(-1) {
            sumMinusTimes += it
        }
        assertPrints(sumMinusTimes, "0")
    }
}