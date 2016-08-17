package tests
import CodedOutputStream

@native
fun require(value: String): dynamic { return null }

val protoBuf = require("protobufjs")

object Util {
    var seedrandom = require("seedrandom")
    var rng = seedrandom("42")
    val ARRAY_MAX_SIZE = 1000

    fun nextInt(min_val: Int, max_val: Int): Int {
        return Math.floor(rng()*(max_val - min_val + 1) + min_val)
    }

    fun nextInt(max_val: Int): Int {
        return nextInt(0, max_val)
    }

    fun nextInt(): Int {
        return nextInt(Int.MIN_VALUE, Int.MAX_VALUE)
    }

    fun nextLong(min_val: Long, max_val: Long): Long {
       return nextInt().toLong()
    }

    fun nextLong(max_val: Long): Long {
        return nextLong(0L, max_val)
    }

    fun nextLong(): Long {
        return nextLong(Long.MIN_VALUE, Long.MAX_VALUE)
    }

    fun nextBoolean(): Boolean {
        return if (nextInt() % 2 == 0) false else true
    }

    fun getKtOutputStream(size: Int): CodedOutputStream {
        val ba = ByteArray(size)
        val outs = CodedOutputStream(ba)
        return outs
    }

    fun assert(condition: Boolean) {
        if (!condition) {
            println("Assertion failed")
            js("process.exit(1)")
        }
    }

    fun generateIntArray(): IntArray {
        val size = nextInt(ARRAY_MAX_SIZE)
        val arr = IntArray(size)
        for (i in 0..(size - 1)) {
            arr[i] = nextInt()
        }
        return arr
    }

    fun <T> compareArrays (lhs: Iterable<T>, rhs: Iterable<T>): Boolean {
        return lhs.count() == rhs.count() && lhs.filter { !rhs.contains(it) }.isEmpty()
    }

    fun generateLongArray(): LongArray {
        val size = nextInt(ARRAY_MAX_SIZE)
        val arr = LongArray(size)
        for (i in 0..(size - 1)) {
            arr[i] = nextLong()
        }
        return arr
    }

    fun generateBoolArray(): BooleanArray {
        val size = nextInt(ARRAY_MAX_SIZE)
        val arr = BooleanArray(size)
        for (i in 0..(size - 1)) {
            arr[i] = nextBoolean()
        }
        return arr
    }
}