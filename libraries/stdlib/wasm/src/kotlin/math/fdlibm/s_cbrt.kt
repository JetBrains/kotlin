/* @(#)s_cbrt.c 1.3 95/01/18 */
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

/* ieee_cbrt(x)
 * Return cube root of x
 */

package kotlin.math.fdlibm

private const val B1 = 715094163 /* B1 = (682-0.03306235651)*2**20 */
private const val B2 = 696219795 /* B2 = (664-0.03306235651)*2**20 */
private const val C =  5.42857142857142815906e-01 /* 19/35     = 0x3FE15F15, 0xF15F15F1 */
private const val D = -7.05306122448979611050e-01 /* -864/1225 = 0xBFE691DE, 0x2532C834 */
private const val E =  1.41428571428571436819e+00 /* 99/70     = 0x3FF6A0EA, 0x0EA0EA0F */
private const val F =  1.60714285714285720630e+00 /* 45/28     = 0x3FF9B6DB, 0x6DB6DB6E */
private const val G =  3.57142857142857150787e-01 /* 5/14      = 0x3FD6DB6D, 0xB6DB6DB7 */

internal fun __ieee754_cbrt(v: Double): Double {
    var hx: Int = 0
    var r: Double = 0.0
    var s: Double = 0.0
    var t: Double = 0.0
    var w: Double = 0.0
    var sign: Int = 0

    var x = v

    hx = __HI(x) /* high word of x */
    sign = hx and 0x80000000.toInt() /* sign= sign(x) */
    hx = hx xor sign
    if (hx >= 0x7ff00000) {  /* ieee_cbrt(NaN,INF) is itself */
        return (x + x)
    }
    if ((hx or __LO(x)) == 0) {
        return (x) /* ieee_cbrt(0) is itself */
    }

    x = doubleSetWord(d = x, hi = hx) /* x <- |x| */
    /* rough cbrt to 5 bits */
    if (hx < 0x00100000) { /* subnormal number */
        t = doubleSetWord(d = t, hi = 0x43500000) /* set t= 2**54 */
        t *= x
        t = doubleSetWord(d = t, hi = __HI(t) / 3 + B2)
    } else {
        t = doubleSetWord(d = t, hi = hx / 3 + B1)
    }

    /* new cbrt to 23 bits, may be implemented in single precision */
    r = t * t / x
    s = C + r * t
    t *= G + F / (s + E + D / s)

    /* chopped to 20 bits and make it larger than ieee_cbrt(x) */
    t = doubleSetWord(d = t, hi = __HI(t) + 0x00000001, lo = 0)

    /* one step newton iteration to 53 bits with error less than 0.667 ulps */
    s = t * t /* t*t is exact */
    r = x / s
    w = t + t
    r = (r - t) / (w + r) /* r-s is exact */
    t = t + t * r

    /* retore the sign bit */
    t = doubleSetWord(d = t, hi = __HI(t) or sign)
    return (t)
}
