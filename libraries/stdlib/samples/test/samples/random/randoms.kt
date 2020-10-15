/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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

    @Sample
    fun nextBits() {
        // always generates a 0
        println(Random.nextBits(0))
        // get a random 1 bit value
        println(Random.nextBits(1))
        // get a random 8 bit value
        println(Random.nextBits(8))
        // get a random 16 bit value
        println(Random.nextBits(16))
        // get a random 32 bit value
        println(Random.nextBits(32))
    }

    @Sample
    fun nextBoolean() {
        // get a random Boolean value
        println(Random.nextBoolean())
    }

    @Sample
    fun nextBytes() {
        var bytes = ByteArray(4)
        assertPrints(bytes[0], "0")
        assertPrints(bytes[1], "0")
        assertPrints(bytes[2], "0")
        assertPrints(bytes[3], "0")
        Random.nextBytes(bytes, 1, 3)
        for(i in 1..2)println("bytes[${i}]: ${bytes[i]}")
        Random.nextBytes(bytes)
        for(i in 0..3)println("bytes[${i}]: ${bytes[i]}")
        val new_bytes = Random.nextBytes(3)
        for(i in 0..2)println("new_bytes[${i}]: ${new_bytes[i]}")
    }

    @Sample
    fun nextDouble() {
        println(Random.nextDouble())
        println(Random.nextDouble(133.7))
        println(Random.nextDouble(-42.0, 133.7))
    }

    @Sample
    fun nextFloat() {
        println(Random.nextFloat())
    }

    @Sample
    fun nextInt() {
        println(Random.nextInt())
        println(Random.nextInt(64))
        println(Random.nextInt(101, 837))
    }

    @Sample
    fun nextLong() {
        println(Random.nextLong())
        println(Random.nextLong(3000000000))
        println(Random.nextLong(-4000000000, 4000000000))
    }
}
