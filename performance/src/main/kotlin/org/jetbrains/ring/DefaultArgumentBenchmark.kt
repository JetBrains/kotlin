package org.jetbrains.ring

/**
 * Created by Mikhail.Glukhikh on 10/03/2015.
 *
 * Tests performance for function calls with default parameters
 */
open class DefaultArgumentBenchmark {
    private var arg = 0

    fun setup() {
        arg = Random.nextInt()
    }

    
    fun sumTwo(first: Int, second: Int = 0): Int {
        return first + second
    }

    
    fun sumFour(first: Int, second: Int = 0, third: Int = 1, fourth: Int = third): Int {
        return first + second + third + fourth
    }

    
    fun sumEight(first: Int, second: Int = 0, third: Int = 1, fourth: Int = third,
                 fifth: Int = fourth, sixth: Int = fifth, seventh: Int = second, eighth: Int = seventh): Int {
        return first + second + third + fourth + fifth + sixth + seventh + eighth
    }

    
    //Benchmark
    fun testOneOfTwo() {
        sumTwo(arg)
    }

    
    //Benchmark
    fun testTwoOfTwo() {
        sumTwo(arg, arg)
    }
    
    //Benchmark
    fun testOneOfFour() {
        sumFour(arg)
    }

    
    //Benchmark
    fun testFourOfFour() {
        sumFour(arg, arg, arg, arg)
    }

    
    //Benchmark
    fun testOneOfEight() {
        sumEight(arg)
    }

    
    //Benchmark
    fun testEightOfEight() {
        sumEight(arg, arg, arg, arg, arg, arg, arg, arg)
    }
}
