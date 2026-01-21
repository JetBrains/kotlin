package org.jetbrains.ring

class ForLoopsBenchmark {

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

    fun arrayLoop(): Long {
        var sum = 0L
        for (e in array) {
            sum += e
        }
        return sum
    }

    fun intArrayLoop(): Long {
        var sum = 0L
        for (e in intArray) {
            sum += e
        }
        return sum
    }

    fun charArrayLoop(): Long {
        var sum = 0L
        for (e in charArray) {
            sum += e.toLong()
        }
        return sum
    }

    fun stringLoop(): Long {
        var sum = 0L
        for (e in string) {
            sum += e.hashCode()
        }
        return sum
    }

    fun stringArrayLoop(): Long {
        var sum = 0L
        for (e in stringArray) {
            sum += e.length.toLong()
        }
        return sum
    }

    fun floatArrayLoop(): Double {
        var sum = 0.0
        for (e in floatArray) {
            sum += e
        }
        return sum
    }

    fun uIntArrayLoop(): ULong {
        var sum: ULong = 0u
        for (e in uIntArray) {
            sum += e
        }
        return sum
    }

    fun uShortArrayLoop(): ULong {
        var sum: ULong = 0u
        for (e in uShortArray) {
            sum += e
        }
        return sum
    }

    fun uLongArrayLoop(): ULong {
        var sum: ULong = 0u
        for (e in uLongArray) {
            sum += e
        }
        return sum
    }

    // Iterations over .indices

    fun arrayIndicesLoop(): Long {
        var sum = 0L
        for (i in array.indices) {
            sum += array[i]
        }
        return sum
    }

    fun intArrayIndicesLoop(): Long {
        var sum = 0L
        for (i in intArray.indices) {
            sum += intArray[i]
        }
        return sum
    }

    fun charArrayIndicesLoop(): Long {
        var sum = 0L
        for (i in charArray.indices) {
            sum += charArray[i].toLong()
        }
        return sum
    }

    fun stringIndicesLoop(): Long {
        var sum = 0L
        for (i in string.indices) {
            sum += string[i].hashCode()
        }
        return sum
    }

    fun floatArrayIndicesLoop(): Double {
        var sum = 0.0
        for (i in floatArray.indices) {
            sum += floatArray[i]
        }
        return sum
    }

    fun uIntArrayIndicesLoop(): ULong {
        var sum: ULong = 0u
        for (i in uIntArray.indices) {
            sum += uIntArray[i]
        }
        return sum
    }

    fun uShortArrayIndicesLoop(): ULong {
        var sum: ULong = 0u
        for (i in uShortArray.indices) {
            sum += uShortArray[i]
        }
        return sum
    }

    fun uLongArrayIndicesLoop(): ULong {
        var sum: ULong = 0u
        for (i in uLongArray.indices) {
            sum += uLongArray[i]
        }
        return sum
    }
}