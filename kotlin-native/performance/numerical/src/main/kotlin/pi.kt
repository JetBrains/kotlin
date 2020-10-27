/*
 * Computation of the n'th decimal digit of \pi with very little memory.
 * Written by Fabrice Bellard on January 8, 1997.
 *
 * We use a slightly modified version of the method described by Simon
 * Plouffe in "On the Computation of the n'th decimal digit of various
 * transcendental numbers" (November 1996). We have modified the algorithm
 * to get a running time of O(n^2) instead of O(n^3log(n)^3).
 */

import kotlin.math.ln
import kotlin.math.sqrt

private fun mul_mod(a: Int, b: Int, m: Int)
        = ((a.toLong() * b.toLong()) % m).toInt()

/* return the inverse of x mod y */
private fun inv_mod(x: Int, y: Int): Int {
    var u = x
    var v = y
    var c = 1
    var a = 0
    do {
        val q = v / u
        var t = c
        c = a - q * c
        a = t
        t = u
        u = v - q * u
        v = t
    } while (u != 0)
    a = a % y
    if (a < 0)
        a = y + a
    return a
}

/* return (a^b) mod m */
private fun pow_mod(a: Int, b: Int, m: Int): Int {
    var b = b
    var r = 1
    var aa = a
    while (true) {
        if (b and 1 != 0)
            r = mul_mod(r, aa, m)
        b = b shr 1
        if (b == 0)
            break
        aa = mul_mod(aa, aa, m)
    }
    return r
}

/* return true if n is prime */
private fun is_prime(n: Int): Boolean {
    if (n % 2 == 0)
        return false
    val r = sqrt(n.toDouble()).toInt()
    var i = 3
    while (i <= r) {
        if (n % i == 0)
            return false
        i += 2
    }
    return true
}

/* return the prime number immediatly after n */
private fun next_prime(n: Int): Int {
    var n = n
    do {
        n++
    } while (!is_prime(n))
    return n
}

fun pi_nth_digit(n: Int): Int {

    val N = ((n + 20) * ln(10.0) / ln(2.0)).toInt()
    var sum = 0.0
    var a = 3
    var t: Int

    while (a <= 2 * N) {

        val vmax = (ln((2 * N).toDouble()) / ln(a.toDouble())).toInt()
        var av = 1
        var i = 0
        while (i < vmax) {
            av = av * a
            i++
        }

        var s = 0
        var num = 1
        var den = 1
        var v = 0
        var kq = 1
        var kq2 = 1

        var k = 1
        while (k <= N) {

            t = k
            if (kq >= a) {
                do {
                    t = t / a
                    v--
                } while (t % a == 0)
                kq = 0
            }
            kq++
            num = mul_mod(num, t, av)

            t = 2 * k - 1
            if (kq2 >= a) {
                if (kq2 == a) {
                    do {
                        t = t / a
                        v++
                    } while (t % a == 0)
                }
                kq2 -= a
            }
            den = mul_mod(den, t, av)
            kq2 += 2

            if (v > 0) {
                t = inv_mod(den, av)
                t = mul_mod(t, num, av)
                t = mul_mod(t, k, av)
                i = v
                while (i < vmax) {
                    t = mul_mod(t, a, av)
                    i++
                }
                s += t
                if (s >= av)
                    s -= av
            }
            k++

        }

        t = pow_mod(10, n - 1, av)
        s = mul_mod(s, t, av)
        sum = (sum + s.toDouble() / av.toDouble()) % 1.0
        a = next_prime(a)
    }

    return (sum * 1e9).toInt()
}
