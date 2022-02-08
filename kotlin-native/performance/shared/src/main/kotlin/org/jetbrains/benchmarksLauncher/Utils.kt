/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.benchmarksLauncher

expect fun writeToFile(fileName: String, text: String)

expect fun assert(value: Boolean)

expect inline fun measureNanoTime(block: () -> Unit): Long

expect fun cleanup()

expect fun printStderr(message: String)

expect fun currentTime(): String

expect fun nanoTime(): Long

expect class Blackhole {
    companion object {
        var consumer: Int
        fun consume(value: Any)
    }
}

expect class Random() {
    companion object {
        var seedInt: Int
        fun nextInt(boundary: Int = 100): Int

        var seedDouble: Double
        fun nextDouble(boundary: Double = 100.0): Double
    }
}