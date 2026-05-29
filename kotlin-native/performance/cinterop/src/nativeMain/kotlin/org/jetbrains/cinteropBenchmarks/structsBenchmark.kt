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

package org.jetbrains.structsBenchmarks

import kotlinx.benchmark.*
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.math.sqrt
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly
import org.jetbrains.structsProducedByMacrosBenchmarks.*

private const val BENCHMARK_SIZE = 1000

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class CinteropHideName : SkipWhenBaseOnly() {
    @Benchmark
    fun macros(bh: Blackhole) {
        skipWhenBaseOnly()
        memScoped {
            val ints = new_list_int()
            for (i in 1..BENCHMARK_SIZE / 10) {
                list_push_front_int(ints, i)
            }
            val floats = new_list_float()
            // Copy integer list to float one.
            ints?.pointed?.apply {
                var current = _first
                while(current != null) {
                    list_push_front_float(floats, current.pointed._data.toFloat())
                    current = current.pointed._next
                }
            }
            // Reverse list.
            var previous: CPointer<list_elem_float>? = null
            var current = floats?.pointed?._first
            while (current != null) {
                val next = current.pointed._next
                current.pointed._next = previous
                previous = current
                current = next
            }
            floats?.pointed?._first = previous
            free_list_int(ints)
            bh.consume(list_front_float(floats))
            free_list_float(floats)
        }
    }

    @Benchmark
    fun struct(bh: Blackhole) {
        skipWhenBaseOnly()
        memScoped {
            val containsFunction = staticCFunction<CPointer<ElementS>?, CPointer<ElementS>?, Int> { first, second ->
                if (first == null || second == null) {
                    0
                } else if (first.pointed.string.toKString().contains(second.pointed.string.toKString())) {
                    1
                }
                else {
                    0
                }
            }
            val elementsList = mutableListOf<ElementS>()
            // Fill list.
            for (i in 1..BENCHMARK_SIZE) {
                val element = alloc<ElementS>()
                element.floatValue = i + sqrt(i.toDouble()).toFloat()
                element.integer = i.toLong()
                sprintf(element.string, "%d", i)
                element.contains = containsFunction

                elementsList.add(element)
            }
            val summary = elementsList.map { multiplyElementS(it.readValue(), (0..10).random()) }
                    .reduce { acc, it -> sumElementSPtr(acc.ptr, it.ptr)!!.pointed.readValue() }
            val intValue = summary.useContents { integer }
            bh.consume(elementsList.last().contains!!(elementsList.last().ptr, elementsList.first().ptr))
        }
    }

    @Benchmark
    fun union(bh: Blackhole) {
        skipWhenBaseOnly()
        memScoped {
            val elementsList = mutableListOf<ElementU>()
            // Fill list.
            for (i in 1..BENCHMARK_SIZE) {
                val element = alloc<ElementU>()
                element.integer = i.toLong()
                elementsList.add(element)
            }
            elementsList.forEach {
                it.floatValue = it.integer + sqrt(it.integer.toDouble()).toFloat()
            }
            val summary = elementsList.map { multiplyElementU(it.readValue(), (0..10).random()) }
                    .reduce { acc, it -> sumElementUPtr(acc.ptr, it.ptr)!!.pointed.readValue() }
            bh.consume(summary.useContents { integer })
        }
    }

    @Benchmark
    fun enum(bh: Blackhole) {
        skipWhenBaseOnly()
        val days = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val enumValues = mutableListOf<WeekDay>()
        for (i in 1..BENCHMARK_SIZE) {
            enumValues.add(getWeekDay(days[(0..6).random()]))
        }
        val weekEnds = enumValues.count { isWeekEnd(it) == 1 }
        bh.consume(weekEnds)
    }
}
