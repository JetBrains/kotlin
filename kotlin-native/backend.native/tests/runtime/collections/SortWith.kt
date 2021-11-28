/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.collections.SortWith

import kotlin.test.*

val correctIncreasing = Comparator<Int> { a, b ->  // correct one.
    when {
        a > b -> 1
        a < b -> -1
        else -> 0
    }
}

val correctDecreasing = Comparator<Int> { a, b ->  // correct one.
    when {
        a < b -> 1
        a > b -> -1
        else -> 0
    }
}

fun Array<Int>.assertSorted(cmp: Comparator<Int>, message: String = "") {
    for (i in 1 until size) {
        assertTrue(cmp.compare(this[i - 1], this[i]) <= 0, message)
    }
}

fun Array<MyComparable>.assertSorted(message: String = "") {
    for (i in 1 until size) {
        assertTrue(this[i - 1] <= this[i], message)
    }
}

data class ComparatorInfo(val name: String, val comparator: Comparator<Int>, val isCorrect: Boolean)

class MyComparable (val value: Int, val comparator: Comparator<Int>): Comparable<MyComparable> {
    override fun compareTo(other: MyComparable): Int = comparator.compare(value, other.value)
}

// Assert that the array is sorted in terms of a comparator only for correct/partially correct cases
val comparators = listOf<ComparatorInfo>(
    ComparatorInfo("Correct increasing", correctIncreasing ,  true),
    ComparatorInfo("Correct decreasing", correctDecreasing ,  true),
    ComparatorInfo("Incorrect increasing", Comparator { a ,b -> if (a > b) 1 else -1 },  false),
    ComparatorInfo("Incorrect decreasing", Comparator { a ,b -> if (a < b) 1 else -1 },  false),
    ComparatorInfo("Always 1",  Comparator { a ,b ->  1  },  false),
    ComparatorInfo("Always -1", Comparator { a ,b ->  -1 },  false),
    ComparatorInfo("Always 0",  Comparator { a ,b ->  0  },  false)
)

val arrays = listOf<Array<Int>>(
    arrayOf(),
    arrayOf(1),
    arrayOf(Int.MIN_VALUE, 0, Int.MAX_VALUE),
    arrayOf(Int.MAX_VALUE, 0, Int.MIN_VALUE),
    arrayOf(1, 2, 3),
    arrayOf(-2, -1, 99, 1, 2),
    arrayOf(90, 91, 0, 98, 99),
    arrayOf(2, 1, 99, -1, 2),
    arrayOf(99, 98, 0, 91, 90),
    arrayOf(42, 42, 42),
    arrayOf(99, 42, 0, 42, 50),
    arrayOf(
        100000, 190001, 200002, 200003, 200004, 210005, 220006, 250007, 300008, 310009, 360010, 365011,
        380012, 390013, 390014, 399015, 400016, 400017, 400018, 400019, 400020, 400021, 400022, 400023,
        400024, 400025, 400026, 450027, 450028, 480029, 480030, 500031, 500032, 500033, 500034, 500035,
        500036, 500037, 500038, 500039, 500040, 500041, 500042, 500043, 500044, 500045, 500046, 500047,
        500048, 500049, 500050, 500051, 500052, 500053, 505054, 510055, 510056, 510057, 510058, 510059,
        510060, 510061, 510062, 510063, 410064, 410065, 511066, 511067, 520068, 520069, 420070, 520071,
        530072, 530073, 530074, 430075, 430076, 530077, 540078, 540079, 540080, 540081, 540082, 540083,
        540084, 490085, 540086, 540087, 542088, 544089, 546090, 550091, 550092, 550093, 550094, 590095,
        590096, 595097, 600098, 600099, 600100, 600101, 600102, 600103, 600104, 550105, 600106, 600107,
        600108, 600109, 600110, 610111, 610112, 610113, 620114, 620115, 620116, 620117, 620118, 620119,
        640120, 640121, 645122, 645123, 645124, 645125, 645126, 645127, 650128, 700129, 700130
    )
)


@Test fun runTest() {
    arrays.forEach { array ->
        comparators.forEach {
            // Test with custom comparator
            val arrayUnderTest = array.copyOf()
            arrayUnderTest.sortWith(it.comparator)
            if (it.isCorrect) {
                arrayUnderTest.assertSorted(it.comparator, """
                    Assert sorted failed for comparator: "${it.name}"
                    Array: ${array.joinToString()}
                    Array after sorting: ${arrayUnderTest.joinToString()}
                """.trimIndent())
            }

            // Test of a custom comparable
            val comparableArrayUnderTest = Array(array.size) { i ->
                MyComparable(array[i], it.comparator)
            }
            comparableArrayUnderTest.sort()

            if (it.isCorrect) {
                comparableArrayUnderTest.assertSorted("""
                    Assert sorted failed for Comparable: "${it.name}"
                    Array: ${array.joinToString()}
                    Array after sorting: ${comparableArrayUnderTest.joinToString()}
                """.trimIndent())
            }
        }
    }
}
