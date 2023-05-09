/*
 * Copyright 2010-2023 JetBrains s.r.o.
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
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.jetbrains.benchmarksLauncher

import kotlin.native.runtime.GC
import platform.posix.*
import kotlinx.cinterop.*

actual fun writeToFile(fileName: String, text: String) {
    val file = fopen(fileName, "wt") ?: error("Cannot write file '$fileName'")
    try {
        if (fputs(text, file) == EOF) throw Error("File write error")
    } finally {
        fclose(file)
    }
}

// Wrapper for assert funtion in stdlib
actual fun assert(value: Boolean) {
    kotlin.assert(value)
}

// Wrapper for measureNanoTime funtion in stdlib
actual inline fun measureNanoTime(block: () -> Unit): Long {
    return kotlin.system.measureNanoTime(block)
}

@OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
actual fun cleanup() {
    GC.collect()
}

actual fun printStderr(message: String) {
    val STDERR = fdopen(2, "w")
    fprintf(STDERR, message)
    fflush(STDERR)
}

actual fun nanoTime(): Long = kotlin.system.getTimeNanos()

actual class Blackhole {
    @kotlin.native.ThreadLocal
    actual companion object {
        actual var consumer = 0
        actual fun consume(value: Any) {
            consumer += value.hashCode()
        }
    }
}

actual class Random actual constructor() {
    @kotlin.native.ThreadLocal
    actual companion object {
        actual var seedInt = 0
        actual fun nextInt(boundary: Int): Int {
            seedInt = (3 * seedInt + 11) % boundary
            return seedInt
        }

        actual var seedDouble: Double = 0.1
        actual fun nextDouble(boundary: Double): Double {
            seedDouble = (7.0 * seedDouble + 7.0) % boundary
            return seedDouble
        }
    }
}