// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=lateShadowStack=true
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)
package gc_stack_roots.tier5_adversarial

import kotlin.native.runtime.GC

fun boxInt(x: Int): Any = x
fun boxShort(x: Short): Any = x
fun boxChar(x: Char): Any = x
fun boxLong(x: Long): Any = x

fun testIntBoxCache(): String {
    val minCached1: Any = -128
    val minCached2: Any = -128
    val maxCached1: Any = 127
    val maxCached2: Any = 127

    val minUncached1 = boxInt(-129)
    val minUncached2 = boxInt(-129)
    val maxUncached1 = boxInt(128)
    val maxUncached2 = boxInt(128)

    GC.collect()

    if (minCached1 !== minCached2) return "FAIL: Int -128 not cached"
    if (maxCached1 !== maxCached2) return "FAIL: Int 127 not cached"
    if (minCached1 !== boxInt(-128)) return "FAIL: Int -128 dynamic box doesn't match static cache"
    if (maxCached1 !== boxInt(127)) return "FAIL: Int 127 dynamic box doesn't match static cache"
    if (minUncached1 === minUncached2) return "FAIL: Int -129 should not be cached"
    if (maxUncached1 === maxUncached2) return "FAIL: Int 128 should not be cached"

    if ((minCached1 as Int) != -128) return "FAIL: minCached1 value corrupted"
    if ((maxCached1 as Int) != 127) return "FAIL: maxCached1 value corrupted"
    if ((minUncached1 as Int) != -129) return "FAIL: minUncached1 value corrupted"
    if ((maxUncached1 as Int) != 128) return "FAIL: maxUncached1 value corrupted"

    return "OK"
}

fun testByteBoxCache(): String {
    val b1: Any = (-128).toByte()
    val b2: Any = (-128).toByte()
    val b3: Any = 127.toByte()
    val b4: Any = 127.toByte()

    GC.collect()

    if (b1 !== b2) return "FAIL: Byte -128 not cached"
    if (b3 !== b4) return "FAIL: Byte 127 not cached"
    if ((b1 as Byte) != (-128).toByte()) return "FAIL: Byte -128 corrupted"
    if ((b3 as Byte) != 127.toByte()) return "FAIL: Byte 127 corrupted"

    return "OK"
}

fun testShortBoxCache(): String {
    val s1: Any = (-128).toShort()
    val s2: Any = (-128).toShort()
    val s3: Any = 127.toShort()
    val s4: Any = 127.toShort()
    val sOut1 = boxShort(128.toShort())
    val sOut2 = boxShort(128.toShort())

    GC.collect()

    if (s1 !== s2) return "FAIL: Short -128 not cached"
    if (s3 !== s4) return "FAIL: Short 127 not cached"
    if (s1 !== boxShort((-128).toShort())) return "FAIL: Short -128 dynamic box doesn't match cache"
    if (sOut1 === sOut2) return "FAIL: Short 128 should not be cached"

    return "OK"
}

fun testCharBoxCache(): String {
    val c0_1: Any = 0.toChar()
    val c0_2: Any = 0.toChar()
    val c255_1: Any = 255.toChar()
    val c255_2: Any = 255.toChar()
    val c256_1 = boxChar(256.toChar())
    val c256_2 = boxChar(256.toChar())

    GC.collect()

    if (c0_1 !== c0_2) return "FAIL: Char 0 not cached"
    if (c255_1 !== c255_2) return "FAIL: Char 255 not cached"
    if (c0_1 !== boxChar(0.toChar())) return "FAIL: Char 0 dynamic box doesn't match cache"
    if (c256_1 === c256_2) return "FAIL: Char 256 should not be cached"

    return "OK"
}

fun testLongBoxCache(): String {
    val l1: Any = -128L
    val l2: Any = -128L
    val l3: Any = 127L
    val l4: Any = 127L
    val lOut1 = boxLong(128L)
    val lOut2 = boxLong(128L)

    GC.collect()

    if (l1 !== l2) return "FAIL: Long -128 not cached"
    if (l3 !== l4) return "FAIL: Long 127 not cached"
    if (l1 !== boxLong(-128L)) return "FAIL: Long -128 dynamic box doesn't match cache"
    if (lOut1 === lOut2) return "FAIL: Long 128 should not be cached"

    return "OK"
}

fun testBooleanBoxCache(): String {
    val t1: Any = true
    val t2: Any = true
    val f1: Any = false
    val f2: Any = false

    GC.collect()

    if (t1 !== t2) return "FAIL: Boolean true not cached"
    if (f1 !== f2) return "FAIL: Boolean false not cached"

    return "OK"
}

fun box(): String {
    var res = testIntBoxCache()
    if (res != "OK") return res

    res = testByteBoxCache()
    if (res != "OK") return res

    res = testShortBoxCache()
    if (res != "OK") return res

    res = testCharBoxCache()
    if (res != "OK") return res

    res = testLongBoxCache()
    if (res != "OK") return res

    res = testBooleanBoxCache()
    if (res != "OK") return res

    return "OK"
}
