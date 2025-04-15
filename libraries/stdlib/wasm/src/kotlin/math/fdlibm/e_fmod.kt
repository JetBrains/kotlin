
/* @(#)e_fmod.c 1.3 95/01/18 */
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
 * __ieee754_fmod(x,y)
 * Return x mod y in exact arithmetic
 * Method: shift and subtract
 */

package kotlin.math.fdlibm

private const val one = 1.0
private val Zero = arrayOf(0.0, -0.0)

internal fun __ieee754_fmod(_x: Double, y: Double): Double {
    var x: Double = _x
    var n: Int
    var hx: Int
    var hy: Int
    var hz: Int
    var ix: Int
    var iy: Int
    val sx: Int
    var i: Int
    var lx: UInt
    var ly: UInt
    var lz: UInt

    hx = __HI(x)
    lx = __LOu(x)

    hy = __HI(y)
    ly = __LOu(y)

    sx = hx and 0x80000000.toInt()        /* sign of x */
    hx = hx xor sx        /* |x| */
    hy = hy and 0x7fffffff    /* |y| */
    /* purge off exception values */
    if ((hy or ly.toInt()) == 0 || (hx >= 0x7ff00000) ||    /* y=0,or x not finite */
        ((hy or ((ly or ly.negate()) shr 31).toInt()) > 0x7ff00000)
    ) {/* or y is NaN */
        return (x * y) / (x * y)
    }
    if (hx <= hy) {
        if ((hx < hy) || (lx < ly)) return x    /* |x|<|y| return x */
        if (lx == ly) {
            return Zero[(sx.toUInt() shr 31)    /* |x|=|y| return x*0*/.toInt()]
        }
    }
    /* determine ix = ilogb(x) */
    if (hx < 0x00100000) {    /* subnormal x */
        if (hx == 0) {
            //for (ix = -1043, i=lx; i>0; i<<=1) ix -=1;
            ix = -1043
            i = lx.toInt()
            while (i > 0) {
                ix -= 1
                //--
                i = i shl 1
            }
        } else {
            //for (ix = -1022,i=(hx<<11); i>0; i<<=1) ix -=1;
            ix = -1022
            i = hx shl 11
            while (i > 0) {
                ix -= 1
                //--
                i = i shl 1
            }
        }
    } else ix = (hx ushr 20) - 1023
    /* determine iy = ilogb(y) */
    if (hy < 0x00100000) {    /* subnormal y */
        if (hy == 0) {

            //for (iy = -1043, i=ly; i>0; i<<=1) iy -=1;
            iy = -1043
            i = ly.toInt()
            while (i > 0) {
                iy -= 1
                //--
                i = i shl 1
            }
        } else {
            //for (iy = -1022,i=(hy shl 11); i>0; i<<=1) iy -=1;
            iy = -1022
            i = hy shl 11
            while (i > 0) {
                iy -= 1
                //--
                i = i shl 1
            }
        }
    } else iy = (hy ushr 20) - 1023
    /* set up {hx,lx}, {hy,ly} and align y to x */
    if (ix >= -1022)
        hx = 0x00100000 or (0x000fffff and hx)
    else {        /* subnormal x, shift x to normal */
        n = -1022 - ix
        if (n <= 31) {
            hx = (hx shl n) or (lx shr (32 - n)).toInt()
            lx = lx shl n
        } else {
            hx = (lx shl (n - 32)).toInt()
            lx = 0.toUInt()
        }
    }
    if (iy >= -1022) {
        hy = 0x00100000 or (0x000fffff and hy)
    } else {        /* subnormal y, shift y to normal */
        n = -1022 - iy
        if (n <= 31) {
            hy = (hy shl n) or (ly shr (32 - n)).toInt()
            ly = ly shl n
        } else {
            hy = (ly shl (n - 32)).toInt()
            ly = 0U
        }
    }
    /* fix point fmod */
    n = ix - iy
    while (n-- != 0) {
        hz = hx - hy
        lz = lx - ly
        if (lx < ly) {
            hz -= 1
        }
        if (hz < 0) {
            hx = hx + hx + (lx shr 31).toInt()
            lx = lx + lx
        } else {
            if ((hz or lz.toInt()) == 0)        /* return sign(x)*0 */
                return Zero[(sx.toUInt() shr 31).toInt()]
            hx = hz + hz + (lz shr 31).toInt()
            lx = lz + lz
        }
    }
    hz = hx - hy
    lz = lx - ly
    if (lx < ly) {
        hz -= 1
    }
    if (hz >= 0) {
        hx = hz
        lx = lz
    }
    /* convert back to floating value and restore the sign */
    if ((hx or lx.toInt()) == 0)            /* return sign(x)*0 */
        return Zero[(sx.toUInt() shr 31).toInt()]
    while (hx < 0x00100000) {        /* normalize x */
        hx = hx + hx + (lx shr 31).toInt()
        lx = lx + lx
        iy -= 1
    }
    if (iy >= -1022) {    /* normalize output */
        hx = ((hx - 0x00100000) or ((iy + 1023) shl 20))
        x = doubleSetWord(hi = hx or sx, lo = lx.toInt())
    } else {        /* subnormal output */
        n = -1022 - iy
        if (n <= 20) {
            lx = (lx shr n) or (hx.toUInt() shl (32 - n))
            hx = hx ushr n
        } else if (n <= 31) {
            lx = ((hx shl (32 - n)) or (lx shr n).toInt()).toUInt()
            hx = sx
        } else {
            lx = (hx ushr (n - 32)).toUInt()
            hx = sx
        }
        x = doubleSetWord(hi = hx or sx, lo = lx.toInt())
        x *= one        /* create necessary signal */
    }
    return x        /* exact output */
}