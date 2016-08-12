package tests

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.locks.Condition
import kotlin.system.exitProcess

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

    fun getKtInputStream(): main.kotlin.CodedInputStream {
        val ba = ByteArray(100000)
        val ins = main.kotlin.CodedInputStream(ba)
        return ins
    }

    fun getKtOutputStream(size: Int): main.kotlin.CodedOutputStream {
        val ba = ByteArray(size)
        val outs = main.kotlin.CodedOutputStream(ba)
        return outs
    }

    fun KtOutputStreamToInputStream(kt: main.kotlin.CodedOutputStream): InputStream {
        return kt.buffer.inputStream()
    }

    fun KtInputStreamToOutputStream(kt: main.kotlin.CodedInputStream): OutputStream {
        val baos = ByteArrayOutputStream(kt.buffer.size)
        return baos
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