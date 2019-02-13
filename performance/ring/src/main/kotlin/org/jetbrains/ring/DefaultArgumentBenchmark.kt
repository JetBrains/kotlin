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

/**
 * Created by Mikhail.Glukhikh on 10/03/2015.
 *
 * Tests performance for function calls with default parameters
 */
open class DefaultArgumentBenchmark {
    private var arg = 0

    init {
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
