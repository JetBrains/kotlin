/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.wasm.internal

// Based on the AssemblyScript implementation [https://github.com/AssemblyScript/assemblyscript/blob/1e0466ef94fa5cacd0984e4f31a0087de51538a8/std/assembly/util/number.ts]

private enum class CharCodes(val code: Int) {
//  PERCENT(0x25),
    PLUS(0x2B),
    MINUS(0x2D),
    DOT(0x2E),
    _0(0x30),
//  _1(0x31),
//  _2(0x32),
//  _3(0x33),
//  _4(0x34),
//  _5(0x35),
//  _6(0x36),
//  _7(0x37),
//  _8(0x38),
//  _9(0x39),
//  A(0x41),
//  B(0x42),
//  E(0x45),
//  I(0x49),
//  N(0x4E),
//  O(0x4F),
//  X(0x58),
//  Z(0x5A),
//  a(0x61),
//  b(0x62),
    e(0x65),
//  n(0x6E),
//  o(0x6F),
//  u(0x75),
//  x(0x78),
//  z(0x7A)
}

private fun digitToChar(input: Int): Char {
    assert(input in 0..9)
    return (CharCodes._0.code + input).toChar()
}

internal fun itoa32(inputValue: Int): String {
    if (inputValue == 0) return "0"

    val isNegative = inputValue < 0
    val absValue = if (isNegative) -inputValue else inputValue
    val absValueString = utoa32(absValue.toUInt())

    return if (isNegative) "-$absValueString" else absValueString
}

internal fun utoa32(inputValue: UInt): String {
    if (inputValue == 0U) return "0"

    val decimals = decimalCount32(inputValue)
    val buf = WasmCharArray(decimals)

    utoaDecSimple(buf, inputValue, decimals)

    return buf.createString()
}

private fun utoaDecSimple(buffer: WasmCharArray, numInput: UInt, offsetInput: Int) {
    assert(numInput != 0U)
    assert(buffer.len() > 0)
    assert(offsetInput > 0 && offsetInput <= buffer.len())

    var num = numInput
    var offset = offsetInput
    do {
        val t = num / 10U
        val r = num % 10U
        num = t
        offset--
        buffer.set(offset, digitToChar(r.toInt()))
    } while (num > 0U)
}

private fun utoaDecSimple64(buffer: WasmCharArray, numInput: ULong, offsetInput: Int) {
    assert(numInput != 0UL)
    assert(buffer.len() > 0)
    assert(offsetInput > 0 && offsetInput <= buffer.len())

    var num = numInput
    var offset = offsetInput
    do {
        val t = num / 10U
        val r = num % 10U
        num = t
        offset--
        buffer.set(offset, digitToChar(r.toInt()))
    } while (num > 0U)
}


private fun Boolean.toLong() = toInt().toLong()

private fun decimalCount32(value: UInt): Int {
    if (value < 100000u) {
        if (value < 100u) {
            return 1 + (value >= 10u).toInt()
        } else {
            return 3 + (value >= 10000u).toInt() + (value >= 1000u).toInt()
        }
    } else {
        if (value < 10000000u) {
            return 6 + (value >= 1000000u).toInt()
        } else {
            return 8 + (value >= 1000000000u).toInt() + (value >= 100000000u).toInt()
        }
    }
}

internal fun itoa64(inputValue: Long): String {
    if (inputValue in Int.MIN_VALUE..Int.MAX_VALUE)
        return itoa32(inputValue.toInt())

    val isNegative = inputValue < 0
    val absValue = if (isNegative) -inputValue else inputValue
    val absValueString = utoa64(absValue.toULong())

    return if (isNegative) "-$absValueString" else absValueString
}

internal fun utoa64(inputValue: ULong): String {
    if (inputValue <= UInt.MAX_VALUE) return utoa32(inputValue.toUInt())

    val decimals = decimalCount64High(inputValue)
    val buf = WasmCharArray(decimals)

    utoaDecSimple64(buf, inputValue, decimals)

    return buf.createString()
}

// Count number of decimals for u64 values
// In our case input value always greater than 2^32-1 so we can skip some parts
private fun decimalCount64High(value: ULong): Int {
    if (value < 1000000000000000UL) {
        if (value < 1000000000000UL) {
            return 10 + (value >= 100000000000UL).toInt() + (value >= 10000000000UL).toInt()
        } else {
            return 13 + (value >= 100000000000000UL).toInt() + (value >= 10000000000000UL).toInt()
        }
    } else {
        if (value < 100000000000000000UL) {
            return 16 + (value >= 10000000000000000UL).toInt()
        } else {
            return 18 + (value >= 10000000000000000000UL).toInt() + (value >= 1000000000000000000UL).toInt()
        }
    }
}

private const val MAX_DOUBLE_LENGTH = 28

internal fun dtoa(value: Double): String {
    if (value == 0.0) {
        return if (value.toRawBits() == 0L) "0.0" else "-0.0"
    }

    if (!value.isFinite()) {
        if (value.isNaN()) return "NaN"
        return if (value < 0) "-Infinity" else "Infinity"
    }

    val buf = WasmCharArray(MAX_DOUBLE_LENGTH)
    val size = dtoaCore(buf, value)
    val ret = WasmCharArray(size)
    buf.copyInto(ret, 0, 0, size)
    return ret.createString()
}

private fun dtoaCore(buffer: WasmCharArray, valueInp: Double): Int {
    var value = valueInp

    val sign = (value < 0).toInt()
    if (sign == 1) {
        value = -value
        buffer.set(0, CharCodes.MINUS.code.toChar())
    }
    var len = grisu2(value, buffer, sign)
    len = prettify(BufferWithOffset(buffer, sign), len - sign, _K)
    return len + sign
}

// These are needed for grisu2 implementation
// TODO: What we are going to do with multiple threads?
private var _K: Int = 0
private var _exp: Int = 0
private var _frc_minus: Long = 0
private var _frc_plus:  Long = 0
private var _frc_pow: Long = 0
private var _exp_pow: Int = 0

private val EXP_POWERS = shortArrayOf(
    -1220, -1193, -1166, -1140, -1113, -1087, -1060, -1034, -1007, -980,
    -954, -927, -901, -874, -847, -821, -794, -768, -741, -715,
    -688, -661, -635, -608, -582, -555, -529, -502, -475, -449,
    -422, -396, -369, -343, -316, -289, -263, -236, -210, -183,
    -157, -130, -103, -77, -50, -24, 3, 30, 56, 83,
    109, 136, 162, 189, 216, 242, 269, 295, 322, 348,
    375, 402, 428, 455, 481, 508, 534, 561, 588, 614,
    641, 667, 694, 720, 747, 774, 800, 827, 853, 880,
    907, 933, 960, 986, 1013, 1039, 1066
)

// 1e-348, 1e-340, ..., 1e340
private val FRC_POWERS = longArrayOf(
    0xFA8FD5A0081C0288UL.toLong(), 0xBAAEE17FA23EBF76UL.toLong(), 0x8B16FB203055AC76UL.toLong(), 0xCF42894A5DCE35EAUL.toLong(),
    0x9A6BB0AA55653B2DUL.toLong(), 0xE61ACF033D1A45DFUL.toLong(), 0xAB70FE17C79AC6CAUL.toLong(), 0xFF77B1FCBEBCDC4FUL.toLong(),
    0xBE5691EF416BD60CUL.toLong(), 0x8DD01FAD907FFC3CUL.toLong(), 0xD3515C2831559A83UL.toLong(), 0x9D71AC8FADA6C9B5UL.toLong(),
    0xEA9C227723EE8BCBUL.toLong(), 0xAECC49914078536DUL.toLong(), 0x823C12795DB6CE57UL.toLong(), 0xC21094364DFB5637UL.toLong(),
    0x9096EA6F3848984FUL.toLong(), 0xD77485CB25823AC7UL.toLong(), 0xA086CFCD97BF97F4UL.toLong(), 0xEF340A98172AACE5UL.toLong(),
    0xB23867FB2A35B28EUL.toLong(), 0x84C8D4DFD2C63F3BUL.toLong(), 0xC5DD44271AD3CDBAUL.toLong(), 0x936B9FCEBB25C996UL.toLong(),
    0xDBAC6C247D62A584UL.toLong(), 0xA3AB66580D5FDAF6UL.toLong(), 0xF3E2F893DEC3F126UL.toLong(), 0xB5B5ADA8AAFF80B8UL.toLong(),
    0x87625F056C7C4A8BUL.toLong(), 0xC9BCFF6034C13053UL.toLong(), 0x964E858C91BA2655UL.toLong(), 0xDFF9772470297EBDUL.toLong(),
    0xA6DFBD9FB8E5B88FUL.toLong(), 0xF8A95FCF88747D94UL.toLong(), 0xB94470938FA89BCFUL.toLong(), 0x8A08F0F8BF0F156BUL.toLong(),
    0xCDB02555653131B6UL.toLong(), 0x993FE2C6D07B7FACUL.toLong(), 0xE45C10C42A2B3B06UL.toLong(), 0xAA242499697392D3UL.toLong(),
    0xFD87B5F28300CA0EUL.toLong(), 0xBCE5086492111AEBUL.toLong(), 0x8CBCCC096F5088CCUL.toLong(), 0xD1B71758E219652CUL.toLong(),
    0x9C40000000000000UL.toLong(), 0xE8D4A51000000000UL.toLong(), 0xAD78EBC5AC620000UL.toLong(), 0x813F3978F8940984UL.toLong(),
    0xC097CE7BC90715B3UL.toLong(), 0x8F7E32CE7BEA5C70UL.toLong(), 0xD5D238A4ABE98068UL.toLong(), 0x9F4F2726179A2245UL.toLong(),
    0xED63A231D4C4FB27UL.toLong(), 0xB0DE65388CC8ADA8UL.toLong(), 0x83C7088E1AAB65DBUL.toLong(), 0xC45D1DF942711D9AUL.toLong(),
    0x924D692CA61BE758UL.toLong(), 0xDA01EE641A708DEAUL.toLong(), 0xA26DA3999AEF774AUL.toLong(), 0xF209787BB47D6B85UL.toLong(),
    0xB454E4A179DD1877UL.toLong(), 0x865B86925B9BC5C2UL.toLong(), 0xC83553C5C8965D3DUL.toLong(), 0x952AB45CFA97A0B3UL.toLong(),
    0xDE469FBD99A05FE3UL.toLong(), 0xA59BC234DB398C25UL.toLong(), 0xF6C69A72A3989F5CUL.toLong(), 0xB7DCBF5354E9BECEUL.toLong(),
    0x88FCF317F22241E2UL.toLong(), 0xCC20CE9BD35C78A5UL.toLong(), 0x98165AF37B2153DFUL.toLong(), 0xE2A0B5DC971F303AUL.toLong(),
    0xA8D9D1535CE3B396UL.toLong(), 0xFB9B7CD9A4A7443CUL.toLong(), 0xBB764C4CA7A44410UL.toLong(), 0x8BAB8EEFB6409C1AUL.toLong(),
    0xD01FEF10A657842CUL.toLong(), 0x9B10A4E5E9913129UL.toLong(), 0xE7109BFBA19C0C9DUL.toLong(), 0xAC2820D9623BF429UL.toLong(),
    0x80444B5E7AA7CF85UL.toLong(), 0xBF21E44003ACDD2DUL.toLong(), 0x8E679C2F5E44FF8FUL.toLong(), 0xD433179D9C8CB841UL.toLong(),
    0x9E19DB92B4E31BA9UL.toLong(), 0xEB96BF6EBADF77D9UL.toLong(), 0xAF87023B9BF0EE6BUL.toLong()
)

private fun grisu2(value: Double, buffer: WasmCharArray, sign: Int): Int {
    // frexp routine
    val uv = value.toBits()
    var exp = ((uv and 0x7FF0000000000000) ushr 52).toInt()
    val sid = uv and 0x000FFFFFFFFFFFFF
    var frc = ((exp != 0).toLong() shl 52) + sid
    exp = (if (exp != 0) exp else 1) - (0x3FF + 52)

    normalizedBoundaries(frc, exp)
    getCachedPower(_exp)

    // normalize
    val off = frc.countLeadingZeroBits()
    frc = frc shl off
    exp -= off

    var frc_pow = _frc_pow
    var exp_pow = _exp_pow

    var w_frc = umul64f(frc, frc_pow)

    var wp_frc = umul64f(_frc_plus, frc_pow) - 1
    var wp_exp = umul64e(_exp, exp_pow)

    var wm_frc = umul64f(_frc_minus, frc_pow) + 1
    var delta = wp_frc - wm_frc

    return genDigits(buffer, w_frc, wp_frc, wp_exp, delta, sign);
}

private fun umul64f(u: Long, v: Long): Long {
    val u0 = u and 0xFFFFFFFF
    val v0 = v and 0xFFFFFFFF

    val u1 = u ushr 32
    val v1 = v ushr 32

    val l = u0 * v0
    var t = u1 * v0 + (l ushr 32)
    var w = u0 * v1 + (t and 0xFFFFFFFF)

    w += 0x7FFFFFFF // rounding

    t = t ushr 32
    w = w ushr 32

    return u1 * v1 + t + w
}

private fun umul64e(e1: Int, e2: Int): Int {
    return e1 + e2 + 64 // where 64 is significand size
}

private fun normalizedBoundaries(f: Long, e: Int) {
    var frc = (f shl 1) + 1
    var exp = e - 1
    val off = frc.countLeadingZeroBits()
    frc = frc shl off
    exp -= off

    val m = 1 + (f == 0x0010000000000000).toInt()

    _frc_plus = frc
    _frc_minus = ((f shl m) - 1) shl e - m - exp
    _exp = exp
}

private fun getCachedPower(minExp: Int) {
    val c = Double.fromBits(0x3FD34413509F79FE) // 1 / lg(10) = 0.30102999566398114
    val dk = (-61 - minExp) * c + 347 // dk must be positive, so can do ceiling in positive
    var k = dk.toInt()
    k += (k.toDouble() != dk).toInt() // conversion with ceil

    val index = (k shr 3) + 1
    _K = 348 - (index shl 3)    // decimal exponent no need lookup table
    _frc_pow = FRC_POWERS[index]
    _exp_pow = EXP_POWERS[index].toInt()
}

private fun genDigits(buffer: WasmCharArray, w_frc: Long, mp_frc: Long, mp_exp: Int, deltaInp: Long, sign: Int): Int {
    var delta = deltaInp
    val one_exp = -mp_exp
    val one_frc = 1L shl one_exp
    val mask = one_frc - 1

    var wp_w_frc = mp_frc - w_frc

    var p1 = (mp_frc ushr one_exp).toInt()
    var p2 = mp_frc and mask

    var kappa = decimalCount32(p1.toUInt())
    var len = sign

    while (kappa > 0) {
        var d: Int
        var pow10: Long
        when (kappa) {
            0 -> { d = p1 / 1000000000; p1 %= 1000000000; pow10 = 1000000000; }
            9 -> { d = p1 /  100000000; p1 %=  100000000; pow10 = 100000000; }
            8 -> { d = p1 /   10000000; p1 %=   10000000; pow10 = 10000000; }
            7 -> { d = p1 /    1000000; p1 %=    1000000; pow10 = 1000000; }
            6 -> { d = p1 /     100000; p1 %=     100000; pow10 = 100000; }
            5 -> { d = p1 /      10000; p1 %=      10000; pow10 = 10000; }
            4 -> { d = p1 /       1000; p1 %=       1000; pow10 = 1000; }
            3 -> { d = p1 /        100; p1 %=        100; pow10 = 100; }
            2 -> { d = p1 /         10; p1 %=         10; pow10 = 10; }
            1 -> { d = p1;              p1 =           0; pow10 = 1; }
            else -> { d = 0; pow10 = 1; }
        }

        if (d or len != 0)
            buffer.set(len++, digitToChar(d))

        --kappa
        val tmp = (p1.toLong() shl one_exp) + p2
        if (tmp <= delta) {
            _K += kappa
            grisuRound(buffer, len, delta, tmp, pow10 shl one_exp, wp_w_frc)
            return len;
        }
    }

    var unit = 1L
    while (true) {
        p2 *= 10
        delta *= 10
        unit *= 10

        val d = p2 ushr one_exp
        if (d or len.toLong() != 0L)
            buffer.set(len++, digitToChar(d.toInt()))

        p2 = p2 and mask
        --kappa
        if (p2 < delta) {
            _K += kappa
            grisuRound(buffer, len, delta, p2, one_frc, wp_w_frc * unit)
            return len
        }
    }
}

private fun grisuRound(buffer: WasmCharArray, len: Int, delta: Long, restInp: Long, ten_kappa: Long, wp_w: Long) {
    var rest = restInp
    val lastp = len - 1
    var digit = buffer.get(lastp)
    while (
        rest < wp_w &&
        delta - rest >= ten_kappa && (
                rest + ten_kappa < wp_w ||
                        wp_w - rest > rest + ten_kappa - wp_w
                )
    ) {
        --digit
        rest += ten_kappa;
    }
    buffer.set(lastp, digit)
}

private fun WasmCharArray.copyInto(destination: WasmCharArray, destinationOffset: Int, sourceOffset: Int, len: Int) {
    var srcIndex: Int
    var dstIndex: Int
    var increment: Int
    if (destinationOffset <= sourceOffset) {
        srcIndex = sourceOffset
        dstIndex = destinationOffset
        increment = 1
    } else {
        srcIndex = sourceOffset + len - 1
        dstIndex = destinationOffset + len - 1
        increment = -1
    }

    repeat(len) {
        destination.set(dstIndex, this.get(srcIndex))
        srcIndex += increment
        dstIndex += increment
    }
}

private class BufferWithOffset(val buf: WasmCharArray, val off: Int) {
    operator fun set(addr: Int, value: Char) {
        buf.set(off + addr, value)
    }

    fun memoryCopy(destAddr: Int, srcAddr: Int, len: Int) {
        val startIdx = off + srcAddr
        buf.copyInto(buf, off + destAddr, startIdx, len)
    }

    fun offsetABitMore(anotherOff: Int) = BufferWithOffset(buf, off + anotherOff)
}

private fun prettify(buffer: BufferWithOffset, lengthInp: Int, k: Int): Int {
    var length = lengthInp
    if (k == 0) {
        buffer[length] = CharCodes.DOT.code.toChar()
        buffer[length + 1] = CharCodes._0.code.toChar()
        return length + 2
    }

    var kk = length + k
    if (length <= kk && kk <= 21) {
        // 1234e7 -> 12340000000
        for (i in length until kk) {
            buffer[i] = CharCodes._0.code.toChar()
        }
        buffer[kk] = CharCodes.DOT.code.toChar()
        buffer[kk + 1] = CharCodes._0.code.toChar()
        return kk + 2
    } else if (kk > 0 && kk <= 21) {
        // 1234e-2 -> 12.34
        buffer.memoryCopy(kk + 1, kk, -k)
        buffer[kk] = CharCodes.DOT.code.toChar()
        return length + 1
    } else if (-6 < kk && kk <= 0) {
        // 1234e-6 -> 0.001234
        val offset = 2 - kk
        buffer.memoryCopy(offset, 0, length)
        buffer[0] = CharCodes._0.code.toChar()
        buffer[1] = CharCodes.DOT.code.toChar()
        for (i in 2 until offset) {
            buffer[i] = CharCodes._0.code.toChar()
        }
        return length + offset
    } else if (length == 1) {
        // 1e30
        buffer[1] = CharCodes.e.code.toChar()
        length = genExponent(buffer.offsetABitMore(2), kk - 1)
        return length + 2
    } else {
        val len = length
        buffer.memoryCopy(2, 1, len - 1)
        buffer[1] = CharCodes.DOT.code.toChar()
        buffer[len + 1] = CharCodes.e.code.toChar()
        length += genExponent(buffer.offsetABitMore(len + 2), kk - 1)
        return length + 2
    }
}

private fun genExponent(buffer: BufferWithOffset, kInp: Int): Int {
    var k = kInp
    val sign = k < 0
    if (sign) k = -k
    val kStr = k.toString()
    for (i in kStr.indices)
        buffer[i + 1] = kStr[i]
    buffer[0] = if (sign) CharCodes.MINUS.code.toChar() else CharCodes.PLUS.code.toChar()
    return kStr.length + 1
}
