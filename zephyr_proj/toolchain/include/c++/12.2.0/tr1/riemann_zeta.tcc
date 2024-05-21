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

/** @file tr1/riemann_zeta.tcc
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{tr1/cmath}
 */

//
// ISO C++ 14882 TR1: 5.2  Special functions
//

// Written by Edward Smith-Rowland based on:
//   (1) Handbook of Mathematical Functions,
//       Ed. by Milton Abramowitz and Irene A. Stegun,
//       Dover Publications, New-York, Section 5, pp. 807-808.
//   (2) The Gnu Scientific Library, http://www.gnu.org/software/gsl
//   (3) Gamma, Exploring Euler's Constant, Julian Havil,
//       Princeton, 2003.

#ifndef _GLIBCXX_TR1_RIEMANN_ZETA_TCC
#define _GLIBCXX_TR1_RIEMANN_ZETA_TCC 1

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
     *   @brief  Compute the Riemann zeta function @f$ \zeta(s) @f$
     *           by summation for s > 1.
     * 
     *   The Riemann zeta function is defined by:
     *    \f[
     *      \zeta(s) = \sum_{k=1}^{\infty} \frac{1}{k^{s}} for s > 1
     *    \f]
     *   For s < 1 use the reflection formula:
     *    \f[
     *      \zeta(s) = 2^s \pi^{s-1} \Gamma(1-s) \zeta(1-s)
     *    \f]
     */
    template<typename _Tp>
    _Tp
    __riemann_zeta_sum(_Tp __s)
    {
      //  A user shouldn't get to this.
      if (__s < _Tp(1))
        std::__throw_domain_error(__N("Bad argument in zeta sum."));

      const unsigned int max_iter = 10000;
      _Tp __zeta = _Tp(0);
      for (unsigned int __k = 1; __k < max_iter; ++__k)
        {
          _Tp __term = std::pow(static_cast<_Tp>(__k), -__s);
          if (__term < std::numeric_limits<_Tp>::epsilon())
            {
              break;
            }
          __zeta += __term;
        }

      return __zeta;
    }


    /**
     *   @brief  Evaluate the Riemann zeta function @f$ \zeta(s) @f$
     *           by an alternate series for s > 0.
     * 
     *   The Riemann zeta function is defined by:
     *    \f[
     *      \zeta(s) = \sum_{k=1}^{\infty} \frac{1}{k^{s}} for s > 1
     *    \f]
     *   For s < 1 use the reflection formula:
     *    \f[
     *      \zeta(s) = 2^s \pi^{s-1} \Gamma(1-s) \zeta(1-s)
     *    \f]
     */
    template<typename _Tp>
    _Tp
    __riemann_zeta_alt(_Tp __s)
    {
      _Tp __sgn = _Tp(1);
      _Tp __zeta = _Tp(0);
      for (unsigned int __i = 1; __i < 10000000; ++__i)
        {
          _Tp __term = __sgn / std::pow(__i, __s);
          if (std::abs(__term) < std::numeric_limits<_Tp>::epsilon())
            break;
          __zeta += __term;
          __sgn *= _Tp(-1);
        }
      __zeta /= _Tp(1) - std::pow(_Tp(2), _Tp(1) - __s);

      return __zeta;
    }


    /**
     *   @brief  Evaluate the Riemann zeta function by series for all s != 1.
     *           Convergence is great until largish negative numbers.
     *           Then the convergence of the > 0 sum gets better.
     *
     *   The series is:
     *    \f[
     *      \zeta(s) = \frac{1}{1-2^{1-s}}
     *                 \sum_{n=0}^{\infty} \frac{1}{2^{n+1}}
     *                 \sum_{k=0}^{n} (-1)^k \frac{n!}{(n-k)!k!} (k+1)^{-s}
     *    \f]
     *   Havil 2003, p. 206.
     *
     *   The Riemann zeta function is defined by:
     *    \f[
     *      \zeta(s) = \sum_{k=1}^{\infty} \frac{1}{k^{s}} for s > 1
     *    \f]
     *   For s < 1 use the reflection formula:
     *    \f[
     *      \zeta(s) = 2^s \pi^{s-1} \Gamma(1-s) \zeta(1-s)
     *    \f]
     */
    template<typename _Tp>
    _Tp
    __riemann_zeta_glob(_Tp __s)
    {
      _Tp __zeta = _Tp(0);

      const _Tp __eps = std::numeric_limits<_Tp>::epsilon();
      //  Max e exponent before overflow.
      const _Tp __max_bincoeff = std::numeric_limits<_Tp>::max_exponent10
                               * std::log(_Tp(10)) - _Tp(1);

      //  This series works until the binomial coefficient blows up
      //  so use reflection.
      if (__s < _Tp(0))
        {
#if _GLIBCXX_USE_C99_MATH_TR1
          if (_GLIBCXX_MATH_NS::fmod(__s,_Tp(2)) == _Tp(0))
            return _Tp(0);
          else
#endif
            {
              _Tp __zeta = __riemann_zeta_glob(_Tp(1) - __s);
              __zeta *= std::pow(_Tp(2)
                     * __numeric_constants<_Tp>::__pi(), __s)
                     * std::sin(__numeric_constants<_Tp>::__pi_2() * __s)
#if _GLIBCXX_USE_C99_MATH_TR1
                     * std::exp(_GLIBCXX_MATH_NS::lgamma(_Tp(1) - __s))
#else
                     * std::exp(__log_gamma(_Tp(1) - __s))
#endif
                     / __numeric_constants<_Tp>::__pi();
              return __zeta;
            }
        }

      _Tp __num = _Tp(0.5L);
      const unsigned int __maxit = 10000;
      for (unsigned int __i = 0; __i < __maxit; ++__i)
        {
          bool __punt = false;
          _Tp __sgn = _Tp(1);
          _Tp __term = _Tp(0);
          for (unsigned int __j = 0; __j <= __i; ++__j)
            {
#if _GLIBCXX_USE_C99_MATH_TR1
              _Tp __bincoeff =  _GLIBCXX_MATH_NS::lgamma(_Tp(1 + __i))
                              - _GLIBCXX_MATH_NS::lgamma(_Tp(1 + __j))
                              - _GLIBCXX_MATH_NS::lgamma(_Tp(1 + __i - __j));
#else
              _Tp __bincoeff =  __log_gamma(_Tp(1 + __i))
                              - __log_gamma(_Tp(1 + __j))
                              - __log_gamma(_Tp(1 + __i - __j));
#endif
              if (__bincoeff > __max_bincoeff)
                {
                  //  This only gets hit for x << 0.
                  __punt = true;
                  break;
                }
              __bincoeff = std::exp(__bincoeff);
              __term += __sgn * __bincoeff * std::pow(_Tp(1 + __j), -__s);
              __sgn *= _Tp(-1);
            }
          if (__punt)
            break;
          __term *= __num;
          __zeta += __term;
          if (std::abs(__term/__zeta) < __eps)
            break;
          __num *= _Tp(0.5L);
        }

      __zeta /= _Tp(1) - std::pow(_Tp(2), _Tp(1) - __s);

      return __zeta;
    }


    /**
     *   @brief  Compute the Riemann zeta function @f$ \zeta(s) @f$
     *           using the product over prime factors.
     *    \f[
     *      \zeta(s) = \Pi_{i=1}^\infty \frac{1}{1 - p_i^{-s}}
     *    \f]
     *    where @f$ {p_i} @f$ are the prime numbers.
     * 
     *   The Riemann zeta function is defined by:
     *    \f[
     *      \zeta(s) = \sum_{k=1}^{\infty} \frac{1}{k^{s}} for s > 1
     *    \f]
     *   For s < 1 use the reflection formula:
     *    \f[
     *      \zeta(s) = 2^s \pi^{s-1} \Gamma(1-s) \zeta(1-s)
     *    \f]
     */
    template<typename _Tp>
    _Tp
    __riemann_zeta_product(_Tp __s)
    {
      static const _Tp __prime[] = {
        _Tp(2), _Tp(3), _Tp(5), _Tp(7), _Tp(11), _Tp(13), _Tp(17), _Tp(19),
        _Tp(23), _Tp(29), _Tp(31), _Tp(37), _Tp(41), _Tp(43), _Tp(47),
        _Tp(53), _Tp(59), _Tp(61), _Tp(67), _Tp(71), _Tp(73), _Tp(79),
        _Tp(83), _Tp(89), _Tp(97), _Tp(101), _Tp(103), _Tp(107), _Tp(109)
      };
      static const unsigned int __num_primes = sizeof(__prime) / sizeof(_Tp);

      _Tp __zeta = _Tp(1);
      for (unsigned int __i = 0; __i < __num_primes; ++__i)
        {
          const _Tp __fact = _Tp(1) - std::pow(__prime[__i], -__s);
          __zeta *= __fact;
          if (_Tp(1) - __fact < std::numeric_limits<_Tp>::epsilon())
            break;
        }

      __zeta = _Tp(1) / __zeta;

      return __zeta;
    }


    /**
     *   @brief  Return the Riemann zeta function @f$ \zeta(s) @f$.
     * 
     *   The Riemann zeta function is defined by:
     *    \f[
     *      \zeta(s) = \sum_{k=1}^{\infty} k^{-s} for s > 1
     *                 \frac{(2\pi)^s}{pi} sin(\frac{\pi s}{2})
     *                 \Gamma (1 - s) \zeta (1 - s) for s < 1
     *    \f]
     *   For s < 1 use the reflection formula:
     *    \f[
     *      \zeta(s) = 2^s \pi^{s-1} \Gamma(1-s) \zeta(1-s)
     *    \f]
     */
    template<typename _Tp>
    _Tp
    __riemann_zeta(_Tp __s)
    {
      if (__isnan(__s))
        return std::numeric_limits<_Tp>::quiet_NaN();
      else if (__s == _Tp(1))
        return std::numeric_limits<_Tp>::infinity();
      else if (__s < -_Tp(19))
        {
          _Tp __zeta = __riemann_zeta_product(_Tp(1) - __s);
          __zeta *= std::pow(_Tp(2) * __numeric_constants<_Tp>::__pi(), __s)
                 * std::sin(__numeric_constants<_Tp>::__pi_2() * __s)
#if _GLIBCXX_USE_C99_MATH_TR1
                 * std::exp(_GLIBCXX_MATH_NS::lgamma(_Tp(1) - __s))
#else
                 * std::exp(__log_gamma(_Tp(1) - __s))
#endif
                 / __numeric_constants<_Tp>::__pi();
          return __zeta;
        }
      else if (__s < _Tp(20))
        {
          //  Global double sum or McLaurin?
          bool __glob = true;
          if (__glob)
            return __riemann_zeta_glob(__s);
          else
            {
              if (__s > _Tp(1))
                return __riemann_zeta_sum(__s);
              else
                {
                  _Tp __zeta = std::pow(_Tp(2)
                                * __numeric_constants<_Tp>::__pi(), __s)
                         * std::sin(__numeric_constants<_Tp>::__pi_2() * __s)
#if _GLIBCXX_USE_C99_MATH_TR1
                             * _GLIBCXX_MATH_NS::tgamma(_Tp(1) - __s)
#else
                             * std::exp(__log_gamma(_Tp(1) - __s))
#endif
                             * __riemann_zeta_sum(_Tp(1) - __s);
                  return __zeta;
                }
            }
        }
      else
        return __riemann_zeta_product(__s);
    }


    /**
     *   @brief  Return the Hurwitz zeta function @f$ \zeta(x,s) @f$
     *           for all s != 1 and x > -1.
     * 
     *   The Hurwitz zeta function is defined by:
     *   @f[
     *     \zeta(x,s) = \sum_{n=0}^{\infty} \frac{1}{(n + x)^s}
     *   @f]
     *   The Riemann zeta function is a special case:
     *   @f[
     *     \zeta(s) = \zeta(1,s)
     *   @f]
     * 
     *   This functions uses the double sum that converges for s != 1
     *   and x > -1:
     *   @f[
     *     \zeta(x,s) = \frac{1}{s-1}
     *                \sum_{n=0}^{\infty} \frac{1}{n + 1}
     *                \sum_{k=0}^{n} (-1)^k \frac{n!}{(n-k)!k!} (x+k)^{-s}
     *   @f]
     */
    template<typename _Tp>
    _Tp
    __hurwitz_zeta_glob(_Tp __a, _Tp __s)
    {
      _Tp __zeta = _Tp(0);

      const _Tp __eps = std::numeric_limits<_Tp>::epsilon();
      //  Max e exponent before overflow.
      const _Tp __max_bincoeff = std::numeric_limits<_Tp>::max_exponent10
                               * std::log(_Tp(10)) - _Tp(1);

      const unsigned int __maxit = 10000;
      for (unsigned int __i = 0; __i < __maxit; ++__i)
        {
          bool __punt = false;
          _Tp __sgn = _Tp(1);
          _Tp __term = _Tp(0);
          for (unsigned int __j = 0; __j <= __i; ++__j)
            {
#if _GLIBCXX_USE_C99_MATH_TR1
              _Tp __bincoeff =  _GLIBCXX_MATH_NS::lgamma(_Tp(1 + __i))
                              - _GLIBCXX_MATH_NS::lgamma(_Tp(1 + __j))
                              - _GLIBCXX_MATH_NS::lgamma(_Tp(1 + __i - __j));
#else
              _Tp __bincoeff =  __log_gamma(_Tp(1 + __i))
                              - __log_gamma(_Tp(1 + __j))
                              - __log_gamma(_Tp(1 + __i - __j));
#endif
              if (__bincoeff > __max_bincoeff)
                {
                  //  This only gets hit for x << 0.
                  __punt = true;
                  break;
                }
              __bincoeff = std::exp(__bincoeff);
              __term += __sgn * __bincoeff * std::pow(_Tp(__a + __j), -__s);
              __sgn *= _Tp(-1);
            }
          if (__punt)
            break;
          __term /= _Tp(__i + 1);
          if (std::abs(__term / __zeta) < __eps)
            break;
          __zeta += __term;
        }

      __zeta /= __s - _Tp(1);

      return __zeta;
    }


    /**
     *   @brief  Return the Hurwitz zeta function @f$ \zeta(x,s) @f$
     *           for all s != 1 and x > -1.
     * 
     *   The Hurwitz zeta function is defined by:
     *   @f[
     *     \zeta(x,s) = \sum_{n=0}^{\infty} \frac{1}{(n + x)^s}
     *   @f]
     *   The Riemann zeta function is a special case:
     *   @f[
     *     \zeta(s) = \zeta(1,s)
     *   @f]
     */
    template<typename _Tp>
    inline _Tp
    __hurwitz_zeta(_Tp __a, _Tp __s)
    { return __hurwitz_zeta_glob(__a, __s); }
  } // namespace __detail
#undef _GLIBCXX_MATH_NS
#if ! _GLIBCXX_USE_STD_SPEC_FUNCS && defined(_GLIBCXX_TR1_CMATH)
} // namespace tr1
#endif

_GLIBCXX_END_NAMESPACE_VERSION
}

#endif // _GLIBCXX_TR1_RIEMANN_ZETA_TCC
