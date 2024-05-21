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

/** @file tr1/modified_bessel_func.tcc
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{tr1/cmath}
 */

//
// ISO C++ 14882 TR1: 5.2  Special functions
//

// Written by Edward Smith-Rowland.
//
// References:
//   (1) Handbook of Mathematical Functions,
//       Ed. Milton Abramowitz and Irene A. Stegun,
//       Dover Publications,
//       Section 9, pp. 355-434, Section 10 pp. 435-478
//   (2) The Gnu Scientific Library, http://www.gnu.org/software/gsl
//   (3) Numerical Recipes in C, by W. H. Press, S. A. Teukolsky,
//       W. T. Vetterling, B. P. Flannery, Cambridge University Press (1992),
//       2nd ed, pp. 246-249.

#ifndef _GLIBCXX_TR1_MODIFIED_BESSEL_FUNC_TCC
#define _GLIBCXX_TR1_MODIFIED_BESSEL_FUNC_TCC 1

#include <tr1/special_function_util.h>

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

#if _GLIBCXX_USE_STD_SPEC_FUNCS
#elif defined(_GLIBCXX_TR1_CMATH)
namespace tr1
{
#else
# error do not include this header directly, use <cmath> or <tr1/cmath>
#endif
  // [5.2] Special functions

  // Implementation-space details.
  namespace __detail
  {
    /**
     *   @brief  Compute the modified Bessel functions @f$ I_\nu(x) @f$ and
     *           @f$ K_\nu(x) @f$ and their first derivatives
     *           @f$ I'_\nu(x) @f$ and @f$ K'_\nu(x) @f$ respectively.
     *           These four functions are computed together for numerical
     *           stability.
     *
     *   @param  __nu  The order of the Bessel functions.
     *   @param  __x   The argument of the Bessel functions.
     *   @param  __Inu  The output regular modified Bessel function.
     *   @param  __Knu  The output irregular modified Bessel function.
     *   @param  __Ipnu  The output derivative of the regular
     *                   modified Bessel function.
     *   @param  __Kpnu  The output derivative of the irregular
     *                   modified Bessel function.
     */
    template <typename _Tp>
    void
    __bessel_ik(_Tp __nu, _Tp __x,
                _Tp & __Inu, _Tp & __Knu, _Tp & __Ipnu, _Tp & __Kpnu)
    {
      if (__x == _Tp(0))
        {
          if (__nu == _Tp(0))
            {
              __Inu = _Tp(1);
              __Ipnu = _Tp(0);
            }
          else if (__nu == _Tp(1))
            {
              __Inu = _Tp(0);
              __Ipnu = _Tp(0.5L);
            }
          else
            {
              __Inu = _Tp(0);
              __Ipnu = _Tp(0);
            }
          __Knu = std::numeric_limits<_Tp>::infinity();
          __Kpnu = -std::numeric_limits<_Tp>::infinity();
          return;
        }

      const _Tp __eps = std::numeric_limits<_Tp>::epsilon();
      const _Tp __fp_min = _Tp(10) * std::numeric_limits<_Tp>::epsilon();
      const int __max_iter = 15000;
      const _Tp __x_min = _Tp(2);

      const int __nl = static_cast<int>(__nu + _Tp(0.5L));

      const _Tp __mu = __nu - __nl;
      const _Tp __mu2 = __mu * __mu;
      const _Tp __xi = _Tp(1) / __x;
      const _Tp __xi2 = _Tp(2) * __xi;
      _Tp __h = __nu * __xi;
      if ( __h < __fp_min )
        __h = __fp_min;
      _Tp __b = __xi2 * __nu;
      _Tp __d = _Tp(0);
      _Tp __c = __h;
      int __i;
      for ( __i = 1; __i <= __max_iter; ++__i )
        {
          __b += __xi2;
          __d = _Tp(1) / (__b + __d);
          __c = __b + _Tp(1) / __c;
          const _Tp __del = __c * __d;
          __h *= __del;
          if (std::abs(__del - _Tp(1)) < __eps)
            break;
        }
      if (__i > __max_iter)
        std::__throw_runtime_error(__N("Argument x too large "
                                       "in __bessel_ik; "
                                       "try asymptotic expansion."));
      _Tp __Inul = __fp_min;
      _Tp __Ipnul = __h * __Inul;
      _Tp __Inul1 = __Inul;
      _Tp __Ipnu1 = __Ipnul;
      _Tp __fact = __nu * __xi;
      for (int __l = __nl; __l >= 1; --__l)
        {
          const _Tp __Inutemp = __fact * __Inul + __Ipnul;
          __fact -= __xi;
          __Ipnul = __fact * __Inutemp + __Inul;
          __Inul = __Inutemp;
        }
      _Tp __f = __Ipnul / __Inul;
      _Tp __Kmu, __Knu1;
      if (__x < __x_min)
        {
          const _Tp __x2 = __x / _Tp(2);
          const _Tp __pimu = __numeric_constants<_Tp>::__pi() * __mu;
          const _Tp __fact = (std::abs(__pimu) < __eps
                            ? _Tp(1) : __pimu / std::sin(__pimu));
          _Tp __d = -std::log(__x2);
          _Tp __e = __mu * __d;
          const _Tp __fact2 = (std::abs(__e) < __eps
                            ? _Tp(1) : std::sinh(__e) / __e);
          _Tp __gam1, __gam2, __gampl, __gammi;
          __gamma_temme(__mu, __gam1, __gam2, __gampl, __gammi);
          _Tp __ff = __fact
                   * (__gam1 * std::cosh(__e) + __gam2 * __fact2 * __d);
          _Tp __sum = __ff;
          __e = std::exp(__e);
          _Tp __p = __e / (_Tp(2) * __gampl);
          _Tp __q = _Tp(1) / (_Tp(2) * __e * __gammi);
          _Tp __c = _Tp(1);
          __d = __x2 * __x2;
          _Tp __sum1 = __p;
          int __i;
          for (__i = 1; __i <= __max_iter; ++__i)
            {
              __ff = (__i * __ff + __p + __q) / (__i * __i - __mu2);
              __c *= __d / __i;
              __p /= __i - __mu;
              __q /= __i + __mu;
              const _Tp __del = __c * __ff;
              __sum += __del; 
              const _Tp __del1 = __c * (__p - __i * __ff);
              __sum1 += __del1;
              if (std::abs(__del) < __eps * std::abs(__sum))
                break;
            }
          if (__i > __max_iter)
            std::__throw_runtime_error(__N("Bessel k series failed to converge "
                                           "in __bessel_ik."));
          __Kmu = __sum;
          __Knu1 = __sum1 * __xi2;
        }
      else
        {
          _Tp __b = _Tp(2) * (_Tp(1) + __x);
          _Tp __d = _Tp(1) / __b;
          _Tp __delh = __d;
          _Tp __h = __delh;
          _Tp __q1 = _Tp(0);
          _Tp __q2 = _Tp(1);
          _Tp __a1 = _Tp(0.25L) - __mu2;
          _Tp __q = __c = __a1;
          _Tp __a = -__a1;
          _Tp __s = _Tp(1) + __q * __delh;
          int __i;
          for (__i = 2; __i <= __max_iter; ++__i)
            {
              __a -= 2 * (__i - 1);
              __c = -__a * __c / __i;
              const _Tp __qnew = (__q1 - __b * __q2) / __a;
              __q1 = __q2;
              __q2 = __qnew;
              __q += __c * __qnew;
              __b += _Tp(2);
              __d = _Tp(1) / (__b + __a * __d);
              __delh = (__b * __d - _Tp(1)) * __delh;
              __h += __delh;
              const _Tp __dels = __q * __delh;
              __s += __dels;
              if ( std::abs(__dels / __s) < __eps )
                break;
            }
          if (__i > __max_iter)
            std::__throw_runtime_error(__N("Steed's method failed "
                                           "in __bessel_ik."));
          __h = __a1 * __h;
          __Kmu = std::sqrt(__numeric_constants<_Tp>::__pi() / (_Tp(2) * __x))
                * std::exp(-__x) / __s;
          __Knu1 = __Kmu * (__mu + __x + _Tp(0.5L) - __h) * __xi;
        }

      _Tp __Kpmu = __mu * __xi * __Kmu - __Knu1;
      _Tp __Inumu = __xi / (__f * __Kmu - __Kpmu);
      __Inu = __Inumu * __Inul1 / __Inul;
      __Ipnu = __Inumu * __Ipnu1 / __Inul;
      for ( __i = 1; __i <= __nl; ++__i )
        {
          const _Tp __Knutemp = (__mu + __i) * __xi2 * __Knu1 + __Kmu;
          __Kmu = __Knu1;
          __Knu1 = __Knutemp;
        }
      __Knu = __Kmu;
      __Kpnu = __nu * __xi * __Kmu - __Knu1;
  
      return;
    }


    /**
     *   @brief  Return the regular modified Bessel function of order
     *           \f$ \nu \f$: \f$ I_{\nu}(x) \f$.
     *
     *   The regular modified cylindrical Bessel function is:
     *   @f[
     *    I_{\nu}(x) = \sum_{k=0}^{\infty}
     *              \frac{(x/2)^{\nu + 2k}}{k!\Gamma(\nu+k+1)}
     *   @f]
     *
     *   @param  __nu  The order of the regular modified Bessel function.
     *   @param  __x   The argument of the regular modified Bessel function.
     *   @return  The output regular modified Bessel function.
     */
    template<typename _Tp>
    _Tp
    __cyl_bessel_i(_Tp __nu, _Tp __x)
    {
      if (__nu < _Tp(0) || __x < _Tp(0))
        std::__throw_domain_error(__N("Bad argument "
                                      "in __cyl_bessel_i."));
      else if (__isnan(__nu) || __isnan(__x))
        return std::numeric_limits<_Tp>::quiet_NaN();
      else if (__x * __x < _Tp(10) * (__nu + _Tp(1)))
        return __cyl_bessel_ij_series(__nu, __x, +_Tp(1), 200);
      else
        {
          _Tp __I_nu, __K_nu, __Ip_nu, __Kp_nu;
          __bessel_ik(__nu, __x, __I_nu, __K_nu, __Ip_nu, __Kp_nu);
          return __I_nu;
        }
    }


    /**
     *   @brief  Return the irregular modified Bessel function
     *           \f$ K_{\nu}(x) \f$ of order \f$ \nu \f$.
     *
     *   The irregular modified Bessel function is defined by:
     *   @f[
     *      K_{\nu}(x) = \frac{\pi}{2}
     *                   \frac{I_{-\nu}(x) - I_{\nu}(x)}{\sin \nu\pi}
     *   @f]
     *   where for integral \f$ \nu = n \f$ a limit is taken:
     *   \f$ lim_{\nu \to n} \f$.
     *
     *   @param  __nu  The order of the irregular modified Bessel function.
     *   @param  __x   The argument of the irregular modified Bessel function.
     *   @return  The output irregular modified Bessel function.
     */
    template<typename _Tp>
    _Tp
    __cyl_bessel_k(_Tp __nu, _Tp __x)
    {
      if (__nu < _Tp(0) || __x < _Tp(0))
        std::__throw_domain_error(__N("Bad argument "
                                      "in __cyl_bessel_k."));
      else if (__isnan(__nu) || __isnan(__x))
        return std::numeric_limits<_Tp>::quiet_NaN();
      else
        {
          _Tp __I_nu, __K_nu, __Ip_nu, __Kp_nu;
          __bessel_ik(__nu, __x, __I_nu, __K_nu, __Ip_nu, __Kp_nu);
          return __K_nu;
        }
    }


    /**
     *   @brief  Compute the spherical modified Bessel functions
     *           @f$ i_n(x) @f$ and @f$ k_n(x) @f$ and their first
     *           derivatives @f$ i'_n(x) @f$ and @f$ k'_n(x) @f$
     *           respectively.
     *
     *   @param  __n  The order of the modified spherical Bessel function.
     *   @param  __x  The argument of the modified spherical Bessel function.
     *   @param  __i_n  The output regular modified spherical Bessel function.
     *   @param  __k_n  The output irregular modified spherical
     *                  Bessel function.
     *   @param  __ip_n  The output derivative of the regular modified
     *                   spherical Bessel function.
     *   @param  __kp_n  The output derivative of the irregular modified
     *                   spherical Bessel function.
     */
    template <typename _Tp>
    void
    __sph_bessel_ik(unsigned int __n, _Tp __x,
                    _Tp & __i_n, _Tp & __k_n, _Tp & __ip_n, _Tp & __kp_n)
    {
      const _Tp __nu = _Tp(__n) + _Tp(0.5L);

      _Tp __I_nu, __Ip_nu, __K_nu, __Kp_nu;
      __bessel_ik(__nu, __x, __I_nu, __K_nu, __Ip_nu, __Kp_nu);

      const _Tp __factor = __numeric_constants<_Tp>::__sqrtpio2()
                         / std::sqrt(__x);

      __i_n = __factor * __I_nu;
      __k_n = __factor * __K_nu;
      __ip_n = __factor * __Ip_nu - __i_n / (_Tp(2) * __x);
      __kp_n = __factor * __Kp_nu - __k_n / (_Tp(2) * __x);

      return;
    }


    /**
     *   @brief  Compute the Airy functions
     *           @f$ Ai(x) @f$ and @f$ Bi(x) @f$ and their first
     *           derivatives @f$ Ai'(x) @f$ and @f$ Bi(x) @f$
     *           respectively.
     *
     *   @param  __x  The argument of the Airy functions.
     *   @param  __Ai  The output Airy function of the first kind.
     *   @param  __Bi  The output Airy function of the second kind.
     *   @param  __Aip  The output derivative of the Airy function
     *                  of the first kind.
     *   @param  __Bip  The output derivative of the Airy function
     *                  of the second kind.
     */
    template <typename _Tp>
    void
    __airy(_Tp __x, _Tp & __Ai, _Tp & __Bi, _Tp & __Aip, _Tp & __Bip)
    {
      const _Tp __absx = std::abs(__x);
      const _Tp __rootx = std::sqrt(__absx);
      const _Tp __z = _Tp(2) * __absx * __rootx / _Tp(3);
      const _Tp _S_inf = std::numeric_limits<_Tp>::infinity();

      if (__isnan(__x))
        __Bip = __Aip = __Bi = __Ai = std::numeric_limits<_Tp>::quiet_NaN();
      else if (__z == _S_inf)
        {
	  __Aip = __Ai = _Tp(0);
	  __Bip = __Bi = _S_inf;
	}
      else if (__z == -_S_inf)
	__Bip = __Aip = __Bi = __Ai = _Tp(0);
      else if (__x > _Tp(0))
        {
          _Tp __I_nu, __Ip_nu, __K_nu, __Kp_nu;

          __bessel_ik(_Tp(1) / _Tp(3), __z, __I_nu, __K_nu, __Ip_nu, __Kp_nu);
          __Ai = __rootx * __K_nu
               / (__numeric_constants<_Tp>::__sqrt3()
                * __numeric_constants<_Tp>::__pi());
          __Bi = __rootx * (__K_nu / __numeric_constants<_Tp>::__pi()
                 + _Tp(2) * __I_nu / __numeric_constants<_Tp>::__sqrt3());

          __bessel_ik(_Tp(2) / _Tp(3), __z, __I_nu, __K_nu, __Ip_nu, __Kp_nu);
          __Aip = -__x * __K_nu
                / (__numeric_constants<_Tp>::__sqrt3()
                 * __numeric_constants<_Tp>::__pi());
          __Bip = __x * (__K_nu / __numeric_constants<_Tp>::__pi()
                      + _Tp(2) * __I_nu
                      / __numeric_constants<_Tp>::__sqrt3());
        }
      else if (__x < _Tp(0))
        {
          _Tp __J_nu, __Jp_nu, __N_nu, __Np_nu;

          __bessel_jn(_Tp(1) / _Tp(3), __z, __J_nu, __N_nu, __Jp_nu, __Np_nu);
          __Ai = __rootx * (__J_nu
                    - __N_nu / __numeric_constants<_Tp>::__sqrt3()) / _Tp(2);
          __Bi = -__rootx * (__N_nu
                    + __J_nu / __numeric_constants<_Tp>::__sqrt3()) / _Tp(2);

          __bessel_jn(_Tp(2) / _Tp(3), __z, __J_nu, __N_nu, __Jp_nu, __Np_nu);
          __Aip = __absx * (__N_nu / __numeric_constants<_Tp>::__sqrt3()
                          + __J_nu) / _Tp(2);
          __Bip = __absx * (__J_nu / __numeric_constants<_Tp>::__sqrt3()
                          - __N_nu) / _Tp(2);
        }
      else
        {
          //  Reference:
          //    Abramowitz & Stegun, page 446 section 10.4.4 on Airy functions.
          //  The number is Ai(0) = 3^{-2/3}/\Gamma(2/3).
          __Ai = _Tp(0.35502805388781723926L);
          __Bi = __Ai * __numeric_constants<_Tp>::__sqrt3();

          //  Reference:
          //    Abramowitz & Stegun, page 446 section 10.4.5 on Airy functions.
          //  The number is Ai'(0) = -3^{-1/3}/\Gamma(1/3).
          __Aip = -_Tp(0.25881940379280679840L);
          __Bip = -__Aip * __numeric_constants<_Tp>::__sqrt3();
        }

      return;
    }
  } // namespace __detail
#if ! _GLIBCXX_USE_STD_SPEC_FUNCS && defined(_GLIBCXX_TR1_CMATH)
} // namespace tr1
#endif

_GLIBCXX_END_NAMESPACE_VERSION
}

#endif // _GLIBCXX_TR1_MODIFIED_BESSEL_FUNC_TCC
