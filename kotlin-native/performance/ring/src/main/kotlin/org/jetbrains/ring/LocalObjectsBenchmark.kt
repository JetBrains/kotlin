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

open class LocalObjectsBenchmark {
    //Benchmark
    fun localArray(): Int {
        val size = 48
        val array = IntArray(size)
        for (i in 1..size) {
            array[i - 1] = i * 2
        }
        var result = 0
        for (i in 0 until size) {
            result += array[i]
        }
        if (result > 10) {
            return 1
        }
        return 2
    }
}
