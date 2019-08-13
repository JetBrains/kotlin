/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.performance

data class IntervalData(val intervalStart: Double, val intervalEnd: Double, val count: Int)

class IntervalCounter(
        private val minPower: Int,
        private val maxPower: Int,
        private val exponent: Double,
        val data: Array<Int> = Array(maxPower - minPower, { 0 })
) {

    fun register(value: Long) {
        val log = roundedLog(value)
        val bucket = Math.min(maxPower - 1, Math.max(minPower, log.toInt())) - minPower
        data[bucket] += 1
    }

    fun intervals(): List<IntervalData> {
        return data.indices.map { interval(it) }
    }

    private fun interval(index: Int): IntervalData {
        val start = Math.pow(exponent, minPower + index.toDouble())
        val end = Math.pow(exponent, minPower + (index + 1).toDouble())

        val count = data[index]

        return IntervalData(start, end, count)
    }

    private fun roundedLog(time: Long): Double {
        val dTime = time.toDouble()
        return (Math.log(dTime) / Math.log(exponent))
    }

}