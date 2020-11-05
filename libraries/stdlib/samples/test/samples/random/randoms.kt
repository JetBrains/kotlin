/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.random

import samples.*
import kotlin.math.sin
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
        // randomly generates a 0 or 1
        println(Random.nextBits(1))
        // generates a random non-negative Int value less than 256
        println(Random.nextBits(8))
        // generates a random Int value, may generate a negative value as well
        println(Random.nextBits(32))
    }

    @Sample
    fun nextBoolean() {
        val presents = listOf("Candy", "Balloon", "Ball")
        // a random partition, the result may be different every time
        val (alicePresents, bobPresents) = presents.partition { Random.nextBoolean() }

        println("Alice receives $alicePresents")
        println("Bob receives $bobPresents")
    }

    @Sample
    fun nextBytes() {
        val bytes = ByteArray(4)
        assertPrints(bytes.contentToString(), "[0, 0, 0, 0]")

        Random.nextBytes(bytes, 1, 3)
        // second and third bytes are generated, rest unchanged
        println(bytes.contentToString())

        Random.nextBytes(bytes)
        // all bytes are newly generated
        println(bytes.contentToString())

        val newBytes = Random.nextBytes(5)
        // a new byte array filled with random values
        println(newBytes.contentToString())
    }

    @Sample
    fun nextDouble() {
        if (Random.nextDouble() <= 0.3) {
            println("There was 30% possibility of rainy weather today and it is raining.")
        } else {
            println("There was 70% possibility of sunny weather today and the sun is shining.")
        }
    }

    @Sample
    fun nextDoubleFromUntil() {
        val firstAngle = Random.nextDouble(until = Math.PI / 6);
        assertTrue(sin(firstAngle) < 0.5)

        val secondAngle = Random.nextDouble(from = Math.PI / 6, until = Math.PI / 2)
        val sinValue = sin(secondAngle)
        assertTrue(sinValue >= 0.5 && sinValue < 1.0)
    }

    @Sample
    fun nextFloat() {
        if (Random.nextFloat() <= 0.3) {
            println("There was 30% possibility of rainy weather today and it is raining.")
        } else {
            println("There was 70% possibility of sunny weather today and the sun is shining.")
        }
    }

    @Sample
    fun nextInt() {
        val randomInts = List(5) { Random.nextInt() }
        println(randomInts)
        val sortedRandomInts = randomInts.sorted()
        println(sortedRandomInts)
    }

    @Sample
    fun nextIntFromUntil() {
        val menu = listOf("Omelette", "Porridge", "Cereal", "Chicken", "Pizza", "Pasta")
        val forBreakfast = Random.nextInt(until = 3).let { menu[it] }
        val forLunch = Random.nextInt(from = 3, until = 6).let { menu[it] }
        // new meals every time
        println("Today I want $forBreakfast for breakfast, and $forLunch for lunch.")
    }

    @Sample
    fun nextLong() {
        val randomLongs = List(5) { Random.nextLong() }
        println(randomLongs)
        val sortedRandomLongs = randomLongs.sorted()
        println(sortedRandomLongs)
    }

    @Sample
    fun nextLongFromUntil() {
        val fileSize = Random.nextLong(until = 1_099_511_627_776)
        println("A file of $fileSize bytes fits on a 1TB storage.")
        val long = Random.nextLong(Int.MAX_VALUE + 1L, Long.MAX_VALUE)
        println("Number $long doesn't fit in an Int.")
    }
}
