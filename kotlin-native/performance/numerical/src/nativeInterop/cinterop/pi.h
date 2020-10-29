/*
 * Computation of the n'th decimal digit of \pi with very little memory.
 * Written by Fabrice Bellard on January 8, 1997.
 *
 * We use a slightly modified version of the method described by Simon
 * Plouffe in "On the Computation of the n'th decimal digit of various
 * transcendental numbers" (November 1996). We have modified the algorithm
 * to get a running time of O(n^2) instead of O(n^3log(n)^3).
 */

#ifndef _BELLARD_PI_H
#define _BELLARD_PI_H

#ifdef __cplusplus
extern "C" {
#endif
int pi_nth_digit(int n);
#ifdef __cplusplus
}
#endif

#endif /*_BELLARD_PI_H*/
