/*
 * Licensed to the .NET Foundation under one or more agreements.
 * The .NET Foundation licenses this file to you under the MIT license.
 */
@file:OptIn(ExperimentalUnsignedTypes::class)
package kotlin.text

import kotlin.math.abs

private val s_Pow10MantissaTable: ULongArray = ulongArrayOf(
    // powers of 10
    0xA0000000_00000000UL,     // 1
    0xC8000000_00000000UL,     // 2
    0xFA000000_00000000UL,     // 3
    0x9C400000_00000000UL,     // 4
    0xC3500000_00000000UL,     // 5
    0xF4240000_00000000UL,     // 6
    0x98968000_00000000UL,     // 7
    0xBEBC2000_00000000UL,     // 8
    0xEE6B2800_00000000UL,     // 9
    0x9502F900_00000000UL,     // 10
    0xBA43B740_00000000UL,     // 11
    0xE8D4A510_00000000UL,     // 12
    0x9184E72A_00000000UL,     // 13
    0xB5E620F4_80000000UL,     // 14
    0xE35FA931_A0000000UL,     // 15

    // powers of 0.1
    0xCCCCCCCC_CCCCCCCDUL,     // 1
    0xA3D70A3D_70A3D70BUL,     // 2
    0x83126E97_8D4FDF3CUL,     // 3
    0xD1B71758_E219652EUL,     // 4
    0xA7C5AC47_1B478425UL,     // 5
    0x8637BD05_AF6C69B7UL,     // 6
    0xD6BF94D5_E57A42BEUL,     // 7
    0xABCC7711_8461CEFFUL,     // 8
    0x89705F41_36B4A599UL,     // 9
    0xDBE6FECE_BDEDD5C2UL,     // 10
    0xAFEBFF0B_CB24AB02UL,     // 11
    0x8CBCCC09_6F5088CFUL,     // 12
    0xE12E1342_4BB40E18UL,     // 13
    0xB424DC35_095CD813UL,     // 14
    0x901D7CF7_3AB0ACDCUL,     // 15
)

private val s_Pow10ExponentTable: ShortArray = shortArrayOf(
    // exponents for both powers of 10 and 0.1
    4,      // 1
    7,      // 2
    10,     // 3
    14,     // 4
    17,     // 5
    20,     // 6
    24,     // 7
    27,     // 8
    30,     // 9
    34,     // 10
    37,     // 11
    40,     // 12
    44,     // 13
    47,     // 14
    50,     // 15
)

private val s_Pow10By16MantissaTable: ULongArray = ulongArrayOf(
    // powers of 10^16
    0x8E1BC9BF_04000000UL,     // 1
    0x9DC5ADA8_2B70B59EUL,     // 2
    0xAF298D05_0E4395D6UL,     // 3
    0xC2781F49_FFCFA6D4UL,     // 4
    0xD7E77A8F_87DAF7FAUL,     // 5
    0xEFB3AB16_C59B14A0UL,     // 6
    0x850FADC0_9923329CUL,     // 7
    0x93BA47C9_80E98CDEUL,     // 8
    0xA402B9C5_A8D3A6E6UL,     // 9
    0xB616A12B_7FE617A8UL,     // 10
    0xCA28A291_859BBF90UL,     // 11
    0xE070F78D_39275566UL,     // 12
    0xF92E0C35_37826140UL,     // 13
    0x8A5296FF_E33CC92CUL,     // 14
    0x9991A6F3_D6BF1762UL,     // 15
    0xAA7EEBFB_9DF9DE8AUL,     // 16
    0xBD49D14A_A79DBC7EUL,     // 17
    0xD226FC19_5C6A2F88UL,     // 18
    0xE950DF20_247C83F8UL,     // 19
    0x81842F29_F2CCE373UL,     // 20
    0x8FCAC257_558EE4E2UL,     // 21

    // powers of 0.1^16
    0xE69594BE_C44DE160UL,     // 1
    0xCFB11EAD_453994C3UL,     // 2
    0xBB127C53_B17EC165UL,     // 3
    0xA87FEA27_A539E9B3UL,     // 4
    0x97C560BA_6B0919B5UL,     // 5
    0x88B402F7_FD7553ABUL,     // 6
    0xF64335BC_F065D3A0UL,     // 7
    0xDDD0467C_64BCE4C4UL,     // 8
    0xC7CABA6E_7C5382EDUL,     // 9
    0xB3F4E093_DB73A0B7UL,     // 10
    0xA21727DB_38CB0053UL,     // 11
    0x91FF8377_5423CC29UL,     // 12
    0x8380DEA9_3DA4BC82UL,     // 13
    0xECE53CEC_4A314F00UL,     // 14
    0xD5605FCD_CF32E217UL,     // 15
    0xC0314325_637A1978UL,     // 16
    0xAD1C8EAB_5EE43BA2UL,     // 17
    0x9BECCE62_836AC5B0UL,     // 18
    0x8C71DCD9_BA0B495CUL,     // 19
    0xFD00B897_47823938UL,     // 20
    0xE3E27A44_4D8D991AUL,     // 21
)

private val s_Pow10By16ExponentTable: ShortArray = shortArrayOf(
    // exponents for both powers of 10^16 and 0.1^16
    54,     // 1
    107,    // 2
    160,    // 3
    213,    // 4
    266,    // 5
    319,    // 6
    373,    // 7
    426,    // 8
    479,    // 9
    532,    // 10
    585,    // 11
    638,    // 12
    691,    // 13
    745,    // 14
    798,    // 15
    851,    // 16
    904,    // 17
    957,    // 18
    1010,   // 19
    1064,   // 20
    1117,   // 21
)


// get 32-bit integer from at most 9 digits
private fun digitsToInt(p: String, pIndex: Int, count: Int): UInt {
    var res: UInt = (p[pIndex] - '0').toUInt()
    for (index in 1 until count) {
        res = (10U * res) + (p[pIndex + index] - '0').toUInt()
    }
    return res
}

private fun mul32x32To64(a: UInt, b: UInt): ULong = a.toULong() * b.toULong()

private fun mul64Lossy(a: ULong, b: ULong): ULong {
    // it's ok to lose some precision here - Mul64 will be called
    // at most twice during the conversion, so the error won't propagate
    // to any of the 53 significant bits of the result
    return mul32x32To64((a shr 32).toUInt(), (b shr 32).toUInt()) +
            (mul32x32To64((a shr 32).toUInt(), b.toUInt()) shr 32) +
            (mul32x32To64((a).toUInt(), (b shr 32).toUInt()) shr 32)
}

//TODO:
//This is partially implementation of parser. The issue with it is incorrect even rounding so the result number
//could slightly different from other backends. True implementation needs Big Integer
//Original implementation https://github.com/mono/corert/blob/master/src/System.Private.CoreLib/src/System/Number.CoreRT.cs
internal fun numberToDouble(signed: Boolean, numberScale: Int, number: String): Double {
    var srcIndex = 0
    val total = number.length
    var remaining = total

    // skip the leading zeros
    while (remaining != 0 && number[srcIndex] == '0') {
        remaining--
        srcIndex++
    }

    if (remaining == 0) {
        return if(signed) -0.0 else 0.0
    }

    var count: Int = minOf(remaining, 9)
    remaining -= count
    var resultValue = digitsToInt(number, srcIndex, count).toULong()
    srcIndex += count

    if (remaining > 0) {
        count = minOf(remaining, 9)
        remaining -= count

        // get the denormalized power of 10
        val mult: UInt = (s_Pow10MantissaTable[count - 1] shr (64 - s_Pow10ExponentTable[count - 1])).toUInt()
        resultValue = mul32x32To64((resultValue).toUInt(), mult) + digitsToInt(number, srcIndex, count)
        srcIndex += count
    }

    val scale = numberScale - (total - remaining)
    val absScale: Int = abs(scale)
    if (absScale >= 22 * 16) {
        // overflow / underflow
        return if (scale > 0) {
            if(signed) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
        } else {
            if(signed) -0.0 else 0.0
        }
    }

    var exp = 64

    // normalize the mantissa
    if ((resultValue and 0xFFFFFFFF_00000000UL) == 0UL) { resultValue = resultValue shl 32; exp -= 32 }
    if ((resultValue and 0xFFFF0000_00000000UL) == 0UL) { resultValue = resultValue shl 16; exp -= 16 }
    if ((resultValue and 0xFF000000_00000000UL) == 0UL) { resultValue = resultValue shl 8; exp -= 8 }
    if ((resultValue and 0xF0000000_00000000UL) == 0UL) { resultValue = resultValue shl 4; exp -= 4 }
    if ((resultValue and 0xC0000000_00000000UL) == 0UL) { resultValue = resultValue shl 2; exp -= 2 }
    if ((resultValue and 0x80000000_00000000UL) == 0UL) { resultValue = resultValue shl 1; exp -= 1 }

    var index: Int = absScale and 15
    if (index != 0) {
        val multexp: Int = s_Pow10ExponentTable[index - 1].toInt()
        // the exponents are shared between the inverted and regular table
        exp += if(scale < 0) (-multexp + 1) else multexp

        val multVal: ULong = s_Pow10MantissaTable[index + (if (scale < 0) 15 else 0) - 1]

        resultValue = mul64Lossy(resultValue, multVal)
        // normalize
        if ((resultValue and 0x80000000_00000000UL) == 0UL) {
            resultValue = resultValue shl 1
            exp--
        }
    }

    index = absScale ushr 4
    if (index != 0) {
        val multexp: Int = s_Pow10By16ExponentTable[index - 1].toInt()
        // the exponents are shared between the inverted and regular table
        exp += if (scale < 0) (-multexp + 1) else multexp

        val multVal: ULong = s_Pow10By16MantissaTable[index + (if (scale < 0) 21 else 0) - 1]
        resultValue = mul64Lossy(resultValue, multVal)
        // normalize
        if ((resultValue and 0x80000000_00000000UL) == 0UL) {
            resultValue = resultValue shl 1
            exp--
        }
    }

    // round & scale down
    if ((resultValue and (1UL shl 10)) != 0UL) {
        var tmp: ULong = resultValue + ((1UL shl 10) - 1UL) + ((resultValue shr 11) and 1UL)
        if (tmp < resultValue) {
            // overflow
            tmp = (tmp shr 1) or 0x8000000000000000UL
            exp += 1
        }
        resultValue = tmp
    }

    // return the exponent to a biased state
    exp += 0x3FE

    // handle overflow, underflow, "Epsilon - 1/2 Epsilon", denormalized, and the normal case
    if (exp <= 0) {
        if (exp == -52 && (resultValue >= 0x8000000000000058UL)) {
            // round X where {Epsilon > X >= 2.470328229206232730000000E-324} up to Epsilon (instead of down to zero)
            resultValue = 0x0000000000000001UL
        } else if (exp <= -52) {
            // underflow
            resultValue = 0UL
        } else {
            // denormalized
            resultValue = resultValue shr (-exp + 11 + 1)
        }
    }
    else if (exp >= 0x7FF) {
        // overflow
        resultValue = 0x7FF0000000000000UL
    } else {
        // normal postive exponent case
        resultValue = (exp.toULong() shl 52) + ((resultValue shr 11) and 0x000FFFFFFFFFFFFFUL)
    }

    if (signed) {
        resultValue = resultValue or 0x8000000000000000UL
    }

    return Double.fromBits(resultValue.toLong())
}