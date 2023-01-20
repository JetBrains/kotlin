/* @(#)s_scalbn.c 1.3 95/01/18 */
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
 * scalbn (double x, int n)
 * scalbn(x,n) returns x* 2**n  computed by  exponent  
 * manipulation rather than by actually performing an 
 * exponentiation or a multiplication.
 */

package kotlin.math.fdlibm

import kotlin.wasm.internal.wasm_f64_copysign as copysign

private const val two54 = 1.80143985094819840000e+16 /* 0x43500000, 0x00000000 */
private const val twom54 = 5.55111512312578270212e-17 /* 0x3C900000, 0x00000000 */
private const val huge = 1.0e+300
private const val tiny = 1.0e-300

internal fun scalbn(_x: Double, n: Int): Double {
    var x: Double = _x
    var k: Int
    var hx: Int
    var lx: Int
    hx = __HI(x)
    lx = __LO(x)
    k = ((hx and 0x7ff00000) shr 20)        /* extract exponent */
    if (k == 0) {                /* 0 or subnormal x */
        if ((lx or (hx and 0x7fffffff)) == 0) return x /* +-0 */
        x *= two54
        hx = __HI(x)
        k = (((hx and 0x7ff00000) shr 20) - 54)
        if (n < -50000) return tiny * x    /*underflow*/
    }
    if (k == 0x7ff) return x + x        /* NaN or Inf */
    k = k + n
    if (k > 0x7fe) return huge * copysign(huge, x) /* overflow  */
    if (k > 0)                /* normal result */ {
        x = doubleSetWord(d = x, hi = (hx and 0x800fffff.toInt()) or (k shl 20)); return x
    }
    if (k <= -54)
        if (n > 50000)    /* in case integer overflow in n+k */
            return huge * copysign(huge, x)    /*overflow*/
        else return tiny * copysign(tiny, x)    /*underflow*/
    k += 54                /* subnormal result */
    x = doubleSetWord(d = x, hi = (hx and 0x800fffff.toInt()) or (k shl 20))
    return x * twom54
}
