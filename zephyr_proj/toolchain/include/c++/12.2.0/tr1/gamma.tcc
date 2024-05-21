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

/** @file tr1/gamma.tcc
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
//       Section 6, pp. 253-266
//   (2) The Gnu Scientific Library, http://www.gnu.org/software/gsl
//   (3) Numerical Recipes in C, by W. H. Press, S. A. Teukolsky,
//       W. T. Vetterling, B. P. Flannery, Cambridge University Press (1992),
//       2nd ed, pp. 213-216
//   (4) Gamma, Exploring Euler's Constant, Julian Havil,
//       Princeton, 2003.

#ifndef _GLIBCXX_TR1_GAMMA_TCC
#define _GLIBCXX_TR1_GAMMA_TCC 1

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
  // Implementation-space details.
  namespace __detail
  {
    /**
     *   @brief This returns Bernoulli numbers from a table or by summation
     *          for larger values.
     *
     *   Recursion is unstable.
     *
     *   @param __n the order n of the Bernoulli number.
     *   @return  The Bernoulli number of order n.
     */
    template <typename _Tp>
    _Tp
    __bernoulli_series(unsigned int __n)
    {

      static const _Tp __num[28] = {
        _Tp(1UL),                        -_Tp(1UL) / _Tp(2UL),
        _Tp(1UL) / _Tp(6UL),             _Tp(0UL),
        -_Tp(1UL) / _Tp(30UL),           _Tp(0UL),
        _Tp(1UL) / _Tp(42UL),            _Tp(0UL),
        -_Tp(1UL) / _Tp(30UL),           _Tp(0UL),
        _Tp(5UL) / _Tp(66UL),            _Tp(0UL),
        -_Tp(691UL) / _Tp(2730UL),       _Tp(0UL),
        _Tp(7UL) / _Tp(6UL),             _Tp(0UL),
        -_Tp(3617UL) / _Tp(510UL),       _Tp(0UL),
        _Tp(43867UL) / _Tp(798UL),       _Tp(0UL),
        -_Tp(174611) / _Tp(330UL),       _Tp(0UL),
        _Tp(854513UL) / _Tp(138UL),      _Tp(0UL),
        -_Tp(236364091UL) / _Tp(2730UL), _Tp(0UL),
        _Tp(8553103UL) / _Tp(6UL),       _Tp(0UL)
      };

      if (__n == 0)
        return _Tp(1);

      if (__n == 1)
        return -_Tp(1) / _Tp(2);

      //  Take care of the rest of the odd ones.
      if (__n % 2 == 1)
        return _Tp(0);

      //  Take care of some small evens that are painful for the series.
      if (__n < 28)
        return __num[__n];


      _Tp __fact = _Tp(1);
      if ((__n / 2) % 2 == 0)
        __fact *= _Tp(-1);
      for (unsigned int __k = 1; __k <= __n; ++__k)
        __fact *= __k / (_Tp(2) * __numeric_constants<_Tp>::__pi());
      __fact *= _Tp(2);

      _Tp __sum = _Tp(0);
      for (unsigned int __i = 1; __i < 1000; ++__i)
        {
          _Tp __term = std::pow(_Tp(__i), -_Tp(__n));
          if (__term < std::numeric_limits<_Tp>::epsilon())
            break;
          __sum += __term;
        }

      return __fact * __sum;
    }


    /**
     *   @brief This returns Bernoulli number \f$B_n\f$.
     *
     *   @param __n the order n of the Bernoulli number.
     *   @return  The Bernoulli number of order n.
     */
    template<typename _Tp>
    inline _Tp
    __bernoulli(int __n)
    { return __bernoulli_series<_Tp>(__n); }


    /**
     *   @brief Return \f$log(\Gamma(x))\f$ by asymptotic expansion
     *          with Bernoulli number coefficients.  This is like
     *          Sterling's approximation.
     *
     *   @param __x The argument of the log of the gamma function.
     *   @return  The logarithm of the gamma function.
     */
    template<typename _Tp>
    _Tp
    __log_gamma_bernoulli(_Tp __x)
    {
      _Tp __lg = (__x - _Tp(0.5L)) * std::log(__x) - __x
               + _Tp(0.5L) * std::log(_Tp(2)
               * __numeric_constants<_Tp>::__pi());

      const _Tp __xx = __x * __x;
      _Tp __help = _Tp(1) / __x;
      for ( unsigned int __i = 1; __i < 20; ++__i )
        {
          const _Tp __2i = _Tp(2 * __i);
          __help /= __2i * (__2i - _Tp(1)) * __xx;
          __lg += __bernoulli<_Tp>(2 * __i) * __help;
        }

      return __lg;
    }


    /**
     *   @brief Return \f$log(\Gamma(x))\f$ by the Lanczos method.
     *          This method dominates all others on the positive axis I think.
     *
     *   @param __x The argument of the log of the gamma function.
     *   @return  The logarithm of the gamma function.
     */
    template<typename _Tp>
    _Tp
    __log_gamma_lanczos(_Tp __x)
    {
      const _Tp __xm1 = __x - _Tp(1);

      static const _Tp __lanczos_cheb_7[9] = {
       _Tp( 0.99999999999980993227684700473478L),
       _Tp( 676.520368121885098567009190444019L),
       _Tp(-1259.13921672240287047156078755283L),
       _Tp( 771.3234287776530788486528258894L),
       _Tp(-176.61502916214059906584551354L),
       _Tp( 12.507343278686904814458936853L),
       _Tp(-0.13857109526572011689554707L),
       _Tp( 9.984369578019570859563e-6L),
       _Tp( 1.50563273514931155834e-7L)
      };

      static const _Tp __LOGROOT2PI
          = _Tp(0.9189385332046727417803297364056176L);

      _Tp __sum = __lanczos_cheb_7[0];
      for(unsigned int __k = 1; __k < 9; ++__k)
        __sum += __lanczos_cheb_7[__k] / (__xm1 + __k);

      const _Tp __term1 = (__xm1 + _Tp(0.5L))
                        * std::log((__xm1 + _Tp(7.5L))
                       / __numeric_constants<_Tp>::__euler());
      const _Tp __term2 = __LOGROOT2PI + std::log(__sum);
      const _Tp __result = __term1 + (__term2 - _Tp(7));

      return __result;
    }


    /**
     *   @brief Return \f$ log(|\Gamma(x)|) \f$.
     *          This will return values even for \f$ x < 0 \f$.
     *          To recover the sign of \f$ \Gamma(x) \f$ for
     *          any argument use @a __log_gamma_sign.
     *
     *   @param __x The argument of the log of the gamma function.
     *   @return  The logarithm of the gamma function.
     */
    template<typename _Tp>
    _Tp
    __log_gamma(_Tp __x)
    {
      if (__x > _Tp(0.5L))
        return __log_gamma_lanczos(__x);
      else
        {
          const _Tp __sin_fact
                 = std::abs(std::sin(__numeric_constants<_Tp>::__pi() * __x));
          if (__sin_fact == _Tp(0))
            std::__throw_domain_error(__N("Argument is nonpositive integer "
                                          "in __log_gamma"));
          return __numeric_constants<_Tp>::__lnpi()
                     - std::log(__sin_fact)
                     - __log_gamma_lanczos(_Tp(1) - __x);
        }
    }


    /**
     *   @brief Return the sign of \f$ \Gamma(x) \f$.
     *          At nonpositive integers zero is returned.
     *
     *   @param __x The argument of the gamma function.
     *   @return  The sign of the gamma function.
     */
    template<typename _Tp>
    _Tp
    __log_gamma_sign(_Tp __x)
    {
      if (__x > _Tp(0))
        return _Tp(1);
      else
        {
          const _Tp __sin_fact
                  = std::sin(__numeric_constants<_Tp>::__pi() * __x);
          if (__sin_fact > _Tp(0))
            return (1);
          else if (__sin_fact < _Tp(0))
            return -_Tp(1);
          else
            return _Tp(0);
        }
    }


    /**
     *   @brief Return the logarithm of the binomial coefficient.
     *   The binomial coefficient is given by:
     *   @f[
     *   \left(  \right) = \frac{n!}{(n-k)! k!}
     *   @f]
     *
     *   @param __n The first argument of the binomial coefficient.
     *   @param __k The second argument of the binomial coefficient.
     *   @return  The binomial coefficient.
     */
    template<typename _Tp>
    _Tp
    __log_bincoef(unsigned int __n, unsigned int __k)
    {
      //  Max e exponent before overflow.
      static const _Tp __max_bincoeff
                      = std::numeric_limits<_Tp>::max_exponent10
                      * std::log(_Tp(10)) - _Tp(1);
#if _GLIBCXX_USE_C99_MATH_TR1
      _Tp __coeff =  _GLIBCXX_MATH_NS::lgamma(_Tp(1 + __n))
                  - _GLIBCXX_MATH_NS::lgamma(_Tp(1 + __k))
                  - _GLIBCXX_MATH_NS::lgamma(_Tp(1 + __n - __k));
#else
      _Tp __coeff =  __log_gamma(_Tp(1 + __n))
                  - __log_gamma(_Tp(1 + __k))
                  - __log_gamma(_Tp(1 + __n - __k));
#endif
    }


    /**
     *   @brief Return the binomial coefficient.
     *   The binomial coefficient is given by:
     *   @f[
     *   \left(  \right) = \frac{n!}{(n-k)! k!}
     *   @f]
     *
     *   @param __n The first argument of the binomial coefficient.
     *   @param __k The second argument of the binomial coefficient.
     *   @return  The binomial coefficient.
     */
    template<typename _Tp>
    _Tp
    __bincoef(unsigned int __n, unsigned int __k)
    {
      //  Max e exponent before overflow.
      static const _Tp __max_bincoeff
                      = std::numeric_limits<_Tp>::max_exponent10
                      * std::log(_Tp(10)) - _Tp(1);

      const _Tp __log_coeff = __log_bincoef<_Tp>(__n, __k);
      if (__log_coeff > __max_bincoeff)
        return std::numeric_limits<_Tp>::quiet_NaN();
      else
        return std::exp(__log_coeff);
    }


    /**
     *   @brief Return \f$ \Gamma(x) \f$.
     *
     *   @param __x The argument of the gamma function.
     *   @return  The gamma function.
     */
    template<typename _Tp>
    inline _Tp
    __gamma(_Tp __x)
    { return std::exp(__log_gamma(__x)); }


    /**
     *   @brief  Return the digamma function by series expansion.
     *   The digamma or @f$ \psi(x) @f$ function is defined by
     *   @f[
     *     \psi(x) = \frac{\Gamma'(x)}{\Gamma(x)}
     *   @f]
     *
     *   The series is given by:
     *   @f[
     *     \psi(x) = -\gamma_E - \frac{1}{x}
     *              \sum_{k=1}^{\infty} \frac{x}{k(x + k)}
     *   @f]
     */
    template<typename _Tp>
    _Tp
    __psi_series(_Tp __x)
    {
      _Tp __sum = -__numeric_constants<_Tp>::__gamma_e() - _Tp(1) / __x;
      const unsigned int __max_iter = 100000;
      for (unsigned int __k = 1; __k < __max_iter; ++__k)
        {
          const _Tp __term = __x / (__k * (__k + __x));
          __sum += __term;
          if (std::abs(__term / __sum) < std::numeric_limits<_Tp>::epsilon())
            break;
        }
      return __sum;
    }


    /**
     *   @brief  Return the digamma function for large argument.
     *   The digamma or @f$ \psi(x) @f$ function is defined by
     *   @f[
     *     \psi(x) = \frac{\Gamma'(x)}{\Gamma(x)}
     *   @f]
     *
     *   The asymptotic series is given by:
     *   @f[
     *     \psi(x) = \ln(x) - \frac{1}{2x}
     *             - \sum_{n=1}^{\infty} \frac{B_{2n}}{2 n x^{2n}}
     *   @f]
     */
    template<typename _Tp>
    _Tp
    __psi_asymp(_Tp __x)
    {
      _Tp __sum = std::log(__x) - _Tp(0.5L) / __x;
      const _Tp __xx = __x * __x;
      _Tp __xp = __xx;
      const unsigned int __max_iter = 100;
      for (unsigned int __k = 1; __k < __max_iter; ++__k)
        {
          const _Tp __term = __bernoulli<_Tp>(2 * __k) / (2 * __k * __xp);
          __sum -= __term;
          if (std::abs(__term / __sum) < std::numeric_limits<_Tp>::epsilon())
            break;
          __xp *= __xx;
        }
      return __sum;
    }


    /**
     *   @brief  Return the digamma function.
     *   The digamma or @f$ \psi(x) @f$ function is defined by
     *   @f[
     *     \psi(x) = \frac{\Gamma'(x)}{\Gamma(x)}
     *   @f]
     *   For negative argument the reflection formula is used:
     *   @f[
     *     \psi(x) = \psi(1-x) - \pi \cot(\pi x)
     *   @f]
     */
    template<typename _Tp>
    _Tp
    __psi(_Tp __x)
    {
      const int __n = static_cast<int>(__x + 0.5L);
      const _Tp __eps = _Tp(4) * std::numeric_limits<_Tp>::epsilon();
      if (__n <= 0 && std::abs(__x - _Tp(__n)) < __eps)
        return std::numeric_limits<_Tp>::quiet_NaN();
      else if (__x < _Tp(0))
        {
          const _Tp __pi = __numeric_constants<_Tp>::__pi();
          return __psi(_Tp(1) - __x)
               - __pi * std::cos(__pi * __x) / std::sin(__pi * __x);
        }
      else if (__x > _Tp(100))
        return __psi_asymp(__x);
      else
        return __psi_series(__x);
    }


    /**
     *   @brief  Return the polygamma function @f$ \psi^{(n)}(x) @f$.
     * 
     *   The polygamma function is related to the Hurwitz zeta function:
     *   @f[
     *     \psi^{(n)}(x) = (-1)^{n+1} m! \zeta(m+1,x)
     *   @f]
     */
    template<typename _Tp>
    _Tp
    __psi(unsigned int __n, _Tp __x)
    {
      if (__x <= _Tp(0))
        std::__throw_domain_error(__N("Argument out of range "
                                      "in __psi"));
      else if (__n == 0)
        return __psi(__x);
      else
        {
          const _Tp __hzeta = __hurwitz_zeta(_Tp(__n + 1), __x);
#if _GLIBCXX_USE_C99_MATH_TR1
          const _Tp __ln_nfact = _GLIBCXX_MATH_NS::lgamma(_Tp(__n + 1));
#else
          const _Tp __ln_nfact = __log_gamma(_Tp(__n + 1));
#endif
          _Tp __result = std::exp(__ln_nfact) * __hzeta;
          if (__n % 2 == 1)
            __result = -__result;
          return __result;
        }
    }
  } // namespace __detail
#undef _GLIBCXX_MATH_NS
#if ! _GLIBCXX_USE_STD_SPEC_FUNCS && defined(_GLIBCXX_TR1_CMATH)
} // namespace tr1
#endif

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std

#endif // _GLIBCXX_TR1_GAMMA_TCC

