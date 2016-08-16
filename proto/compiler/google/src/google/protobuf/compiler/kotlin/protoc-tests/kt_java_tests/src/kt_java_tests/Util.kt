package kt_java_tests

import java.io.InputStream
import kotlin.system.exitProcess
import CodedOutputStream

object Util {
    var ARRAY_MAX_SIZE = 1000

    fun assert(condition: Boolean) {
        if (!condition) {
            println("Assertion failed")
            exitProcess(1)
        }
    }

    fun <T> compareArrays (lhs: Iterable<T>, rhs: Iterable<T>): Boolean {
        return lhs.count() == rhs.count() && lhs.filter { !rhs.contains(it) }.isEmpty()
    }

    fun getKtOutputStream(size: Int): CodedOutputStream {
        val ba = ByteArray(size)
        val outs = CodedOutputStream(ba)
        return outs
    }

    fun KtOutputStreamToInputStream(kt: CodedOutputStream): InputStream {
        return kt.buffer.inputStream()
    }

    fun generateIntArray(): IntArray {
        val size = RandomGen.rnd.nextInt(ARRAY_MAX_SIZE)
        val arr = IntArray(size)
        for (i in 0..(size - 1)) {
            arr[i] = RandomGen.rnd.nextInt()
        }
        return arr
    }

    fun generateLongArray(): LongArray {
        val size = RandomGen.rnd.nextInt(ARRAY_MAX_SIZE)
        val arr = LongArray(size)
        for (i in 0..(size - 1)) {
            arr[i] = RandomGen.rnd.nextLong()
        }
        return arr
    }

    fun generateBoolArray(): BooleanArray {
        val size = RandomGen.rnd.nextInt(ARRAY_MAX_SIZE)
        val arr = BooleanArray(size)
        for (i in 0..(size - 1)) {
            arr[i] = RandomGen.rnd.nextBoolean()
        }
        return arr
    }
}