/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.ring

var counter = 0

open class AllocationBenchmark {

    class MyClass {
        fun inc() {
            counter++
        }
    }

    //Benchmark
    fun allocateObjects() {
        repeat(BENCHMARK_SIZE) {
            MyClass().inc()
        }
    }

}