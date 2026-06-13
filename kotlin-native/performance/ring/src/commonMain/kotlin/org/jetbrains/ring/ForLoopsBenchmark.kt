package org.jetbrains.ring

import kotlinx.benchmark.*
import org.jetbrains.benchmarksLauncher.SkipWhenBaseOnly

private const val BENCHMARK_SIZE = 10000

@State(Scope.Benchmark)
@Measurement(time = 100, timeUnit = BenchmarkTimeUnit.MILLISECONDS)
class ForLoops : SkipWhenBaseOnly() {

    private val array: Array<Int> = Array(BENCHMARK_SIZE) {
        it
    }

    private val intArray: IntArray = IntArray(BENCHMARK_SIZE) {
        it
    }

    private val charArray: CharArray = CharArray(BENCHMARK_SIZE) {
        it.toChar()
    }

    private val string: String = charArray.joinToString()

    private val stringArray: Array<String> = Array(BENCHMARK_SIZE) {
        it.toString()
    }

    private val floatArray: FloatArray = FloatArray(BENCHMARK_SIZE) {
        it.toFloat()
    }

    private val uIntArray = UIntArray(BENCHMARK_SIZE) {
        it.toUInt()
    }

    private val uShortArray = UShortArray(BENCHMARK_SIZE) {
        it.toUShort()
    }

    private val uLongArray = ULongArray(BENCHMARK_SIZE) {
        it.toULong()
    }

    @Benchmark
    fun arrayLoop(bh: Blackhole) {
        var sum = 0L
        for (e in array) {
            sum += e
        }
        bh.consume(sum)
    }

    @Benchmark
    fun intArrayLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        var sum = 0L
        for (e in intArray) {
            sum += e
        }
        bh.consume(sum)
    }

    @Benchmark
    fun charArrayLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        var sum = 0L
        for (e in charArray) {
            sum += e.code.toLong()
        }
        bh.consume(sum)
    }

    @Benchmark
    fun stringLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        var sum = 0L
        for (e in string) {
            sum += e.hashCode()
        }
        bh.consume(sum)
    }

    @Benchmark
    fun stringArrayLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        var sum = 0L
        for (e in stringArray) {
            sum += e.length.toLong()
        }
        bh.consume(sum)
    }

    @Benchmark
    fun floatArrayLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        var sum = 0.0
        for (e in floatArray) {
            sum += e
        }
        bh.consume(sum)
    }

    @Benchmark
    fun uIntArrayLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        var sum: ULong = 0u
        for (e in uIntArray) {
            sum += e
        }
        bh.consume(sum)
    }

    @Benchmark
    fun uShortArrayLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        var sum: ULong = 0u
        for (e in uShortArray) {
            sum += e
        }
        bh.consume(sum)
    }

    @Benchmark
    fun uLongArrayLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        var sum: ULong = 0u
        for (e in uLongArray) {
            sum += e
        }
        bh.consume(sum)
    }

    // Iterations over .indices

    @Benchmark
    fun arrayIndicesLoop(bh: Blackhole) {
        var sum = 0L
        for (i in array.indices) {
            sum += array[i]
        }
        bh.consume(sum)
    }

    @Benchmark
    fun intArrayIndicesLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        var sum = 0L
        for (i in intArray.indices) {
            sum += intArray[i]
        }
        bh.consume(sum)
    }

    @Benchmark
    fun charArrayIndicesLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        var sum = 0L
        for (i in charArray.indices) {
            sum += charArray[i].code.toLong()
        }
        bh.consume(sum)
    }

    @Benchmark
    fun stringIndicesLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        var sum = 0L
        for (i in string.indices) {
            sum += string[i].hashCode()
        }
        bh.consume(sum)
    }

    @Benchmark
    fun floatArrayIndicesLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        var sum = 0.0
        for (i in floatArray.indices) {
            sum += floatArray[i]
        }
        bh.consume(sum)
    }

    @Benchmark
    fun uIntArrayIndicesLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        var sum: ULong = 0u
        for (i in uIntArray.indices) {
            sum += uIntArray[i]
        }
        bh.consume(sum)
    }

    @Benchmark
    fun uShortArrayIndicesLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        var sum: ULong = 0u
        for (i in uShortArray.indices) {
            sum += uShortArray[i]
        }
        bh.consume(sum)
    }

    @Benchmark
    fun uLongArrayIndicesLoop(bh: Blackhole) {
        skipWhenBaseOnly()
        var sum: ULong = 0u
        for (i in uLongArray.indices) {
            sum += uLongArray[i]
        }
        bh.consume(sum)
    }
}
