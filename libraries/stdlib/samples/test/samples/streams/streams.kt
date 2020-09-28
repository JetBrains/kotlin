/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.streams

import samples.*
import java.util.stream.*
import kotlin.streams.*

class Streams {

    @Sample
    fun streamAsSequence() {
        val stringStream: Stream<String> = Stream.of("Never", "gonna", "give", "you", "up")
        val stringSequence: Sequence<String> = stringStream.asSequence()
        assertPrints(stringSequence.joinToString(" "), "Never gonna give you up")
    }

    @Sample
    fun intStreamAsSequence() {
        val intStream: IntStream = IntStream.of(5, 6, 7)
        val intSequence: Sequence<Int> = intStream.asSequence()
        assertPrints(intSequence.joinToString(", "), "5, 6, 7")
    }

    @Sample
    fun longStreamAsSequence() {
        val longStream: LongStream = LongStream.of(5_000_000_000, 6_000_000_000, 7_000_000_000)
        val longSequence: Sequence<Long> = longStream.asSequence()
        assertPrints(longSequence.joinToString(", "), "5000000000, 6000000000, 7000000000")
    }

    @Sample
    fun doubleStreamAsSequence() {
        val doubleStream: DoubleStream = DoubleStream.of(1e2, 1e3, 1e4)
        val doubleSequence: Sequence<Double> = doubleStream.asSequence()
        assertPrints(doubleSequence.joinToString(", "), "100.0, 1000.0, 10000.0")
    }

    @Sample
    fun sequenceAsStream() {
        val evenNumbersSequence: Sequence<Int> = generateSequence(2, { it + 2 }).take(5)
        val evenNumberStream: Stream<Int> = evenNumbersSequence.asStream()
        assertPrints(evenNumberStream.toList(), "[2, 4, 6, 8, 10]")
    }

    @Sample
    fun streamToList() {
        val stringStream: Stream<String> = Stream.of("Lion", "Leopard", "Jaguar", "Tiger")
        val stringList: List<String> = stringStream.toList()
        assertPrints(stringList, "[Lion, Leopard, Jaguar, Tiger]")
    }

    @Sample
    fun intStreamToList() {
        val intStream: IntStream = IntStream.of(10, 20, 30)
        val intList: List<Int> = intStream.toList()
        assertPrints(intList, "[10, 20, 30]")
    }

    @Sample
    fun longStreamToList() {
        val longStream: LongStream = LongStream.of(3_000_000_000, 4_000_000_000, 5_000_000_000)
        val longList: List<Long> = longStream.toList()
        assertPrints(longList, "[3000000000, 4000000000, 5000000000]")
    }

    @Sample
    fun doubleStreamToList() {
        val doubleStream: DoubleStream = DoubleStream.of(1e2, 1e3, 1e4)
        val doubleList: List<Double> = doubleStream.toList()
        assertPrints(doubleList, "[100.0, 1000.0, 10000.0]")
    }
}