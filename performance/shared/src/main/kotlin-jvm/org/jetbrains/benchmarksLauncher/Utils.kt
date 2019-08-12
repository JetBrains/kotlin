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

package org.jetbrains.benchmarksLauncher

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

actual fun writeToFile(fileName: String, text: String) {
    File(fileName).printWriter().use { out ->
        out.println(text)
    }
}

// Wrapper for assert funtion in stdlib.
actual fun assert(value: Boolean) {
    kotlin.assert(value)
}

// Wrapper for measureNanoTime funtion in stdlib.
actual inline fun measureNanoTime(block: () -> Unit): Long {
    return kotlin.system.measureNanoTime(block)
}

actual fun cleanup() {}

actual fun printStderr(message: String) {
    System.err.print(message)
}

actual fun currentTime(): String =
        SimpleDateFormat("HH:mm:ss").format(Date())

actual fun nanoTime(): Long = System.nanoTime()

actual class Blackhole {
    actual companion object {
        actual var consumer = 0
        actual fun consume(value: Any) {
            consumer += value.hashCode()
        }
    }
}

