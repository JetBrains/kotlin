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

    // Despite the name, functions below returns Ints instead of Longs.
    // JS doesn't support fair 64-bits ints, so both Kotlin and ProtobufJS
    // emulates them via additional classes.
    // Of course, there is no easy way to convert Kotlin-Long to ProtobufJS-Long,
    // so this part of Protobuf library left uncovered.
    fun nextLong(): Int {
        return nextInt()
    }

    fun nextLong(min_val: Long, max_val: Long): Int {
        return nextInt(min_val.toInt(), max_val.toInt())
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

    fun JSBufferToByteArray(buffer: dynamic): ByteArray {
        val size = buffer.length
        val byteArray = ByteArray(size)
        for (i in 0..(size - 1)) {
            byteArray[i] = buffer[i]
        }
        return byteArray
    }

    fun compareUints(kt: Int, jvs: dynamic): Boolean {
        val fairUInt: Long = kt.toLong() and (-1L ushr 32)
        return fairUInt.toString() == jvs.toString()
    }

    fun compareUlongs(kt: Long, jvs: dynamic): Boolean {
        return kt.toString() == jvs.toString()
    }

    fun generateIntArray(): IntArray {
        val size = nextInt(ARRAY_MAX_SIZE)
        val arr = IntArray(size)
        for (i in 0..(size - 1)) {
            arr[i] = nextInt()
        }
        return arr
    }

    fun compareArrays(kt: dynamic, jvs: dynamic): Boolean {
        if (kt.length != jvs.length) {
            return false
        }
        for (i in 0..(kt.length - 1)) {
            if (kt[i].toString() != jvs[i].toString()) {
                return false
            }
        }
        return true
    }

    fun compareUIntArrays(kt: dynamic, jvs: dynamic): Boolean {
        if (kt.length != jvs.length) {
            return false
        }
        for (i in 0..(kt.length - 1)) {
            if (!compareUints(kt[i], jvs[i])) {
                println("elements ${kt[i].toString()} and ${jvs[i].toString()} differs")
                return false
            }
        }
        return true
    }

    fun compareULongArrays(kt: dynamic, jvs: dynamic): Boolean {
        if (kt.length != jvs.length) {
            return false
        }
        for (i in 0..(kt.length - 1)) {
            if (!compareUlongs(kt[i], jvs[i])) {
                println("elements ${kt[i].toString()} and ${jvs[i].toString()} differs")
                return false
            }
        }
        return true
    }

    fun generateLongArray(): LongArray {
        val size = nextInt(ARRAY_MAX_SIZE)
        val arr = LongArray(size)
        for (i in 0..(size - 1)) {
            arr[i] = nextLong().toLong()
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

    fun KotlinArrToJS(arr: dynamic): dynamic {
        // note that even we know that arr is in fact Kotlin array that should have size() method,
        // during translation it's type (and all it's methods) are lost
        val size = arr.length
        var jsArr: dynamic = js("[]")
        for (i in 0..(size - 1)) {
            jsArr.push(arr[i])
        }
        return jsArr
    }
}