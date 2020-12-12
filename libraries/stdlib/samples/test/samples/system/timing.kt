/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.system

import samples.*
import kotlin.system.*

class Timing {

    @Sample
    fun measureBlockTimeMillis() {
        val numbers: List<Int>
        val timeInMillis = measureTimeMillis {
            numbers = buildList {
                addAll(0..100)
                shuffle()
                sortDescending()
            }
        }
        // here numbers are initialized and sorted
        assertPrints(numbers.first(), "100")

        println("(The operation took $timeInMillis ms)")
    }

    @Sample
    fun measureBlockNanoTime() {
        var sqrt = 0
        val number = 1000
        val timeInNanos = measureNanoTime {
            while (sqrt * sqrt < number) sqrt++
        }
        println("(The operation took $timeInNanos ns)")
        println("The approximate square root of $number is between ${sqrt - 1} and $sqrt")
    }
}
