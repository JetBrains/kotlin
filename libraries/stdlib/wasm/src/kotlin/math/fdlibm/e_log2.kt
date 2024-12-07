/* @(#)e_log2.c 1.3 95/01/18 */
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

/* __ieee754_log2(x)
 * Return the base 2 logarithm of x
 *
 * Method :
 *	Let ivln2   = 1/log(2) rounded.
 *	Then
 *		n = ilogb(x),
 *		if(n<0)  n = n+1;
 *		x = scalbn(x,-n);
 *		log2(x) := n + ivln2*log(x)
 *
 * Special cases:
 *	log2(x) is NaN with signal if x < 0;
 *	log2(+INF) is +INF with no signal; log2(0) is -INF with signal;
 *	log2(NaN) is that NaN with no signal;
 *	log2(2**N) = N  for N=−1022 to +1023.
 *
 * Constants:
 * The hexadecimal values are the intended ones for the following constants.
 * The decimal values may be used, provided that the compiler will convert
 * from decimal to binary accurately enough to produce the hexadecimal values
 * shown.
 */

package kotlin.math.fdlibm

private const val two54 = 1.80143985094819840000e+16 /* 0x43500000, 0x00000000 */
private const val ivln2 = 0.14426950408889634073e+01 /* 0x3ff71547, 0x652b82fe */

private const val zero = 0.0

internal fun __ieee754_log2(_x: Double): Double {
    var x: Double = _x
    var y: Double
    var z: Double
    var i: Int
    var k: Int
    var hx: Int
    var lx: UInt

    hx = __HI(x)    /* high word of x */
    lx = __LOu(x)    /* low word of x */

    k = 0
    if (hx < 0x00100000) {                  /* x < 2**-1022  */
        if (((hx and 0x7fffffff) or lx.toInt()) == 0)
            return Double.NEGATIVE_INFINITY             /* log(+-0)=-inf */
        if (hx < 0) return Double.NaN        /* log(-#) = NaN */
        k -= 54; x *= two54 /* subnormal number, scale up x */
        hx = __HI(x)                /* high word of x */
    }
    if (hx >= 0x7ff00000) return x + x
    k += (hx shr 20) - 1023
    i = ((k.toUInt() and Int.MIN_VALUE.toUInt()) shr 31).toInt()
    hx = (hx and 0x000fffff) or ((0x3ff - i) shl 20)
    y = (k + i).toDouble()
    x = doubleSetWord(d = x, hi = hx)
    z = y + ivln2 * __ieee754_log(x)
    return z
}
