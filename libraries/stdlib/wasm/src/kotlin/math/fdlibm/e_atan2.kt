/* @(#)e_atan2.c 1.3 95/01/18 */
/*
 * ====================================================
 * Copyright (C) 1993 by Sun Microsystems, Inc. All rights reserved.
 *
 * Developed at SunSoft, a Sun Microsystems, Inc. business.
 * Permission to use, copy, modify, and distribute this
 * software is freely granted, provided that this notice 
 * is preserved.
 * ====================================================
 *
 */

/* __ieee754_atan2(y,x)
 * Method :
 *	1. Reduce y to positive by atan2(y,x)=-atan2(-y,x).
 *	2. Reduce x to positive by (if x and y are unexceptional): 
 *		ARG (x+iy) = arctan(y/x)   	   ... if x > 0,
 *		ARG (x+iy) = pi - arctan[y/(-x)]   ... if x < 0,
 *
 * Special cases:
 *
 *	ATAN2((anything), NaN ) is NaN;
 *	ATAN2(NAN , (anything) ) is NaN;
 *	ATAN2(+-0, +(anything but NaN)) is +-0  ;
 *	ATAN2(+-0, -(anything but NaN)) is +-pi ;
 *	ATAN2(+-(anything but 0 and NaN), 0) is +-pi/2;
 *	ATAN2(+-(anything but INF and NaN), +INF) is +-0 ;
 *	ATAN2(+-(anything but INF and NaN), -INF) is +-pi;
 *	ATAN2(+-INF,+INF ) is +-pi/4 ;
 *	ATAN2(+-INF,-INF ) is +-3pi/4;
 *	ATAN2(+-INF, (anything but,0,NaN, and INF)) is +-pi/2;
 *
 * Constants:
 * The hexadecimal values are the intended ones for the following 
 * constants. The decimal values may be used, provided that the 
 * compiler will convert from decimal to binary accurately enough 
 * to produce the hexadecimal values shown.
 */

package kotlin.math.fdlibm

private const val tiny = 1.0e-300
private const val zero = 0.0

private const val pi_o_4 = 7.8539816339744827900E-01 /* 0x3FE921FB, 0x54442D18 */
private const val pi_o_2 = 1.5707963267948965580E+00 /* 0x3FF921FB, 0x54442D18 */
private const val pi = 3.1415926535897931160E+00 /* 0x400921FB, 0x54442D18 */
private const val pi_lo = 1.2246467991473531772E-16 /* 0x3CA1A626, 0x33145C07 */

internal fun __ieee754_atan2(y: Double, x: Double): Double {
    var z: Double
    var k: Int
    var m: Int
    var hx: Int
    var hy: Int
    var ix: Int
    var iy: Int
    var lx: UInt
    var ly: UInt

    hx = __HI(x); ix = hx and 0x7fffffff
    lx = __LOu(x)
    hy = __HI(y); iy = hy and 0x7fffffff
    ly = __LOu(y)
    if (((ix or ((lx or lx.negate()) shr 31).toInt()) > 0x7ff00000) ||
        ((iy or ((ly or ly.negate()) shr 31).toInt()) > 0x7ff00000)
    )    /* x or y is NaN */
        return x + y
    if (((hx - 0x3ff00000) or lx.toInt()) == 0) return atan(y)   /* x=1.0 */
    m = ((hy shr 31) and 1) or ((hx shr 30) and 2)    /* 2*sign(x)+sign(y) */

    /* when y = 0 */
    if ((iy or ly.toInt()) == 0) {
        when (m) {
            0, 1 -> return y    /* atan(+-0,+anything)=+-0 */
            2 -> return pi + tiny/* atan(+0,-anything) = pi */
            3 -> return -pi - tiny/* atan(-0,-anything) =-pi */
        }
    }
    /* when x = 0 */
    if ((ix or lx.toInt()) == 0) return if (hy < 0) -pi_o_2 - tiny else pi_o_2 + tiny

    /* when x is INF */
    if (ix == 0x7ff00000) {
        if (iy == 0x7ff00000) {
            when (m) {
                0 -> return pi_o_4 + tiny/* atan(+INF,+INF) */
                1 -> return -pi_o_4 - tiny/* atan(-INF,+INF) */
                2 -> return 3.0 * pi_o_4 + tiny/*atan(+INF,-INF)*/
                3 -> return -3.0 * pi_o_4 - tiny/*atan(-INF,-INF)*/
            }
        } else {
            when (m) {
                0 -> return zero    /* atan(+...,+INF) */
                1 -> return -zero    /* atan(-...,+INF) */
                2 -> return pi + tiny    /* atan(+...,-INF) */
                3 -> return -pi - tiny    /* atan(-...,-INF) */
            }
        }
    }
    /* when y is INF */
    if (iy == 0x7ff00000) return if (hy < 0) -pi_o_2 - tiny else pi_o_2 + tiny

    /* compute y/x */
    k = (iy - ix) shr 20
    if (k > 60) z = pi_o_2 + 0.5 * pi_lo    /* |y/x| >  2**60 */
    else if (hx < 0 && k < -60) z = 0.0    /* |y|/x < -2**60 */
    else z = atan(fabs(y / x))        /* safe to do y/x */
    when (m) {
        0 -> return z    /* atan(+,+) */
        1 -> {
            z = doubleSetWord(d = z, hi = __HI(z) xor Int.MIN_VALUE)
            return z    /* atan(-,+) */
        }
        2 -> return pi - (z - pi_lo)/* atan(+,-) */
        else -> /* case 3 */ return (z - pi_lo) - pi/* atan(-,-) */
    }
}
