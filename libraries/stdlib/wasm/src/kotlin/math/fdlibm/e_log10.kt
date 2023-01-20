/* @(#)e_log10.c 1.3 95/01/18 */
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

/* __ieee754_log10(x)
 * Return the base 10 logarithm of x
 * 
 * Method :
 *	Let log10_2hi = leading 40 bits of log10(2) and
 *	    log10_2lo = log10(2) - log10_2hi,
 *	    ivln10   = 1/log(10) rounded.
 *	Then
 *		n = ilogb(x), 
 *		if(n<0)  n = n+1;
 *		x = scalbn(x,-n);
 *		log10(x) := n*log10_2hi + (n*log10_2lo + ivln10*log(x))
 *
 * Note 1:
 *	To guarantee log10(10**n)=n, where 10**n is normal, the rounding 
 *	mode must set to Round-to-Nearest.
 * Note 2:
 *	[1/log(10)] rounded to 53 bits has error  .198   ulps;
 *	log10 is monotonic at all binary break points.
 *
 * Special cases:
 *	log10(x) is NaN with signal if x < 0; 
 *	log10(+INF) is +INF with no signal; log10(0) is -INF with signal;
 *	log10(NaN) is that NaN with no signal;
 *	log10(10**N) = N  for N=0,1,...,22.
 *
 * Constants:
 * The hexadecimal values are the intended ones for the following constants.
 * The decimal values may be used, provided that the compiler will convert
 * from decimal to binary accurately enough to produce the hexadecimal values
 * shown.
 */

package kotlin.math.fdlibm

private const val two54 = 1.80143985094819840000e+16 /* 0x43500000, 0x00000000 */
private const val ivln10 = 4.34294481903251816668e-01 /* 0x3FDBCB7B, 0x1526E50E */
private const val log10_2hi = 3.01029995663611771306e-01 /* 0x3FD34413, 0x509F6000 */
private const val log10_2lo = 3.69423907715893078616e-13 /* 0x3D59FEF3, 0x11F12B36 */

private const val zero = 0.0

internal fun __ieee754_log10(_x: Double): Double {
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
    z = y * log10_2lo + ivln10 * __ieee754_log(x)
    return z + y * log10_2hi
}
