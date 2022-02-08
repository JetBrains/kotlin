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

import org.jetbrains.benchmarksLauncher.Blackhole
import org.jetbrains.benchmarksLauncher.Random

open class ElvisBenchmark {

    class Value(var value: Int)

    var array : Array<Value?> = arrayOf()

    init {
        array = Array(BENCHMARK_SIZE) {
            if (Random.nextInt(BENCHMARK_SIZE) < BENCHMARK_SIZE / 10) null else Value(Random.nextInt())
        }
    }

    //Benchmark
    fun testElvis() {
        for (obj in array) {
            Blackhole.consume(obj?.value ?: 0)
        }
    }

    class Composite(val x : Int, val y : Composite?)

    fun check(a : Composite?) : Int {
        return a?.y?.x ?: (a?.x ?: 3)
    }

    fun testCompositeElvis(): Int {
        var result = 0
        for (i in 0..BENCHMARK_SIZE)
            result += check(Composite(Random.nextInt(), Composite(Random.nextInt(), null)))
        return result
    }
}
