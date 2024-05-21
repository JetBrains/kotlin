// <max_size_type.h> -*- C++ -*-

// Copyright (C) 2019-2022 Free Software Foundation, Inc.
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

/** @file bits/max_size_type.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{iterator}
 */

#ifndef _GLIBCXX_MAX_SIZE_TYPE_H
#define _GLIBCXX_MAX_SIZE_TYPE_H 1

#pragma GCC system_header

#if __cplusplus > 201703L && __cpp_lib_concepts
#include <ext/numeric_traits.h>
#include <numbers>

// This header implements unsigned and signed integer-class types (as per
// [iterator.concept.winc]) that are one bit wider than the widest supported
// integer type.
//
// The set of integer types we consider includes __int128 and unsigned __int128
// (when they exist), even though they are really integer types only in GNU
// mode.  This is to obtain a consistent ABI for these integer-class types
// across strict mode and GNU mode.

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

template<typename _Tp>
  struct numeric_limits;

namespace ranges
{
  namespace __detail
  {
    class __max_size_type
    {
    public:
      __max_size_type() = default;

      template<typename _Tp> requires integral<_Tp> || __is_int128<_Tp>
	constexpr
	__max_size_type(_Tp __i) noexcept
	  : _M_val(__i), _M_msb(__i < 0)
	{ }

      constexpr explicit
      __max_size_type(const __max_diff_type& __d) noexcept;

      template<typename _Tp> requires integral<_Tp> || __is_int128<_Tp>
	constexpr explicit
	operator _Tp() const noexcept
	{ return _M_val; }

      constexpr explicit
      operator bool() const noexcept
      { return _M_val != 0 || _M_msb != 0; }

      constexpr __max_size_type
      operator+() const noexcept
      { return *this; }

      constexpr __max_size_type
      operator~() const noexcept
      { return __max_size_type{~_M_val, !_M_msb}; }

      constexpr __max_size_type
      operator-() const noexcept
      { return operator~() + 1; }

      constexpr __max_size_type&
      operator++() noexcept
      { return *this += 1; }

      constexpr __max_size_type
      operator++(int) noexcept
      {
	auto __tmp = *this;
	++*this;
	return __tmp;
      }

      constexpr __max_size_type&
      operator--() noexcept
      { return *this -= 1; }

      constexpr __max_size_type
      operator--(int) noexcept
      {
	auto __tmp = *this;
	--*this;
	return __tmp;
      }

      constexpr __max_size_type&
      operator+=(const __max_size_type& __r) noexcept
      {
	const auto __sum = _M_val + __r._M_val;
	const bool __overflow = (__sum < _M_val);
	_M_msb = _M_msb ^ __r._M_msb ^ __overflow;
	_M_val = __sum;
	return *this;
      }

      constexpr __max_size_type&
      operator-=(const __max_size_type& __r) noexcept
      { return *this += -__r; }

      constexpr __max_size_type&
      operator*=(__max_size_type __r) noexcept
      {
	constexpr __max_size_type __threshold
	  = __rep(1) << (_S_rep_bits / 2 - 1);
	if (_M_val < __threshold && __r < __threshold)
	  // When both operands are below this threshold then the
	  // multiplication can be safely computed in the base precision.
	  _M_val = _M_val * __r._M_val;
	else
	  {
	    // Otherwise, perform the multiplication in four steps, by
	    // decomposing the LHS and the RHS into 2*x+a and 2*y+b,
	    // respectively, and computing 4*x*y + 2*x*b + 2*y*a + a*b.
	    const bool __lsb = _M_val & 1;
	    const bool __rlsb = __r._M_val & 1;
	    *this >>= 1;
	    __r >>= 1;
	    _M_val = (2 * _M_val * __r._M_val
		      + _M_val * __rlsb + __r._M_val * __lsb);
	    *this <<= 1;
	    *this += __rlsb * __lsb;
	  }

	return *this;
      }

      constexpr __max_size_type&
      operator/=(const __max_size_type& __r) noexcept
      {
	__glibcxx_assert(__r != 0);

	if (!_M_msb && !__r._M_msb) [[likely]]
	  _M_val /= __r._M_val;
	else if (_M_msb && __r._M_msb)
	  {
	    _M_val = (_M_val >= __r._M_val);
	    _M_msb = 0;
	  }
	else if (!_M_msb && __r._M_msb)
	  _M_val = 0;
	else if (_M_msb && !__r._M_msb)
	  {
	    // The non-trivial case: the dividend has its MSB set and the
	    // divisor doesn't.  In this case we compute ((LHS/2)/RHS)*2
	    // in the base precision.  This quantity is either the true
	    // quotient or one less than the true quotient.
	    const auto __orig = *this;
	    *this >>= 1;
	    _M_val /= __r._M_val;
	    *this <<= 1;
	    if (__orig - *this * __r >= __r)
	      ++_M_val;
	  }
	return *this;
      }

      constexpr __max_size_type&
      operator%=(const __max_size_type& __r) noexcept
      {
	if (!_M_msb && !__r._M_msb) [[likely]]
	  _M_val %= __r._M_val;
	else
	  *this -= (*this / __r) * __r;
	return *this;
      }

      constexpr __max_size_type&
      operator<<=(const __max_size_type& __r) noexcept
      {
	__glibcxx_assert(__r <= _S_rep_bits);
	if (__r != 0)
	  {
	    _M_msb = (_M_val >> (_S_rep_bits - __r._M_val)) & 1;

	    if (__r._M_val == _S_rep_bits) [[unlikely]]
	      _M_val = 0;
	    else
	      _M_val <<= __r._M_val;
	  }
	return *this;
      }

      constexpr __max_size_type&
      operator>>=(const __max_size_type& __r) noexcept
      {
	__glibcxx_assert(__r <= _S_rep_bits);
	if (__r != 0)
	  {
	    if (__r._M_val == _S_rep_bits) [[unlikely]]
	      _M_val = 0;
	    else
	      _M_val >>= __r._M_val;

	    if (_M_msb) [[unlikely]]
	      {
		_M_val |= __rep(1) << (_S_rep_bits - __r._M_val);
		_M_msb = 0;
	      }
	  }
	return *this;
      }

      constexpr __max_size_type&
      operator&=(const __max_size_type& __r) noexcept
      {
	_M_val &= __r._M_val;
	_M_msb &= __r._M_msb;
	return *this;
      }

      constexpr __max_size_type&
      operator|=(const __max_size_type& __r) noexcept
      {
	_M_val |= __r._M_val;
	_M_msb |= __r._M_msb;
	return *this;
      }

      constexpr __max_size_type&
      operator^=(const __max_size_type& __r) noexcept
      {
	_M_val ^= __r._M_val;
	_M_msb ^= __r._M_msb;
	return *this;
      }

      template<typename _Tp> requires integral<_Tp> || __is_int128<_Tp>
	friend constexpr _Tp&
	operator+=(_Tp& __a, const __max_size_type& __b) noexcept
	{ return (__a = static_cast<_Tp>(__a + __b)); }

      template<typename _Tp> requires integral<_Tp> || __is_int128<_Tp>
	friend constexpr _Tp&
	operator-=(_Tp& __a, const __max_size_type& __b) noexcept
	{ return (__a = static_cast<_Tp>(__a - __b)); }

      template<typename _Tp> requires integral<_Tp> || __is_int128<_Tp>
	friend constexpr _Tp&
	operator*=(_Tp& __a, const __max_size_type& __b) noexcept
	{ return (__a = static_cast<_Tp>(__a * __b)); }

      template<typename _Tp> requires integral<_Tp> || __is_int128<_Tp>
	friend constexpr _Tp&
	operator/=(_Tp& __a, const __max_size_type& __b) noexcept
	{ return (__a = static_cast<_Tp>(__a / __b)); }

      template<typename _Tp> requires integral<_Tp> || __is_int128<_Tp>
	friend constexpr _Tp&
	operator%=(_Tp& __a, const __max_size_type& __b) noexcept
	{ return (__a = static_cast<_Tp>(__a % __b)); }

      template<typename _Tp> requires integral<_Tp> || __is_int128<_Tp>
	friend constexpr _Tp&
	operator&=(_Tp& __a, const __max_size_type& __b) noexcept
	{ return (__a = static_cast<_Tp>(__a & __b)); }

      template<typename _Tp> requires integral<_Tp> || __is_int128<_Tp>
	friend constexpr _Tp&
	operator|=(_Tp& __a, const __max_size_type& __b) noexcept
	{ return (__a = static_cast<_Tp>(__a | __b)); }

      template<typename _Tp> requires integral<_Tp> || __is_int128<_Tp>
	friend constexpr _Tp&
	operator^=(_Tp& __a, const __max_size_type& __b) noexcept
	{ return (__a = static_cast<_Tp>(__a ^ __b)); }

      template<typename _Tp> requires integral<_Tp> || __is_int128<_Tp>
	friend constexpr _Tp&
	operator<<=(_Tp& __a, const __max_size_type& __b) noexcept
	{ return (__a = static_cast<_Tp>(__a << __b)); }

      template<typename _Tp> requires integral<_Tp> || __is_int128<_Tp>
	friend constexpr _Tp&
	operator>>=(_Tp& __a, const __max_size_type& __b) noexcept
	{ return (__a = static_cast<_Tp>(__a >> __b)); }

      friend constexpr __max_size_type
      operator+(__max_size_type __l, const __max_size_type& __r) noexcept
      {
	__l += __r;
	return __l;
      }

      friend constexpr __max_size_type
      operator-(__max_size_type __l, const __max_size_type& __r) noexcept
      {
	__l -= __r;
	return __l;
      }

      friend constexpr __max_size_type
      operator*(__max_size_type __l, const __max_size_type& __r) noexcept
      {
	__l *= __r;
	return __l;
      }

      friend constexpr __max_size_type
      operator/(__max_size_type __l, const __max_size_type& __r) noexcept
      {
	__l /= __r;
	return __l;
      }

      friend constexpr __max_size_type
      operator%(__max_size_type __l, const __max_size_type& __r) noexcept
      {
	__l %= __r;
	return __l;
      }

      friend constexpr __max_size_type
      operator<<(__max_size_type __l, const __max_size_type& __r) noexcept
      {
	__l <<= __r;
	return __l;
      }

      friend constexpr __max_size_type
      operator>>(__max_size_type __l, const __max_size_type& __r) noexcept
      {
	__l >>= __r;
	return __l;
      }

      friend constexpr __max_size_type
      operator&(__max_size_type __l, const __max_size_type& __r) noexcept
      {
	__l &= __r;
	return __l;
      }

      friend constexpr __max_size_type
      operator|(__max_size_type __l, const __max_size_type& __r) noexcept
      {
	__l |= __r;
	return __l;
      }

      friend constexpr __max_size_type
      operator^(__max_size_type __l, const __max_size_type& __r) noexcept
      {
	__l ^= __r;
	return __l;
      }

      friend constexpr bool
      operator==(const __max_size_type& __l, const __max_size_type& __r) noexcept
      { return __l._M_val == __r._M_val && __l._M_msb == __r._M_msb; }

#if __cpp_lib_three_way_comparison
      friend constexpr strong_ordering
      operator<=>(const __max_size_type& __l, const __max_size_type& __r) noexcept
      {
	if (__l._M_msb ^ __r._M_msb)
	  return __l._M_msb ? strong_ordering::greater : strong_ordering::less;
	else
	  return __l._M_val <=> __r._M_val;
      }
#else
      friend constexpr bool
      operator!=(const __max_size_type& __l, const __max_size_type& __r) noexcept
      { return !(__l == __r); }

      friend constexpr bool
      operator<(const __max_size_type& __l, const __max_size_type& __r) noexcept
      {
	if (__l._M_msb == __r._M_msb)
	  return __l._M_val < __r._M_val;
	else
	  return __r._M_msb;
      }

      friend constexpr bool
      operator>(const __max_size_type& __l, const __max_size_type& __r) noexcept
      { return __r < __l; }

      friend constexpr bool
      operator<=(const __max_size_type& __l, const __max_size_type& __r) noexcept
      { return !(__l > __r); }

      friend constexpr bool
      operator>=(const __max_size_type& __l, const __max_size_type& __r) noexcept
      { return __r <= __l; }
#endif

#if __SIZEOF_INT128__
      __extension__
      using __rep = unsigned __int128;
#else
      using __rep = unsigned long long;
#endif
      static constexpr size_t _S_rep_bits = sizeof(__rep) * __CHAR_BIT__;
    private:
      __rep _M_val = 0;
      unsigned _M_msb:1 = 0;

      constexpr explicit
      __max_size_type(__rep __val, int __msb) noexcept
	: _M_val(__val), _M_msb(__msb)
      { }

      friend __max_diff_type;
      friend std::numeric_limits<__max_size_type>;
      friend std::numeric_limits<__max_diff_type>;
    };

    class __max_diff_type
    {
    public:
      __max_diff_type() = default;

      template<typename _Tp> requires integral<_Tp> || __is_int128<_Tp>
	constexpr
	__max_diff_type(_Tp __i) noexcept
	  : _M_rep(__i)
	{ }

      constexpr explicit
      __max_diff_type(const __max_size_type& __d) noexcept
	: _M_rep(__d)
      { }

      template<typename _Tp> requires integral<_Tp> || __is_int128<_Tp>
	constexpr explicit
	operator _Tp() const noexcept
	{ return static_cast<_Tp>(_M_rep); }

      constexpr explicit
      operator bool() const noexcept
      { return _M_rep != 0; }

      constexpr __max_diff_type
      operator+() const noexcept
      { return *this; }

      constexpr __max_diff_type
      operator-() const noexcept
      { return __max_diff_type(-_M_rep); }

      constexpr __max_diff_type
      operator~() const noexcept
      { return __max_diff_type(~_M_rep); }

      constexpr __max_diff_type&
      operator++() noexcept
      { return *this += 1; }

      constexpr __max_diff_type
      operator++(int) noexcept
      {
	auto __tmp = *this;
	++*this;
	return __tmp;
      }

      constexpr __max_diff_type&
      operator--() noexcept
      { return *this -= 1; }

      constexpr __max_diff_type
      operator--(int) noexcept
      {
	auto __tmp = *this;
	--*this;
	return __tmp;
      }

      constexpr __max_diff_type&
      operator+=(const __max_diff_type& __r) noexcept
      {
	_M_rep += __r._M_rep;
	return *this;
      }

      constexpr __max_diff_type&
      operator-=(const __max_diff_type& __r) noexcept
      {
	_M_rep -= __r._M_rep;
	return *this;
      }

      constexpr __max_diff_type&
      operator*=(const __max_diff_type& __r) noexcept
      {
	_M_rep *= __r._M_rep;
	return *this;
      }

      constexpr __max_diff_type&
      operator/=(const __max_diff_type& __r) noexcept
      {
	__glibcxx_assert (__r != 0);
	const bool __neg = *this < 0;
	const bool __rneg = __r < 0;
	if (!__neg && !__rneg)
	  _M_rep = _M_rep / __r._M_rep;
	else if (__neg && __rneg)
	  _M_rep = -_M_rep / -__r._M_rep;
	else if (__neg && !__rneg)
	  _M_rep = -(-_M_rep / __r._M_rep);
	else
	  _M_rep = -(_M_rep / -__r._M_rep);
	return *this ;
      }

      constexpr __max_diff_type&
      operator%=(const __max_diff_type& __r) noexcept
      {
	__glibcxx_assert (__r != 0);
	if (*this >= 0 && __r > 0)
	  _M_rep %= __r._M_rep;
	else
	  *this -= (*this / __r) * __r;
	return *this;
      }

      constexpr __max_diff_type&
      operator<<=(const __max_diff_type& __r) noexcept
      {
	_M_rep.operator<<=(__r._M_rep);
	return *this;
      }

      constexpr __max_diff_type&
      operator>>=(const __max_diff_type& __r) noexcept
      {
	// Arithmetic right shift.
	const auto __msb = _M_rep._M_msb;
	_M_rep >>= __r._M_rep;
	_M_rep._M_msb |= __msb;
	return *this;
      }

      constexpr __max_diff_type&
      operator&=(const __max_diff_type& __r) noexcept
      {
	_M_rep &= __r._M_rep;
	return *this;
      }

      constexpr __max_diff_type&
      operator|=(const __max_diff_type& __r) noexcept
      {
	_M_rep |= __r._M_rep;
	return *this;
      }

      constexpr __max_diff_type&
      operator^=(const __max_diff_type& __r) noexcept
      {
	_M_rep ^= __r._M_rep;
	return *this;
      }

      template<typename _Tp> requires integral<_Tp> || __is_int128<_Tp>
	friend constexpr _Tp&
	operator+=(_Tp& __a, const __max_diff_type& __b) noexcept
	{ return (__a = static_cast<_Tp>(__a + __b)); }

      template<typename _Tp> requires integral<_Tp> || __is_int128<_Tp>
	friend constexpr _Tp&
	operator-=(_Tp& __a, const __max_diff_type& __b) noexcept
	{ return (__a = static_cast<_Tp>(__a - __b)); }

      template<typename _Tp> requires integral<_Tp> || __is_int128<_Tp>
	friend constexpr _Tp&
	operator*=(_Tp& __a, const __max_diff_type& __b) noexcept
	{ return (__a = static_cast<_Tp>(__a * __b)); }

      template<typename _Tp> requires integral<_Tp> || __is_int128<_Tp>
	friend constexpr _Tp&
	operator/=(_Tp& __a, const __max_diff_type& __b) noexcept
	{ return (__a = static_cast<_Tp>(__a / __b)); }

      template<typename _Tp> requires integral<_Tp> || __is_int128<_Tp>
	friend constexpr _Tp&
	operator%=(_Tp& __a, const __max_diff_type& __b) noexcept
	{ return (__a = static_cast<_Tp>(__a % __b)); }

      template<typename _Tp> requires integral<_Tp> || __is_int128<_Tp>
	friend constexpr _Tp&
	operator&=(_Tp& __a, const __max_diff_type& __b) noexcept
	{ return (__a = static_cast<_Tp>(__a & __b)); }

      template<typename _Tp> requires integral<_Tp> || __is_int128<_Tp>
	friend constexpr _Tp&
	operator|=(_Tp& __a, const __max_diff_type& __b) noexcept
	{ return (__a = static_cast<_Tp>(__a | __b)); }

      template<typename _Tp> requires integral<_Tp> || __is_int128<_Tp>
	friend constexpr _Tp&
	operator^=(_Tp& __a, const __max_diff_type& __b) noexcept
	{ return (__a = static_cast<_Tp>(__a ^ __b)); }

      template<typename _Tp> requires integral<_Tp> || __is_int128<_Tp>
	friend constexpr _Tp&
	operator<<=(_Tp& __a, const __max_diff_type& __b) noexcept
	{ return (__a = static_cast<_Tp>(__a << __b)); }

      template<typename _Tp> requires integral<_Tp> || __is_int128<_Tp>
	friend constexpr _Tp&
	operator>>=(_Tp& __a, const __max_diff_type& __b) noexcept
	{ return (__a = static_cast<_Tp>(__a >> __b)); }

      friend constexpr __max_diff_type
      operator+(__max_diff_type __l, const __max_diff_type& __r) noexcept
      {
	__l += __r;
	return __l;
      }

      friend constexpr __max_diff_type
      operator-(__max_diff_type __l, const __max_diff_type& __r) noexcept
      {
	__l -= __r;
	return __l;
      }

      friend constexpr __max_diff_type
      operator*(__max_diff_type __l, const __max_diff_type& __r) noexcept
      {
	__l *= __r;
	return __l;
      }

      friend constexpr __max_diff_type
      operator/(__max_diff_type __l, const __max_diff_type& __r) noexcept
      {
	__l /= __r;
	return __l;
      }

      friend constexpr __max_diff_type
      operator%(__max_diff_type __l, const __max_diff_type& __r) noexcept
      {
	__l %= __r;
	return __l;
      }

      friend constexpr __max_diff_type
      operator<<(__max_diff_type __l, const __max_diff_type& __r) noexcept
      {
	__l <<= __r;
	return __l;
      }

      friend constexpr __max_diff_type
      operator>>(__max_diff_type __l, const __max_diff_type& __r) noexcept
      {
	__l >>= __r;
	return __l;
      }

      friend constexpr __max_diff_type
      operator&(__max_diff_type __l, const __max_diff_type& __r) noexcept
      {
	__l &= __r;
	return __l;
      }

      friend constexpr __max_diff_type
      operator|(__max_diff_type __l, const __max_diff_type& __r) noexcept
      {
	__l |= __r;
	return __l;
      }

      friend constexpr __max_diff_type
      operator^(__max_diff_type __l, const __max_diff_type& __r) noexcept
      {
	__l ^= __r;
	return __l;
      }

      friend constexpr bool
      operator==(const __max_diff_type& __l, const __max_diff_type& __r) noexcept
      { return __l._M_rep == __r._M_rep; }

#if __cpp_lib_three_way_comparison
      constexpr strong_ordering
      operator<=>(const __max_diff_type& __r) const noexcept
      {
	const auto __lsign = _M_rep._M_msb;
	const auto __rsign = __r._M_rep._M_msb;
	if (__lsign ^ __rsign)
	  return __lsign ? strong_ordering::less : strong_ordering::greater;
	else
	  return _M_rep <=> __r._M_rep;
      }
#else
      friend constexpr bool
      operator!=(const __max_diff_type& __l, const __max_diff_type& __r) noexcept
      { return !(__l == __r); }

      constexpr bool
      operator<(const __max_diff_type& __r) const noexcept
      {
	const auto __lsign = _M_rep._M_msb;
	const auto __rsign = __r._M_rep._M_msb;
	if (__lsign ^ __rsign)
	  return __lsign;
	else
	  return _M_rep < __r._M_rep;
      }

      friend constexpr bool
      operator>(const __max_diff_type& __l, const __max_diff_type& __r) noexcept
      { return __r < __l; }

      friend constexpr bool
      operator<=(const __max_diff_type& __l, const __max_diff_type& __r) noexcept
      { return !(__r < __l); }

      friend constexpr bool
      operator>=(const __max_diff_type& __l, const __max_diff_type& __r) noexcept
      { return !(__l < __r); }
#endif

    private:
      __max_size_type _M_rep = 0;

      friend class __max_size_type;
    };

    constexpr
    __max_size_type::__max_size_type(const __max_diff_type& __d) noexcept
      : __max_size_type(__d._M_rep)
    { }

  } // namespace __detail
} // namespace ranges

  template<>
    struct numeric_limits<ranges::__detail::__max_size_type>
    {
      using _Sp = ranges::__detail::__max_size_type;
      static constexpr bool is_specialized = true;
      static constexpr bool is_signed = false;
      static constexpr bool is_integer = true;
      static constexpr bool is_exact = true;
      static constexpr int digits
	= __gnu_cxx::__int_traits<_Sp::__rep>::__digits + 1;
      static constexpr int digits10
	= static_cast<int>(digits * numbers::ln2 / numbers::ln10);

      static constexpr _Sp
      min() noexcept
      { return 0; }

      static constexpr _Sp
      max() noexcept
      { return _Sp(static_cast<_Sp::__rep>(-1), 1); }

      static constexpr _Sp
      lowest() noexcept
      { return min(); }
    };

  template<>
    struct numeric_limits<ranges::__detail::__max_diff_type>
    {
      using _Dp = ranges::__detail::__max_diff_type;
      using _Sp = ranges::__detail::__max_size_type;
      static constexpr bool is_specialized = true;
      static constexpr bool is_signed = true;
      static constexpr bool is_integer = true;
      static constexpr bool is_exact = true;
      static constexpr int digits = numeric_limits<_Sp>::digits - 1;
      static constexpr int digits10
	= static_cast<int>(digits * numbers::ln2 / numbers::ln10);

      static constexpr _Dp
      min() noexcept
      { return _Dp(_Sp(0, 1)); }

      static constexpr _Dp
      max() noexcept
      { return _Dp(_Sp(static_cast<_Sp::__rep>(-1), 0)); }

      static constexpr _Dp
      lowest() noexcept
      { return min(); }
    };

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace

#endif // C++20 && library concepts
#endif // _GLIBCXX_MAX_SIZE_TYPE_H
