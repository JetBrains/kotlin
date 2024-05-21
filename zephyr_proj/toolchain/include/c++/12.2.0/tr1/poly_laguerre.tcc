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

/** @file tr1/poly_laguerre.tcc
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{tr1/cmath}
 */

//
// ISO C++ 14882 TR1: 5.2  Special functions
//

// Written by Edward Smith-Rowland based on:
//   (1) Handbook of Mathematical Functions,
//       Ed. Milton Abramowitz and Irene A. Stegun,
//       Dover Publications,
//       Section 13, pp. 509-510, Section 22 pp. 773-802
//   (2) The Gnu Scientific Library, http://www.gnu.org/software/gsl

#ifndef _GLIBCXX_TR1_POLY_LAGUERRE_TCC
#define _GLIBCXX_TR1_POLY_LAGUERRE_TCC 1

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
     *   @brief This routine returns the associated Laguerre polynomial 
     *          of order @f$ n @f$, degree @f$ \alpha @f$ for large n.
     *   Abramowitz & Stegun, 13.5.21
     *
     *   @param __n The order of the Laguerre function.
     *   @param __alpha The degree of the Laguerre function.
     *   @param __x The argument of the Laguerre function.
     *   @return The value of the Laguerre function of order n,
     *           degree @f$ \alpha @f$, and argument x.
     *
     *  This is from the GNU Scientific Library.
     */
    template<typename _Tpa, typename _Tp>
    _Tp
    __poly_laguerre_large_n(unsigned __n, _Tpa __alpha1, _Tp __x)
    {
      const _Tp __a = -_Tp(__n);
      const _Tp __b = _Tp(__alpha1) + _Tp(1);
      const _Tp __eta = _Tp(2) * __b - _Tp(4) * __a;
      const _Tp __cos2th = __x / __eta;
      const _Tp __sin2th = _Tp(1) - __cos2th;
      const _Tp __th = std::acos(std::sqrt(__cos2th));
      const _Tp __pre_h = __numeric_constants<_Tp>::__pi_2()
                        * __numeric_constants<_Tp>::__pi_2()
                        * __eta * __eta * __cos2th * __sin2th;

#if _GLIBCXX_USE_C99_MATH_TR1
      const _Tp __lg_b = _GLIBCXX_MATH_NS::lgamma(_Tp(__n) + __b);
      const _Tp __lnfact = _GLIBCXX_MATH_NS::lgamma(_Tp(__n + 1));
#else
      const _Tp __lg_b = __log_gamma(_Tp(__n) + __b);
      const _Tp __lnfact = __log_gamma(_Tp(__n + 1));
#endif

      _Tp __pre_term1 = _Tp(0.5L) * (_Tp(1) - __b)
                      * std::log(_Tp(0.25L) * __x * __eta);
      _Tp __pre_term2 = _Tp(0.25L) * std::log(__pre_h);
      _Tp __lnpre = __lg_b - __lnfact + _Tp(0.5L) * __x
                      + __pre_term1 - __pre_term2;
      _Tp __ser_term1 = std::sin(__a * __numeric_constants<_Tp>::__pi());
      _Tp __ser_term2 = std::sin(_Tp(0.25L) * __eta
                              * (_Tp(2) * __th
                               - std::sin(_Tp(2) * __th))
                               + __numeric_constants<_Tp>::__pi_4());
      _Tp __ser = __ser_term1 + __ser_term2;

      return std::exp(__lnpre) * __ser;
    }


    /**
     *  @brief  Evaluate the polynomial based on the confluent hypergeometric
     *          function in a safe way, with no restriction on the arguments.
     *
     *   The associated Laguerre function is defined by
     *   @f[
     *       L_n^\alpha(x) = \frac{(\alpha + 1)_n}{n!}
     *                       _1F_1(-n; \alpha + 1; x)
     *   @f]
     *   where @f$ (\alpha)_n @f$ is the Pochhammer symbol and
     *   @f$ _1F_1(a; c; x) @f$ is the confluent hypergeometric function.
     *
     *  This function assumes x != 0.
     *
     *  This is from the GNU Scientific Library.
     */
    template<typename _Tpa, typename _Tp>
    _Tp
    __poly_laguerre_hyperg(unsigned int __n, _Tpa __alpha1, _Tp __x)
    {
      const _Tp __b = _Tp(__alpha1) + _Tp(1);
      const _Tp __mx = -__x;
      const _Tp __tc_sgn = (__x < _Tp(0) ? _Tp(1)
                         : ((__n % 2 == 1) ? -_Tp(1) : _Tp(1)));
      //  Get |x|^n/n!
      _Tp __tc = _Tp(1);
      const _Tp __ax = std::abs(__x);
      for (unsigned int __k = 1; __k <= __n; ++__k)
        __tc *= (__ax / __k);

      _Tp __term = __tc * __tc_sgn;
      _Tp __sum = __term;
      for (int __k = int(__n) - 1; __k >= 0; --__k)
        {
          __term *= ((__b + _Tp(__k)) / _Tp(int(__n) - __k))
                  * _Tp(__k + 1) / __mx;
          __sum += __term;
        }

      return __sum;
    }


    /**
     *   @brief This routine returns the associated Laguerre polynomial 
     *          of order @f$ n @f$, degree @f$ \alpha @f$: @f$ L_n^\alpha(x) @f$
     *          by recursion.
     *
     *   The associated Laguerre function is defined by
     *   @f[
     *       L_n^\alpha(x) = \frac{(\alpha + 1)_n}{n!}
     *                       _1F_1(-n; \alpha + 1; x)
     *   @f]
     *   where @f$ (\alpha)_n @f$ is the Pochhammer symbol and
     *   @f$ _1F_1(a; c; x) @f$ is the confluent hypergeometric function.
     *
     *   The associated Laguerre polynomial is defined for integral
     *   @f$ \alpha = m @f$ by:
     *   @f[
     *       L_n^m(x) = (-1)^m \frac{d^m}{dx^m} L_{n + m}(x)
     *   @f]
     *   where the Laguerre polynomial is defined by:
     *   @f[
     *       L_n(x) = \frac{e^x}{n!} \frac{d^n}{dx^n} (x^ne^{-x})
     *   @f]
     *
     *   @param __n The order of the Laguerre function.
     *   @param __alpha The degree of the Laguerre function.
     *   @param __x The argument of the Laguerre function.
     *   @return The value of the Laguerre function of order n,
     *           degree @f$ \alpha @f$, and argument x.
     */
    template<typename _Tpa, typename _Tp>
    _Tp
    __poly_laguerre_recursion(unsigned int __n, _Tpa __alpha1, _Tp __x)
    {
      //   Compute l_0.
      _Tp __l_0 = _Tp(1);
      if  (__n == 0)
        return __l_0;

      //  Compute l_1^alpha.
      _Tp __l_1 = -__x + _Tp(1) + _Tp(__alpha1);
      if  (__n == 1)
        return __l_1;

      //  Compute l_n^alpha by recursion on n.
      _Tp __l_n2 = __l_0;
      _Tp __l_n1 = __l_1;
      _Tp __l_n = _Tp(0);
      for  (unsigned int __nn = 2; __nn <= __n; ++__nn)
        {
            __l_n = (_Tp(2 * __nn - 1) + _Tp(__alpha1) - __x)
                  * __l_n1 / _Tp(__nn)
                  - (_Tp(__nn - 1) + _Tp(__alpha1)) * __l_n2 / _Tp(__nn);
            __l_n2 = __l_n1;
            __l_n1 = __l_n;
        }

      return __l_n;
    }


    /**
     *   @brief This routine returns the associated Laguerre polynomial
     *          of order n, degree @f$ \alpha @f$: @f$ L_n^alpha(x) @f$.
     *
     *   The associated Laguerre function is defined by
     *   @f[
     *       L_n^\alpha(x) = \frac{(\alpha + 1)_n}{n!}
     *                       _1F_1(-n; \alpha + 1; x)
     *   @f]
     *   where @f$ (\alpha)_n @f$ is the Pochhammer symbol and
     *   @f$ _1F_1(a; c; x) @f$ is the confluent hypergeometric function.
     *
     *   The associated Laguerre polynomial is defined for integral
     *   @f$ \alpha = m @f$ by:
     *   @f[
     *       L_n^m(x) = (-1)^m \frac{d^m}{dx^m} L_{n + m}(x)
     *   @f]
     *   where the Laguerre polynomial is defined by:
     *   @f[
     *       L_n(x) = \frac{e^x}{n!} \frac{d^n}{dx^n} (x^ne^{-x})
     *   @f]
     *
     *   @param __n The order of the Laguerre function.
     *   @param __alpha The degree of the Laguerre function.
     *   @param __x The argument of the Laguerre function.
     *   @return The value of the Laguerre function of order n,
     *           degree @f$ \alpha @f$, and argument x.
     */
    template<typename _Tpa, typename _Tp>
    _Tp
    __poly_laguerre(unsigned int __n, _Tpa __alpha1, _Tp __x)
    {
      if (__x < _Tp(0))
        std::__throw_domain_error(__N("Negative argument "
                                      "in __poly_laguerre."));
      //  Return NaN on NaN input.
      else if (__isnan(__x))
        return std::numeric_limits<_Tp>::quiet_NaN();
      else if (__n == 0)
        return _Tp(1);
      else if (__n == 1)
        return _Tp(1) + _Tp(__alpha1) - __x;
      else if (__x == _Tp(0))
        {
          _Tp __prod = _Tp(__alpha1) + _Tp(1);
          for (unsigned int __k = 2; __k <= __n; ++__k)
            __prod *= (_Tp(__alpha1) + _Tp(__k)) / _Tp(__k);
          return __prod;
        }
      else if (__n > 10000000 && _Tp(__alpha1) > -_Tp(1)
            && __x < _Tp(2) * (_Tp(__alpha1) + _Tp(1)) + _Tp(4 * __n))
        return __poly_laguerre_large_n(__n, __alpha1, __x);
      else if (_Tp(__alpha1) >= _Tp(0)
           || (__x > _Tp(0) && _Tp(__alpha1) < -_Tp(__n + 1)))
        return __poly_laguerre_recursion(__n, __alpha1, __x);
      else
        return __poly_laguerre_hyperg(__n, __alpha1, __x);
    }


    /**
     *   @brief This routine returns the associated Laguerre polynomial
     *          of order n, degree m: @f$ L_n^m(x) @f$.
     *
     *   The associated Laguerre polynomial is defined for integral
     *   @f$ \alpha = m @f$ by:
     *   @f[
     *       L_n^m(x) = (-1)^m \frac{d^m}{dx^m} L_{n + m}(x)
     *   @f]
     *   where the Laguerre polynomial is defined by:
     *   @f[
     *       L_n(x) = \frac{e^x}{n!} \frac{d^n}{dx^n} (x^ne^{-x})
     *   @f]
     *
     *   @param __n The order of the Laguerre polynomial.
     *   @param __m The degree of the Laguerre polynomial.
     *   @param __x The argument of the Laguerre polynomial.
     *   @return The value of the associated Laguerre polynomial of order n,
     *           degree m, and argument x.
     */
    template<typename _Tp>
    inline _Tp
    __assoc_laguerre(unsigned int __n, unsigned int __m, _Tp __x)
    { return __poly_laguerre<unsigned int, _Tp>(__n, __m, __x); }


    /**
     *   @brief This routine returns the Laguerre polynomial
     *          of order n: @f$ L_n(x) @f$.
     *
     *   The Laguerre polynomial is defined by:
     *   @f[
     *       L_n(x) = \frac{e^x}{n!} \frac{d^n}{dx^n} (x^ne^{-x})
     *   @f]
     *
     *   @param __n The order of the Laguerre polynomial.
     *   @param __x The argument of the Laguerre polynomial.
     *   @return The value of the Laguerre polynomial of order n
     *           and argument x.
     */
    template<typename _Tp>
    inline _Tp
    __laguerre(unsigned int __n, _Tp __x)
    { return __poly_laguerre<unsigned int, _Tp>(__n, 0, __x); }
  } // namespace __detail
#undef _GLIBCXX_MATH_NS
#if ! _GLIBCXX_USE_STD_SPEC_FUNCS && defined(_GLIBCXX_TR1_CMATH)
} // namespace tr1
#endif

_GLIBCXX_END_NAMESPACE_VERSION
}

#endif // _GLIBCXX_TR1_POLY_LAGUERRE_TCC
