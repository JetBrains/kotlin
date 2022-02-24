/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformArraysTest {

    @Test
    fun testConstructors() {
        assertPrints(PlatformIntArray(3) { index -> pli(-index) }.contentToString(), "[0, -1, -2]")
        assertPrints(PlatformIntArray(3).contentToString(), "[0, 0, 0]")
        assertPrints(PlatformUIntArray(3).contentToString(), "[0, 0, 0]")
    }

    @Test
    fun testGetSetSigned() {
        val signedArray = PlatformIntArray(1)
        assertPrints(signedArray.contentToString(), "[0]")
        signedArray[0] = pli(-1)
        assertPrints(signedArray.contentToString(), "[-1]")
        assertPrints(signedArray[0], "-1")
    }

    @Test
    fun testGetSetUnsigned() {
        val signedArray = PlatformUIntArray(1)
        assertPrints(signedArray.contentToString(), "[0]")
        signedArray[0] = plui(1u)
        assertPrints(signedArray.contentToString(), "[1]")
        assertPrints(signedArray[0], "1")
    }

    @Test
    fun testSize() {
        assertPrints(PlatformIntArray(3).size, "3")
        assertPrints(PlatformUIntArray(3).size, "3")
    }

    @Test
    fun testIterator() {
        val signedArrayIterator = PlatformIntArray(3).iterator()
        while (signedArrayIterator.hasNext()) {
            assertPrints(signedArrayIterator.next(), "0")
        }

        val unsignedArrayIterator = PlatformUIntArray(3).iterator()
        while (unsignedArrayIterator.hasNext()) {
            assertPrints(unsignedArrayIterator.next(), "0")
        }
    }

    @Test
    fun testAll() {
        assertPrints(PlatformIntArray(2).all { it == pli(0) }, "true")
        assertPrints(PlatformUIntArray(2).all { it == plui(0u) }, "true")
    }

    @Test
    fun testAny() {
        assertPrints(PlatformIntArray(1).any(), "true")
        assertPrints(PlatformUIntArray(1).any(), "true")
        assertPrints(PlatformIntArray(1).any { it > pli(0) }, "false")
        assertPrints(PlatformUIntArray(1).any { it > plui(0u) }, "false")
    }

    @Test
    fun testAsIterable() {
        assertPrints(PlatformIntArray(0).asIterable().iterator().hasNext(), "false")
        assertPrints(PlatformUIntArray(0).asIterable().iterator().hasNext(), "false")
    }

    @Test
    fun testAsList() {
        assertPrints(PlatformIntArray(3).asList().subList(0, 2), "[0, 0]")
        assertPrints(PlatformUIntArray(3).asList().subList(0, 2), "[0, 0]")
    }

    @Test
    fun testAsSequence() {
        assertPrints(PlatformIntArray(3).asSequence().take(2).joinToString(), "0, 0")
        assertPrints(PlatformUIntArray(3).asSequence().take(2).joinToString(), "0, 0")
    }

    @Test
    fun testAssociate() {
        assertPrints(PlatformIntArray(1).associate { it.dec() to it.inc() }, "{-1=1}")
        assertPrints(PlatformUIntArray(1).associate { it.dec() to it.inc() }, "{${PlatformUInt.MAX_VALUE}=1}")
    }

    @Test
    fun testAssociateBy() {
        assertPrints(PlatformIntArray(1).associateBy { it.toString() }, "{0=0}")
        assertPrints(PlatformUIntArray(1).associateBy { it.toString() }, "{0=0}")
        assertPrints(PlatformIntArray(1).associateBy({ it.toString() }, { it.toFloat() }), "{0=0.0}")
        assertPrints(PlatformIntArray(1).associateBy({ it.toString() }, { it.toFloat() }), "{0=0.0}")
    }

    @Test
    fun testAssociateByTo() {
        assertPrints(PlatformIntArray(1).associateByTo(mutableMapOf()) { it.toString() }, "{0=0}")
        assertPrints(PlatformUIntArray(1).associateByTo(mutableMapOf()) { it.toString() }, "{0=0}")
        assertPrints(PlatformIntArray(1).associateByTo(mutableMapOf(), { it.toString() }, { it.toFloat() }), "{0=0.0}")
        assertPrints(PlatformUIntArray(1).associateByTo(mutableMapOf(), { it.toString() }, { it.toFloat() }), "{0=0.0}")
    }

    @Test
    fun testAssociateTo() {
        assertPrints(PlatformIntArray(1).associateTo(mutableMapOf()) { it.dec() to it.inc() }, "{-1=1}")
        assertPrints(PlatformUIntArray(1).associateTo(mutableMapOf()) { it.dec() to it.inc() }, "{${PlatformUInt.MAX_VALUE}=1}")
    }

    @Test
    fun testAssociateWith() {
        assertPrints(PlatformIntArray(1).associateWith { it.toFloat() }, "{0=0.0}")
        assertPrints(PlatformUIntArray(1).associateWith { it.toFloat() }, "{0=0.0}")
    }

    @Test
    fun testAssociateWithTo() {
        assertPrints(PlatformIntArray(1).associateWithTo(mutableMapOf()) { it.toFloat() }, "{0=0.0}")
        assertPrints(PlatformUIntArray(1).associateWithTo(mutableMapOf()) { it.toFloat() }, "{0=0.0}")
    }

    @Test
    fun testAverage() {
        assertPrints(PlatformIntArray(3) { pli(it) }.average(), "1.0")
    }

    @Test
    fun testComponentN() {
        val (c1, c2, c3, c4, c5) = PlatformIntArray(5) { pli(it) }
        assertPrints("$c1 $c2 $c3 $c4 $c5", "0 1 2 3 4")
        val (c6, c7, c8, c9, c10) = PlatformUIntArray(5)
        assertPrints("$c6 $c7 $c8 $c9 $c10", "0 0 0 0 0")
    }

    @Test
    fun testContains() {
        assertPrints(PlatformIntArray(1).contains(pli(0)), "true")
        assertPrints(PlatformUIntArray(1).contains(plui(1u)), "false")
    }

    @Test
    fun testContentEquals() {
        assertPrints(PlatformIntArray(1).contentEquals(PlatformIntArray(1)), "true")
        assertPrints(PlatformUIntArray(1).contentEquals(PlatformUIntArray(1)), "true")
    }

    @Test
    fun testContentHashCode() {
        assertPrints(PlatformIntArray(0).contentHashCode() == 0, "false")
        assertPrints(PlatformUIntArray(0).contentHashCode() == 0, "false")
    }

    @Test
    fun testContentToString() {
        assertPrints(PlatformIntArray(1).contentToString(), "[0]")
        assertPrints(PlatformUIntArray(1).contentToString(), "[0]")
    }

    @Test
    fun testCopyInto() {
        val from = PlatformIntArray(4) { pli(it.inc()) }
        val to = PlatformIntArray(5)
        from.copyInto(to, destinationOffset = 1, startIndex = 1, endIndex = from.size - 1)
        assertPrints(to.contentToString(), "[0, 2, 3, 0, 0]")

        val fromUnsigned = PlatformUIntArray(4)
        for (i in 0 until fromUnsigned.size) {
            fromUnsigned[i] = plui(i.inc().toUInt())
        }
        val toUnsigned = PlatformUIntArray(5)
        fromUnsigned.copyInto(toUnsigned, destinationOffset = 1, startIndex = 1, endIndex = from.size - 1)
        assertPrints(to.contentToString(), "[0, 2, 3, 0, 0]")
    }

    @Test
    fun testCopyOf() {
        assertPrints(PlatformIntArray(3).copyOf().contentToString(), "[0, 0, 0]")
        assertPrints(PlatformUIntArray(3).copyOf().contentToString(), "[0, 0, 0]")
        assertPrints(PlatformIntArray(3).copyOf(newSize = 2).contentToString(), "[0, 0]")
        assertPrints(PlatformUIntArray(3).copyOf(newSize = 2).contentToString(), "[0, 0]")
    }

    @Test
    fun testCopyOfRange() {
        assertPrints(PlatformIntArray(3) { pli(it) }.copyOfRange(1, 3).contentToString(), "[1, 2]")
        assertPrints(PlatformUIntArray(3).copyOfRange(1, 3).contentToString(), "[0, 0]")
    }

    @Test
    fun testCount() {
        assertPrints(PlatformIntArray(3).count(), "3")
        assertPrints(PlatformUIntArray(3).count(), "3")
        assertPrints(PlatformIntArray(3) { pli(it) }.count { it > pli(0) }, "2")
        assertPrints(PlatformUIntArray(3).count { it > plui(0u) }, "0")
    }

    @Test
    fun testDistinct() {
        assertPrints(PlatformIntArray(3).distinct(), "[0]")
        assertPrints(PlatformUIntArray(3).distinct(), "[0]")
    }

    @Test
    fun testDistinctBy() {
        assertPrints(PlatformIntArray(3) { pli(it) }.distinctBy { it % pli(2) }, "[0, 1]")
        assertPrints(PlatformUIntArray(3).distinctBy { it % plui(2u) }, "[0]")
    }

    @Test
    fun testDrop() {
        assertPrints(PlatformIntArray(3) { pli(it) }.drop(1), "[1, 2]")
        assertPrints(PlatformUIntArray(3).drop(1), "[0, 0]")
    }

    @Test
    fun testDropLast() {
        assertPrints(PlatformIntArray(3) { pli(it) }.dropLast(1), "[0, 1]")
        assertPrints(PlatformUIntArray(3).drop(1), "[0, 0]")
    }

    @Test
    fun testDropLastWhile() {
        assertPrints(PlatformIntArray(3) { pli(it) }.dropLastWhile { it > pli(0) }, "[0]")
        assertPrints(PlatformUIntArray(3).dropLastWhile { it == plui(0u) }, "[]")
    }

    @Test
    fun testDropWhile() {
        assertPrints(PlatformIntArray(3) { pli(it) }.dropWhile { it < pli(2) }, "[2]")
        assertPrints(PlatformUIntArray(3).dropLastWhile { it == plui(0u) }, "[]")
    }

    @Test
    fun testElementAt() {
        assertPrints(PlatformIntArray(3) { pli(it) }.elementAt(1), "1")
        assertPrints(PlatformUIntArray(3).elementAt(1), "0")
    }

    @Test
    fun testElementAtOrElse() {
        assertPrints(PlatformIntArray(3) { pli(it) }.elementAtOrElse(1) { pli(42) }, "1")
        assertPrints(PlatformUIntArray(3).elementAtOrElse(3) { plui(42u) }, "42")
    }

    @Test
    fun testElementAtOrNull() {
        assertPrints(PlatformIntArray(3) { pli(it) }.elementAtOrNull(1), "1")
        assertPrints(PlatformUIntArray(3).elementAtOrNull(3), "null")
    }

    @Test
    fun testFill() {
        PlatformIntArray(3).let { array ->
            array.fill(pli(1))
            assertPrints(array.contentToString(), "[1, 1, 1]")
        }

        PlatformUIntArray(3).let { array ->
            array.fill(plui(1u))
            assertPrints(array.contentToString(), "[1, 1, 1]")
        }
    }

    @Test
    fun testFilter() {
        assertPrints(PlatformIntArray(3) { pli(it) }.filter { it != pli(1) }, "[0, 2]")
        assertPrints(PlatformUIntArray(3).filter { it != plui(0u) }, "[]")
    }

    @Test
    fun testFilterIndexed() {
        assertPrints(PlatformIntArray(3) { pli(it) }.filterIndexed { index, _ -> index != 0 }, "[1, 2]")
        assertPrints(PlatformUIntArray(3).filterIndexed { index, it -> it != plui(index.toUInt()) }, "[0, 0]")
    }

    @Test
    fun testFilterIndexedTo() {
        assertPrints(PlatformIntArray(3) { pli(it) }.filterIndexedTo(mutableListOf()) { index, _ -> index != 0 }, "[1, 2]")
        assertPrints(PlatformUIntArray(3).filterIndexedTo(mutableListOf()) { index, it -> it != plui(index.toUInt()) }, "[0, 0]")
    }

    @Test
    fun testFilterNot() {
        assertPrints(PlatformIntArray(3) { pli(it) }.filterNot { it == pli(0) }, "[1, 2]")
        assertPrints(PlatformUIntArray(3).filterNot { it == plui(0u) }, "[]")
    }

    @Test
    fun testFilterNotTo() {
        assertPrints(PlatformIntArray(3) { pli(it) }.filterNotTo(mutableListOf()) { it == pli(0) }, "[1, 2]")
        assertPrints(PlatformUIntArray(3).filterNotTo(mutableListOf()) { it == plui(0u) }, "[]")
    }

    @Test
    fun testFilterTo() {
        assertPrints(PlatformIntArray(3) { pli(it) }.filterTo(mutableListOf()) { it == pli(0) }, "[0]")
        assertPrints(PlatformUIntArray(3).filterTo(mutableListOf()) { it == plui(0u) }, "[0, 0, 0]")
    }

    @Test
    fun testFind() {
        assertPrints(PlatformIntArray(3) { pli(it) }.find { it % pli(2) == pli(0) }, "0")
        assertPrints(PlatformUIntArray(3).find { it == plui(1u) }, "null")
    }

    @Test
    fun testFindLast() {
        assertPrints(PlatformIntArray(3) { pli(it) }.findLast { it % pli(2) == pli(0) }, "2")
        assertPrints(PlatformUIntArray(3).findLast { it == plui(1u) }, "null")
    }

    @Test
    fun testFirst() {
        assertPrints(PlatformIntArray(3) { pli(it) }.first(), "0")
        assertPrints(PlatformUIntArray(3).first(), "0")
        assertPrints(PlatformIntArray(3) { pli(it) }.first { it > pli(0) }, "1")
        assertPrints(PlatformUIntArray(3).first { it == plui(0u) }, "0")
    }

    @Test
    fun testFirstOrNull() {
        assertPrints(PlatformIntArray(3) { pli(it) }.firstOrNull(), "0")
        assertPrints(PlatformUIntArray(0).firstOrNull(), "null")
        assertPrints(PlatformIntArray(3) { pli(it) }.firstOrNull { it > pli(0) }, "1")
        assertPrints(PlatformUIntArray(3).firstOrNull { it > plui(0u) }, "null")
    }

    @Test
    fun testFlatMap() {
        assertPrints(PlatformIntArray(3) { pli(it) }.flatMap { listOf(it) }, "[0, 1, 2]")
        assertPrints(PlatformUIntArray(3).flatMap { listOf(it) }, "[0, 0, 0]")
    }

    @Test
    fun testFlatMapIndexed() {
        assertPrints(PlatformIntArray(3) { pli(it) }.flatMapIndexed { index, it -> listOf(index, it) }, "[0, 0, 1, 1, 2, 2]")
        assertPrints(PlatformUIntArray(3).flatMapIndexed { index, it -> listOf(index, it) }, "[0, 0, 1, 0, 2, 0]")
    }

    @Test
    fun testFlatMapIndexedTo() {
        assertPrints(
            PlatformIntArray(3) { pli(it) }.flatMapIndexedTo(mutableListOf()) { index, it -> listOf(index, it) },
            "[0, 0, 1, 1, 2, 2]"
        )
        assertPrints(PlatformUIntArray(3).flatMapIndexedTo(mutableListOf()) { index, it -> listOf(index, it) }, "[0, 0, 1, 0, 2, 0]")
    }

    @Test
    fun testFlatMapTo() {
        assertPrints(PlatformIntArray(3) { pli(it) }.flatMapTo(mutableListOf()) { listOf(it) }, "[0, 1, 2]")
        assertPrints(PlatformUIntArray(3).flatMapTo(mutableListOf()) { listOf(it) }, "[0, 0, 0]")
    }

    @Test
    fun testFold() {
        assertPrints(PlatformIntArray(3) { pli(it) }.fold(pli(0)) { acc, it -> acc + it }, "3")
        assertPrints(PlatformUIntArray(3).fold(plui(0u)) { acc, it -> acc + it }, "0")
    }

    @Test
    fun testFoldIndexed() {
        assertPrints(PlatformIntArray(3) { pli(it) }.foldIndexed(pli(0)) { index, acc, it -> acc + it + pli(index) }, "6")
        assertPrints(PlatformUIntArray(3).foldIndexed(plui(0u)) { index, acc, it -> acc + it + plui(index.toUInt()) }, "3")
    }

    @Test
    fun testFoldRight() {
        assertPrints(PlatformIntArray(3) { pli(it) }.foldRight(pli(0)) { acc, it -> acc + it }, "3")
        assertPrints(PlatformUIntArray(3).foldRight(plui(0u)) { acc, it -> acc + it }, "0")
    }

    @Test
    fun testFoldRightIndexed() {
        assertPrints(PlatformIntArray(3) { pli(it) }.foldRightIndexed(pli(0)) { index, acc, it -> acc + it + pli(index) }, "6")
        assertPrints(PlatformUIntArray(3).foldRightIndexed(plui(0u)) { index, acc, it -> acc + it + plui(index.toUInt()) }, "3")
    }

    @Test
    fun testForEach() {
        var s = pli(0)
        PlatformIntArray(3) { pli(it) }.forEach { s += it }
        assertPrints(s, "3")

        var u = plui(0u)
        PlatformUIntArray(3).forEach { u += it }
        assertPrints(u, "0")
    }

    @Test
    fun testForEachIndexed() {
        var s = pli(0)
        PlatformIntArray(3) { pli(it) }.forEachIndexed { index, it -> s += it + pli(index) }
        assertPrints(s, "6")

        var u = plui(0u)
        PlatformUIntArray(3).forEachIndexed { index, it -> u += it + plui(index.toUInt()) }
        assertPrints(u, "3")
    }

    @Test
    fun testGetOrElse() {
        assertPrints(PlatformIntArray(3) { pli(it) }.getOrElse(2) { pli(42) }, "2")
        assertPrints(PlatformUIntArray(3).getOrElse(3) { plui(42u) }, "42")
    }

    @Test
    fun testGetOrNull() {
        assertPrints(PlatformIntArray(3) { pli(it) }.getOrNull(2), "2")
        assertPrints(PlatformUIntArray(3).getOrNull(3), "null")
    }

    @Test
    fun testGroupBy() {
        assertPrints(PlatformIntArray(3) { pli(it) }.groupBy { it % pli(2) }, "{0=[0, 2], 1=[1]}")
        assertPrints(PlatformUIntArray(3).groupBy { it }, "{0=[0, 0, 0]}")
        assertPrints(PlatformIntArray(3) { pli(it) }.groupBy({ it % pli(2) }, { it.toDouble() }), "{0=[0.0, 2.0], 1=[1.0]}")
        assertPrints(PlatformUIntArray(3).groupBy({ it }, { it.toDouble() }), "{0=[0.0, 0.0, 0.0]}")
    }


    @Test
    fun testGroupByTo() {
        assertPrints(PlatformIntArray(3) { pli(it) }.groupByTo(mutableMapOf()) { it % pli(2) }, "{0=[0, 2], 1=[1]}")
        assertPrints(PlatformUIntArray(3).groupByTo(mutableMapOf()) { it }, "{0=[0, 0, 0]}")
        assertPrints(
            PlatformIntArray(3) { pli(it) }.groupByTo(mutableMapOf(), { it % pli(2) }, { it.toDouble() }),
            "{0=[0.0, 2.0], 1=[1.0]}"
        )
        assertPrints(PlatformUIntArray(3).groupByTo(mutableMapOf(), { it }, { it.toDouble() }), "{0=[0.0, 0.0, 0.0]}")
    }

    @Test
    fun testIndexOf() {
        assertPrints(PlatformIntArray(3) { pli(it) }.indexOf(pli(1)), "1")
        assertPrints(PlatformUIntArray(3).indexOf(plui(0u)), "0")
    }

    @Test
    fun testIndexOfFirst() {
        assertPrints(PlatformIntArray(3) { pli(it) }.indexOfFirst { it == (pli(1)) }, "1")
        assertPrints(PlatformUIntArray(3).indexOfFirst { it == plui(0u) }, "0")
    }

    @Test
    fun testIndexOfLast() {
        assertPrints(PlatformIntArray(3) { pli(it) }.indexOfLast { it == (pli(1)) }, "1")
        assertPrints(PlatformUIntArray(3).indexOfLast { it == plui(0u) }, "2")
    }

    @Test
    fun testIndices() {
        assertPrints(PlatformIntArray(3).indices, "0..2")
        assertPrints(PlatformUIntArray(3).indices, "0..2")
    }

    @Test
    fun testIntersect() {
        assertPrints(PlatformIntArray(3) { pli(it) }.intersect(listOf(pli(2), pli(3))).sorted(), "[2]")
        assertPrints(PlatformUIntArray(3).intersect(listOf(plui(0u), plui(1u))).sorted(), "[0]")
    }

    @Test
    fun testIsEmpty() {
        assertPrints(PlatformIntArray(3).isEmpty(), "false")
        assertPrints(PlatformUIntArray(0).isEmpty(), "true")
    }

    @Test
    fun testIsNotEmpty() {
        assertPrints(PlatformIntArray(3).isNotEmpty(), "true")
        assertPrints(PlatformUIntArray(0).isNotEmpty(), "false")
    }

    @Test
    fun testJoinTo() {
        assertPrints(PlatformIntArray(3) { pli(it) }.joinTo(StringBuilder()), "0, 1, 2")
        assertPrints(PlatformUIntArray(3).joinTo(StringBuilder()), "0, 0, 0")
    }

    @Test
    fun testJoinToString() {
        assertPrints(PlatformIntArray(3) { pli(it) }.joinToString(), "0, 1, 2")
        assertPrints(PlatformUIntArray(3).joinToString(), "0, 0, 0")
    }

    @Test
    fun testLast() {
        assertPrints(PlatformIntArray(3) { pli(it) }.last(), "2")
        assertPrints(PlatformUIntArray(3).last(), "0")
        assertPrints(PlatformIntArray(3) { pli(it) }.last { it % pli(2) == pli(1) }, "1")
        assertPrints(PlatformUIntArray(3).last { it == plui(0u) }, "0")
    }

    @Test
    fun testLastIndex() {
        assertPrints(PlatformIntArray(3).lastIndex, "2")
        assertPrints(PlatformUIntArray(3).lastIndex, "2")
    }

    @Test
    fun testLastIndexOf() {
        assertPrints(PlatformIntArray(3) { pli(it) }.lastIndexOf(pli(1)), "1")
        assertPrints(PlatformUIntArray(3).lastIndexOf(plui(0u)), "2")
    }

    @Test
    fun testLastOrNull() {
        assertPrints(PlatformIntArray(3) { pli(it) }.lastOrNull(), "2")
        assertPrints(PlatformUIntArray(0).lastOrNull(), "null")
        assertPrints(PlatformIntArray(3) { pli(it) }.lastOrNull { it > pli(5) }, "null")
        assertPrints(PlatformUIntArray(3).lastOrNull { it == plui(0u) }, "0")
    }

    @Test
    fun testMap() {
        assertPrints(PlatformIntArray(3) { pli(it) }.map { it.toDouble() }, "[0.0, 1.0, 2.0]")
        assertPrints(PlatformUIntArray(3).map { it.toDouble() }, "[0.0, 0.0, 0.0]")
    }

    @Test
    fun testMapIndexed() {
        assertPrints(PlatformIntArray(3) { pli(it) }.mapIndexed { index, it -> "$index-$it" }, "[0-0, 1-1, 2-2]")
        assertPrints(PlatformUIntArray(3).mapIndexed { index, it -> "$index-$it" }, "[0-0, 1-0, 2-0]")
    }

    @Test
    fun testMapIndexedTo() {
        assertPrints(PlatformIntArray(3) { pli(it) }.mapIndexedTo(mutableListOf()) { index, it -> "$index-$it" }, "[0-0, 1-1, 2-2]")
        assertPrints(PlatformUIntArray(3).mapIndexedTo(mutableListOf()) { index, it -> "$index-$it" }, "[0-0, 1-0, 2-0]")
    }

    @Test
    fun testMapTo() {
        assertPrints(PlatformIntArray(3) { pli(it) }.mapTo(mutableListOf()) { it.toDouble() }, "[0.0, 1.0, 2.0]")
        assertPrints(PlatformUIntArray(3).mapTo(mutableListOf()) { it.toDouble() }, "[0.0, 0.0, 0.0]")
    }

    @Test
    fun testMaxByOrNull() {
        assertPrints(PlatformIntArray(3) { pli(it) }.maxByOrNull { it.inv() }, "0")
        assertPrints(PlatformIntArray(0).maxByOrNull { it }, "null")
    }

    @Test
    fun testMaxOf() {
        assertPrints(PlatformIntArray(3) { pli(it) }.maxOf { it.toDouble() }, "2.0")
        assertPrints(PlatformIntArray(3) { pli(it) }.maxOf { it.toFloat() }, "2.0")
        assertPrints(PlatformIntArray(3) { pli(it) }.maxOf { "s$it" }, "s2")
        assertPrints(PlatformUIntArray(3).maxOf { it.toDouble() }, "0.0")
        assertPrints(PlatformUIntArray(3).maxOf { it.toFloat() }, "0.0")
        assertPrints(PlatformUIntArray(3).maxOf { "s$it" }, "s0")
    }

    @Test
    fun testMaxOfOrNull() {
        assertPrints(PlatformIntArray(3) { pli(it) }.maxOfOrNull { it.toDouble() }, "2.0")
        assertPrints(PlatformIntArray(3) { pli(it) }.maxOfOrNull { it.toFloat() }, "2.0")
        assertPrints(PlatformIntArray(3) { pli(it) }.maxOfOrNull { "s$it" }, "s2")
        assertPrints(PlatformUIntArray(0).maxOfOrNull { it.toDouble() }, "null")
        assertPrints(PlatformUIntArray(0).maxOfOrNull { it.toFloat() }, "null")
        assertPrints(PlatformUIntArray(0).maxOfOrNull { "s$it" }, "null")
    }

    @Test
    fun testMaxOfWith() {
        assertPrints(PlatformIntArray(3) { pli(it) }.maxOfWith({ a, b -> b.compareTo(a) }, { it.toDouble() }), "0.0")
        assertPrints(PlatformIntArray(3) { pli(it) }.maxOfWith({ a, b -> b.compareTo(a) }, { it.toFloat() }), "0.0")
        assertPrints(PlatformIntArray(3) { pli(it) }.maxOfWith({ a, b -> b.compareTo(a) }, { "s$it" }), "s0")
        assertPrints(PlatformUIntArray(3).maxOfWith({ a, b -> b.compareTo(a) }, { it.toDouble() }), "0.0")
        assertPrints(PlatformUIntArray(3).maxOfWith({ a, b -> b.compareTo(a) }, { it.toFloat() }), "0.0")
        assertPrints(PlatformUIntArray(3).maxOfWith({ a, b -> b.compareTo(a) }, { "s$it" }), "s0")
    }

    @Test
    fun testMaxOfWithOrNull() {
        assertPrints(PlatformIntArray(3) { pli(it) }.maxOfWithOrNull({ a, b -> b.compareTo(a) }, { it.toDouble() }), "0.0")
        assertPrints(PlatformIntArray(3) { pli(it) }.maxOfWithOrNull({ a, b -> b.compareTo(a) }, { it.toFloat() }), "0.0")
        assertPrints(PlatformIntArray(3) { pli(it) }.maxOfWithOrNull({ a, b -> b.compareTo(a) }, { "s$it" }), "s0")
        assertPrints(PlatformUIntArray(0).maxOfWithOrNull({ a, b -> b.compareTo(a) }, { it.toDouble() }), "null")
        assertPrints(PlatformUIntArray(0).maxOfWithOrNull({ a, b -> b.compareTo(a) }, { it.toFloat() }), "null")
        assertPrints(PlatformUIntArray(0).maxOfWithOrNull({ a, b -> b.compareTo(a) }, { "s$it" }), "null")
    }

    @Test
    fun testMaxOrNull() {
        assertPrints(PlatformIntArray(3) { pli(it) }.maxOrNull(), "2")
        assertPrints(PlatformUIntArray(0).maxOrNull(), "null")
    }

    @Test
    fun testMaxWithOrNull() {
        assertPrints(PlatformIntArray(3) { pli(it) }.maxWithOrNull { a, b -> b.compareTo(a) }, "0")
        assertPrints(PlatformUIntArray(0).maxWithOrNull { a, b -> b.compareTo(a) }, "null")
    }

    @Test
    fun testMinByOrNull() {
        assertPrints(PlatformIntArray(3) { pli(it) }.minByOrNull { it.inv() }, "2")
        assertPrints(PlatformIntArray(0).minByOrNull { it }, "null")
    }

    @Test
    fun testMinOf() {
        assertPrints(PlatformIntArray(3) { pli(it) }.minOf { it.toDouble() }, "0.0")
        assertPrints(PlatformIntArray(3) { pli(it) }.minOf { it.toFloat() }, "0.0")
        assertPrints(PlatformIntArray(3) { pli(it) }.minOf { "s$it" }, "s0")
        assertPrints(PlatformUIntArray(3).minOf { it.toDouble() }, "0.0")
        assertPrints(PlatformUIntArray(3).minOf { it.toFloat() }, "0.0")
        assertPrints(PlatformUIntArray(3).minOf { "s$it" }, "s0")
    }

    @Test
    fun testMinOfOrNull() {
        assertPrints(PlatformIntArray(3) { pli(it) }.minOfOrNull { it.toDouble() }, "0.0")
        assertPrints(PlatformIntArray(3) { pli(it) }.minOfOrNull { it.toFloat() }, "0.0")
        assertPrints(PlatformIntArray(3) { pli(it) }.minOfOrNull { "s$it" }, "s0")
        assertPrints(PlatformUIntArray(0).minOfOrNull { it.toDouble() }, "null")
        assertPrints(PlatformUIntArray(0).minOfOrNull { it.toFloat() }, "null")
        assertPrints(PlatformUIntArray(0).minOfOrNull { "s$it" }, "null")
    }

    @Test
    fun testMinOfWith() {
        assertPrints(PlatformIntArray(3) { pli(it) }.minOfWith({ a, b -> b.compareTo(a) }, { it.toDouble() }), "2.0")
        assertPrints(PlatformIntArray(3) { pli(it) }.minOfWith({ a, b -> b.compareTo(a) }, { it.toFloat() }), "2.0")
        assertPrints(PlatformIntArray(3) { pli(it) }.minOfWith({ a, b -> b.compareTo(a) }, { "s$it" }), "s2")
        assertPrints(PlatformUIntArray(3).minOfWith({ a, b -> b.compareTo(a) }, { it.toDouble() }), "0.0")
        assertPrints(PlatformUIntArray(3).minOfWith({ a, b -> b.compareTo(a) }, { it.toFloat() }), "0.0")
        assertPrints(PlatformUIntArray(3).minOfWith({ a, b -> b.compareTo(a) }, { "s$it" }), "s0")
    }

    @Test
    fun testMinOfWithOrNull() {
        assertPrints(PlatformIntArray(3) { pli(it) }.minOfWithOrNull({ a, b -> b.compareTo(a) }, { it.toDouble() }), "2.0")
        assertPrints(PlatformIntArray(3) { pli(it) }.minOfWithOrNull({ a, b -> b.compareTo(a) }, { it.toFloat() }), "2.0")
        assertPrints(PlatformIntArray(3) { pli(it) }.minOfWithOrNull({ a, b -> b.compareTo(a) }, { "s$it" }), "s2")
        assertPrints(PlatformUIntArray(0).minOfWithOrNull({ a, b -> b.compareTo(a) }, { it.toDouble() }), "null")
        assertPrints(PlatformUIntArray(0).minOfWithOrNull({ a, b -> b.compareTo(a) }, { it.toFloat() }), "null")
        assertPrints(PlatformUIntArray(0).minOfWithOrNull({ a, b -> b.compareTo(a) }, { "s$it" }), "null")
    }

    @Test
    fun testMinOrNull() {
        assertPrints(PlatformIntArray(3) { pli(it) }.minOrNull(), "0")
        assertPrints(PlatformUIntArray(0).minOrNull(), "null")
    }

    @Test
    fun testMinWithOrNull() {
        assertPrints(PlatformIntArray(3) { pli(it) }.minWithOrNull { a, b -> b.compareTo(a) }, "2")
        assertPrints(PlatformUIntArray(0).minWithOrNull { a, b -> b.compareTo(a) }, "null")
    }

    @Test
    fun testNone() {
        assertPrints(PlatformIntArray(3).none(), "false")
        assertPrints(PlatformUIntArray(0).none(), "true")
        assertPrints(PlatformIntArray(3) { pli(it) }.none { it > pli(0) }, "false")
        assertPrints(PlatformUIntArray(0).none { it > plui(0u) }, "true")
    }

    @Test
    fun testOnEach() {
        var counter = 0
        assertPrints(PlatformIntArray(3).onEach { counter++ }.contentToString() to counter, "([0, 0, 0], 3)")
        assertPrints(PlatformUIntArray(3).onEach { counter++ }.contentToString() to counter, "([0, 0, 0], 6)")
    }

    @Test
    fun testOnEachIndexed() {
        var counter = 0
        assertPrints(PlatformIntArray(3).onEachIndexed { index, _ -> counter += index }.contentToString() to counter, "([0, 0, 0], 3)")
        assertPrints(PlatformUIntArray(3).onEachIndexed { index, _ -> counter += index }.contentToString() to counter, "([0, 0, 0], 6)")
    }

    @Test
    fun testPartition() {
        assertPrints(PlatformIntArray(3) { pli(it) }.partition { it % pli(2) == pli(0) }, "([0, 2], [1])")
        assertPrints(PlatformUIntArray(3).partition { it % plui(2u) == plui(0u) }, "([0, 0, 0], [])")
    }

    @Test
    fun testPlus() {
        assertPrints((PlatformIntArray(3) { pli(it) } + pli(3)).contentToString(), "[0, 1, 2, 3]")
        assertPrints((PlatformIntArray(3) { pli(it) } + listOf(pli(3), pli(4))).contentToString(), "[0, 1, 2, 3, 4]")
        assertPrints((PlatformIntArray(3) { pli(it) } + PlatformIntArray(1)).contentToString(), "[0, 1, 2, 0]")
        assertPrints((PlatformUIntArray(3) + plui(1u)).contentToString(), "[0, 0, 0, 1]")
        assertPrints((PlatformUIntArray(3) + listOf(plui(1u), plui(1u))).contentToString(), "[0, 0, 0, 1, 1]")
        assertPrints((PlatformUIntArray(3) + PlatformUIntArray(1)).contentToString(), "[0, 0, 0, 0]")
    }

    @Test
    fun testRandom() {
        assertPrints(PlatformIntArray(3).random(), "0")
        assertPrints(PlatformUIntArray(3).random(), "0")
        assertPrints(PlatformIntArray(3).random(Random(42)), "0")
        assertPrints(PlatformUIntArray(3).random(Random(42)), "0")
    }

    @Test
    fun testRandomOrNull() {
        assertPrints(PlatformIntArray(3).randomOrNull(), "0")
        assertPrints(PlatformUIntArray(3).randomOrNull(), "0")
        assertPrints(PlatformIntArray(3).randomOrNull(Random(42)), "0")
        assertPrints(PlatformUIntArray(3).randomOrNull(Random(42)), "0")
        assertPrints(PlatformIntArray(0).randomOrNull(), "null")
        assertPrints(PlatformUIntArray(0).randomOrNull(), "null")
        assertPrints(PlatformIntArray(0).randomOrNull(Random(42)), "null")
        assertPrints(PlatformUIntArray(0).randomOrNull(Random(42)), "null")
    }

    @Test
    fun testReduce() {
        assertPrints(PlatformIntArray(3) { pli(it) }.reduce { acc, next -> acc + next }, "3")
        assertPrints(PlatformUIntArray(3).reduce { acc, next -> acc + next }, "0")
    }

    @Test
    fun testReduceIndexed() {
        assertPrints(PlatformIntArray(3) { pli(it) }.reduceIndexed { index, acc, next -> acc + next + pli(index) }, "6")
        assertPrints(PlatformUIntArray(3).reduceIndexed { index, acc, next -> acc + next + plui(index.toUInt()) }, "3")
    }

    @Test
    fun testReduceIndexedOrNull() {
        assertPrints(PlatformIntArray(3) { pli(it) }.reduceIndexedOrNull { index, acc, next -> acc + next + pli(index) }, "6")
        assertPrints(PlatformUIntArray(3).reduceIndexedOrNull { index, acc, next -> acc + next + plui(index.toUInt()) }, "3")
        assertPrints(PlatformIntArray(0).reduceIndexedOrNull { index, acc, next -> acc + next + pli(index) }, "null")
        assertPrints(PlatformUIntArray(0).reduceIndexedOrNull { index, acc, next -> acc + next + plui(index.toUInt()) }, "null")
    }

    @Test
    fun testReduceOrNull() {
        assertPrints(PlatformIntArray(3) { pli(it) }.reduceOrNull { acc, next -> acc + next }, "3")
        assertPrints(PlatformUIntArray(3).reduceOrNull { acc, next -> acc + next }, "0")
        assertPrints(PlatformIntArray(0) { pli(it) }.reduceOrNull { acc, next -> acc + next }, "null")
        assertPrints(PlatformUIntArray(0).reduceOrNull { acc, next -> acc + next }, "null")
    }

    @Test
    fun testReduceRight() {
        assertPrints(PlatformIntArray(3) { pli(it) }.reduceRight { acc, next -> acc + next }, "3")
        assertPrints(PlatformUIntArray(3).reduceRight { acc, next -> acc + next }, "0")
    }

    @Test
    fun testReduceRightIndexed() {
        assertPrints(PlatformIntArray(3) { pli(it) }.reduceRightIndexed { index, acc, next -> acc + next + pli(index) }, "4")
        assertPrints(PlatformUIntArray(3).reduceRightIndexed { index, acc, next -> acc + next + plui(index.toUInt()) }, "1")
    }

    @Test
    fun testReduceRightIndexedOrNull() {
        assertPrints(PlatformIntArray(3) { pli(it) }.reduceRightIndexedOrNull { index, acc, next -> acc + next + pli(index) }, "4")
        assertPrints(PlatformUIntArray(3).reduceRightIndexedOrNull { index, acc, next -> acc + next + plui(index.toUInt()) }, "1")
        assertPrints(PlatformIntArray(0).reduceRightIndexedOrNull { index, acc, next -> acc + next + pli(index) }, "null")
        assertPrints(PlatformUIntArray(0).reduceRightIndexedOrNull { index, acc, next -> acc + next + plui(index.toUInt()) }, "null")
    }

    @Test
    fun testReduceRightOrNull() {
        assertPrints(PlatformIntArray(3) { pli(it) }.reduceRightOrNull { acc, next -> acc + next }, "3")
        assertPrints(PlatformUIntArray(3).reduceRightOrNull { acc, next -> acc + next }, "0")
        assertPrints(PlatformIntArray(0) { pli(it) }.reduceRightOrNull { acc, next -> acc + next }, "null")
        assertPrints(PlatformUIntArray(0).reduceRightOrNull { acc, next -> acc + next }, "null")
    }

    @Test
    fun testReverse() {
        assertPrints(PlatformIntArray(3) { pli(it) }.let { it.reverse(); it.contentToString() }, "[2, 1, 0]")
        assertPrints(PlatformUIntArray(3).let { it[0] = plui(1u); it.reverse(); it.contentToString() }, "[0, 0, 1]")
    }

    @Test
    fun testReversed() {
        assertPrints(PlatformIntArray(3) { pli(it) }.reversed(), "[2, 1, 0]")
        assertPrints(PlatformUIntArray(3).let { it[0] = plui(1u); it.reversed() }, "[0, 0, 1]")
    }

    @Test
    fun testReversedArray() {
        assertPrints(PlatformIntArray(3) { pli(it) }.reversedArray().contentToString(), "[2, 1, 0]")
        assertPrints(PlatformUIntArray(3).let { it[0] = plui(1u); it.reversedArray().contentToString() }, "[0, 0, 1]")
    }

    @Test
    fun testRunningFold() {
        assertPrints(PlatformIntArray(3) { pli(it) }.runningFold(1) { acc, next -> acc + next.toInt() }, "[1, 1, 2, 4]")
        assertPrints(PlatformUIntArray(3).runningFold(1) { acc, next -> acc + next.toInt() }, "[1, 1, 1, 1]")
    }

    @Test
    fun testRunningFoldIndexed() {
        assertPrints(
            PlatformIntArray(3) { pli(it) }.runningFoldIndexed(1) { index, acc, next -> acc + index + next.toInt() },
            "[1, 1, 3, 7]"
        )
        assertPrints(
            PlatformUIntArray(3).runningFoldIndexed(1) { index, acc, next -> acc + index + next.toInt() },
            "[1, 1, 2, 4]"
        )
    }

    @Test
    fun testRunningReduce() {
        assertPrints(PlatformIntArray(3) { pli(it) }.runningReduce { acc, next -> acc + next }, "[0, 1, 3]")
        assertPrints(PlatformUIntArray(3).runningReduce { acc, next -> acc + next }, "[0, 0, 0]")
    }

    @Test
    fun testRunningReduceIndexed() {
        assertPrints(PlatformIntArray(3) { pli(it) }.runningReduceIndexed { index, acc, next -> acc + next + pli(index) }, "[0, 2, 6]")
        assertPrints(PlatformUIntArray(3).runningReduceIndexed { index, acc, next -> acc + next + plui(index.toUInt()) }, "[0, 1, 3]")
    }

    @Test
    fun testScan() {
        assertPrints(PlatformIntArray(3) { pli(it) }.runningFold(1) { acc, next -> acc + next.toInt() }, "[1, 1, 2, 4]")
        assertPrints(PlatformUIntArray(3).runningFold(1) { acc, next -> acc + next.toInt() }, "[1, 1, 1, 1]")
    }

    @Test
    fun testScanIndexed() {
        assertPrints(
            PlatformIntArray(3) { pli(it) }.runningFoldIndexed(1) { index, acc, next -> acc + index + next.toInt() },
            "[1, 1, 3, 7]"
        )
        assertPrints(
            PlatformUIntArray(3).runningFoldIndexed(1) { index, acc, next -> acc + index + next.toInt() },
            "[1, 1, 2, 4]"
        )
    }

    @Test
    fun testShuffle() {
        assertPrints(PlatformIntArray(3).also { it.shuffle() }.contentToString(), "[0, 0, 0]")
        assertPrints(PlatformUIntArray(3).also { it.shuffle() }.contentToString(), "[0, 0, 0]")
        assertPrints(PlatformIntArray(3).also { it.shuffle(Random(42)) }.contentToString(), "[0, 0, 0]")
        assertPrints(PlatformUIntArray(3).also { it.shuffle(Random(42)) }.contentToString(), "[0, 0, 0]")
    }

    @Test
    fun testSingle() {
        assertPrints(PlatformIntArray(1).single(), "0")
        assertPrints(PlatformUIntArray(1).single(), "0")
    }

    @Test
    fun testSingleOrNull() {
        assertPrints(PlatformIntArray(1).singleOrNull(), "0")
        assertPrints(PlatformUIntArray(1).singleOrNull(), "0")
        assertPrints(PlatformIntArray(0).singleOrNull(), "null")
        assertPrints(PlatformUIntArray(0).singleOrNull(), "null")
        assertPrints(PlatformIntArray(2).singleOrNull(), "null")
        assertPrints(PlatformUIntArray(2).singleOrNull(), "null")
    }

    @Test
    fun testSlice() {
        assertPrints(PlatformIntArray(3) { pli(it) }.slice(0..1), "[0, 1]")
        assertPrints(PlatformUIntArray(3).slice(0..1), "[0, 0]")
        assertPrints(PlatformIntArray(3) { pli(it) }.slice(listOf(0, 2)), "[0, 2]")
        assertPrints(PlatformUIntArray(3).slice(listOf(0, 2)), "[0, 0]")
    }

    @Test
    fun testSliceArray() {
        assertPrints(PlatformIntArray(3) { pli(it) }.sliceArray(0..1).contentToString(), "[0, 1]")
        assertPrints(PlatformUIntArray(3).sliceArray(0..1).contentToString(), "[0, 0]")
        assertPrints(PlatformIntArray(3) { pli(it) }.sliceArray(listOf(0, 2)).contentToString(), "[0, 2]")
        assertPrints(PlatformUIntArray(3).sliceArray(listOf(0, 2)).contentToString(), "[0, 0]")
    }

    @Test
    fun testSort() {
        assertPrints(PlatformIntArray(3) { pli(-it) }.also { it.sort() }.contentToString(), "[-2, -1, 0]")
        assertPrints(PlatformUIntArray(3).also { it[0] = plui(1u); it.sort() }.contentToString(), "[0, 0, 1]")
    }

    @Test
    fun testSortDescending() {
        assertPrints(PlatformIntArray(3) { pli(it) }.also { it.sortDescending() }.contentToString(), "[2, 1, 0]")
        assertPrints(PlatformUIntArray(3).also { it[0] = plui(1u); it.sortDescending() }.contentToString(), "[1, 0, 0]")
    }

    @Test
    fun testSorted() {
        assertPrints(PlatformIntArray(3) { pli(-it) }.sorted(), "[-2, -1, 0]")
        assertPrints(PlatformUIntArray(3).also { it[0] = plui(1u) }.sorted(), "[0, 0, 1]")
    }

    @Test
    fun testSortedArray() {
        assertPrints(PlatformIntArray(3) { pli(-it) }.sortedArray().contentToString(), "[-2, -1, 0]")
        assertPrints(PlatformUIntArray(3).also { it[0] = plui(1u) }.sortedArray().contentToString(), "[0, 0, 1]")
    }

    @Test
    fun testSortedArrayDescending() {
        assertPrints(PlatformIntArray(3) { pli(it) }.sortedArrayDescending().contentToString(), "[2, 1, 0]")
        assertPrints(PlatformUIntArray(3).also { it[0] = plui(1u) }.sortedArrayDescending().contentToString(), "[1, 0, 0]")
    }

    @Test
    fun testSortedBy() {
        assertPrints(PlatformIntArray(3) { pli(it) }.sortedBy { -it }, "[2, 1, 0]")
        assertPrints(PlatformUIntArray(3).also { it[0] = plui(1u) }.sortedBy { it.toString() }, "[0, 0, 1]")
    }

    @Test
    fun testSortedByDescending() {
        assertPrints(PlatformIntArray(3) { pli(it) }.sortedByDescending { it }, "[2, 1, 0]")
        assertPrints(PlatformUIntArray(3).also { it[0] = plui(1u) }.sortedByDescending { it.toString() }, "[1, 0, 0]")
    }

    @Test
    fun testSortedDescending() {
        assertPrints(PlatformIntArray(3) { pli(it) }.sortedDescending(), "[2, 1, 0]")
        assertPrints(PlatformUIntArray(3).also { it[0] = plui(1u) }.sortedDescending(), "[1, 0, 0]")
    }

    @Test
    fun testSortedWith() {
        assertPrints(PlatformIntArray(3) { pli(it) }.sortedWith { a, b -> b.compareTo(a) }, "[2, 1, 0]")
        assertPrints(PlatformUIntArray(3).also { it[0] = plui(1u) }.sortedWith { a, b -> b.compareTo(a) }, "[1, 0, 0]")
    }

    @Test
    fun testSubtract() {
        assertPrints(PlatformIntArray(3) { pli(it) }.subtract(listOf(pli(1))).sorted(), "[0, 2]")
        assertPrints(PlatformUIntArray(3).subtract(listOf(plui(0u))), "[]")
    }

    @Test
    fun testSum() {
        assertPrints(PlatformIntArray(3) { pli(it) }.sum(), "3")
        assertPrints(PlatformUIntArray(3).sum(), "0")
    }

    @Test
    fun testSumBy() {
        assertPrints(PlatformIntArray(3) { pli(it) }.sumBy { it.inc().toInt() }, "6")
        assertPrints(PlatformUIntArray(3).sumBy { it.inc().toUInt() }, "3")
    }

    @Test
    fun testSumByDouble() {
        assertPrints(PlatformIntArray(3) { pli(it) }.sumByDouble { it.inc().toDouble() }, "6.0")
        assertPrints(PlatformUIntArray(3).sumByDouble { it.inc().toDouble() }, "3.0")
    }

    @Test
    fun testSumOf() {
        assertPrints(PlatformIntArray(3) { pli(it) }.sumOf { it.toInt() }, "3")
        assertPrints(PlatformIntArray(3) { pli(it) }.sumOf { it.toLong() }, "3")
        assertPrints(PlatformIntArray(3) { pli(it) }.sumOf { it.toUInt() }, "3")
        assertPrints(PlatformIntArray(3) { pli(it) }.sumOf { it.toULong() }, "3")
        assertPrints(PlatformIntArray(3) { pli(it) }.sumOf { it.toDouble() }, "3.0")
        assertPrints(PlatformUIntArray(3).sumOf { it.toInt() }, "0")
        assertPrints(PlatformUIntArray(3).sumOf { it.toLong() }, "0")
        assertPrints(PlatformUIntArray(3).sumOf { it.toUInt() }, "0")
        assertPrints(PlatformUIntArray(3).sumOf { it.toULong() }, "0")
        assertPrints(PlatformUIntArray(3).sumOf { it.toDouble() }, "0.0")
    }

    @Test
    fun testTake() {
        assertPrints(PlatformIntArray(3) { pli(it) }.take(2), "[0, 1]")
        assertPrints(PlatformUIntArray(3).take(2), "[0, 0]")
    }

    @Test
    fun testTakeLast() {
        assertPrints(PlatformIntArray(3) { pli(it) }.takeLast(2), "[1, 2]")
        assertPrints(PlatformUIntArray(3).takeLast(2), "[0, 0]")
    }

    @Test
    fun testTakeLastWhile() {
        assertPrints(PlatformIntArray(3) { pli(it) }.takeLastWhile { it > pli(0) }, "[1, 2]")
        assertPrints(PlatformUIntArray(3).takeLastWhile { it == plui(0u) }, "[0, 0, 0]")
    }

    @Test
    fun testTakeWhile() {
        assertPrints(PlatformIntArray(3) { pli(it) }.takeWhile { it > pli(0) }, "[]")
        assertPrints(PlatformUIntArray(3).takeWhile { it == plui(0u) }, "[0, 0, 0]")
    }

    @Test
    fun testToCollection() {
        assertPrints(PlatformIntArray(3) { pli(it) }.toCollection(mutableListOf()), "[0, 1, 2]")
        assertPrints(PlatformUIntArray(3).toCollection(mutableListOf()), "[0, 0, 0]")
    }

    @Test
    fun testToHashSet() {
        assertPrints(PlatformIntArray(3) { pli(it) }.toHashSet().sorted(), "[0, 1, 2]")
        assertPrints(PlatformUIntArray(3).toHashSet().sorted(), "[0]")
    }

    @Test
    fun testToList() {
        assertPrints(PlatformIntArray(3) { pli(it) }.toList(), "[0, 1, 2]")
        assertPrints(PlatformUIntArray(3).toList(), "[0, 0, 0]")
    }

    @Test
    fun testToMutableList() {
        assertPrints(PlatformIntArray(3) { pli(it) }.toMutableList(), "[0, 1, 2]")
        assertPrints(PlatformUIntArray(3).toMutableList(), "[0, 0, 0]")
    }

    @Test
    fun testToMutableSet() {
        assertPrints(PlatformIntArray(3) { pli(it) }.toMutableSet().sorted(), "[0, 1, 2]")
        assertPrints(PlatformUIntArray(3).toMutableSet().sorted(), "[0]")
    }

    @Test
    fun testToSet() {
        assertPrints(PlatformIntArray(3) { pli(it) }.toSet().sorted(), "[0, 1, 2]")
        assertPrints(PlatformUIntArray(3).toSet().sorted(), "[0]")
    }

    @Test
    fun testToTypedArray() {
        assertPrints(PlatformIntArray(3) { pli(it) }.toTypedArray().contentToString(), "[0, 1, 2]")
        assertPrints(PlatformUIntArray(3).toTypedArray().contentToString(), "[0, 0, 0]")
    }

    @Test
    fun testUnion() {
        assertPrints(PlatformIntArray(3) { pli(it) }.union(listOf(pli(2), pli(3))).sorted(), "[0, 1, 2, 3]")
        assertPrints(PlatformUIntArray(3).union(listOf(plui(0u), plui(1u))).sorted(), "[0, 1]")
    }

    @Test
    fun testWithIndex() {
        for ((index, value) in PlatformIntArray(3) { pli(it) }.withIndex()) {
            assertEquals(index, value.toInt())
        }
        for ((_, value) in PlatformUIntArray(3).withIndex()) {
            assertPrints(value, "0")
        }
    }

    @Test
    fun testZip() {
        assertPrints(PlatformIntArray(3) { pli(it) }.zip(arrayOf(0, 1)), "[(0, 0), (1, 1)]")
        assertPrints(PlatformIntArray(3) { pli(it) }.zip(arrayOf(0, 1)) { pi, i -> pi.toDouble() + i.toDouble() }, "[0.0, 2.0]")
        assertPrints(PlatformIntArray(3) { pli(it) }.let { it.zip(it) }, "[(0, 0), (1, 1), (2, 2)]")
        assertPrints(PlatformIntArray(3) { pli(it) }.let { it.zip(it) { pi1, pi2 -> pi1 + pi2 } }, "[0, 2, 4]")
        assertPrints(PlatformIntArray(3) { pli(it) }.zip(listOf(0, 1, 2, 3)), "[(0, 0), (1, 1), (2, 2)]")
        assertPrints(PlatformIntArray(3) { pli(it) }.zip(listOf(0, 1, 2, 3)) { pi, i -> pi.toString() + i.toString() }, "[00, 11, 22]")

        assertPrints(PlatformUIntArray(3).zip(arrayOf(0, 1)), "[(0, 0), (0, 1)]")
        assertPrints(PlatformUIntArray(3).zip(arrayOf(0, 1)) { pi, i -> pi.toDouble() + i.toDouble() }, "[0.0, 1.0]")
        assertPrints(PlatformUIntArray(3).let { it.zip(it) }, "[(0, 0), (0, 0), (0, 0)]")
        assertPrints(PlatformUIntArray(3).let { it.zip(it) { pi1, pi2 -> pi1 + pi2 } }, "[0, 0, 0]")
        assertPrints(PlatformUIntArray(3).zip(listOf(0, 1, 2, 3)), "[(0, 0), (0, 1), (0, 2)]")
        assertPrints(PlatformUIntArray(3).zip(listOf(0, 1, 2, 3)) { pi, i -> pi.toString() + i.toString() }, "[00, 01, 02]")
    }
}
