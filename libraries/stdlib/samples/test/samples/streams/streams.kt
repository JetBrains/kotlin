/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.streams

import samples.Sample
import samples.assertPrints
import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream
import kotlin.streams.asSequence
import kotlin.streams.asStream
import kotlin.streams.toList

class Streams {

    @Sample
    fun streamAsSequence() {
        val stringStream = Stream.of("Never", "gonna", "give", "you", "up")
        val stringSequence = stringStream.asSequence()
        assertPrints(stringSequence.joinToString(" "), "Never gonna give you up")
    }

    @Sample
    fun intStreamAsSequence() {
        val intStream = IntStream.of(5, 6, 7)
        val intSequence = intStream.asSequence()
        assertPrints(intSequence.joinToString(", "), "5, 6, 7")
    }

    @Sample
    fun longStreamAsSequence() {
        val longStream = LongStream.of(5_000_000_000, 6_000_000_000, 7_000_000_000)
        val longSequence = longStream.asSequence()
        assertPrints(longSequence.joinToString(", "), "5000000000, 6000000000, 7000000000")
    }

    @Sample
    fun doubleStreamAsSequence() {
        val doubleStream = DoubleStream.of(1e2, 1e3, 1e4)
        val doubleSequence = doubleStream.asSequence()
        assertPrints(doubleSequence.joinToString(", "), "100.0, 1000.0, 10000.0")
    }

    @Sample
    fun sequenceAsStream() {
        val evenNumbersSequence = generateSequence(2, { it + 2 }).take(5)
        val evenNumberStream = evenNumbersSequence.asStream()
        assertPrints(evenNumberStream.toList(), "[2, 4, 6, 8, 10]")
    }

    @Sample
    fun streamToList() {
        val stringStream = Stream.of("Lion", "Leopard", "Jaguar", "Tiger")
        val stringList = stringStream.toList()
        assertPrints(stringList, "[Lion, Leopard, Jaguar, Tiger]")
    }

    @Sample
    fun intStreamToList() {
        val intStream = IntStream.of(10, 20, 30)
        val intList = intStream.toList()
        assertPrints(intList, "[10, 20, 30]")
    }

    @Sample
    fun longStreamToList() {
        val longStream = LongStream.of(3_000_000_000, 4_000_000_000, 5_000_000_000)
        val longList = longStream.toList()
        assertPrints(longList, "[3000000000, 4000000000, 5000000000]")
    }

    @Sample
    fun doubleStreamToList() {
        val doubleStream = DoubleStream.of(1e2, 1e3, 1e4)
        val doubleList = doubleStream.toList()
        assertPrints(doubleList, "[100.0, 1000.0, 10000.0]")
    }
}