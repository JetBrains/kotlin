/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package samples.random

import samples.*
import kotlin.random.Random
import kotlin.test.assertTrue

class Randoms {
    @Sample
    fun defaultRandom() {
        val randomValues = List(10) { Random.nextInt(0, 100) }
        // prints new sequence every time
        println(randomValues)

        val nextValues = List(10) { Random.nextInt(0, 100) }
        println(nextValues)
        assertTrue(randomValues != nextValues)
    }

    @Sample
    fun seededRandom() {
        fun getRandomList(random: Random): List<Int> =
            List(10) { random.nextInt(0, 100) }

        val randomValues1 = getRandomList(Random(42))
        // prints the same sequence every time
        assertPrints(randomValues1, "[33, 40, 41, 2, 41, 32, 21, 40, 69, 87]")

        val randomValues2 = getRandomList(Random(42))
        // random with the same seed produce the same sequence
        assertTrue(randomValues1 == randomValues2)

        val randomValues3 = getRandomList(Random(0))
        // random with another seed produce another sequence
        assertPrints(randomValues3, "[14, 48, 57, 67, 82, 7, 61, 27, 14, 59]")
    }
}