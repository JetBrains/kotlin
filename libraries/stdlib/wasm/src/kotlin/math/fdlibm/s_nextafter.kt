/* @(#)s_nextafter.c 1.3 95/01/18 */
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

/* IEEE functions
 *	nextafter(x,y)
 *	return the next machine floating-point number of x in the
 *	direction toward y.
 *   Special cases:
 */

package kotlin.math.fdlibm

internal fun nextafter(_x: Double, _y: Double): Double {
    var x: Double = _x
    var y: Double = _y
    var hx: Int
    var hy: Int
    var ix: Int
    var iy: Int
    var lx: UInt
    var ly: UInt

    hx = __HI(x)        /* high word of x */
    lx = __LOu(x)        /* low  word of x */
    hy = __HI(y)        /* high word of y */
    ly = __LOu(y)        /* low  word of y */
    ix = hx and 0x7fffffff        /* |x| */
    iy = hy and 0x7fffffff        /* |y| */

    if (((ix >= 0x7ff00000) && ((ix - 0x7ff00000) or lx.toInt()) != 0) ||   /* x is nan */
        ((iy >= 0x7ff00000) && ((iy - 0x7ff00000) or ly.toInt()) != 0)
    )     /* y is nan */
        return x + y
    if (x == y) return x        /* x=y, return x */
    if ((ix or lx.toInt()) == 0) {            /* x == 0 */
        x = doubleSetWord(d = x, hi = hy and Int.MIN_VALUE)    /* return +-minsubnormal */
        x = doubleSetWord(d = x, lo = 1)
        y = x * x
        if (y == x) return y; else return x    /* raise underflow flag */
    }
    if (hx >= 0) {                /* x > 0 */
        if (hx > hy || ((hx == hy) && (lx > ly))) {    /* x > y, x -= ulp */
            if (lx == 0U) hx -= 1
            lx -= 1U
        } else {                /* x < y, x += ulp */
            lx += 1U
            if (lx == 0U) hx += 1
        }
    } else {                /* x < 0 */
        if (hy >= 0 || hx > hy || ((hx == hy) && (lx > ly))) {/* x < y, x -= ulp */
            if (lx == 0U) hx -= 1
            lx -= 1U
        } else {                /* x > y, x += ulp */
            lx += 1U
            if (lx == 0U) hx += 1
        }
    }
    hy = hx and 0x7ff00000
    if (hy >= 0x7ff00000) return x + x    /* overflow  */
    if (hy < 0x00100000) {        /* underflow */
        y = x * x
        if (y != x) {        /* raise underflow flag */
            y = doubleSetWord(d = y, hi = hx, lo = lx.toInt())
            return y
        }
    }
    x = doubleSetWord(d = x, hi = hx, lo = lx.toInt())
    return x
}