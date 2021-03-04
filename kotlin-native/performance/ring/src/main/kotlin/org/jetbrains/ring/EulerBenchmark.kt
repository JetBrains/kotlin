/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.ring

fun fibonacci(): Sequence<Int> {
    var a = 0
    var b = 1
    fun next(): Int {
        val res = a + b
        a = b
        b = res
        return res
    }
    return generateSequence { next() }
}

fun Any.isPalindrome() = toString() == toString().reversed()

inline fun IntRange.sum(predicate: (Int) -> Boolean): Int {
    var sum = 0
    for (i in this) if (predicate(i)) sum += i
    return sum
}

inline fun Sequence<Int>.sum(predicate: (Int) -> Boolean): Int {
    var sum = 0
    for (i in this) if (predicate(i)) sum += i
    return sum
}

/**
 * A class tests decisions of various Euler problems
 *
 * NB: all tests here work slower than Java, probably because of all these functional wrappers
 */
open class EulerBenchmark {

    //Benchmark
    fun problem1bySequence() = (1..BENCHMARK_SIZE).asSequence().sum( { it % 3 == 0 || it % 5 == 0} )
    
    //Benchmark
    fun problem1() = (1..BENCHMARK_SIZE).sum( { it % 3 == 0 || it % 5 == 0} )
    
    //Benchmark
    fun problem2() = fibonacci().takeWhile { it < BENCHMARK_SIZE }.sum { it % 2 == 0 }
    
    //Benchmark
    fun problem4(): Long {
        val s: Long = BENCHMARK_SIZE.toLong()
        val maxLimit = (s-1)*(s-1)
        val minLimit = (s/10)*(s/10)
        val maxDiv = BENCHMARK_SIZE-1
        val minDiv = BENCHMARK_SIZE/10
        for (i in maxLimit downTo minLimit) {
            if (!i.isPalindrome()) continue;
            for (j in minDiv..maxDiv) {
                if (i % j == 0L) {
                    val res = i / j
                    if (res in minDiv.toLong()..maxDiv.toLong()) {
                        return i
                    }
                }
            }
        }
        return -1
    }

    private val veryLongNumber = """
        73167176531330624919225119674426574742355349194934
        96983520312774506326239578318016984801869478851843
        85861560789112949495459501737958331952853208805511
        12540698747158523863050715693290963295227443043557
        66896648950445244523161731856403098711121722383113
        62229893423380308135336276614282806444486645238749
        30358907296290491560440772390713810515859307960866
        70172427121883998797908792274921901699720888093776
        65727333001053367881220235421809751254540594752243
        52584907711670556013604839586446706324415722155397
        53697817977846174064955149290862569321978468622482
        83972241375657056057490261407972968652414535100474
        82166370484403199890008895243450658541227588666881
        16427171479924442928230863465674813919123162824586
        17866458359124566529476545682848912883142607690042
        24219022671055626321111109370544217506941658960408
        07198403850962455444362981230987879927244284909188
        84580156166097919133875499200524063689912560717606
        05886116467109405077541002256983155200055935729725
        71636269561882670428252483600823257530420752963450
    """

    
    //Benchmark
    fun problem8(): Long {
        val productSize = when(BENCHMARK_SIZE) {
            in 1..10 -> 4
            in 11..1000 -> 8
            else -> 13
        }
        val digits: MutableList<Int> = ArrayList()
        for (digit in veryLongNumber) {
            if (digit in '0'..'9') {
                digits.add(digit.toInt() - '0'.toInt())
            }
        }
        var largest = 0L
        for (i in 0..digits.size -productSize-1) {
            var product = 1L
            for (j in 0..productSize-1) {
                product *= digits[i+j]
            }
            if (product > largest) largest = product
        }
        return largest
    }

    
    //Benchmark
    fun problem9(): Long {
        val BENCHMARK_SIZE = BENCHMARK_SIZE // Looks awful but removes all implicit getSize() calls
        for (c in BENCHMARK_SIZE/3..BENCHMARK_SIZE-3) {
            val c2 = c.toLong() * c.toLong()
            for (b in (BENCHMARK_SIZE-c)/2..c-1) {
                if (b+c >= BENCHMARK_SIZE)
                    break
                val a = BENCHMARK_SIZE - b - c
                if (a >= b)
                    continue
                val b2 = b.toLong() * b.toLong()
                val a2 = a.toLong() * a.toLong()
                if (c2 == b2 + a2) {
                    return a.toLong() * b.toLong() * c.toLong()
                }
            }
        }
        return -1L
    }

    data class Children(val left: Int, val right: Int)

    //Benchmark
    fun problem14(): List<Int> {
        // Simplified problem is solved here: it's not allowed to leave the interval [0..BENCHMARK_SIZE) inside a number chain
        val BENCHMARK_SIZE = BENCHMARK_SIZE
        // Build a tree
        // index is produced from first & second
        val tree = Array(BENCHMARK_SIZE, { i -> Children(i*2, if (i>4 && (i+2) % 6 == 0) (i-1)/3 else 0)})
        // Find longest chain by DFS
        fun dfs(begin: Int): List<Int> {
            if (begin == 0 || begin >= BENCHMARK_SIZE)
                return listOf()
            val left = dfs(tree[begin].left)
            val right = dfs(tree[begin].right)
            return listOf(begin) + if (left.size > right.size) left else right
        }
        return dfs(1)
    }

    data class Way(val length: Int, val next: Int)

    
    //Benchmark
    fun problem14full(): List<Int> {
        val BENCHMARK_SIZE = BENCHMARK_SIZE
        // Previous achievements: map (number) -> (length, next)
        val map: MutableMap<Int, Way> = HashMap()
        // Starting point
        map.put(1, Way(0, 0))
        // Check all other numbers
        var bestNum = 0
        var bestLen = 0
        fun go(begin: Int): Way {
            val res = map[begin]
            if (res != null)
                return res
            val next = if (begin % 2 == 0) begin/2 else 3*begin+1
            val childRes = go(next)
            val myRes = Way(childRes.length + 1, next)
            map[begin] = myRes
            return myRes
        }
        for (i in 2..BENCHMARK_SIZE-1) {
            val res = go(i)
            if (res.length > bestLen) {
                bestLen = res.length
                bestNum = i
            }
        }
        fun unroll(begin: Int): List<Int> {
            if (begin == 0)
                return listOf()
            val next = map[begin]?.next ?: 0
            return listOf(begin) + unroll(next)
        }
        return unroll(bestNum)
    }
}
