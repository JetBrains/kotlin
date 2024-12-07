/* @(#)s_rint.c 1.3 95/01/18 */
/*
 * ====================================================
 * Copyright (C) 1993 by Sun Microsystems, Inc. All rights reserved.
 *
 * Developed at SunSoft, a Sun Microsystems, Inc. business.
 * Permission to use, copy, modify, and distribute this
 * software is freely granted, provided that this notice 
 * is preserved.
 * ====================================================
 */

/*
 * rint(x)
 * Return x rounded to integral value according to the prevailing
 * rounding mode.
 * Method:
 *	Using floating addition.
 * Exception:
 *	Inexact flag raised if x not equal to rint(x).
 */

package kotlin.math.fdlibm

private val TWO52 = doubleArrayOf(
    4.50359962737049600000e+15, /* 0x43300000, 0x00000000 */
    -4.50359962737049600000e+15, /* 0xC3300000, 0x00000000 */
)

internal fun rint(_x: Double): Double {
    var x: Double = _x
    var i0: Int
    var j0: Int
    var sx: Int
    var i: UInt
    var i1: UInt
    var w: Double
    var t: Double

    i0 = __HI(x)
    sx = (i0 shr 31) and 1
    i1 = __LOu(x)
    j0 = ((i0 shr 20) and 0x7ff) - 0x3ff
    if (j0 < 20) {
        if (j0 < 0) {
            if (((i0 and 0x7fffffff) or i1.toInt()) == 0) return x
            i1 = i1 or (i0 and 0x0fffff).toUInt()
            i0 = i0 and 0xfffe0000.toInt()
            i0 = i0 or (((i1 or i1.negate()) shr 12) and 0x80000.toUInt()).toInt()
            x = doubleSetWord(d = x, hi = i0)
            w = TWO52[sx] + x
            t = w - TWO52[sx]
            i0 = __HI(t)
            t = doubleSetWord(d = t, hi = (i0 and 0x7fffffff) or (sx shl 31))
            return t
        } else {
            i = ((0x000fffff) shr j0).toUInt()
            if (((i0 and i.toInt()) or i1.toInt()) == 0) return x /* x is integral */
            i = i shr 1
            if (((i0 and i.toInt()) or i1.toInt()) != 0) {
                if (j0 == 19) i1 = 0x40000000.toUInt(); else
                    i0 = (i0 and i.inv().toInt()) or ((0x20000) shr j0)
            }
        }
    } else if (j0 > 51) {
        if (j0 == 0x400) return x + x    /* inf or NaN */
        else return x        /* x is integral */
    } else {
        i = ((0xffffffff.toUInt())) shr (j0 - 20)
        if ((i1 and i) == 0U) return x    /* x is integral */
        i = i shr 1
        if ((i1 and i) != 0U) i1 = (i1 and (i.inv())) or ((0x40000000) shr (j0 - 20)).toUInt()
    }
    x = doubleSetWord(x, hi = i0, lo = i1.toInt())
    w = TWO52[sx] + x
    return w - TWO52[sx]
}