/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.time.TimeSource

@kotlin.wasm.WasmExport
fun runBenchmark(): Long {
    val warmupIterations = 20
    val benchmarkIterations = 30
    
    // Warmup phase
    for (i in 0 until warmupIterations) {
        box()
    }
    
    // Benchmark phase
    val times = mutableListOf<Long>()
    
    for (i in 0 until benchmarkIterations) {
        val startMark = TimeSource.Monotonic.markNow()
        val boxResult = box()
        val duration = startMark.elapsedNow()
        
        val isOk = boxResult == "OK"
        if (!isOk) {
            println("Wrong box result '${boxResult}'; Expected 'OK'")
            return -1L
        }
        
        times.add(duration.inWholeNanoseconds)
    }
    
    // Calculate statistics
    val sortedTimes = times.sorted()
    val median = sortedTimes[sortedTimes.size / 2]
    val average = times.sum() / times.size
    val min = sortedTimes.first()
    val max = sortedTimes.last()
    
    println("Benchmark Results (nanoseconds):")
    println("  Iterations: $benchmarkIterations")
    println("  Min: $min")
    println("  Max: $max")
    println("  Average: $average")
    println("  Median: $median")
    
    return median
}

@kotlin.wasm.WasmImport("ssw_util", "proc_exit")
private external fun wasiProcExit(code: Int)

@kotlin.wasm.WasmExport
fun startTest() {
    try {
        val result = runBenchmark()
        if (result < 0) {
            wasiProcExit(1)
        }
    } catch (e: Throwable) {
        println("Failed with exception!")
        println(e.message)
        println(e.printStackTrace())
        wasiProcExit(1)
    }
}
