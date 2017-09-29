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

package kotlin.system

@SymbolName("Kotlin_system_getTimeMillis")
public external fun getTimeMillis() : Long

@SymbolName("Kotlin_system_getTimeNanos")
public external fun getTimeNanos() : Long

@SymbolName("Kotlin_system_getTimeMicros")
public external fun getTimeMicros() : Long

/** Executes the given block and returns elapsed time in milliseconds. */
public inline fun measureTimeMillis(block: () -> Unit) : Long {
    val start = getTimeMillis()
    block()
    return getTimeMillis() - start
}

/** Executes the given block and returns elapsed time in microseconds (Kotlin/Native only). */
public inline fun measureTimeMicros(block: () -> Unit) : Long {
    val start = getTimeMicros()
    block()
    return getTimeMicros() - start
}

/** Executes the given block and returns elapsed time in nanoseconds. */
public inline fun measureNanoTime(block: () -> Unit) : Long {
    val start = getTimeNanos()
    block()
    return getTimeNanos() - start
}
