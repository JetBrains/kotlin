// chrono::duration and chrono::time_point -*- C++ -*-

// Copyright (C) 2008-2022 Free Software Foundation, Inc.
//
// This file is part of the GNU ISO C++ Library.  This library is free
// software; you can redistribute it and/or modify it under the
// terms of the GNU General Public License as published by the
// Free Software Foundation; either version 3, or (at your option)
// any later version.

// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// Under Section 7 of GPL version 3, you are granted additional
// permissions described in the GCC Runtime Library Exception, version
// 3.1, as published by the Free Software Foundation.

// You should have received a copy of the GNU General Public License and
// a copy of the GCC Runtime Library Exception along with this program;
// see the files COPYING3 and COPYING.RUNTIME respectively.  If not, see
// <http://www.gnu.org/licenses/>.

/** @file include/bits/chrono.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{chrono}
 */

#ifndef _GLIBCXX_CHRONO_H
#define _GLIBCXX_CHRONO_H 1

#pragma GCC system_header

#if __cplusplus >= 201103L

#include <ratio>
#include <type_traits>
#include <limits>
#include <ctime>
#include <bits/parse_numbers.h> // for literals support.
#if __cplusplus >= 202002L
# include <concepts>
# include <compare>
#endif

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

#if __cplusplus >= 201703L
  namespace filesystem { struct __file_clock; };
#endif

  namespace chrono
  {
    /// @addtogroup chrono
    /// @{

    /// `chrono::duration` represents a distance between two points in time
    template<typename _Rep, typename _Period = ratio<1>>
      struct duration;

    /// `chrono::time_point` represents a point in time as measured by a clock
    template<typename _Clock, typename _Dur = typename _Clock::duration>
      struct time_point;
    /// @}
  }

  /// @addtogroup chrono
  /// @{

  // 20.11.4.3 specialization of common_type (for duration, sfinae-friendly)

  /// @cond undocumented

  template<typename _CT, typename _Period1, typename _Period2, typename = void>
    struct __duration_common_type
    { };

  template<typename _CT, typename _Period1, typename _Period2>
    struct __duration_common_type<_CT, _Period1, _Period2,
				  __void_t<typename _CT::type>>
    {
    private:
      using __gcd_num = __static_gcd<_Period1::num, _Period2::num>;
      using __gcd_den = __static_gcd<_Period1::den, _Period2::den>;
      using __cr = typename _CT::type;
      using __r = ratio<__gcd_num::value,
			(_Period1::den / __gcd_den::value) * _Period2::den>;

    public:
      using type = chrono::duration<__cr, typename __r::type>;
    };

  /// @endcond

  /// @{
  /// @relates chrono::duration

  /// Specialization of common_type for chrono::duration types.
  template<typename _Rep1, typename _Period1, typename _Rep2, typename _Period2>
    struct common_type<chrono::duration<_Rep1, _Period1>,
		       chrono::duration<_Rep2, _Period2>>
    : __duration_common_type<common_type<_Rep1, _Rep2>,
			     typename _Period1::type,
			     typename _Period2::type>
    { };

  /// Specialization of common_type for two identical chrono::duration types.
  template<typename _Rep, typename _Period>
    struct common_type<chrono::duration<_Rep, _Period>,
		       chrono::duration<_Rep, _Period>>
    {
      using type = chrono::duration<typename common_type<_Rep>::type,
				    typename _Period::type>;
    };

  /// Specialization of common_type for one chrono::duration type.
  template<typename _Rep, typename _Period>
    struct common_type<chrono::duration<_Rep, _Period>>
    {
      using type = chrono::duration<typename common_type<_Rep>::type,
				    typename _Period::type>;
    };
  /// @}

  // 20.11.4.3 specialization of common_type (for time_point, sfinae-friendly)

  /// @cond undocumented

  template<typename _CT, typename _Clock, typename = void>
    struct __timepoint_common_type
    { };

  template<typename _CT, typename _Clock>
    struct __timepoint_common_type<_CT, _Clock, __void_t<typename _CT::type>>
    {
      using type = chrono::time_point<_Clock, typename _CT::type>;
    };

  /// @endcond

  /// @{
  /// @relates chrono::time_point

  /// Specialization of common_type for chrono::time_point types.
  template<typename _Clock, typename _Duration1, typename _Duration2>
    struct common_type<chrono::time_point<_Clock, _Duration1>,
		       chrono::time_point<_Clock, _Duration2>>
    : __timepoint_common_type<common_type<_Duration1, _Duration2>, _Clock>
    { };

  /// Specialization of common_type for two identical chrono::time_point types.
  template<typename _Clock, typename _Duration>
    struct common_type<chrono::time_point<_Clock, _Duration>,
		       chrono::time_point<_Clock, _Duration>>
    { using type = chrono::time_point<_Clock, _Duration>; };

  /// Specialization of common_type for one chrono::time_point type.
  template<typename _Clock, typename _Duration>
    struct common_type<chrono::time_point<_Clock, _Duration>>
    { using type = chrono::time_point<_Clock, _Duration>; };
  /// @}

  /// @} group chrono

  namespace chrono
  {
    /// @addtogroup chrono
    /// @{

    /// @cond undocumented

    // Primary template for duration_cast impl.
    template<typename _ToDur, typename _CF, typename _CR,
	     bool _NumIsOne = false, bool _DenIsOne = false>
      struct __duration_cast_impl
      {
	template<typename _Rep, typename _Period>
	  static constexpr _ToDur
	  __cast(const duration<_Rep, _Period>& __d)
	  {
	    typedef typename _ToDur::rep			__to_rep;
	    return _ToDur(static_cast<__to_rep>(static_cast<_CR>(__d.count())
	      * static_cast<_CR>(_CF::num)
	      / static_cast<_CR>(_CF::den)));
	  }
      };

    template<typename _ToDur, typename _CF, typename _CR>
      struct __duration_cast_impl<_ToDur, _CF, _CR, true, true>
      {
	template<typename _Rep, typename _Period>
	  static constexpr _ToDur
	  __cast(const duration<_Rep, _Period>& __d)
	  {
	    typedef typename _ToDur::rep			__to_rep;
	    return _ToDur(static_cast<__to_rep>(__d.count()));
	  }
      };

    template<typename _ToDur, typename _CF, typename _CR>
      struct __duration_cast_impl<_ToDur, _CF, _CR, true, false>
      {
	template<typename _Rep, typename _Period>
	  static constexpr _ToDur
	  __cast(const duration<_Rep, _Period>& __d)
	  {
	    typedef typename _ToDur::rep			__to_rep;
	    return _ToDur(static_cast<__to_rep>(
	      static_cast<_CR>(__d.count()) / static_cast<_CR>(_CF::den)));
	  }
      };

    template<typename _ToDur, typename _CF, typename _CR>
      struct __duration_cast_impl<_ToDur, _CF, _CR, false, true>
      {
	template<typename _Rep, typename _Period>
	  static constexpr _ToDur
	  __cast(const duration<_Rep, _Period>& __d)
	  {
	    typedef typename _ToDur::rep			__to_rep;
	    return _ToDur(static_cast<__to_rep>(
	      static_cast<_CR>(__d.count()) * static_cast<_CR>(_CF::num)));
	  }
      };

    template<typename _Tp>
      struct __is_duration
      : std::false_type
      { };

    template<typename _Rep, typename _Period>
      struct __is_duration<duration<_Rep, _Period>>
      : std::true_type
      { };

    template<typename _Tp>
      using __enable_if_is_duration
	= typename enable_if<__is_duration<_Tp>::value, _Tp>::type;

    template<typename _Tp>
      using __disable_if_is_duration
	= typename enable_if<!__is_duration<_Tp>::value, _Tp>::type;

    /// @endcond

    /// duration_cast
    template<typename _ToDur, typename _Rep, typename _Period>
      constexpr __enable_if_is_duration<_ToDur>
      duration_cast(const duration<_Rep, _Period>& __d)
      {
	typedef typename _ToDur::period				__to_period;
	typedef typename _ToDur::rep				__to_rep;
	typedef ratio_divide<_Period, __to_period> 		__cf;
	typedef typename common_type<__to_rep, _Rep, intmax_t>::type __cr;
	typedef  __duration_cast_impl<_ToDur, __cf, __cr,
				      __cf::num == 1, __cf::den == 1> __dc;
	return __dc::__cast(__d);
      }

    /// treat_as_floating_point
    template<typename _Rep>
      struct treat_as_floating_point
      : is_floating_point<_Rep>
      { };

#if __cplusplus > 201402L
    template <typename _Rep>
      inline constexpr bool treat_as_floating_point_v =
	treat_as_floating_point<_Rep>::value;
#endif // C++17

#if __cplusplus > 201703L
    template<typename _Tp>
      struct is_clock;

    template<typename _Tp>
      inline constexpr bool is_clock_v = is_clock<_Tp>::value;

#if __cpp_lib_concepts
    template<typename _Tp>
      struct is_clock : false_type
      { };

    template<typename _Tp>
      requires requires {
	typename _Tp::rep;
	typename _Tp::period;
	typename _Tp::duration;
	typename _Tp::time_point::clock;
	typename _Tp::time_point::duration;
	{ &_Tp::is_steady } -> same_as<const bool*>;
	{ _Tp::now() } -> same_as<typename _Tp::time_point>;
	requires same_as<typename _Tp::duration,
			 duration<typename _Tp::rep, typename _Tp::period>>;
	requires same_as<typename _Tp::time_point::duration,
			 typename _Tp::duration>;
      }
      struct is_clock<_Tp> : true_type
      { };
#else
    template<typename _Tp, typename = void>
      struct __is_clock_impl : false_type
      { };

    template<typename _Tp>
      struct __is_clock_impl<_Tp,
			     void_t<typename _Tp::rep, typename _Tp::period,
				    typename _Tp::duration,
				    typename _Tp::time_point::duration,
				    decltype(_Tp::is_steady),
				    decltype(_Tp::now())>>
      : __and_<is_same<typename _Tp::duration,
		       duration<typename _Tp::rep, typename _Tp::period>>,
	       is_same<typename _Tp::time_point::duration,
		       typename _Tp::duration>,
	       is_same<decltype(&_Tp::is_steady), const bool*>,
	       is_same<decltype(_Tp::now()), typename _Tp::time_point>>::type
      { };

    template<typename _Tp>
      struct is_clock : __is_clock_impl<_Tp>::type
      { };
#endif
#endif // C++20

#if __cplusplus >= 201703L
# define __cpp_lib_chrono 201611L

    template<typename _ToDur, typename _Rep, typename _Period>
      constexpr __enable_if_is_duration<_ToDur>
      floor(const duration<_Rep, _Period>& __d)
      {
	auto __to = chrono::duration_cast<_ToDur>(__d);
	if (__to > __d)
	  return __to - _ToDur{1};
	return __to;
      }

    template<typename _ToDur, typename _Rep, typename _Period>
      constexpr __enable_if_is_duration<_ToDur>
      ceil(const duration<_Rep, _Period>& __d)
      {
	auto __to = chrono::duration_cast<_ToDur>(__d);
	if (__to < __d)
	  return __to + _ToDur{1};
	return __to;
      }

    template <typename _ToDur, typename _Rep, typename _Period>
      constexpr enable_if_t<
	__and_<__is_duration<_ToDur>,
	       __not_<treat_as_floating_point<typename _ToDur::rep>>>::value,
	_ToDur>
      round(const duration<_Rep, _Period>& __d)
      {
	_ToDur __t0 = chrono::floor<_ToDur>(__d);
	_ToDur __t1 = __t0 + _ToDur{1};
	auto __diff0 = __d - __t0;
	auto __diff1 = __t1 - __d;
	if (__diff0 == __diff1)
	{
	    if (__t0.count() & 1)
		return __t1;
	    return __t0;
	}
	else if (__diff0 < __diff1)
	    return __t0;
	return __t1;
      }

    template<typename _Rep, typename _Period>
      constexpr
      enable_if_t<numeric_limits<_Rep>::is_signed, duration<_Rep, _Period>>
      abs(duration<_Rep, _Period> __d)
      {
	if (__d >= __d.zero())
	  return __d;
	return -__d;
      }

    // Make chrono::ceil<D> also usable as chrono::__detail::ceil<D>.
    namespace __detail { using chrono::ceil; }

#else // ! C++17

    // We want to use ceil even when compiling for earlier standards versions.
    // C++11 only allows a single statement in a constexpr function, so we
    // need to move the comparison into a separate function, __ceil_impl.
    namespace __detail
    {
      template<typename _Tp, typename _Up>
	constexpr _Tp
	__ceil_impl(const _Tp& __t, const _Up& __u)
	{
	  return (__t < __u) ? (__t + _Tp{1}) : __t;
	}

      // C++11-friendly version of std::chrono::ceil<D> for internal use.
      template<typename _ToDur, typename _Rep, typename _Period>
	constexpr _ToDur
	ceil(const duration<_Rep, _Period>& __d)
	{
	  return __detail::__ceil_impl(chrono::duration_cast<_ToDur>(__d), __d);
	}
    }
#endif // C++17

    /// duration_values
    template<typename _Rep>
      struct duration_values
      {
	static constexpr _Rep
	zero() noexcept
	{ return _Rep(0); }

	static constexpr _Rep
	max() noexcept
	{ return numeric_limits<_Rep>::max(); }

	static constexpr _Rep
	min() noexcept
	{ return numeric_limits<_Rep>::lowest(); }
      };

    /// @cond undocumented

    template<typename _Tp>
      struct __is_ratio
      : std::false_type
      { };

    template<intmax_t _Num, intmax_t _Den>
      struct __is_ratio<ratio<_Num, _Den>>
      : std::true_type
      { };

    /// @endcond

    template<typename _Rep, typename _Period>
      struct duration
      {
      private:
	template<typename _Rep2>
	  using __is_float = treat_as_floating_point<_Rep2>;

	static constexpr intmax_t
	_S_gcd(intmax_t __m, intmax_t __n) noexcept
	{
	  // Duration only allows positive periods so we don't need to
	  // handle negative values here (unlike __static_gcd and std::gcd).
#if __cplusplus >= 201402L
	  do
	    {
	      intmax_t __rem = __m % __n;
	      __m = __n;
	      __n = __rem;
	    }
	  while (__n != 0);
	  return __m;
#else
	  // C++11 doesn't allow loops in constexpr functions, but this
	  // recursive version can be more expensive to evaluate.
	  return (__n == 0) ? __m : _S_gcd(__n, __m % __n);
#endif
	}

	// _GLIBCXX_RESOLVE_LIB_DEFECTS
	// 2094. overflow shouldn't participate in overload resolution
	// 3090. What is [2094] intended to mean?
	// This only produces a valid type if no overflow occurs.
	template<typename _R1, typename _R2,
		 intmax_t __gcd1 = _S_gcd(_R1::num, _R2::num),
		 intmax_t __gcd2 = _S_gcd(_R1::den, _R2::den)>
	  using __divide = ratio<(_R1::num / __gcd1) * (_R2::den / __gcd2),
				 (_R1::den / __gcd2) * (_R2::num / __gcd1)>;

	// _Period2 is an exact multiple of _Period
	template<typename _Period2>
	  using __is_harmonic
	    = __bool_constant<__divide<_Period2, _Period>::den == 1>;

      public:

	using rep = _Rep;
	using period = typename _Period::type;

	static_assert(!__is_duration<_Rep>::value, "rep cannot be a duration");
	static_assert(__is_ratio<_Period>::value,
		      "period must be a specialization of ratio");
	static_assert(_Period::num > 0, "period must be positive");

	// 20.11.5.1 construction / copy / destroy
	constexpr duration() = default;

	duration(const duration&) = default;

	// _GLIBCXX_RESOLVE_LIB_DEFECTS
	// 3050. Conversion specification problem in chrono::duration
	template<typename _Rep2, typename = _Require<
		 is_convertible<const _Rep2&, rep>,
		 __or_<__is_float<rep>, __not_<__is_float<_Rep2>>>>>
	  constexpr explicit duration(const _Rep2& __rep)
	  : __r(static_cast<rep>(__rep)) { }

	template<typename _Rep2, typename _Period2, typename = _Require<
		 is_convertible<const _Rep2&, rep>,
		 __or_<__is_float<rep>,
		       __and_<__is_harmonic<_Period2>,
			      __not_<__is_float<_Rep2>>>>>>
	  constexpr duration(const duration<_Rep2, _Period2>& __d)
	  : __r(duration_cast<duration>(__d).count()) { }

	~duration() = default;
	duration& operator=(const duration&) = default;

	// 20.11.5.2 observer
	constexpr rep
	count() const
	{ return __r; }

	// 20.11.5.3 arithmetic

	constexpr duration<typename common_type<rep>::type, period>
	operator+() const
	{ return duration<typename common_type<rep>::type, period>(__r); }

	constexpr duration<typename common_type<rep>::type, period>
	operator-() const
	{ return duration<typename common_type<rep>::type, period>(-__r); }

	_GLIBCXX17_CONSTEXPR duration&
	operator++()
	{
	  ++__r;
	  return *this;
	}

	_GLIBCXX17_CONSTEXPR duration
	operator++(int)
	{ return duration(__r++); }

	_GLIBCXX17_CONSTEXPR duration&
	operator--()
	{
	  --__r;
	  return *this;
	}

	_GLIBCXX17_CONSTEXPR duration
	operator--(int)
	{ return duration(__r--); }

	_GLIBCXX17_CONSTEXPR duration&
	operator+=(const duration& __d)
	{
	  __r += __d.count();
	  return *this;
	}

	_GLIBCXX17_CONSTEXPR duration&
	operator-=(const duration& __d)
	{
	  __r -= __d.count();
	  return *this;
	}

	_GLIBCXX17_CONSTEXPR duration&
	operator*=(const rep& __rhs)
	{
	  __r *= __rhs;
	  return *this;
	}

	_GLIBCXX17_CONSTEXPR duration&
	operator/=(const rep& __rhs)
	{
	  __r /= __rhs;
	  return *this;
	}

	// DR 934.
	template<typename _Rep2 = rep>
	  _GLIBCXX17_CONSTEXPR
	  typename enable_if<!treat_as_floating_point<_Rep2>::value,
			     duration&>::type
	  operator%=(const rep& __rhs)
	  {
	    __r %= __rhs;
	    return *this;
	  }

	template<typename _Rep2 = rep>
	  _GLIBCXX17_CONSTEXPR
	  typename enable_if<!treat_as_floating_point<_Rep2>::value,
			     duration&>::type
	  operator%=(const duration& __d)
	  {
	    __r %= __d.count();
	    return *this;
	  }

	// 20.11.5.4 special values
	static constexpr duration
	zero() noexcept
	{ return duration(duration_values<rep>::zero()); }

	static constexpr duration
	min() noexcept
	{ return duration(duration_values<rep>::min()); }

	static constexpr duration
	max() noexcept
	{ return duration(duration_values<rep>::max()); }

      private:
	rep __r;
      };

    /// @{
    /// @relates std::chrono::duration

    /// The sum of two durations.
    template<typename _Rep1, typename _Period1,
	     typename _Rep2, typename _Period2>
      constexpr typename common_type<duration<_Rep1, _Period1>,
				     duration<_Rep2, _Period2>>::type
      operator+(const duration<_Rep1, _Period1>& __lhs,
		const duration<_Rep2, _Period2>& __rhs)
      {
	typedef duration<_Rep1, _Period1>			__dur1;
	typedef duration<_Rep2, _Period2>			__dur2;
	typedef typename common_type<__dur1,__dur2>::type	__cd;
	return __cd(__cd(__lhs).count() + __cd(__rhs).count());
      }

    /// The difference between two durations.
    template<typename _Rep1, typename _Period1,
	     typename _Rep2, typename _Period2>
      constexpr typename common_type<duration<_Rep1, _Period1>,
				     duration<_Rep2, _Period2>>::type
      operator-(const duration<_Rep1, _Period1>& __lhs,
		const duration<_Rep2, _Period2>& __rhs)
      {
	typedef duration<_Rep1, _Period1>			__dur1;
	typedef duration<_Rep2, _Period2>			__dur2;
	typedef typename common_type<__dur1,__dur2>::type	__cd;
	return __cd(__cd(__lhs).count() - __cd(__rhs).count());
      }

    /// @}

    /// @cond undocumented

    // SFINAE helper to obtain common_type<_Rep1, _Rep2> only if _Rep2
    // is implicitly convertible to it.
    // _GLIBCXX_RESOLVE_LIB_DEFECTS
    // 3050. Conversion specification problem in chrono::duration constructor
    template<typename _Rep1, typename _Rep2,
	     typename _CRep = typename common_type<_Rep1, _Rep2>::type>
      using __common_rep_t = typename
	enable_if<is_convertible<const _Rep2&, _CRep>::value, _CRep>::type;

    /// @endcond

    /** @{
     * Arithmetic operators for chrono::duration
     * @relates std::chrono::duration
     */

    template<typename _Rep1, typename _Period, typename _Rep2>
      constexpr duration<__common_rep_t<_Rep1, _Rep2>, _Period>
      operator*(const duration<_Rep1, _Period>& __d, const _Rep2& __s)
      {
	typedef duration<typename common_type<_Rep1, _Rep2>::type, _Period>
	  __cd;
	return __cd(__cd(__d).count() * __s);
      }

    template<typename _Rep1, typename _Rep2, typename _Period>
      constexpr duration<__common_rep_t<_Rep2, _Rep1>, _Period>
      operator*(const _Rep1& __s, const duration<_Rep2, _Period>& __d)
      { return __d * __s; }

    template<typename _Rep1, typename _Period, typename _Rep2>
      constexpr
      duration<__common_rep_t<_Rep1, __disable_if_is_duration<_Rep2>>, _Period>
      operator/(const duration<_Rep1, _Period>& __d, const _Rep2& __s)
      {
	typedef duration<typename common_type<_Rep1, _Rep2>::type, _Period>
	  __cd;
	return __cd(__cd(__d).count() / __s);
      }

    template<typename _Rep1, typename _Period1,
	     typename _Rep2, typename _Period2>
      constexpr typename common_type<_Rep1, _Rep2>::type
      operator/(const duration<_Rep1, _Period1>& __lhs,
		const duration<_Rep2, _Period2>& __rhs)
      {
	typedef duration<_Rep1, _Period1>			__dur1;
	typedef duration<_Rep2, _Period2>			__dur2;
	typedef typename common_type<__dur1,__dur2>::type	__cd;
	return __cd(__lhs).count() / __cd(__rhs).count();
      }

    // DR 934.
    template<typename _Rep1, typename _Period, typename _Rep2>
      constexpr
      duration<__common_rep_t<_Rep1, __disable_if_is_duration<_Rep2>>, _Period>
      operator%(const duration<_Rep1, _Period>& __d, const _Rep2& __s)
      {
	typedef duration<typename common_type<_Rep1, _Rep2>::type, _Period>
	  __cd;
	return __cd(__cd(__d).count() % __s);
      }

    template<typename _Rep1, typename _Period1,
	     typename _Rep2, typename _Period2>
      constexpr typename common_type<duration<_Rep1, _Period1>,
				     duration<_Rep2, _Period2>>::type
      operator%(const duration<_Rep1, _Period1>& __lhs,
		const duration<_Rep2, _Period2>& __rhs)
      {
	typedef duration<_Rep1, _Period1>			__dur1;
	typedef duration<_Rep2, _Period2>			__dur2;
	typedef typename common_type<__dur1,__dur2>::type	__cd;
	return __cd(__cd(__lhs).count() % __cd(__rhs).count());
      }
    /// @}

    // comparisons

    /** @{
     * Comparisons for chrono::duration
     * @relates std::chrono::duration
     */

    template<typename _Rep1, typename _Period1,
	     typename _Rep2, typename _Period2>
      constexpr bool
      operator==(const duration<_Rep1, _Period1>& __lhs,
		 const duration<_Rep2, _Period2>& __rhs)
      {
	typedef duration<_Rep1, _Period1>			__dur1;
	typedef duration<_Rep2, _Period2>			__dur2;
	typedef typename common_type<__dur1,__dur2>::type	__ct;
	return __ct(__lhs).count() == __ct(__rhs).count();
      }

    template<typename _Rep1, typename _Period1,
	     typename _Rep2, typename _Period2>
      constexpr bool
      operator<(const duration<_Rep1, _Period1>& __lhs,
		const duration<_Rep2, _Period2>& __rhs)
      {
	typedef duration<_Rep1, _Period1>			__dur1;
	typedef duration<_Rep2, _Period2>			__dur2;
	typedef typename common_type<__dur1,__dur2>::type	__ct;
	return __ct(__lhs).count() < __ct(__rhs).count();
      }

#if __cpp_lib_three_way_comparison
    template<typename _Rep1, typename _Period1,
	     typename _Rep2, typename _Period2>
      requires three_way_comparable<common_type_t<_Rep1, _Rep2>>
      constexpr auto
      operator<=>(const duration<_Rep1, _Period1>& __lhs,
		  const duration<_Rep2, _Period2>& __rhs)
      {
	using __ct = common_type_t<duration<_Rep1, _Period1>,
				   duration<_Rep2, _Period2>>;
	return __ct(__lhs).count() <=> __ct(__rhs).count();
      }
#else
    template<typename _Rep1, typename _Period1,
	     typename _Rep2, typename _Period2>
      constexpr bool
      operator!=(const duration<_Rep1, _Period1>& __lhs,
		 const duration<_Rep2, _Period2>& __rhs)
      { return !(__lhs == __rhs); }
#endif

    template<typename _Rep1, typename _Period1,
	     typename _Rep2, typename _Period2>
      constexpr bool
      operator<=(const duration<_Rep1, _Period1>& __lhs,
		 const duration<_Rep2, _Period2>& __rhs)
      { return !(__rhs < __lhs); }

    template<typename _Rep1, typename _Period1,
	     typename _Rep2, typename _Period2>
      constexpr bool
      operator>(const duration<_Rep1, _Period1>& __lhs,
		const duration<_Rep2, _Period2>& __rhs)
      { return __rhs < __lhs; }

    template<typename _Rep1, typename _Period1,
	     typename _Rep2, typename _Period2>
      constexpr bool
      operator>=(const duration<_Rep1, _Period1>& __lhs,
		 const duration<_Rep2, _Period2>& __rhs)
      { return !(__lhs < __rhs); }

    /// @}

    /// @cond undocumented
#ifdef _GLIBCXX_USE_C99_STDINT_TR1
# define _GLIBCXX_CHRONO_INT64_T int64_t
#elif defined __INT64_TYPE__
# define _GLIBCXX_CHRONO_INT64_T __INT64_TYPE__
#else
    static_assert(std::numeric_limits<unsigned long long>::digits >= 64,
	"Representation type for nanoseconds must have at least 64 bits");
# define _GLIBCXX_CHRONO_INT64_T long long
#endif
    /// @endcond

    /// nanoseconds
    using nanoseconds	= duration<_GLIBCXX_CHRONO_INT64_T, nano>;

    /// microseconds
    using microseconds	= duration<_GLIBCXX_CHRONO_INT64_T, micro>;

    /// milliseconds
    using milliseconds	= duration<_GLIBCXX_CHRONO_INT64_T, milli>;

    /// seconds
    using seconds	= duration<_GLIBCXX_CHRONO_INT64_T>;

    /// minutes
    using minutes	= duration<_GLIBCXX_CHRONO_INT64_T, ratio< 60>>;

    /// hours
    using hours		= duration<_GLIBCXX_CHRONO_INT64_T, ratio<3600>>;

#if __cplusplus > 201703L
    /// days
    using days		= duration<_GLIBCXX_CHRONO_INT64_T, ratio<86400>>;

    /// weeks
    using weeks		= duration<_GLIBCXX_CHRONO_INT64_T, ratio<604800>>;

    /// years
    using years		= duration<_GLIBCXX_CHRONO_INT64_T, ratio<31556952>>;

    /// months
    using months	= duration<_GLIBCXX_CHRONO_INT64_T, ratio<2629746>>;
#endif // C++20

#undef _GLIBCXX_CHRONO_INT64_T

    template<typename _Clock, typename _Dur>
      struct time_point
      {
	static_assert(__is_duration<_Dur>::value,
	    "duration must be a specialization of std::chrono::duration");

	typedef _Clock						clock;
	typedef _Dur						duration;
	typedef typename duration::rep				rep;
	typedef typename duration::period			period;

	constexpr time_point() : __d(duration::zero())
	{ }

	constexpr explicit time_point(const duration& __dur)
	: __d(__dur)
	{ }

	// conversions
	template<typename _Dur2,
		 typename = _Require<is_convertible<_Dur2, _Dur>>>
	  constexpr time_point(const time_point<clock, _Dur2>& __t)
	  : __d(__t.time_since_epoch())
	  { }

	// observer
	constexpr duration
	time_since_epoch() const
	{ return __d; }

#if __cplusplus > 201703L
	constexpr time_point&
	operator++()
	{
	  ++__d;
	  return *this;
	}

	constexpr time_point
	operator++(int)
	{ return time_point{__d++}; }

	constexpr time_point&
	operator--()
	{
	  --__d;
	  return *this;
	}

	constexpr time_point
	operator--(int)
	{ return time_point{__d--}; }
#endif

	// arithmetic
	_GLIBCXX17_CONSTEXPR time_point&
	operator+=(const duration& __dur)
	{
	  __d += __dur;
	  return *this;
	}

	_GLIBCXX17_CONSTEXPR time_point&
	operator-=(const duration& __dur)
	{
	  __d -= __dur;
	  return *this;
	}

	// special values
	static constexpr time_point
	min() noexcept
	{ return time_point(duration::min()); }

	static constexpr time_point
	max() noexcept
	{ return time_point(duration::max()); }

      private:
	duration __d;
      };

    /// time_point_cast
    template<typename _ToDur, typename _Clock, typename _Dur>
      constexpr typename enable_if<__is_duration<_ToDur>::value,
				   time_point<_Clock, _ToDur>>::type
      time_point_cast(const time_point<_Clock, _Dur>& __t)
      {
	typedef time_point<_Clock, _ToDur>			__time_point;
	return __time_point(duration_cast<_ToDur>(__t.time_since_epoch()));
      }

#if __cplusplus > 201402L
    template<typename _ToDur, typename _Clock, typename _Dur>
      constexpr
      enable_if_t<__is_duration<_ToDur>::value, time_point<_Clock, _ToDur>>
      floor(const time_point<_Clock, _Dur>& __tp)
      {
	return time_point<_Clock, _ToDur>{
	    chrono::floor<_ToDur>(__tp.time_since_epoch())};
      }

    template<typename _ToDur, typename _Clock, typename _Dur>
      constexpr
      enable_if_t<__is_duration<_ToDur>::value, time_point<_Clock, _ToDur>>
      ceil(const time_point<_Clock, _Dur>& __tp)
      {
	return time_point<_Clock, _ToDur>{
	    chrono::ceil<_ToDur>(__tp.time_since_epoch())};
      }

    template<typename _ToDur, typename _Clock, typename _Dur>
      constexpr enable_if_t<
	__and_<__is_duration<_ToDur>,
	       __not_<treat_as_floating_point<typename _ToDur::rep>>>::value,
	time_point<_Clock, _ToDur>>
      round(const time_point<_Clock, _Dur>& __tp)
      {
	return time_point<_Clock, _ToDur>{
	    chrono::round<_ToDur>(__tp.time_since_epoch())};
      }
#endif // C++17

    /// @{
    /// @relates time_point

    /// Adjust a time point forwards by the given duration.
    template<typename _Clock, typename _Dur1,
	     typename _Rep2, typename _Period2>
      constexpr time_point<_Clock,
	typename common_type<_Dur1, duration<_Rep2, _Period2>>::type>
      operator+(const time_point<_Clock, _Dur1>& __lhs,
		const duration<_Rep2, _Period2>& __rhs)
      {
	typedef duration<_Rep2, _Period2>			__dur2;
	typedef typename common_type<_Dur1,__dur2>::type	__ct;
	typedef time_point<_Clock, __ct>			__time_point;
	return __time_point(__lhs.time_since_epoch() + __rhs);
      }

    /// Adjust a time point forwards by the given duration.
    template<typename _Rep1, typename _Period1,
	     typename _Clock, typename _Dur2>
      constexpr time_point<_Clock,
	typename common_type<duration<_Rep1, _Period1>, _Dur2>::type>
      operator+(const duration<_Rep1, _Period1>& __lhs,
		const time_point<_Clock, _Dur2>& __rhs)
      {
	typedef duration<_Rep1, _Period1>			__dur1;
	typedef typename common_type<__dur1,_Dur2>::type	__ct;
	typedef time_point<_Clock, __ct>			__time_point;
	return __time_point(__rhs.time_since_epoch() + __lhs);
      }

    /// Adjust a time point backwards by the given duration.
    template<typename _Clock, typename _Dur1,
	     typename _Rep2, typename _Period2>
      constexpr time_point<_Clock,
	typename common_type<_Dur1, duration<_Rep2, _Period2>>::type>
      operator-(const time_point<_Clock, _Dur1>& __lhs,
		const duration<_Rep2, _Period2>& __rhs)
      {
	typedef duration<_Rep2, _Period2>			__dur2;
	typedef typename common_type<_Dur1,__dur2>::type	__ct;
	typedef time_point<_Clock, __ct>			__time_point;
	return __time_point(__lhs.time_since_epoch() -__rhs);
      }

    /// The difference between two time points (as a duration)
    template<typename _Clock, typename _Dur1, typename _Dur2>
      constexpr typename common_type<_Dur1, _Dur2>::type
      operator-(const time_point<_Clock, _Dur1>& __lhs,
		const time_point<_Clock, _Dur2>& __rhs)
      { return __lhs.time_since_epoch() - __rhs.time_since_epoch(); }
    /// @}

    /** @{
     * Comparisons for time_point
     * @relates chrono::time_point
     */

    template<typename _Clock, typename _Dur1, typename _Dur2>
      constexpr bool
      operator==(const time_point<_Clock, _Dur1>& __lhs,
		 const time_point<_Clock, _Dur2>& __rhs)
      { return __lhs.time_since_epoch() == __rhs.time_since_epoch(); }

#if __cpp_lib_three_way_comparison
    template<typename _Clock, typename _Dur1,
	     three_way_comparable_with<_Dur1> _Dur2>
      constexpr auto
      operator<=>(const time_point<_Clock, _Dur1>& __lhs,
		  const time_point<_Clock, _Dur2>& __rhs)
      { return __lhs.time_since_epoch() <=> __rhs.time_since_epoch(); }
#else
    template<typename _Clock, typename _Dur1, typename _Dur2>
      constexpr bool
      operator!=(const time_point<_Clock, _Dur1>& __lhs,
		 const time_point<_Clock, _Dur2>& __rhs)
      { return !(__lhs == __rhs); }
#endif

    template<typename _Clock, typename _Dur1, typename _Dur2>
      constexpr bool
      operator<(const time_point<_Clock, _Dur1>& __lhs,
		const time_point<_Clock, _Dur2>& __rhs)
      { return  __lhs.time_since_epoch() < __rhs.time_since_epoch(); }

    template<typename _Clock, typename _Dur1, typename _Dur2>
      constexpr bool
      operator<=(const time_point<_Clock, _Dur1>& __lhs,
		 const time_point<_Clock, _Dur2>& __rhs)
      { return !(__rhs < __lhs); }

    template<typename _Clock, typename _Dur1, typename _Dur2>
      constexpr bool
      operator>(const time_point<_Clock, _Dur1>& __lhs,
		const time_point<_Clock, _Dur2>& __rhs)
      { return __rhs < __lhs; }

    template<typename _Clock, typename _Dur1, typename _Dur2>
      constexpr bool
      operator>=(const time_point<_Clock, _Dur1>& __lhs,
		 const time_point<_Clock, _Dur2>& __rhs)
      { return !(__lhs < __rhs); }

    /// @}
    /// @} group chrono

    // Clocks.

    // Why nanosecond resolution as the default?
    // Why have std::system_clock always count in the highest
    // resolution (ie nanoseconds), even if on some OSes the low 3
    // or 9 decimal digits will be always zero? This allows later
    // implementations to change the system_clock::now()
    // implementation any time to provide better resolution without
    // changing function signature or units.

    // To support the (forward) evolution of the library's defined
    // clocks, wrap inside inline namespace so that the current
    // defintions of system_clock, steady_clock, and
    // high_resolution_clock types are uniquely mangled. This way, new
    // code can use the latests clocks, while the library can contain
    // compatibility definitions for previous versions.  At some
    // point, when these clocks settle down, the inlined namespaces
    // can be removed.  XXX GLIBCXX_ABI Deprecated
    inline namespace _V2 {

    /**
     *  @brief System clock.
     *
     *  Time returned represents wall time from the system-wide clock.
     *  @ingroup chrono
    */
    struct system_clock
    {
      typedef chrono::nanoseconds				duration;
      typedef duration::rep					rep;
      typedef duration::period					period;
      typedef chrono::time_point<system_clock, duration> 	time_point;

      static_assert(system_clock::duration::min()
		    < system_clock::duration::zero(),
		    "a clock's minimum duration cannot be less than its epoch");

      static constexpr bool is_steady = false;

      static time_point
      now() noexcept;

      // Map to C API
      static std::time_t
      to_time_t(const time_point& __t) noexcept
      {
	return std::time_t(duration_cast<chrono::seconds>
			   (__t.time_since_epoch()).count());
      }

      static time_point
      from_time_t(std::time_t __t) noexcept
      {
	typedef chrono::time_point<system_clock, seconds>	__from;
	return time_point_cast<system_clock::duration>
	       (__from(chrono::seconds(__t)));
      }
    };


    /**
     *  @brief Monotonic clock
     *
     *  Time returned has the property of only increasing at a uniform rate.
     *  @ingroup chrono
    */
    struct steady_clock
    {
      typedef chrono::nanoseconds				duration;
      typedef duration::rep					rep;
      typedef duration::period					period;
      typedef chrono::time_point<steady_clock, duration>	time_point;

      static constexpr bool is_steady = true;

      static time_point
      now() noexcept;
    };


    /**
     *  @brief Highest-resolution clock
     *
     *  This is the clock "with the shortest tick period." Alias to
     *  std::system_clock until higher-than-nanosecond definitions
     *  become feasible.
     *  @ingroup chrono
    */
    using high_resolution_clock = system_clock;

    } // end inline namespace _V2

#if __cplusplus >= 202002L
    /// @addtogroup chrono
    /// @{
    template<typename _Duration>
      using sys_time = time_point<system_clock, _Duration>;
    using sys_seconds = sys_time<seconds>;
    using sys_days = sys_time<days>;

    using file_clock = ::std::filesystem::__file_clock;

    template<typename _Duration>
      using file_time = time_point<file_clock, _Duration>;

    template<> struct is_clock<system_clock> : true_type { };
    template<> struct is_clock<steady_clock> : true_type { };
    template<> struct is_clock<file_clock> : true_type { };

    template<> inline constexpr bool is_clock_v<system_clock> = true;
    template<> inline constexpr bool is_clock_v<steady_clock> = true;
    template<> inline constexpr bool is_clock_v<file_clock> = true;
    /// @}
#endif // C++20
  } // namespace chrono

#if __cplusplus >= 201402L
#define __cpp_lib_chrono_udls 201304L

  inline namespace literals
  {
  /** ISO C++ 2014  namespace for suffixes for duration literals.
   *
   * These suffixes can be used to create `chrono::duration` values with
   * tick periods of hours, minutes, seconds, milliseconds, microseconds
   * or nanoseconds. For example, `std::chrono::seconds(5)` can be written
   * as `5s` after making the suffix visible in the current scope.
   * The suffixes can be made visible by a using-directive or
   * using-declaration such as:
   *  - `using namespace std::chrono_literals;`
   *  - `using namespace std::literals;`
   *  - `using namespace std::chrono;`
   *  - `using namespace std;`
   *  - `using std::chrono_literals::operator""s;`
   *
   * The result of these suffixes on an integer literal is one of the
   * standard typedefs such as `std::chrono::hours`.
   * The result on a floating-point literal is a duration type with the
   * specified tick period and an unspecified floating-point representation,
   * for example `1.5e2ms` might be equivalent to
   * `chrono::duration<long double, chrono::milli>(1.5e2)`.
   *
   * @since C+14
   * @ingroup chrono
   */
  inline namespace chrono_literals
  {
    /// @addtogroup chrono
    /// @{

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wliteral-suffix"
    /// @cond undocumented
    template<typename _Dur, char... _Digits>
      constexpr _Dur __check_overflow()
      {
	using _Val = __parse_int::_Parse_int<_Digits...>;
	constexpr typename _Dur::rep __repval = _Val::value;
	static_assert(__repval >= 0 && __repval == _Val::value,
		      "literal value cannot be represented by duration type");
	return _Dur(__repval);
      }
    /// @endcond

    /// Literal suffix for durations representing non-integer hours
    constexpr chrono::duration<long double, ratio<3600,1>>
    operator""h(long double __hours)
    { return chrono::duration<long double, ratio<3600,1>>{__hours}; }

    /// Literal suffix for durations of type `std::chrono::hours`
    template <char... _Digits>
      constexpr chrono::hours
      operator""h()
      { return __check_overflow<chrono::hours, _Digits...>(); }

    /// Literal suffix for durations representing non-integer minutes
    constexpr chrono::duration<long double, ratio<60,1>>
    operator""min(long double __mins)
    { return chrono::duration<long double, ratio<60,1>>{__mins}; }

    /// Literal suffix for durations of type `std::chrono::minutes`
    template <char... _Digits>
      constexpr chrono::minutes
      operator""min()
      { return __check_overflow<chrono::minutes, _Digits...>(); }

    /// Literal suffix for durations representing non-integer seconds
    constexpr chrono::duration<long double>
    operator""s(long double __secs)
    { return chrono::duration<long double>{__secs}; }

    /// Literal suffix for durations of type `std::chrono::seconds`
    template <char... _Digits>
      constexpr chrono::seconds
      operator""s()
      { return __check_overflow<chrono::seconds, _Digits...>(); }

    /// Literal suffix for durations representing non-integer milliseconds
    constexpr chrono::duration<long double, milli>
    operator""ms(long double __msecs)
    { return chrono::duration<long double, milli>{__msecs}; }

    /// Literal suffix for durations of type `std::chrono::milliseconds`
    template <char... _Digits>
      constexpr chrono::milliseconds
      operator""ms()
      { return __check_overflow<chrono::milliseconds, _Digits...>(); }

    /// Literal suffix for durations representing non-integer microseconds
    constexpr chrono::duration<long double, micro>
    operator""us(long double __usecs)
    { return chrono::duration<long double, micro>{__usecs}; }

    /// Literal suffix for durations of type `std::chrono::microseconds`
    template <char... _Digits>
      constexpr chrono::microseconds
      operator""us()
      { return __check_overflow<chrono::microseconds, _Digits...>(); }

    /// Literal suffix for durations representing non-integer nanoseconds
    constexpr chrono::duration<long double, nano>
    operator""ns(long double __nsecs)
    { return chrono::duration<long double, nano>{__nsecs}; }

    /// Literal suffix for durations of type `std::chrono::nanoseconds`
    template <char... _Digits>
      constexpr chrono::nanoseconds
      operator""ns()
      { return __check_overflow<chrono::nanoseconds, _Digits...>(); }

#pragma GCC diagnostic pop
    /// @}
  } // inline namespace chrono_literals
  } // inline namespace literals

  namespace chrono
  {
    using namespace literals::chrono_literals;
  } // namespace chrono
#endif // C++14

#if __cplusplus >= 201703L
  namespace filesystem
  {
    struct __file_clock
    {
      using duration                  = chrono::nanoseconds;
      using rep                       = duration::rep;
      using period                    = duration::period;
      using time_point                = chrono::time_point<__file_clock>;
      static constexpr bool is_steady = false;

      static time_point
      now() noexcept
      { return _S_from_sys(chrono::system_clock::now()); }

#if __cplusplus > 201703L
      template<typename _Dur>
	static
	chrono::file_time<_Dur>
	from_sys(const chrono::sys_time<_Dur>& __t) noexcept
	{ return _S_from_sys(__t); }

      // For internal use only
      template<typename _Dur>
	static
	chrono::sys_time<_Dur>
	to_sys(const chrono::file_time<_Dur>& __t) noexcept
	{ return _S_to_sys(__t); }
#endif // C++20

    private:
      using __sys_clock = chrono::system_clock;

      // This clock's (unspecified) epoch is 2174-01-01 00:00:00 UTC.
      // A signed 64-bit duration with nanosecond resolution gives roughly
      // +/- 292 years, which covers the 1901-2446 date range for ext4.
      static constexpr chrono::seconds _S_epoch_diff{6437664000};

    protected:
      // For internal use only
      template<typename _Dur>
	static
	chrono::time_point<__file_clock, _Dur>
	_S_from_sys(const chrono::time_point<__sys_clock, _Dur>& __t) noexcept
	{
	  using __file_time = chrono::time_point<__file_clock, _Dur>;
	  return __file_time{__t.time_since_epoch()} - _S_epoch_diff;
	}

      // For internal use only
      template<typename _Dur>
	static
	chrono::time_point<__sys_clock, _Dur>
	_S_to_sys(const chrono::time_point<__file_clock, _Dur>& __t) noexcept
	{
	  using __sys_time = chrono::time_point<__sys_clock, _Dur>;
	  return __sys_time{__t.time_since_epoch()} + _S_epoch_diff;
	}
    };
  } // namespace filesystem
#endif // C++17

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std

#endif // C++11

#endif //_GLIBCXX_CHRONO_H
