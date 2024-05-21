// Special functions -*- C++ -*-

// Copyright (C) 2006-2022 Free Software Foundation, Inc.
//
// This file is part of the GNU ISO C++ Library.  This library is free
// software; you can redistribute it and/or modify it under the
// terms of the GNU General Public License as published by the
// Free Software Foundation; either version 3, or (at your option)
// any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// Under Section 7 of GPL version 3, you are granted additional
// permissions described in the GCC Runtime Library Exception, version
// 3.1, as published by the Free Software Foundation.

// You should have received a copy of the GNU General Public License and
// a copy of the GCC Runtime Library Exception along with this program;
// see the files COPYING3 and COPYING.RUNTIME respectively.  If not, see
// <http://www.gnu.org/licenses/>.

/** @file tr1/legendre_function.tcc
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{tr1/cmath}
 */

//
// ISO C++ 14882 TR1: 5.2  Special functions
//

// Written by Edward Smith-Rowland based on:
//   (1) Handbook of Mathematical Functions,
//       ed. Milton Abramowitz and Irene A. Stegun,
//       Dover Publications,
//       Section 8, pp. 331-341
//   (2) The Gnu Scientific Library, http://www.gnu.org/software/gsl
//   (3) Numerical Recipes in C, by W. H. Press, S. A. Teukolsky,
//       W. T. Vetterling, B. P. Flannery, Cambridge University Press (1992),
//       2nd ed, pp. 252-254

#ifndef _GLIBCXX_TR1_LEGENDRE_FUNCTION_TCC
#define _GLIBCXX_TR1_LEGENDRE_FUNCTION_TCC 1

#include <tr1/special_function_util.h>

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

#if _GLIBCXX_USE_STD_SPEC_FUNCS
# define _GLIBCXX_MATH_NS ::std
#elif defined(_GLIBCXX_TR1_CMATH)
namespace tr1
{
# define _GLIBCXX_MATH_NS ::std::tr1
#else
# error do not include this header directly, use <cmath> or <tr1/cmath>
#endif
  // [5.2] Special functions

  // Implementation-space details.
  namespace __detail
  {
    /**
     *   @brief  Return the Legendre polynomial by recursion on degree
     *           @f$ l @f$.
     * 
     *   The Legendre function of @f$ l @f$ and @f$ x @f$,
     *   @f$ P_l(x) @f$, is defined by:
     *   @f[
     *     P_l(x) = \frac{1}{2^l l!}\frac{d^l}{dx^l}(x^2 - 1)^{l}
     *   @f]
     * 
     *   @param  l  The degree of the Legendre polynomial.  @f$l >= 0@f$.
     *   @param  x  The argument of the Legendre polynomial.  @f$|x| <= 1@f$.
     */
    template<typename _Tp>
    _Tp
    __poly_legendre_p(unsigned int __l, _Tp __x)
    {

      if (__isnan(__x))
        return std::numeric_limits<_Tp>::quiet_NaN();
      else if (__x == +_Tp(1))
        return +_Tp(1);
      else if (__x == -_Tp(1))
        return (__l % 2 == 1 ? -_Tp(1) : +_Tp(1));
      else
        {
          _Tp __p_lm2 = _Tp(1);
          if (__l == 0)
            return __p_lm2;

          _Tp __p_lm1 = __x;
          if (__l == 1)
            return __p_lm1;

          _Tp __p_l = 0;
          for (unsigned int __ll = 2; __ll <= __l; ++__ll)
            {
              //  This arrangement is supposed to be better for roundoff
              //  protection, Arfken, 2nd Ed, Eq 12.17a.
              __p_l = _Tp(2) * __x * __p_lm1 - __p_lm2
                    - (__x * __p_lm1 - __p_lm2) / _Tp(__ll);
              __p_lm2 = __p_lm1;
              __p_lm1 = __p_l;
            }

          return __p_l;
        }
    }


    /**
     *   @brief  Return the associated Legendre function by recursion
     *           on @f$ l @f$.
     * 
     *   The associated Legendre function is derived from the Legendre function
     *   @f$ P_l(x) @f$ by the Rodrigues formula:
     *   @f[
     *     P_l^m(x) = (1 - x^2)^{m/2}\frac{d^m}{dx^m}P_l(x)
     *   @f]
     *   @note @f$ P_l^m(x) = 0 @f$ if @f$ m > l @f$.
     * 
     *   @param  l  The degree of the associated Legendre function.
     *              @f$ l >= 0 @f$.
     *   @param  m  The order of the associated Legendre function.
     *   @param  x  The argument of the associated Legendre function.
     *              @f$ |x| <= 1 @f$.
     *   @param  phase  The phase of the associated Legendre function.
     *                  Use -1 for the Condon-Shortley phase convention.
     */
    template<typename _Tp>
    _Tp
    __assoc_legendre_p(unsigned int __l, unsigned int __m, _Tp __x,
		       _Tp __phase = _Tp(+1))
    {

      if (__m > __l)
        return _Tp(0);
      else if (__isnan(__x))
        return std::numeric_limits<_Tp>::quiet_NaN();
      else if (__m == 0)
        return __poly_legendre_p(__l, __x);
      else
        {
          _Tp __p_mm = _Tp(1);
          if (__m > 0)
            {
              //  Two square roots seem more accurate more of the time
              //  than just one.
              _Tp __root = std::sqrt(_Tp(1) - __x) * std::sqrt(_Tp(1) + __x);
              _Tp __fact = _Tp(1);
              for (unsigned int __i = 1; __i <= __m; ++__i)
                {
                  __p_mm *= __phase * __fact * __root;
                  __fact += _Tp(2);
                }
            }
          if (__l == __m)
            return __p_mm;

          _Tp __p_mp1m = _Tp(2 * __m + 1) * __x * __p_mm;
          if (__l == __m + 1)
            return __p_mp1m;

          _Tp __p_lm2m = __p_mm;
          _Tp __P_lm1m = __p_mp1m;
          _Tp __p_lm = _Tp(0);
          for (unsigned int __j = __m + 2; __j <= __l; ++__j)
            {
              __p_lm = (_Tp(2 * __j - 1) * __x * __P_lm1m
                      - _Tp(__j + __m - 1) * __p_lm2m) / _Tp(__j - __m);
              __p_lm2m = __P_lm1m;
              __P_lm1m = __p_lm;
            }

          return __p_lm;
        }
    }


    /**
     *   @brief  Return the spherical associated Legendre function.
     * 
     *   The spherical associated Legendre function of @f$ l @f$, @f$ m @f$,
     *   and @f$ \theta @f$ is defined as @f$ Y_l^m(\theta,0) @f$ where
     *   @f[
     *      Y_l^m(\theta,\phi) = (-1)^m[\frac{(2l+1)}{4\pi}
     *                                  \frac{(l-m)!}{(l+m)!}]
     *                     P_l^m(\cos\theta) \exp^{im\phi}
     *   @f]
     *   is the spherical harmonic function and @f$ P_l^m(x) @f$ is the
     *   associated Legendre function.
     * 
     *   This function differs from the associated Legendre function by
     *   argument (@f$x = \cos(\theta)@f$) and by a normalization factor
     *   but this factor is rather large for large @f$ l @f$ and @f$ m @f$
     *   and so this function is stable for larger differences of @f$ l @f$
     *   and @f$ m @f$.
     *   @note Unlike the case for __assoc_legendre_p the Condon-Shortley
     *         phase factor @f$ (-1)^m @f$ is present here.
     *   @note @f$ Y_l^m(\theta) = 0 @f$ if @f$ m > l @f$.
     * 
     *   @param  l  The degree of the spherical associated Legendre function.
     *              @f$ l >= 0 @f$.
     *   @param  m  The order of the spherical associated Legendre function.
     *   @param  theta  The radian angle argument of the spherical associated
     *                  Legendre function.
     */
    template <typename _Tp>
    _Tp
    __sph_legendre(unsigned int __l, unsigned int __m, _Tp __theta)
    {
      if (__isnan(__theta))
        return std::numeric_limits<_Tp>::quiet_NaN();

      const _Tp __x = std::cos(__theta);

      if (__m > __l)
        return _Tp(0);
      else if (__m == 0)
        {
          _Tp __P = __poly_legendre_p(__l, __x);
          _Tp __fact = std::sqrt(_Tp(2 * __l + 1)
                     / (_Tp(4) * __numeric_constants<_Tp>::__pi()));
          __P *= __fact;
          return __P;
        }
      else if (__x == _Tp(1) || __x == -_Tp(1))
        {
          //  m > 0 here
          return _Tp(0);
        }
      else
        {
          // m > 0 and |x| < 1 here

          // Starting value for recursion.
          // Y_m^m(x) = sqrt( (2m+1)/(4pi m) gamma(m+1/2)/gamma(m) )
          //             (-1)^m (1-x^2)^(m/2) / pi^(1/4)
          const _Tp __sgn = ( __m % 2 == 1 ? -_Tp(1) : _Tp(1));
          const _Tp __y_mp1m_factor = __x * std::sqrt(_Tp(2 * __m + 3));
#if _GLIBCXX_USE_C99_MATH_TR1
          const _Tp __lncirc = _GLIBCXX_MATH_NS::log1p(-__x * __x);
#else
          const _Tp __lncirc = std::log(_Tp(1) - __x * __x);
#endif
          //  Gamma(m+1/2) / Gamma(m)
#if _GLIBCXX_USE_C99_MATH_TR1
          const _Tp __lnpoch = _GLIBCXX_MATH_NS::lgamma(_Tp(__m + _Tp(0.5L)))
                             - _GLIBCXX_MATH_NS::lgamma(_Tp(__m));
#else
          const _Tp __lnpoch = __log_gamma(_Tp(__m + _Tp(0.5L)))
                             - __log_gamma(_Tp(__m));
#endif
          const _Tp __lnpre_val =
                    -_Tp(0.25L) * __numeric_constants<_Tp>::__lnpi()
                    + _Tp(0.5L) * (__lnpoch + __m * __lncirc);
          const _Tp __sr = std::sqrt((_Tp(2) + _Tp(1) / __m)
                         / (_Tp(4) * __numeric_constants<_Tp>::__pi()));
          _Tp __y_mm = __sgn * __sr * std::exp(__lnpre_val);
          _Tp __y_mp1m = __y_mp1m_factor * __y_mm;

          if (__l == __m)
            return __y_mm;
          else if (__l == __m + 1)
            return __y_mp1m;
          else
            {
              _Tp __y_lm = _Tp(0);

              // Compute Y_l^m, l > m+1, upward recursion on l.
              for (unsigned int __ll = __m + 2; __ll <= __l; ++__ll)
                {
                  const _Tp __rat1 = _Tp(__ll - __m) / _Tp(__ll + __m);
                  const _Tp __rat2 = _Tp(__ll - __m - 1) / _Tp(__ll + __m - 1);
                  const _Tp __fact1 = std::sqrt(__rat1 * _Tp(2 * __ll + 1)
                                                       * _Tp(2 * __ll - 1));
                  const _Tp __fact2 = std::sqrt(__rat1 * __rat2 * _Tp(2 * __ll + 1)
                                                                / _Tp(2 * __ll - 3));
                  __y_lm = (__x * __y_mp1m * __fact1
                         - (__ll + __m - 1) * __y_mm * __fact2) / _Tp(__ll - __m);
                  __y_mm = __y_mp1m;
                  __y_mp1m = __y_lm;
                }

              return __y_lm;
            }
        }
    }
  } // namespace __detail
#undef _GLIBCXX_MATH_NS
#if ! _GLIBCXX_USE_STD_SPEC_FUNCS && defined(_GLIBCXX_TR1_CMATH)
} // namespace tr1
#endif

_GLIBCXX_END_NAMESPACE_VERSION
}

#endif // _GLIBCXX_TR1_LEGENDRE_FUNCTION_TCC
