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

package org.jetbrains.typesBenchmarks

actual class StringBenchmark actual constructor() {
    actual fun stringToCBenchmark() {
        error("Benchmark stringToCBenchmark is unsupported on JVM!")
    }

    actual fun stringToKotlinBenchmark() {
        error("Benchmark stringToKotlinBenchmark is unsupported on JVM!")
    }
}

actual class IntBenchmark actual constructor() {
    actual fun intBenchmark() {
        error("Benchmark intBenchmark is unsupported on JVM!")
    }
}

actual class BoxedIntBenchmark actual constructor() {
    actual fun boxedIntBenchmark() {
        error("Benchmark boxedIntBenchmark is unsupported on JVM!")
    }
}

actual class IntMatrixBenchmark actual constructor() {
    actual fun intMatrixBenchmark() {
        error("Benchmark intMatrixBenchmark is unsupported on JVM!")
    }
}

