/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.jps.build

import org.jetbrains.jps.ModuleChunk
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class TeamcityStatisticsLogger {
    private val isOnTeamcity = System.getenv("TEAMCITY_VERSION") != null

    private val totalTime = AtomicLong()

    //NOTE: mostly copied from TeamCityBuildInfoPrinter
    private fun escapedChar(c: Char): Char {
        return when (c) {
            '\n' -> 'n'
            '\r' -> 'r'
            '\u0085' -> 'x' // next-line character
            '\u2028' -> 'l' // line-separator character
            '\u2029' -> 'p' // paragraph-separator character
            '|' -> '|'
            '\'' -> '\''
            '[' -> '['
            ']' -> ']'
            else -> 0.toChar()
        }
    }

    private fun escape(text: String): String {
        val escaped = StringBuilder()
        for (c in text.toCharArray()) {
            val escChar = escapedChar(c)
            if (escChar == 0.toChar()) {
                escaped.append(c)
            } else {
                escaped.append('|').append(escChar)
            }
        }

        return escaped.toString()
    }

    fun registerStatistic(moduleChunk: ModuleChunk, timeToCompileNs: Long) {
        if (!isOnTeamcity) return

        totalTime.addAndGet(timeToCompileNs)
        printPerChunkStatistics(moduleChunk, timeToCompileNs)
    }

    private fun printPerChunkStatistics(moduleChunk: ModuleChunk, timeToCompileNs: Long) {
        printStatisticMessage(
            "${KotlinBuilder.KOTLIN_BUILDER_NAME} for ${moduleChunk.presentableShortName} compilation time, ms",
            timeToCompileNs.nanosToMillis().toString()
        )
    }

    fun reportTotal() {
        if (!isOnTeamcity) return

        printStatisticMessage(
            "${KotlinBuilder.KOTLIN_BUILDER_NAME} total compilation time, ms",
            totalTime.get().nanosToMillis().toString()
        )
    }


    private fun printStatisticMessage(key: String, value: String) {
        println("##teamcity[buildStatisticValue key='${escape(key)}' value='${escape(value)}']")
    }

    private fun Long.nanosToMillis() = TimeUnit.NANOSECONDS.toMillis(this)
}