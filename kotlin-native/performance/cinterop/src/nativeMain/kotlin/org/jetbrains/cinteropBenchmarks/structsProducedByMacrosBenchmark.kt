/*
 * Copyright 2010-2019 JetBrains s.r.o.
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

package org.jetbrains.structsProducedByMacrosBenchmarks

import kotlinx.benchmark.Blackhole
import kotlinx.cinterop.*

private const val BENCHMARK_SIZE = 10000

actual fun macrosBenchmark(bh: Blackhole) {
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
