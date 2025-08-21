/* e_fmodf.c -- float version of e_fmod.c.
 * Conversion to float by Ian Lance Taylor, Cygnus Support, ian@cygnus.com.
 */
/*
 * ====================================================
 * Copyright (C) 1993 by Sun Microsystems, Inc. All rights reserved.
 *
 * Developed at SunPro, a Sun Microsystems, Inc. business.
 * Permission to use, copy, modify, and distribute this
 * software is freely granted, provided that this notice
 * is preserved.
 * ====================================================
 */

package kotlin.math.fdlibm

private val Zero = floatArrayOf(0.0f, -0.0f)
private const val one = 1.0f

internal fun __ieee754_fmodf(x_arg: Float, y: Float): Float {
    var x: Float = x_arg
    var n: Int
    var hx: Int
    var hy: Int
    var hz: Int
    var ix: Int
    var iy: Int
    var sx: Int
    var i: Int

    hx = x.toRawBits()
    hy = y.toRawBits()

    sx = hx and 0x80000000.toInt()        /* sign of x */
    hx = hx xor sx        /* |x| */
    hy = hy and 0x7fffffff    /* |y| */
    /* purge off exception values */
    if (hy == 0 || (hx >= 0x7f800000) ||        /* y=0,or x not finite */
        (hy > 0x7f800000)
    )            /* or y is NaN */
        return (x * y) / (x * y)
    if (hx < hy) return x            /* |x|<|y| return x */
    if (hx == hy) {
        return Zero[(sx.toUInt() shr 31).toInt()]    /* |x|=|y| return x*0*/
    }
    /* determine ix = ilogb(x) */
    if (hx < 0x00800000) {    /* subnormal x */
        //for (ix = -126,i=(hx<<8); i>0; i<<=1) ix -=1;
        ix = -126
        i = hx shl 8
        while (i > 0) {
            ix -= 1
            //--
            i = i shl 1
        }
    } else {
        ix = (hx ushr 23) - 127
    }
    /* determine iy = ilogb(y) */
    if (hy < 0x00800000) {    /* subnormal y */
        //for (iy = -126,i=(hy<<8); i>=0; i<<=1) iy -=1;
        iy = -126
        i = hy shl 8
        while (i >= 0) {
            iy -= 1
            //--
            i = i shl 1
        }
    } else {
        iy = (hy ushr 23) - 127
    }
    /* set up {hx,lx}, {hy,ly} and align y to x */
    if (ix >= -126)
        hx = 0x00800000 or (0x007fffff and hx)
    else {        /* subnormal x, shift x to normal */
        n = -126 - ix
        hx = hx shl n
    }
    if (iy >= -126) {
        hy = 0x00800000 or (0x007fffff and hy)
    } else {        /* subnormal y, shift y to normal */
        n = -126 - iy
        hy = hy shl n
    }
    /* fix point fmod */
    n = ix - iy
    while (n-- != 0) {
        hz = hx - hy
        if (hz < 0) {
            hx = hx + hx
        } else {
            if (hz == 0) {        /* return sign(x)*0 */
                return Zero[(sx.toUInt() shr 31).toInt()]
            }
            hx = hz + hz
        }
    }
    hz = hx - hy
    if (hz >= 0) {
        hx = hz
    }
    /* convert back to floating value and restore the sign */
    if (hx == 0) {            /* return sign(x)*0 */
        return Zero[(sx.toUInt() shr 31).toInt()]
    }
    while (hx < 0x00800000) {        /* normalize x */
        hx = hx + hx
        iy -= 1
    }
    if (iy >= -126) {        /* normalize output */
        hx = ((hx - 0x00800000) or ((iy + 127) shl 23))
        x = Float.fromBits((hx or sx))
    } else {        /* subnormal output */
        n = -126 - iy
        hx = hx ushr n
        x = Float.fromBits((hx or sx))
        x *= one        /* create necessary signal */
    }
    return x        /* exact output */
}