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

/** @file tr1/bessel_function.tcc
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{tr1/cmath}
 */

/* __cyl_bessel_jn_asymp adapted from GNU GSL version 2.4 specfunc/bessel_j.c
 * Copyright (C) 1996-2003 Gerard Jungman
 */

//
// ISO C++ 14882 TR1: 5.2  Special functions
//

// Written by Edward Smith-Rowland.
//
// References:
//   (1) Handbook of Mathematical Functions,
//       ed. Milton Abramowitz and Irene A. Stegun,
//       Dover Publications,
//       Section 9, pp. 355-434, Section 10 pp. 435-478
//   (2) The Gnu Scientific Library, http://www.gnu.org/software/gsl
//   (3) Numerical Recipes in C, by W. H. Press, S. A. Teukolsky,
//       W. T. Vetterling, B. P. Flannery, Cambridge University Press (1992),
//       2nd ed, pp. 240-245

#ifndef _GLIBCXX_TR1_BESSEL_FUNCTION_TCC
#define _GLIBCXX_TR1_BESSEL_FUNCTION_TCC 1

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
     *   @brief Compute the gamma functions required by the Temme series
     *          expansions of @f$ N_\nu(x) @f$ and @f$ K_\nu(x) @f$.
     *   @f[
     *     \Gamma_1 = \frac{1}{2\mu}
     *                [\frac{1}{\Gamma(1 - \mu)} - \frac{1}{\Gamma(1 + \mu)}]
     *   @f]
     *   and
     *   @f[
     *     \Gamma_2 = \frac{1}{2}
     *                [\frac{1}{\Gamma(1 - \mu)} + \frac{1}{\Gamma(1 + \mu)}]
     *   @f]
     *   where @f$ -1/2 <= \mu <= 1/2 @f$ is @f$ \mu = \nu - N @f$ and @f$ N @f$.
     *   is the nearest integer to @f$ \nu @f$.
     *   The values of \f$ \Gamma(1 + \mu) \f$ and \f$ \Gamma(1 - \mu) \f$
     *   are returned as well.
     * 
     *   The accuracy requirements on this are exquisite.
     *
     *   @param __mu     The input parameter of the gamma functions.
     *   @param __gam1   The output function \f$ \Gamma_1(\mu) \f$
     *   @param __gam2   The output function \f$ \Gamma_2(\mu) \f$
     *   @param __gampl  The output function \f$ \Gamma(1 + \mu) \f$
     *   @param __gammi  The output function \f$ \Gamma(1 - \mu) \f$
     */
    template <typename _Tp>
    void
    __gamma_temme(_Tp __mu,
                  _Tp & __gam1, _Tp & __gam2, _Tp & __gampl, _Tp & __gammi)
    {
#if _GLIBCXX_USE_C99_MATH_TR1
      __gampl = _Tp(1) / _GLIBCXX_MATH_NS::tgamma(_Tp(1) + __mu);
      __gammi = _Tp(1) / _GLIBCXX_MATH_NS::tgamma(_Tp(1) - __mu);
#else
      __gampl = _Tp(1) / __gamma(_Tp(1) + __mu);
      __gammi = _Tp(1) / __gamma(_Tp(1) - __mu);
#endif

      if (std::abs(__mu) < std::numeric_limits<_Tp>::epsilon())
        __gam1 = -_Tp(__numeric_constants<_Tp>::__gamma_e());
      else
        __gam1 = (__gammi - __gampl) / (_Tp(2) * __mu);

      __gam2 = (__gammi + __gampl) / (_Tp(2));

      return;
    }


    /**
     *   @brief  Compute the Bessel @f$ J_\nu(x) @f$ and Neumann
     *           @f$ N_\nu(x) @f$ functions and their first derivatives
     *           @f$ J'_\nu(x) @f$ and @f$ N'_\nu(x) @f$ respectively.
     *           These four functions are computed together for numerical
     *           stability.
     *
     *   @param  __nu  The order of the Bessel functions.
     *   @param  __x   The argument of the Bessel functions.
     *   @param  __Jnu  The output Bessel function of the first kind.
     *   @param  __Nnu  The output Neumann function (Bessel function of the second kind).
     *   @param  __Jpnu  The output derivative of the Bessel function of the first kind.
     *   @param  __Npnu  The output derivative of the Neumann function.
     */
    template <typename _Tp>
    void
    __bessel_jn(_Tp __nu, _Tp __x,
                _Tp & __Jnu, _Tp & __Nnu, _Tp & __Jpnu, _Tp & __Npnu)
    {
      if (__x == _Tp(0))
        {
          if (__nu == _Tp(0))
            {
              __Jnu = _Tp(1);
              __Jpnu = _Tp(0);
            }
          else if (__nu == _Tp(1))
            {
              __Jnu = _Tp(0);
              __Jpnu = _Tp(0.5L);
            }
          else
            {
              __Jnu = _Tp(0);
              __Jpnu = _Tp(0);
            }
          __Nnu = -std::numeric_limits<_Tp>::infinity();
          __Npnu = std::numeric_limits<_Tp>::infinity();
          return;
        }

      const _Tp __eps = std::numeric_limits<_Tp>::epsilon();
      //  When the multiplier is N i.e.
      //  fp_min = N * min()
      //  Then J_0 and N_0 tank at x = 8 * N (J_0 = 0 and N_0 = nan)!
      //const _Tp __fp_min = _Tp(20) * std::numeric_limits<_Tp>::min();
      const _Tp __fp_min = std::sqrt(std::numeric_limits<_Tp>::min());
      const int __max_iter = 15000;
      const _Tp __x_min = _Tp(2);

      const int __nl = (__x < __x_min
                    ? static_cast<int>(__nu + _Tp(0.5L))
                    : std::max(0, static_cast<int>(__nu - __x + _Tp(1.5L))));

      const _Tp __mu = __nu - __nl;
      const _Tp __mu2 = __mu * __mu;
      const _Tp __xi = _Tp(1) / __x;
      const _Tp __xi2 = _Tp(2) * __xi;
      _Tp __w = __xi2 / __numeric_constants<_Tp>::__pi();
      int __isign = 1;
      _Tp __h = __nu * __xi;
      if (__h < __fp_min)
        __h = __fp_min;
      _Tp __b = __xi2 * __nu;
      _Tp __d = _Tp(0);
      _Tp __c = __h;
      int __i;
      for (__i = 1; __i <= __max_iter; ++__i)
        {
          __b += __xi2;
          __d = __b - __d;
          if (std::abs(__d) < __fp_min)
            __d = __fp_min;
          __c = __b - _Tp(1) / __c;
          if (std::abs(__c) < __fp_min)
            __c = __fp_min;
          __d = _Tp(1) / __d;
          const _Tp __del = __c * __d;
          __h *= __del;
          if (__d < _Tp(0))
            __isign = -__isign;
          if (std::abs(__del - _Tp(1)) < __eps)
            break;
        }
      if (__i > __max_iter)
        std::__throw_runtime_error(__N("Argument x too large in __bessel_jn; "
                                       "try asymptotic expansion."));
      _Tp __Jnul = __isign * __fp_min;
      _Tp __Jpnul = __h * __Jnul;
      _Tp __Jnul1 = __Jnul;
      _Tp __Jpnu1 = __Jpnul;
      _Tp __fact = __nu * __xi;
      for ( int __l = __nl; __l >= 1; --__l )
        {
          const _Tp __Jnutemp = __fact * __Jnul + __Jpnul;
          __fact -= __xi;
          __Jpnul = __fact * __Jnutemp - __Jnul;
          __Jnul = __Jnutemp;
        }
      if (__Jnul == _Tp(0))
        __Jnul = __eps;
      _Tp __f= __Jpnul / __Jnul;
      _Tp __Nmu, __Nnu1, __Npmu, __Jmu;
      if (__x < __x_min)
        {
          const _Tp __x2 = __x / _Tp(2);
          const _Tp __pimu = __numeric_constants<_Tp>::__pi() * __mu;
          _Tp __fact = (std::abs(__pimu) < __eps
                      ? _Tp(1) : __pimu / std::sin(__pimu));
          _Tp __d = -std::log(__x2);
          _Tp __e = __mu * __d;
          _Tp __fact2 = (std::abs(__e) < __eps
                       ? _Tp(1) : std::sinh(__e) / __e);
          _Tp __gam1, __gam2, __gampl, __gammi;
          __gamma_temme(__mu, __gam1, __gam2, __gampl, __gammi);
          _Tp __ff = (_Tp(2) / __numeric_constants<_Tp>::__pi())
                   * __fact * (__gam1 * std::cosh(__e) + __gam2 * __fact2 * __d);
          __e = std::exp(__e);
          _Tp __p = __e / (__numeric_constants<_Tp>::__pi() * __gampl);
          _Tp __q = _Tp(1) / (__e * __numeric_constants<_Tp>::__pi() * __gammi);
          const _Tp __pimu2 = __pimu / _Tp(2);
          _Tp __fact3 = (std::abs(__pimu2) < __eps
                       ? _Tp(1) : std::sin(__pimu2) / __pimu2 );
          _Tp __r = __numeric_constants<_Tp>::__pi() * __pimu2 * __fact3 * __fact3;
          _Tp __c = _Tp(1);
          __d = -__x2 * __x2;
          _Tp __sum = __ff + __r * __q;
          _Tp __sum1 = __p;
          for (__i = 1; __i <= __max_iter; ++__i)
            {
              __ff = (__i * __ff + __p + __q) / (__i * __i - __mu2);
              __c *= __d / _Tp(__i);
              __p /= _Tp(__i) - __mu;
              __q /= _Tp(__i) + __mu;
              const _Tp __del = __c * (__ff + __r * __q);
              __sum += __del; 
              const _Tp __del1 = __c * __p - __i * __del;
              __sum1 += __del1;
              if ( std::abs(__del) < __eps * (_Tp(1) + std::abs(__sum)) )
                break;
            }
          if ( __i > __max_iter )
            std::__throw_runtime_error(__N("Bessel y series failed to converge "
                                           "in __bessel_jn."));
          __Nmu = -__sum;
          __Nnu1 = -__sum1 * __xi2;
          __Npmu = __mu * __xi * __Nmu - __Nnu1;
          __Jmu = __w / (__Npmu - __f * __Nmu);
        }
      else
        {
          _Tp __a = _Tp(0.25L) - __mu2;
          _Tp __q = _Tp(1);
          _Tp __p = -__xi / _Tp(2);
          _Tp __br = _Tp(2) * __x;
          _Tp __bi = _Tp(2);
          _Tp __fact = __a * __xi / (__p * __p + __q * __q);
          _Tp __cr = __br + __q * __fact;
          _Tp __ci = __bi + __p * __fact;
          _Tp __den = __br * __br + __bi * __bi;
          _Tp __dr = __br / __den;
          _Tp __di = -__bi / __den;
          _Tp __dlr = __cr * __dr - __ci * __di;
          _Tp __dli = __cr * __di + __ci * __dr;
          _Tp __temp = __p * __dlr - __q * __dli;
          __q = __p * __dli + __q * __dlr;
          __p = __temp;
          int __i;
          for (__i = 2; __i <= __max_iter; ++__i)
            {
              __a += _Tp(2 * (__i - 1));
              __bi += _Tp(2);
              __dr = __a * __dr + __br;
              __di = __a * __di + __bi;
              if (std::abs(__dr) + std::abs(__di) < __fp_min)
                __dr = __fp_min;
              __fact = __a / (__cr * __cr + __ci * __ci);
              __cr = __br + __cr * __fact;
              __ci = __bi - __ci * __fact;
              if (std::abs(__cr) + std::abs(__ci) < __fp_min)
                __cr = __fp_min;
              __den = __dr * __dr + __di * __di;
              __dr /= __den;
              __di /= -__den;
              __dlr = __cr * __dr - __ci * __di;
              __dli = __cr * __di + __ci * __dr;
              __temp = __p * __dlr - __q * __dli;
              __q = __p * __dli + __q * __dlr;
              __p = __temp;
              if (std::abs(__dlr - _Tp(1)) + std::abs(__dli) < __eps)
                break;
          }
          if (__i > __max_iter)
            std::__throw_runtime_error(__N("Lentz's method failed "
                                           "in __bessel_jn."));
          const _Tp __gam = (__p - __f) / __q;
          __Jmu = std::sqrt(__w / ((__p - __f) * __gam + __q));
#if _GLIBCXX_USE_C99_MATH_TR1
          __Jmu = _GLIBCXX_MATH_NS::copysign(__Jmu, __Jnul);
#else
          if (__Jmu * __Jnul < _Tp(0))
            __Jmu = -__Jmu;
#endif
          __Nmu = __gam * __Jmu;
          __Npmu = (__p + __q / __gam) * __Nmu;
          __Nnu1 = __mu * __xi * __Nmu - __Npmu;
      }
      __fact = __Jmu / __Jnul;
      __Jnu = __fact * __Jnul1;
      __Jpnu = __fact * __Jpnu1;
      for (__i = 1; __i <= __nl; ++__i)
        {
          const _Tp __Nnutemp = (__mu + __i) * __xi2 * __Nnu1 - __Nmu;
          __Nmu = __Nnu1;
          __Nnu1 = __Nnutemp;
        }
      __Nnu = __Nmu;
      __Npnu = __nu * __xi * __Nmu - __Nnu1;

      return;
    }


    /**
     *   @brief This routine computes the asymptotic cylindrical Bessel
     *          and Neumann functions of order nu: \f$ J_{\nu} \f$,
     *          \f$ N_{\nu} \f$.
     *
     *   References:
     *    (1) Handbook of Mathematical Functions,
     *        ed. Milton Abramowitz and Irene A. Stegun,
     *        Dover Publications,
     *        Section 9 p. 364, Equations 9.2.5-9.2.10
     *
     *   @param  __nu  The order of the Bessel functions.
     *   @param  __x   The argument of the Bessel functions.
     *   @param  __Jnu  The output Bessel function of the first kind.
     *   @param  __Nnu  The output Neumann function (Bessel function of the second kind).
     */
    template <typename _Tp>
    void
    __cyl_bessel_jn_asymp(_Tp __nu, _Tp __x, _Tp & __Jnu, _Tp & __Nnu)
    {
      const _Tp __mu = _Tp(4) * __nu * __nu;
      const _Tp __8x = _Tp(8) * __x;

      _Tp __P = _Tp(0);
      _Tp __Q = _Tp(0);

      _Tp __k = _Tp(0);
      _Tp __term = _Tp(1);

      int __epsP = 0;
      int __epsQ = 0;

      _Tp __eps = std::numeric_limits<_Tp>::epsilon();

      do
        {
          __term *= (__k == 0
                     ? _Tp(1)
                     : -(__mu - (2 * __k - 1) * (2 * __k - 1)) / (__k * __8x));

          __epsP = std::abs(__term) < __eps * std::abs(__P);
          __P += __term;

          __k++;

          __term *= (__mu - (2 * __k - 1) * (2 * __k - 1)) / (__k * __8x);
          __epsQ = std::abs(__term) < __eps * std::abs(__Q);
          __Q += __term;

          if (__epsP && __epsQ && __k > (__nu / 2.))
            break;

          __k++;
        }
      while (__k < 1000);

      const _Tp __chi = __x - (__nu + _Tp(0.5L))
                             * __numeric_constants<_Tp>::__pi_2();

      const _Tp __c = std::cos(__chi);
      const _Tp __s = std::sin(__chi);

      const _Tp __coef = std::sqrt(_Tp(2)
                             / (__numeric_constants<_Tp>::__pi() * __x));

      __Jnu = __coef * (__c * __P - __s * __Q);
      __Nnu = __coef * (__s * __P + __c * __Q);

      return;
    }


    /**
     *   @brief This routine returns the cylindrical Bessel functions
     *          of order \f$ \nu \f$: \f$ J_{\nu} \f$ or \f$ I_{\nu} \f$
     *          by series expansion.
     *
     *   The modified cylindrical Bessel function is:
     *   @f[
     *    Z_{\nu}(x) = \sum_{k=0}^{\infty}
     *              \frac{\sigma^k (x/2)^{\nu + 2k}}{k!\Gamma(\nu+k+1)}
     *   @f]
     *   where \f$ \sigma = +1 \f$ or\f$  -1 \f$ for
     *   \f$ Z = I \f$ or \f$ J \f$ respectively.
     * 
     *   See Abramowitz & Stegun, 9.1.10
     *       Abramowitz & Stegun, 9.6.7
     *    (1) Handbook of Mathematical Functions,
     *        ed. Milton Abramowitz and Irene A. Stegun,
     *        Dover Publications,
     *        Equation 9.1.10 p. 360 and Equation 9.6.10 p. 375
     *
     *   @param  __nu  The order of the Bessel function.
     *   @param  __x   The argument of the Bessel function.
     *   @param  __sgn  The sign of the alternate terms
     *                  -1 for the Bessel function of the first kind.
     *                  +1 for the modified Bessel function of the first kind.
     *   @return  The output Bessel function.
     */
    template <typename _Tp>
    _Tp
    __cyl_bessel_ij_series(_Tp __nu, _Tp __x, _Tp __sgn,
                           unsigned int __max_iter)
    {
      if (__x == _Tp(0))
	return __nu == _Tp(0) ? _Tp(1) : _Tp(0);

      const _Tp __x2 = __x / _Tp(2);
      _Tp __fact = __nu * std::log(__x2);
#if _GLIBCXX_USE_C99_MATH_TR1
      __fact -= _GLIBCXX_MATH_NS::lgamma(__nu + _Tp(1));
#else
      __fact -= __log_gamma(__nu + _Tp(1));
#endif
      __fact = std::exp(__fact);
      const _Tp __xx4 = __sgn * __x2 * __x2;
      _Tp __Jn = _Tp(1);
      _Tp __term = _Tp(1);

      for (unsigned int __i = 1; __i < __max_iter; ++__i)
        {
          __term *= __xx4 / (_Tp(__i) * (__nu + _Tp(__i)));
          __Jn += __term;
          if (std::abs(__term / __Jn) < std::numeric_limits<_Tp>::epsilon())
            break;
        }

      return __fact * __Jn;
    }


    /**
     *   @brief  Return the Bessel function of order \f$ \nu \f$:
     *           \f$ J_{\nu}(x) \f$.
     *
     *   The cylindrical Bessel function is:
     *   @f[
     *    J_{\nu}(x) = \sum_{k=0}^{\infty}
     *              \frac{(-1)^k (x/2)^{\nu + 2k}}{k!\Gamma(\nu+k+1)}
     *   @f]
     *
     *   @param  __nu  The order of the Bessel function.
     *   @param  __x   The argument of the Bessel function.
     *   @return  The output Bessel function.
     */
    template<typename _Tp>
    _Tp
    __cyl_bessel_j(_Tp __nu, _Tp __x)
    {
      if (__nu < _Tp(0) || __x < _Tp(0))
        std::__throw_domain_error(__N("Bad argument "
                                      "in __cyl_bessel_j."));
      else if (__isnan(__nu) || __isnan(__x))
        return std::numeric_limits<_Tp>::quiet_NaN();
      else if (__x * __x < _Tp(10) * (__nu + _Tp(1)))
        return __cyl_bessel_ij_series(__nu, __x, -_Tp(1), 200);
      else if (__x > _Tp(1000))
        {
          _Tp __J_nu, __N_nu;
          __cyl_bessel_jn_asymp(__nu, __x, __J_nu, __N_nu);
          return __J_nu;
        }
      else
        {
          _Tp __J_nu, __N_nu, __Jp_nu, __Np_nu;
          __bessel_jn(__nu, __x, __J_nu, __N_nu, __Jp_nu, __Np_nu);
          return __J_nu;
        }
    }


    /**
     *   @brief  Return the Neumann function of order \f$ \nu \f$:
     *           \f$ N_{\nu}(x) \f$.
     *
     *   The Neumann function is defined by:
     *   @f[
     *      N_{\nu}(x) = \frac{J_{\nu}(x) \cos \nu\pi - J_{-\nu}(x)}
     *                        {\sin \nu\pi}
     *   @f]
     *   where for integral \f$ \nu = n \f$ a limit is taken:
     *   \f$ lim_{\nu \to n} \f$.
     *
     *   @param  __nu  The order of the Neumann function.
     *   @param  __x   The argument of the Neumann function.
     *   @return  The output Neumann function.
     */
    template<typename _Tp>
    _Tp
    __cyl_neumann_n(_Tp __nu, _Tp __x)
    {
      if (__nu < _Tp(0) || __x < _Tp(0))
        std::__throw_domain_error(__N("Bad argument "
                                      "in __cyl_neumann_n."));
      else if (__isnan(__nu) || __isnan(__x))
        return std::numeric_limits<_Tp>::quiet_NaN();
      else if (__x > _Tp(1000))
        {
          _Tp __J_nu, __N_nu;
          __cyl_bessel_jn_asymp(__nu, __x, __J_nu, __N_nu);
          return __N_nu;
        }
      else
        {
          _Tp __J_nu, __N_nu, __Jp_nu, __Np_nu;
          __bessel_jn(__nu, __x, __J_nu, __N_nu, __Jp_nu, __Np_nu);
          return __N_nu;
        }
    }


    /**
     *   @brief  Compute the spherical Bessel @f$ j_n(x) @f$
     *           and Neumann @f$ n_n(x) @f$ functions and their first
     *           derivatives @f$ j'_n(x) @f$ and @f$ n'_n(x) @f$
     *           respectively.
     *
     *   @param  __n  The order of the spherical Bessel function.
     *   @param  __x  The argument of the spherical Bessel function.
     *   @param  __j_n  The output spherical Bessel function.
     *   @param  __n_n  The output spherical Neumann function.
     *   @param  __jp_n The output derivative of the spherical Bessel function.
     *   @param  __np_n The output derivative of the spherical Neumann function.
     */
    template <typename _Tp>
    void
    __sph_bessel_jn(unsigned int __n, _Tp __x,
                    _Tp & __j_n, _Tp & __n_n, _Tp & __jp_n, _Tp & __np_n)
    {
      const _Tp __nu = _Tp(__n) + _Tp(0.5L);

      _Tp __J_nu, __N_nu, __Jp_nu, __Np_nu;
      __bessel_jn(__nu, __x, __J_nu, __N_nu, __Jp_nu, __Np_nu);

      const _Tp __factor = __numeric_constants<_Tp>::__sqrtpio2()
                         / std::sqrt(__x);

      __j_n = __factor * __J_nu;
      __n_n = __factor * __N_nu;
      __jp_n = __factor * __Jp_nu - __j_n / (_Tp(2) * __x);
      __np_n = __factor * __Np_nu - __n_n / (_Tp(2) * __x);

      return;
    }


    /**
     *   @brief  Return the spherical Bessel function
     *           @f$ j_n(x) @f$ of order n.
     *
     *   The spherical Bessel function is defined by:
     *   @f[
     *    j_n(x) = \left( \frac{\pi}{2x} \right) ^{1/2} J_{n+1/2}(x)
     *   @f]
     *
     *   @param  __n  The order of the spherical Bessel function.
     *   @param  __x  The argument of the spherical Bessel function.
     *   @return  The output spherical Bessel function.
     */
    template <typename _Tp>
    _Tp
    __sph_bessel(unsigned int __n, _Tp __x)
    {
      if (__x < _Tp(0))
        std::__throw_domain_error(__N("Bad argument "
                                      "in __sph_bessel."));
      else if (__isnan(__x))
        return std::numeric_limits<_Tp>::quiet_NaN();
      else if (__x == _Tp(0))
        {
          if (__n == 0)
            return _Tp(1);
          else
            return _Tp(0);
        }
      else
        {
          _Tp __j_n, __n_n, __jp_n, __np_n;
          __sph_bessel_jn(__n, __x, __j_n, __n_n, __jp_n, __np_n);
          return __j_n;
        }
    }


    /**
     *   @brief  Return the spherical Neumann function
     *           @f$ n_n(x) @f$.
     *
     *   The spherical Neumann function is defined by:
     *   @f[
     *    n_n(x) = \left( \frac{\pi}{2x} \right) ^{1/2} N_{n+1/2}(x)
     *   @f]
     *
     *   @param  __n  The order of the spherical Neumann function.
     *   @param  __x  The argument of the spherical Neumann function.
     *   @return  The output spherical Neumann function.
     */
    template <typename _Tp>
    _Tp
    __sph_neumann(unsigned int __n, _Tp __x)
    {
      if (__x < _Tp(0))
        std::__throw_domain_error(__N("Bad argument "
                                      "in __sph_neumann."));
      else if (__isnan(__x))
        return std::numeric_limits<_Tp>::quiet_NaN();
      else if (__x == _Tp(0))
        return -std::numeric_limits<_Tp>::infinity();
      else
        {
          _Tp __j_n, __n_n, __jp_n, __np_n;
          __sph_bessel_jn(__n, __x, __j_n, __n_n, __jp_n, __np_n);
          return __n_n;
        }
    }
  } // namespace __detail
#undef _GLIBCXX_MATH_NS
#if ! _GLIBCXX_USE_STD_SPEC_FUNCS && defined(_GLIBCXX_TR1_CMATH)
} // namespace tr1
#endif

_GLIBCXX_END_NAMESPACE_VERSION
}

#endif // _GLIBCXX_TR1_BESSEL_FUNCTION_TCC
