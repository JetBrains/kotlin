// Simd fixed_size ABI specific implementations -*- C++ -*-

// Copyright (C) 2020-2022 Free Software Foundation, Inc.
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

/*
 * The fixed_size ABI gives the following guarantees:
 *  - simd objects are passed via the stack
 *  - memory layout of `simd<_Tp, _Np>` is equivalent to `array<_Tp, _Np>`
 *  - alignment of `simd<_Tp, _Np>` is `_Np * sizeof(_Tp)` if _Np is __a
 *    power-of-2 value, otherwise `std::__bit_ceil(_Np * sizeof(_Tp))` (Note:
 *    if the alignment were to exceed the system/compiler maximum, it is bounded
 *    to that maximum)
 *  - simd_mask objects are passed like bitset<_Np>
 *  - memory layout of `simd_mask<_Tp, _Np>` is equivalent to `bitset<_Np>`
 *  - alignment of `simd_mask<_Tp, _Np>` is equal to the alignment of
 *    `bitset<_Np>`
 */

#ifndef _GLIBCXX_EXPERIMENTAL_SIMD_FIXED_SIZE_H_
#define _GLIBCXX_EXPERIMENTAL_SIMD_FIXED_SIZE_H_

#if __cplusplus >= 201703L

#include <array>

_GLIBCXX_SIMD_BEGIN_NAMESPACE

// __simd_tuple_element {{{
template <size_t _I, typename _Tp>
  struct __simd_tuple_element;

template <typename _Tp, typename _A0, typename... _As>
  struct __simd_tuple_element<0, _SimdTuple<_Tp, _A0, _As...>>
  { using type = simd<_Tp, _A0>; };

template <size_t _I, typename _Tp, typename _A0, typename... _As>
  struct __simd_tuple_element<_I, _SimdTuple<_Tp, _A0, _As...>>
  {
    using type =
      typename __simd_tuple_element<_I - 1, _SimdTuple<_Tp, _As...>>::type;
  };

template <size_t _I, typename _Tp>
  using __simd_tuple_element_t = typename __simd_tuple_element<_I, _Tp>::type;

// }}}
// __simd_tuple_concat {{{

template <typename _Tp, typename... _A0s, typename... _A1s>
  _GLIBCXX_SIMD_INTRINSIC constexpr _SimdTuple<_Tp, _A0s..., _A1s...>
  __simd_tuple_concat(const _SimdTuple<_Tp, _A0s...>& __left,
		      const _SimdTuple<_Tp, _A1s...>& __right)
  {
    if constexpr (sizeof...(_A0s) == 0)
      return __right;
    else if constexpr (sizeof...(_A1s) == 0)
      return __left;
    else
      return {__left.first, __simd_tuple_concat(__left.second, __right)};
  }

template <typename _Tp, typename _A10, typename... _A1s>
  _GLIBCXX_SIMD_INTRINSIC constexpr _SimdTuple<_Tp, simd_abi::scalar, _A10,
					       _A1s...>
  __simd_tuple_concat(const _Tp& __left,
		      const _SimdTuple<_Tp, _A10, _A1s...>& __right)
  { return {__left, __right}; }

// }}}
// __simd_tuple_pop_front {{{
// Returns the next _SimdTuple in __x that has _Np elements less.
// Precondition: _Np must match the number of elements in __first (recursively)
template <size_t _Np, typename _Tp>
  _GLIBCXX_SIMD_INTRINSIC constexpr decltype(auto)
  __simd_tuple_pop_front(_Tp&& __x)
  {
    if constexpr (_Np == 0)
      return static_cast<_Tp&&>(__x);
    else
      {
	using _Up = __remove_cvref_t<_Tp>;
	static_assert(_Np >= _Up::_S_first_size);
	return __simd_tuple_pop_front<_Np - _Up::_S_first_size>(__x.second);
      }
  }

// }}}
// __get_simd_at<_Np> {{{1
struct __as_simd {};

struct __as_simd_tuple {};

template <typename _Tp, typename _A0, typename... _Abis>
  _GLIBCXX_SIMD_INTRINSIC constexpr simd<_Tp, _A0>
  __simd_tuple_get_impl(__as_simd, const _SimdTuple<_Tp, _A0, _Abis...>& __t,
			_SizeConstant<0>)
  { return {__private_init, __t.first}; }

template <typename _Tp, typename _A0, typename... _Abis>
  _GLIBCXX_SIMD_INTRINSIC constexpr const auto&
  __simd_tuple_get_impl(__as_simd_tuple,
			const _SimdTuple<_Tp, _A0, _Abis...>& __t,
			_SizeConstant<0>)
  { return __t.first; }

template <typename _Tp, typename _A0, typename... _Abis>
  _GLIBCXX_SIMD_INTRINSIC constexpr auto&
  __simd_tuple_get_impl(__as_simd_tuple, _SimdTuple<_Tp, _A0, _Abis...>& __t,
			_SizeConstant<0>)
  { return __t.first; }

template <typename _R, size_t _Np, typename _Tp, typename... _Abis>
  _GLIBCXX_SIMD_INTRINSIC constexpr auto
  __simd_tuple_get_impl(_R, const _SimdTuple<_Tp, _Abis...>& __t,
			_SizeConstant<_Np>)
  { return __simd_tuple_get_impl(_R(), __t.second, _SizeConstant<_Np - 1>()); }

template <size_t _Np, typename _Tp, typename... _Abis>
  _GLIBCXX_SIMD_INTRINSIC constexpr auto&
  __simd_tuple_get_impl(__as_simd_tuple, _SimdTuple<_Tp, _Abis...>& __t,
			_SizeConstant<_Np>)
  {
    return __simd_tuple_get_impl(__as_simd_tuple(), __t.second,
				 _SizeConstant<_Np - 1>());
  }

template <size_t _Np, typename _Tp, typename... _Abis>
  _GLIBCXX_SIMD_INTRINSIC constexpr auto
  __get_simd_at(const _SimdTuple<_Tp, _Abis...>& __t)
  { return __simd_tuple_get_impl(__as_simd(), __t, _SizeConstant<_Np>()); }

// }}}
// __get_tuple_at<_Np> {{{
template <size_t _Np, typename _Tp, typename... _Abis>
  _GLIBCXX_SIMD_INTRINSIC constexpr auto
  __get_tuple_at(const _SimdTuple<_Tp, _Abis...>& __t)
  {
    return __simd_tuple_get_impl(__as_simd_tuple(), __t, _SizeConstant<_Np>());
  }

template <size_t _Np, typename _Tp, typename... _Abis>
  _GLIBCXX_SIMD_INTRINSIC constexpr auto&
  __get_tuple_at(_SimdTuple<_Tp, _Abis...>& __t)
  {
    return __simd_tuple_get_impl(__as_simd_tuple(), __t, _SizeConstant<_Np>());
  }

// __tuple_element_meta {{{1
template <typename _Tp, typename _Abi, size_t _Offset>
  struct __tuple_element_meta : public _Abi::_SimdImpl
  {
    static_assert(is_same_v<typename _Abi::_SimdImpl::abi_type,
			    _Abi>); // this fails e.g. when _SimdImpl is an
				    // alias for _SimdImplBuiltin<_DifferentAbi>
    using value_type = _Tp;
    using abi_type = _Abi;
    using _Traits = _SimdTraits<_Tp, _Abi>;
    using _MaskImpl = typename _Abi::_MaskImpl;
    using _MaskMember = typename _Traits::_MaskMember;
    using simd_type = simd<_Tp, _Abi>;
    static constexpr size_t _S_offset = _Offset;
    static constexpr size_t _S_size() { return simd_size<_Tp, _Abi>::value; }
    static constexpr _MaskImpl _S_mask_impl = {};

    template <size_t _Np, bool _Sanitized>
      _GLIBCXX_SIMD_INTRINSIC static auto
      _S_submask(_BitMask<_Np, _Sanitized> __bits)
      { return __bits.template _M_extract<_Offset, _S_size()>(); }

    template <size_t _Np, bool _Sanitized>
      _GLIBCXX_SIMD_INTRINSIC static _MaskMember
      _S_make_mask(_BitMask<_Np, _Sanitized> __bits)
      {
	return _MaskImpl::template _S_convert<_Tp>(
	  __bits.template _M_extract<_Offset, _S_size()>()._M_sanitized());
      }

    _GLIBCXX_SIMD_INTRINSIC static _ULLong
    _S_mask_to_shifted_ullong(_MaskMember __k)
    { return _MaskImpl::_S_to_bits(__k).to_ullong() << _Offset; }
  };

template <size_t _Offset, typename _Tp, typename _Abi, typename... _As>
  _GLIBCXX_SIMD_INTRINSIC
  __tuple_element_meta<_Tp, _Abi, _Offset>
  __make_meta(const _SimdTuple<_Tp, _Abi, _As...>&)
  { return {}; }

// }}}1
// _WithOffset wrapper class {{{
template <size_t _Offset, typename _Base>
  struct _WithOffset : public _Base
  {
    static inline constexpr size_t _S_offset = _Offset;

    _GLIBCXX_SIMD_INTRINSIC char* _M_as_charptr()
    {
      return reinterpret_cast<char*>(this)
	     + _S_offset * sizeof(typename _Base::value_type);
    }

    _GLIBCXX_SIMD_INTRINSIC const char* _M_as_charptr() const
    {
      return reinterpret_cast<const char*>(this)
	     + _S_offset * sizeof(typename _Base::value_type);
    }
  };

// make _WithOffset<_WithOffset> ill-formed to use:
template <size_t _O0, size_t _O1, typename _Base>
  struct _WithOffset<_O0, _WithOffset<_O1, _Base>> {};

template <size_t _Offset, typename _Tp>
  _GLIBCXX_SIMD_INTRINSIC
  decltype(auto)
  __add_offset(_Tp& __base)
  { return static_cast<_WithOffset<_Offset, __remove_cvref_t<_Tp>>&>(__base); }

template <size_t _Offset, typename _Tp>
  _GLIBCXX_SIMD_INTRINSIC
  decltype(auto)
  __add_offset(const _Tp& __base)
  {
    return static_cast<const _WithOffset<_Offset, __remove_cvref_t<_Tp>>&>(
      __base);
  }

template <size_t _Offset, size_t _ExistingOffset, typename _Tp>
  _GLIBCXX_SIMD_INTRINSIC
  decltype(auto)
  __add_offset(_WithOffset<_ExistingOffset, _Tp>& __base)
  {
    return static_cast<_WithOffset<_Offset + _ExistingOffset, _Tp>&>(
      static_cast<_Tp&>(__base));
  }

template <size_t _Offset, size_t _ExistingOffset, typename _Tp>
  _GLIBCXX_SIMD_INTRINSIC
  decltype(auto)
  __add_offset(const _WithOffset<_ExistingOffset, _Tp>& __base)
  {
    return static_cast<const _WithOffset<_Offset + _ExistingOffset, _Tp>&>(
      static_cast<const _Tp&>(__base));
  }

template <typename _Tp>
  constexpr inline size_t __offset = 0;

template <size_t _Offset, typename _Tp>
  constexpr inline size_t __offset<_WithOffset<_Offset, _Tp>>
    = _WithOffset<_Offset, _Tp>::_S_offset;

template <typename _Tp>
  constexpr inline size_t __offset<const _Tp> = __offset<_Tp>;

template <typename _Tp>
  constexpr inline size_t __offset<_Tp&> = __offset<_Tp>;

template <typename _Tp>
  constexpr inline size_t __offset<_Tp&&> = __offset<_Tp>;

// }}}
// _SimdTuple specializations {{{1
// empty {{{2
template <typename _Tp>
  struct _SimdTuple<_Tp>
  {
    using value_type = _Tp;
    static constexpr size_t _S_tuple_size = 0;
    static constexpr size_t _S_size() { return 0; }
  };

// _SimdTupleData {{{2
template <typename _FirstType, typename _SecondType>
  struct _SimdTupleData
  {
    _FirstType first;
    _SecondType second;

    _GLIBCXX_SIMD_INTRINSIC
    constexpr bool _M_is_constprop() const
    {
      if constexpr (is_class_v<_FirstType>)
	return first._M_is_constprop() && second._M_is_constprop();
      else
	return __builtin_constant_p(first) && second._M_is_constprop();
    }
  };

template <typename _FirstType, typename _Tp>
  struct _SimdTupleData<_FirstType, _SimdTuple<_Tp>>
  {
    _FirstType first;
    static constexpr _SimdTuple<_Tp> second = {};

    _GLIBCXX_SIMD_INTRINSIC
    constexpr bool _M_is_constprop() const
    {
      if constexpr (is_class_v<_FirstType>)
	return first._M_is_constprop();
      else
	return __builtin_constant_p(first);
    }
  };

// 1 or more {{{2
template <typename _Tp, typename _Abi0, typename... _Abis>
  struct _SimdTuple<_Tp, _Abi0, _Abis...>
    : _SimdTupleData<typename _SimdTraits<_Tp, _Abi0>::_SimdMember,
		     _SimdTuple<_Tp, _Abis...>>
  {
    static_assert(!__is_fixed_size_abi_v<_Abi0>);
    using value_type = _Tp;
    using _FirstType = typename _SimdTraits<_Tp, _Abi0>::_SimdMember;
    using _FirstAbi = _Abi0;
    using _SecondType = _SimdTuple<_Tp, _Abis...>;
    static constexpr size_t _S_tuple_size = sizeof...(_Abis) + 1;

    static constexpr size_t _S_size()
    { return simd_size_v<_Tp, _Abi0> + _SecondType::_S_size(); }

    static constexpr size_t _S_first_size = simd_size_v<_Tp, _Abi0>;
    static constexpr bool _S_is_homogeneous = (is_same_v<_Abi0, _Abis> && ...);

    using _Base = _SimdTupleData<typename _SimdTraits<_Tp, _Abi0>::_SimdMember,
				 _SimdTuple<_Tp, _Abis...>>;
    using _Base::first;
    using _Base::second;

    _GLIBCXX_SIMD_INTRINSIC constexpr _SimdTuple() = default;
    _GLIBCXX_SIMD_INTRINSIC constexpr _SimdTuple(const _SimdTuple&) = default;
    _GLIBCXX_SIMD_INTRINSIC constexpr _SimdTuple& operator=(const _SimdTuple&)
      = default;

    template <typename _Up>
      _GLIBCXX_SIMD_INTRINSIC constexpr _SimdTuple(_Up&& __x)
      : _Base{static_cast<_Up&&>(__x)} {}

    template <typename _Up, typename _Up2>
      _GLIBCXX_SIMD_INTRINSIC constexpr _SimdTuple(_Up&& __x, _Up2&& __y)
      : _Base{static_cast<_Up&&>(__x), static_cast<_Up2&&>(__y)} {}

    template <typename _Up>
      _GLIBCXX_SIMD_INTRINSIC constexpr _SimdTuple(_Up&& __x, _SimdTuple<_Tp>)
      : _Base{static_cast<_Up&&>(__x)} {}

    _GLIBCXX_SIMD_INTRINSIC char* _M_as_charptr()
    { return reinterpret_cast<char*>(this); }

    _GLIBCXX_SIMD_INTRINSIC const char* _M_as_charptr() const
    { return reinterpret_cast<const char*>(this); }

    template <size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC constexpr auto& _M_at()
      {
	if constexpr (_Np == 0)
	  return first;
	else
	  return second.template _M_at<_Np - 1>();
      }

    template <size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC constexpr const auto& _M_at() const
      {
	if constexpr (_Np == 0)
	  return first;
	else
	  return second.template _M_at<_Np - 1>();
      }

    template <size_t _Np>
      _GLIBCXX_SIMD_INTRINSIC constexpr auto _M_simd_at() const
      {
	if constexpr (_Np == 0)
	  return simd<_Tp, _Abi0>(__private_init, first);
	else
	  return second.template _M_simd_at<_Np - 1>();
      }

    template <size_t _Offset = 0, typename _Fp>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SimdTuple
      _S_generate(_Fp&& __gen, _SizeConstant<_Offset> = {})
      {
	auto&& __first = __gen(__tuple_element_meta<_Tp, _Abi0, _Offset>());
	if constexpr (_S_tuple_size == 1)
	  return {__first};
	else
	  return {__first,
		  _SecondType::_S_generate(
		    static_cast<_Fp&&>(__gen),
		    _SizeConstant<_Offset + simd_size_v<_Tp, _Abi0>>())};
      }

    template <size_t _Offset = 0, typename _Fp, typename... _More>
      _GLIBCXX_SIMD_INTRINSIC _SimdTuple
      _M_apply_wrapped(_Fp&& __fun, const _More&... __more) const
      {
	auto&& __first
	  = __fun(__make_meta<_Offset>(*this), first, __more.first...);
	if constexpr (_S_tuple_size == 1)
	  return {__first};
	else
	  return {
	    __first,
	    second.template _M_apply_wrapped<_Offset + simd_size_v<_Tp, _Abi0>>(
	      static_cast<_Fp&&>(__fun), __more.second...)};
      }

    template <typename _Tup>
      _GLIBCXX_SIMD_INTRINSIC constexpr decltype(auto)
      _M_extract_argument(_Tup&& __tup) const
      {
	using _TupT = typename __remove_cvref_t<_Tup>::value_type;
	if constexpr (is_same_v<_SimdTuple, __remove_cvref_t<_Tup>>)
	  return __tup.first;
	else if (__builtin_is_constant_evaluated())
	  return __fixed_size_storage_t<_TupT, _S_first_size>::_S_generate([&](
	    auto __meta) constexpr {
	    return __meta._S_generator(
	      [&](auto __i) constexpr { return __tup[__i]; },
	      static_cast<_TupT*>(nullptr));
	  });
	else
	  return [&]() {
	    __fixed_size_storage_t<_TupT, _S_first_size> __r;
	    __builtin_memcpy(__r._M_as_charptr(), __tup._M_as_charptr(),
			     sizeof(__r));
	    return __r;
	  }();
      }

    template <typename _Tup>
      _GLIBCXX_SIMD_INTRINSIC constexpr auto&
      _M_skip_argument(_Tup&& __tup) const
      {
	static_assert(_S_tuple_size > 1);
	using _Up = __remove_cvref_t<_Tup>;
	constexpr size_t __off = __offset<_Up>;
	if constexpr (_S_first_size == _Up::_S_first_size && __off == 0)
	  return __tup.second;
	else if constexpr (_S_first_size > _Up::_S_first_size
			   && _S_first_size % _Up::_S_first_size == 0
			   && __off == 0)
	  return __simd_tuple_pop_front<_S_first_size>(__tup);
	else if constexpr (_S_first_size + __off < _Up::_S_first_size)
	  return __add_offset<_S_first_size>(__tup);
	else if constexpr (_S_first_size + __off == _Up::_S_first_size)
	  return __tup.second;
	else
	  __assert_unreachable<_Tup>();
      }

    template <size_t _Offset, typename... _More>
      _GLIBCXX_SIMD_INTRINSIC constexpr void
      _M_assign_front(const _SimdTuple<_Tp, _Abi0, _More...>& __x) &
      {
	static_assert(_Offset == 0);
	first = __x.first;
	if constexpr (sizeof...(_More) > 0)
	  {
	    static_assert(sizeof...(_Abis) >= sizeof...(_More));
	    second.template _M_assign_front<0>(__x.second);
	  }
      }

    template <size_t _Offset>
      _GLIBCXX_SIMD_INTRINSIC constexpr void
      _M_assign_front(const _FirstType& __x) &
      {
	static_assert(_Offset == 0);
	first = __x;
      }

    template <size_t _Offset, typename... _As>
      _GLIBCXX_SIMD_INTRINSIC constexpr void
      _M_assign_front(const _SimdTuple<_Tp, _As...>& __x) &
      {
	__builtin_memcpy(_M_as_charptr() + _Offset * sizeof(value_type),
			 __x._M_as_charptr(),
			 sizeof(_Tp) * _SimdTuple<_Tp, _As...>::_S_size());
      }

    /*
     * Iterate over the first objects in this _SimdTuple and call __fun for each
     * of them. If additional arguments are passed via __more, chunk them into
     * _SimdTuple or __vector_type_t objects of the same number of values.
     */
    template <typename _Fp, typename... _More>
      _GLIBCXX_SIMD_INTRINSIC constexpr _SimdTuple
      _M_apply_per_chunk(_Fp&& __fun, _More&&... __more) const
      {
	if constexpr ((...
		       || conjunction_v<
			 is_lvalue_reference<_More>,
			 negation<is_const<remove_reference_t<_More>>>>) )
	  {
	    // need to write back at least one of __more after calling __fun
	    auto&& __first = [&](auto... __args) constexpr
	    {
	      auto __r = __fun(__tuple_element_meta<_Tp, _Abi0, 0>(), first,
			       __args...);
	      [[maybe_unused]] auto&& __ignore_me = {(
		[](auto&& __dst, const auto& __src) {
		  if constexpr (is_assignable_v<decltype(__dst),
						decltype(__dst)>)
		    {
		      __dst.template _M_assign_front<__offset<decltype(__dst)>>(
			__src);
		    }
		}(static_cast<_More&&>(__more), __args),
		0)...};
	      return __r;
	    }
	    (_M_extract_argument(__more)...);
	    if constexpr (_S_tuple_size == 1)
	      return {__first};
	    else
	      return {__first,
		      second._M_apply_per_chunk(static_cast<_Fp&&>(__fun),
						_M_skip_argument(__more)...)};
	  }
	else
	  {
	    auto&& __first = __fun(__tuple_element_meta<_Tp, _Abi0, 0>(), first,
				   _M_extract_argument(__more)...);
	    if constexpr (_S_tuple_size == 1)
	      return {__first};
	    else
	      return {__first,
		      second._M_apply_per_chunk(static_cast<_Fp&&>(__fun),
						_M_skip_argument(__more)...)};
	  }
      }

    template <typename _R = _Tp, typename _Fp, typename... _More>
      _GLIBCXX_SIMD_INTRINSIC auto _M_apply_r(_Fp&& __fun,
					      const _More&... __more) const
      {
	auto&& __first = __fun(__tuple_element_meta<_Tp, _Abi0, 0>(), first,
			       __more.first...);
	if constexpr (_S_tuple_size == 1)
	  return __first;
	else
	  return __simd_tuple_concat<_R>(
	    __first, second.template _M_apply_r<_R>(static_cast<_Fp&&>(__fun),
						    __more.second...));
      }

    template <typename _Fp, typename... _More>
      _GLIBCXX_SIMD_INTRINSIC constexpr friend _SanitizedBitMask<_S_size()>
      _M_test(const _Fp& __fun, const _SimdTuple& __x, const _More&... __more)
      {
	const _SanitizedBitMask<_S_first_size> __first
	  = _Abi0::_MaskImpl::_S_to_bits(
	    __fun(__tuple_element_meta<_Tp, _Abi0, 0>(), __x.first,
		  __more.first...));
	if constexpr (_S_tuple_size == 1)
	  return __first;
	else
	  return _M_test(__fun, __x.second, __more.second...)
	    ._M_prepend(__first);
      }

    template <typename _Up, _Up _I>
      _GLIBCXX_SIMD_INTRINSIC constexpr _Tp
      operator[](integral_constant<_Up, _I>) const noexcept
      {
	if constexpr (_I < simd_size_v<_Tp, _Abi0>)
	  return _M_subscript_read(_I);
	else
	  return second[integral_constant<_Up, _I - simd_size_v<_Tp, _Abi0>>()];
      }

    _GLIBCXX_SIMD_INTRINSIC
    _Tp operator[](size_t __i) const noexcept
    {
      if constexpr (_S_tuple_size == 1)
	return _M_subscript_read(__i);
      else
	{
#ifdef _GLIBCXX_SIMD_USE_ALIASING_LOADS
	  return reinterpret_cast<const __may_alias<_Tp>*>(this)[__i];
#else
	  if constexpr (__is_scalar_abi<_Abi0>())
	    {
	      const _Tp* ptr = &first;
	      return ptr[__i];
	    }
	  else
	    return __i < simd_size_v<_Tp, _Abi0>
		     ? _M_subscript_read(__i)
		     : second[__i - simd_size_v<_Tp, _Abi0>];
#endif
	}
    }

    _GLIBCXX_SIMD_INTRINSIC
    void _M_set(size_t __i, _Tp __val) noexcept
    {
      if constexpr (_S_tuple_size == 1)
	return _M_subscript_write(__i, __val);
      else
	{
#ifdef _GLIBCXX_SIMD_USE_ALIASING_LOADS
	  reinterpret_cast<__may_alias<_Tp>*>(this)[__i] = __val;
#else
	  if (__i < simd_size_v<_Tp, _Abi0>)
	    _M_subscript_write(__i, __val);
	  else
	    second._M_set(__i - simd_size_v<_Tp, _Abi0>, __val);
#endif
	}
    }

  private:
    // _M_subscript_read/_write {{{
    _GLIBCXX_SIMD_INTRINSIC
    _Tp _M_subscript_read([[maybe_unused]] size_t __i) const noexcept
    {
      if constexpr (__is_vectorizable_v<_FirstType>)
	return first;
      else
	return first[__i];
    }

    _GLIBCXX_SIMD_INTRINSIC
    void _M_subscript_write([[maybe_unused]] size_t __i, _Tp __y) noexcept
    {
      if constexpr (__is_vectorizable_v<_FirstType>)
	first = __y;
      else
	first._M_set(__i, __y);
    }

    // }}}
  };

// __make_simd_tuple {{{1
template <typename _Tp, typename _A0>
  _GLIBCXX_SIMD_INTRINSIC _SimdTuple<_Tp, _A0>
  __make_simd_tuple(simd<_Tp, _A0> __x0)
  { return {__data(__x0)}; }

template <typename _Tp, typename _A0, typename... _As>
  _GLIBCXX_SIMD_INTRINSIC _SimdTuple<_Tp, _A0, _As...>
  __make_simd_tuple(const simd<_Tp, _A0>& __x0, const simd<_Tp, _As>&... __xs)
  { return {__data(__x0), __make_simd_tuple(__xs...)}; }

template <typename _Tp, typename _A0>
  _GLIBCXX_SIMD_INTRINSIC _SimdTuple<_Tp, _A0>
  __make_simd_tuple(const typename _SimdTraits<_Tp, _A0>::_SimdMember& __arg0)
  { return {__arg0}; }

template <typename _Tp, typename _A0, typename _A1, typename... _Abis>
  _GLIBCXX_SIMD_INTRINSIC _SimdTuple<_Tp, _A0, _A1, _Abis...>
  __make_simd_tuple(
    const typename _SimdTraits<_Tp, _A0>::_SimdMember& __arg0,
    const typename _SimdTraits<_Tp, _A1>::_SimdMember& __arg1,
    const typename _SimdTraits<_Tp, _Abis>::_SimdMember&... __args)
  { return {__arg0, __make_simd_tuple<_Tp, _A1, _Abis...>(__arg1, __args...)}; }

// __to_simd_tuple {{{1
template <typename _Tp, size_t _Np, typename _V, size_t _NV, typename... _VX>
  _GLIBCXX_SIMD_INTRINSIC constexpr __fixed_size_storage_t<_Tp, _Np>
  __to_simd_tuple(const array<_V, _NV>& __from, const _VX... __fromX);

template <typename _Tp, size_t _Np,
	  size_t _Offset = 0, // skip this many elements in __from0
	  typename _R = __fixed_size_storage_t<_Tp, _Np>, typename _V0,
	  typename _V0VT = _VectorTraits<_V0>, typename... _VX>
  _GLIBCXX_SIMD_INTRINSIC _R constexpr __to_simd_tuple(const _V0 __from0,
						       const _VX... __fromX)
  {
    static_assert(is_same_v<typename _V0VT::value_type, _Tp>);
    static_assert(_Offset < _V0VT::_S_full_size);
    using _R0 = __vector_type_t<_Tp, _R::_S_first_size>;
    if constexpr (_R::_S_tuple_size == 1)
      {
	if constexpr (_Np == 1)
	  return _R{__from0[_Offset]};
	else if constexpr (_Offset == 0 && _V0VT::_S_full_size >= _Np)
	  return _R{__intrin_bitcast<_R0>(__from0)};
	else if constexpr (_Offset * 2 == _V0VT::_S_full_size
			   && _V0VT::_S_full_size / 2 >= _Np)
	  return _R{__intrin_bitcast<_R0>(__extract_part<1, 2>(__from0))};
	else if constexpr (_Offset * 4 == _V0VT::_S_full_size
			   && _V0VT::_S_full_size / 4 >= _Np)
	  return _R{__intrin_bitcast<_R0>(__extract_part<1, 4>(__from0))};
	else
	  __assert_unreachable<_Tp>();
      }
    else
      {
	if constexpr (1 == _R::_S_first_size)
	  { // extract one scalar and recurse
	    if constexpr (_Offset + 1 < _V0VT::_S_full_size)
	      return _R{__from0[_Offset],
			__to_simd_tuple<_Tp, _Np - 1, _Offset + 1>(__from0,
								   __fromX...)};
	    else
	      return _R{__from0[_Offset],
			__to_simd_tuple<_Tp, _Np - 1, 0>(__fromX...)};
	  }

	// place __from0 into _R::first and recurse for __fromX -> _R::second
	else if constexpr (_V0VT::_S_full_size == _R::_S_first_size
			   && _Offset == 0)
	  return _R{__from0,
		    __to_simd_tuple<_Tp, _Np - _R::_S_first_size>(__fromX...)};

	// place lower part of __from0 into _R::first and recurse with _Offset
	else if constexpr (_V0VT::_S_full_size > _R::_S_first_size
			   && _Offset == 0)
	  return _R{__intrin_bitcast<_R0>(__from0),
		    __to_simd_tuple<_Tp, _Np - _R::_S_first_size,
				    _R::_S_first_size>(__from0, __fromX...)};

	// place lower part of second quarter of __from0 into _R::first and
	// recurse with _Offset
	else if constexpr (_Offset * 4 == _V0VT::_S_full_size
			   && _V0VT::_S_full_size >= 4 * _R::_S_first_size)
	  return _R{__intrin_bitcast<_R0>(__extract_part<2, 4>(__from0)),
		    __to_simd_tuple<_Tp, _Np - _R::_S_first_size,
				    _Offset + _R::_S_first_size>(__from0,
								 __fromX...)};

	// place lower half of high half of __from0 into _R::first and recurse
	// with _Offset
	else if constexpr (_Offset * 2 == _V0VT::_S_full_size
			   && _V0VT::_S_full_size >= 4 * _R::_S_first_size)
	  return _R{__intrin_bitcast<_R0>(__extract_part<2, 4>(__from0)),
		    __to_simd_tuple<_Tp, _Np - _R::_S_first_size,
				    _Offset + _R::_S_first_size>(__from0,
								 __fromX...)};

	// place high half of __from0 into _R::first and recurse with __fromX
	else if constexpr (_Offset * 2 == _V0VT::_S_full_size
			   && _V0VT::_S_full_size / 2 >= _R::_S_first_size)
	  return _R{__intrin_bitcast<_R0>(__extract_part<1, 2>(__from0)),
		    __to_simd_tuple<_Tp, _Np - _R::_S_first_size, 0>(
		      __fromX...)};

	// ill-formed if some unforseen pattern is needed
	else
	  __assert_unreachable<_Tp>();
      }
  }

template <typename _Tp, size_t _Np, typename _V, size_t _NV, typename... _VX>
  _GLIBCXX_SIMD_INTRINSIC constexpr __fixed_size_storage_t<_Tp, _Np>
  __to_simd_tuple(const array<_V, _NV>& __from, const _VX... __fromX)
  {
    if constexpr (is_same_v<_Tp, _V>)
      {
	static_assert(
	  sizeof...(_VX) == 0,
	  "An array of scalars must be the last argument to __to_simd_tuple");
	return __call_with_subscripts(
	  __from,
	  make_index_sequence<_NV>(), [&](const auto... __args) constexpr {
	    return __simd_tuple_concat(
	      _SimdTuple<_Tp, simd_abi::scalar>{__args}..., _SimdTuple<_Tp>());
	  });
      }
    else
      return __call_with_subscripts(
	__from,
	make_index_sequence<_NV>(), [&](const auto... __args) constexpr {
	  return __to_simd_tuple<_Tp, _Np>(__args..., __fromX...);
	});
  }

template <size_t, typename _Tp>
  using __to_tuple_helper = _Tp;

template <typename _Tp, typename _A0, size_t _NOut, size_t _Np,
	  size_t... _Indexes>
  _GLIBCXX_SIMD_INTRINSIC __fixed_size_storage_t<_Tp, _NOut>
  __to_simd_tuple_impl(index_sequence<_Indexes...>,
      const array<__vector_type_t<_Tp, simd_size_v<_Tp, _A0>>, _Np>& __args)
  {
    return __make_simd_tuple<_Tp, __to_tuple_helper<_Indexes, _A0>...>(
      __args[_Indexes]...);
  }

template <typename _Tp, typename _A0, size_t _NOut, size_t _Np,
	  typename _R = __fixed_size_storage_t<_Tp, _NOut>>
  _GLIBCXX_SIMD_INTRINSIC _R
  __to_simd_tuple_sized(
    const array<__vector_type_t<_Tp, simd_size_v<_Tp, _A0>>, _Np>& __args)
  {
    static_assert(_Np * simd_size_v<_Tp, _A0> >= _NOut);
    return __to_simd_tuple_impl<_Tp, _A0, _NOut>(
      make_index_sequence<_R::_S_tuple_size>(), __args);
  }

// __optimize_simd_tuple {{{1
template <typename _Tp>
  _GLIBCXX_SIMD_INTRINSIC _SimdTuple<_Tp>
  __optimize_simd_tuple(const _SimdTuple<_Tp>)
  { return {}; }

template <typename _Tp, typename _Ap>
  _GLIBCXX_SIMD_INTRINSIC const _SimdTuple<_Tp, _Ap>&
  __optimize_simd_tuple(const _SimdTuple<_Tp, _Ap>& __x)
  { return __x; }

template <typename _Tp, typename _A0, typename _A1, typename... _Abis,
	  typename _R = __fixed_size_storage_t<
	    _Tp, _SimdTuple<_Tp, _A0, _A1, _Abis...>::_S_size()>>
  _GLIBCXX_SIMD_INTRINSIC _R
  __optimize_simd_tuple(const _SimdTuple<_Tp, _A0, _A1, _Abis...>& __x)
  {
    using _Tup = _SimdTuple<_Tp, _A0, _A1, _Abis...>;
    if constexpr (is_same_v<_R, _Tup>)
      return __x;
    else if constexpr (is_same_v<typename _R::_FirstType,
				 typename _Tup::_FirstType>)
      return {__x.first, __optimize_simd_tuple(__x.second)};
    else if constexpr (__is_scalar_abi<_A0>()
		       || _A0::template _S_is_partial<_Tp>)
      return {__generate_from_n_evaluations<_R::_S_first_size,
					    typename _R::_FirstType>(
		[&](auto __i) { return __x[__i]; }),
	      __optimize_simd_tuple(
		__simd_tuple_pop_front<_R::_S_first_size>(__x))};
    else if constexpr (is_same_v<_A0, _A1>
	&& _R::_S_first_size == simd_size_v<_Tp, _A0> + simd_size_v<_Tp, _A1>)
      return {__concat(__x.template _M_at<0>(), __x.template _M_at<1>()),
	      __optimize_simd_tuple(__x.second.second)};
    else if constexpr (sizeof...(_Abis) >= 2
	&& _R::_S_first_size == (4 * simd_size_v<_Tp, _A0>)
	&& simd_size_v<_Tp, _A0> == __simd_tuple_element_t<
	    (sizeof...(_Abis) >= 2 ? 3 : 0), _Tup>::size())
      return {
	__concat(__concat(__x.template _M_at<0>(), __x.template _M_at<1>()),
		 __concat(__x.template _M_at<2>(), __x.template _M_at<3>())),
	__optimize_simd_tuple(__x.second.second.second.second)};
    else
      {
	static_assert(sizeof(_R) == sizeof(__x));
	_R __r;
	__builtin_memcpy(__r._M_as_charptr(), __x._M_as_charptr(),
			 sizeof(_Tp) * _R::_S_size());
	return __r;
      }
  }

// __for_each(const _SimdTuple &, Fun) {{{1
template <size_t _Offset = 0, typename _Tp, typename _A0, typename _Fp>
  _GLIBCXX_SIMD_INTRINSIC constexpr void
  __for_each(const _SimdTuple<_Tp, _A0>& __t, _Fp&& __fun)
  { static_cast<_Fp&&>(__fun)(__make_meta<_Offset>(__t), __t.first); }

template <size_t _Offset = 0, typename _Tp, typename _A0, typename _A1,
	  typename... _As, typename _Fp>
  _GLIBCXX_SIMD_INTRINSIC constexpr void
  __for_each(const _SimdTuple<_Tp, _A0, _A1, _As...>& __t, _Fp&& __fun)
  {
    __fun(__make_meta<_Offset>(__t), __t.first);
    __for_each<_Offset + simd_size<_Tp, _A0>::value>(__t.second,
						     static_cast<_Fp&&>(__fun));
  }

// __for_each(_SimdTuple &, Fun) {{{1
template <size_t _Offset = 0, typename _Tp, typename _A0, typename _Fp>
  _GLIBCXX_SIMD_INTRINSIC constexpr void
  __for_each(_SimdTuple<_Tp, _A0>& __t, _Fp&& __fun)
  { static_cast<_Fp&&>(__fun)(__make_meta<_Offset>(__t), __t.first); }

template <size_t _Offset = 0, typename _Tp, typename _A0, typename _A1,
	  typename... _As, typename _Fp>
  _GLIBCXX_SIMD_INTRINSIC constexpr void
  __for_each(_SimdTuple<_Tp, _A0, _A1, _As...>& __t, _Fp&& __fun)
  {
    __fun(__make_meta<_Offset>(__t), __t.first);
    __for_each<_Offset + simd_size<_Tp, _A0>::value>(__t.second,
						     static_cast<_Fp&&>(__fun));
  }

// __for_each(_SimdTuple &, const _SimdTuple &, Fun) {{{1
template <size_t _Offset = 0, typename _Tp, typename _A0, typename _Fp>
  _GLIBCXX_SIMD_INTRINSIC constexpr void
  __for_each(_SimdTuple<_Tp, _A0>& __a, const _SimdTuple<_Tp, _A0>& __b,
	     _Fp&& __fun)
  {
    static_cast<_Fp&&>(__fun)(__make_meta<_Offset>(__a), __a.first, __b.first);
  }

template <size_t _Offset = 0, typename _Tp, typename _A0, typename _A1,
	  typename... _As, typename _Fp>
  _GLIBCXX_SIMD_INTRINSIC constexpr void
  __for_each(_SimdTuple<_Tp, _A0, _A1, _As...>& __a,
	     const _SimdTuple<_Tp, _A0, _A1, _As...>& __b, _Fp&& __fun)
  {
    __fun(__make_meta<_Offset>(__a), __a.first, __b.first);
    __for_each<_Offset + simd_size<_Tp, _A0>::value>(__a.second, __b.second,
						     static_cast<_Fp&&>(__fun));
  }

// __for_each(const _SimdTuple &, const _SimdTuple &, Fun) {{{1
template <size_t _Offset = 0, typename _Tp, typename _A0, typename _Fp>
  _GLIBCXX_SIMD_INTRINSIC constexpr void
  __for_each(const _SimdTuple<_Tp, _A0>& __a, const _SimdTuple<_Tp, _A0>& __b,
	     _Fp&& __fun)
  {
    static_cast<_Fp&&>(__fun)(__make_meta<_Offset>(__a), __a.first, __b.first);
  }

template <size_t _Offset = 0, typename _Tp, typename _A0, typename _A1,
	  typename... _As, typename _Fp>
  _GLIBCXX_SIMD_INTRINSIC constexpr void
  __for_each(const _SimdTuple<_Tp, _A0, _A1, _As...>& __a,
	     const _SimdTuple<_Tp, _A0, _A1, _As...>& __b, _Fp&& __fun)
  {
    __fun(__make_meta<_Offset>(__a), __a.first, __b.first);
    __for_each<_Offset + simd_size<_Tp, _A0>::value>(__a.second, __b.second,
						     static_cast<_Fp&&>(__fun));
  }

// }}}1
// __extract_part(_SimdTuple) {{{
template <int _Index, int _Total, int _Combine, typename _Tp, typename _A0,
	  typename... _As>
  _GLIBCXX_SIMD_INTRINSIC auto // __vector_type_t or _SimdTuple
  __extract_part(const _SimdTuple<_Tp, _A0, _As...>& __x)
  {
    // worst cases:
    // (a) 4, 4, 4 => 3, 3, 3, 3 (_Total = 4)
    // (b) 2, 2, 2 => 3, 3       (_Total = 2)
    // (c) 4, 2 => 2, 2, 2       (_Total = 3)
    using _Tuple = _SimdTuple<_Tp, _A0, _As...>;
    static_assert(_Index + _Combine <= _Total && _Index >= 0 && _Total >= 1);
    constexpr size_t _Np = _Tuple::_S_size();
    static_assert(_Np >= _Total && _Np % _Total == 0);
    constexpr size_t __values_per_part = _Np / _Total;
    [[maybe_unused]] constexpr size_t __values_to_skip
      = _Index * __values_per_part;
    constexpr size_t __return_size = __values_per_part * _Combine;
    using _RetAbi = simd_abi::deduce_t<_Tp, __return_size>;

    // handle (optimize) the simple cases
    if constexpr (_Index == 0 && _Tuple::_S_first_size == __return_size)
      return __x.first._M_data;
    else if constexpr (_Index == 0 && _Total == _Combine)
      return __x;
    else if constexpr (_Index == 0 && _Tuple::_S_first_size >= __return_size)
      return __intrin_bitcast<__vector_type_t<_Tp, __return_size>>(
	__as_vector(__x.first));

    // recurse to skip unused data members at the beginning of _SimdTuple
    else if constexpr (__values_to_skip >= _Tuple::_S_first_size)
      { // recurse
	if constexpr (_Tuple::_S_first_size % __values_per_part == 0)
	  {
	    constexpr int __parts_in_first
	      = _Tuple::_S_first_size / __values_per_part;
	    return __extract_part<_Index - __parts_in_first,
				  _Total - __parts_in_first, _Combine>(
	      __x.second);
	  }
	else
	  return __extract_part<__values_to_skip - _Tuple::_S_first_size,
				_Np - _Tuple::_S_first_size, __return_size>(
	    __x.second);
      }

    // extract from multiple _SimdTuple data members
    else if constexpr (__return_size > _Tuple::_S_first_size - __values_to_skip)
      {
#ifdef _GLIBCXX_SIMD_USE_ALIASING_LOADS
	const __may_alias<_Tp>* const element_ptr
	  = reinterpret_cast<const __may_alias<_Tp>*>(&__x) + __values_to_skip;
	return __as_vector(simd<_Tp, _RetAbi>(element_ptr, element_aligned));
#else
	[[maybe_unused]] constexpr size_t __offset = __values_to_skip;
	return __as_vector(simd<_Tp, _RetAbi>([&](auto __i) constexpr {
	  constexpr _SizeConstant<__i + __offset> __k;
	  return __x[__k];
	}));
#endif
      }

    // all of the return values are in __x.first
    else if constexpr (_Tuple::_S_first_size % __values_per_part == 0)
      return __extract_part<_Index, _Tuple::_S_first_size / __values_per_part,
			    _Combine>(__x.first);
    else
      return __extract_part<__values_to_skip, _Tuple::_S_first_size,
			    _Combine * __values_per_part>(__x.first);
  }

// }}}
// __fixed_size_storage_t<_Tp, _Np>{{{
template <typename _Tp, int _Np, typename _Tuple,
	  typename _Next = simd<_Tp, _AllNativeAbis::_BestAbi<_Tp, _Np>>,
	  int _Remain = _Np - int(_Next::size())>
  struct __fixed_size_storage_builder;

template <typename _Tp, int _Np>
  struct __fixed_size_storage
  : public __fixed_size_storage_builder<_Tp, _Np, _SimdTuple<_Tp>> {};

template <typename _Tp, int _Np, typename... _As, typename _Next>
  struct __fixed_size_storage_builder<_Tp, _Np, _SimdTuple<_Tp, _As...>, _Next,
				      0>
  { using type = _SimdTuple<_Tp, _As..., typename _Next::abi_type>; };

template <typename _Tp, int _Np, typename... _As, typename _Next, int _Remain>
  struct __fixed_size_storage_builder<_Tp, _Np, _SimdTuple<_Tp, _As...>, _Next,
				      _Remain>
  {
    using type = typename __fixed_size_storage_builder<
      _Tp, _Remain, _SimdTuple<_Tp, _As..., typename _Next::abi_type>>::type;
  };

// }}}
// __autocvt_to_simd {{{
template <typename _Tp, bool = is_arithmetic_v<__remove_cvref_t<_Tp>>>
  struct __autocvt_to_simd
  {
    _Tp _M_data;
    using _TT = __remove_cvref_t<_Tp>;

    _GLIBCXX_SIMD_INTRINSIC
    operator _TT()
    { return _M_data; }

    _GLIBCXX_SIMD_INTRINSIC
    operator _TT&()
    {
      static_assert(is_lvalue_reference<_Tp>::value, "");
      static_assert(!is_const<_Tp>::value, "");
      return _M_data;
    }

    _GLIBCXX_SIMD_INTRINSIC
    operator _TT*()
    {
      static_assert(is_lvalue_reference<_Tp>::value, "");
      static_assert(!is_const<_Tp>::value, "");
      return &_M_data;
    }

    _GLIBCXX_SIMD_INTRINSIC
    constexpr __autocvt_to_simd(_Tp dd) : _M_data(dd) {}

    template <typename _Abi>
      _GLIBCXX_SIMD_INTRINSIC
      operator simd<typename _TT::value_type, _Abi>()
      { return {__private_init, _M_data}; }

    template <typename _Abi>
      _GLIBCXX_SIMD_INTRINSIC
      operator simd<typename _TT::value_type, _Abi>&()
      {
	return *reinterpret_cast<simd<typename _TT::value_type, _Abi>*>(
	  &_M_data);
      }

    template <typename _Abi>
      _GLIBCXX_SIMD_INTRINSIC
      operator simd<typename _TT::value_type, _Abi>*()
      {
	return reinterpret_cast<simd<typename _TT::value_type, _Abi>*>(
	  &_M_data);
      }
  };

template <typename _Tp>
  __autocvt_to_simd(_Tp &&) -> __autocvt_to_simd<_Tp>;

template <typename _Tp>
  struct __autocvt_to_simd<_Tp, true>
  {
    using _TT = __remove_cvref_t<_Tp>;
    _Tp _M_data;
    fixed_size_simd<_TT, 1> _M_fd;

    _GLIBCXX_SIMD_INTRINSIC
    constexpr __autocvt_to_simd(_Tp dd) : _M_data(dd), _M_fd(_M_data) {}

    _GLIBCXX_SIMD_INTRINSIC
    ~__autocvt_to_simd()
    { _M_data = __data(_M_fd).first; }

    _GLIBCXX_SIMD_INTRINSIC
    operator fixed_size_simd<_TT, 1>()
    { return _M_fd; }

    _GLIBCXX_SIMD_INTRINSIC
    operator fixed_size_simd<_TT, 1> &()
    {
      static_assert(is_lvalue_reference<_Tp>::value, "");
      static_assert(!is_const<_Tp>::value, "");
      return _M_fd;
    }

    _GLIBCXX_SIMD_INTRINSIC
    operator fixed_size_simd<_TT, 1> *()
    {
      static_assert(is_lvalue_reference<_Tp>::value, "");
      static_assert(!is_const<_Tp>::value, "");
      return &_M_fd;
    }
  };

// }}}

struct _CommonImplFixedSize;
template <int _Np, typename = __detail::__odr_helper> struct _SimdImplFixedSize;
template <int _Np, typename = __detail::__odr_helper> struct _MaskImplFixedSize;
// simd_abi::_Fixed {{{
template <int _Np>
  struct simd_abi::_Fixed
  {
    template <typename _Tp> static constexpr size_t _S_size = _Np;
    template <typename _Tp> static constexpr size_t _S_full_size = _Np;
    // validity traits {{{
    struct _IsValidAbiTag : public __bool_constant<(_Np > 0)> {};

    template <typename _Tp>
      struct _IsValidSizeFor
      : __bool_constant<(_Np <= simd_abi::max_fixed_size<_Tp>)> {};

    template <typename _Tp>
      struct _IsValid : conjunction<_IsValidAbiTag, __is_vectorizable<_Tp>,
				    _IsValidSizeFor<_Tp>> {};

    template <typename _Tp>
      static constexpr bool _S_is_valid_v = _IsValid<_Tp>::value;

    // }}}
    // _S_masked {{{
    _GLIBCXX_SIMD_INTRINSIC static constexpr _SanitizedBitMask<_Np>
    _S_masked(_BitMask<_Np> __x)
    { return __x._M_sanitized(); }

    _GLIBCXX_SIMD_INTRINSIC static constexpr _SanitizedBitMask<_Np>
    _S_masked(_SanitizedBitMask<_Np> __x)
    { return __x; }

    // }}}
    // _*Impl {{{
    using _CommonImpl = _CommonImplFixedSize;
    using _SimdImpl = _SimdImplFixedSize<_Np>;
    using _MaskImpl = _MaskImplFixedSize<_Np>;

    // }}}
    // __traits {{{
    template <typename _Tp, bool = _S_is_valid_v<_Tp>>
      struct __traits : _InvalidTraits {};

    template <typename _Tp>
      struct __traits<_Tp, true>
      {
	using _IsValid = true_type;
	using _SimdImpl = _SimdImplFixedSize<_Np>;
	using _MaskImpl = _MaskImplFixedSize<_Np>;

	// simd and simd_mask member types {{{
	using _SimdMember = __fixed_size_storage_t<_Tp, _Np>;
	using _MaskMember = _SanitizedBitMask<_Np>;

	static constexpr size_t _S_simd_align
	  = std::__bit_ceil(_Np * sizeof(_Tp));

	static constexpr size_t _S_mask_align = alignof(_MaskMember);

	// }}}
	// _SimdBase / base class for simd, providing extra conversions {{{
	struct _SimdBase
	{
	  // The following ensures, function arguments are passed via the stack.
	  // This is important for ABI compatibility across TU boundaries
	  _GLIBCXX_SIMD_ALWAYS_INLINE
	  _SimdBase(const _SimdBase&) {}
	  _SimdBase() = default;

	  _GLIBCXX_SIMD_ALWAYS_INLINE
	  explicit operator const _SimdMember &() const
	  { return static_cast<const simd<_Tp, _Fixed>*>(this)->_M_data; }

	  _GLIBCXX_SIMD_ALWAYS_INLINE
	  explicit operator array<_Tp, _Np>() const
	  {
	    array<_Tp, _Np> __r;
	    // _SimdMember can be larger because of higher alignment
	    static_assert(sizeof(__r) <= sizeof(_SimdMember), "");
	    __builtin_memcpy(__r.data(), &static_cast<const _SimdMember&>(*this),
			     sizeof(__r));
	    return __r;
	  }
	};

	// }}}
	// _MaskBase {{{
	// empty. The bitset interface suffices
	struct _MaskBase {};

	// }}}
	// _SimdCastType {{{
	struct _SimdCastType
	{
	  _GLIBCXX_SIMD_ALWAYS_INLINE
	  _SimdCastType(const array<_Tp, _Np>&);
	  _GLIBCXX_SIMD_ALWAYS_INLINE
	  _SimdCastType(const _SimdMember& dd) : _M_data(dd) {}
	  _GLIBCXX_SIMD_ALWAYS_INLINE
	  explicit operator const _SimdMember &() const { return _M_data; }

	private:
	  const _SimdMember& _M_data;
	};

	// }}}
	// _MaskCastType {{{
	class _MaskCastType
	{
	  _MaskCastType() = delete;
	};
	// }}}
      };
    // }}}
  };

// }}}
// _CommonImplFixedSize {{{
struct _CommonImplFixedSize
{
  // _S_store {{{
  template <typename _Tp, typename... _As>
    _GLIBCXX_SIMD_INTRINSIC static void
    _S_store(const _SimdTuple<_Tp, _As...>& __x, void* __addr)
    {
      constexpr size_t _Np = _SimdTuple<_Tp, _As...>::_S_size();
      __builtin_memcpy(__addr, &__x, _Np * sizeof(_Tp));
    }

  // }}}
};

// }}}
// _SimdImplFixedSize {{{1
// fixed_size should not inherit from _SimdMathFallback in order for
// specializations in the used _SimdTuple Abis to get used
template <int _Np, typename>
  struct _SimdImplFixedSize
  {
    // member types {{{2
    using _MaskMember = _SanitizedBitMask<_Np>;

    template <typename _Tp>
      using _SimdMember = __fixed_size_storage_t<_Tp, _Np>;

    template <typename _Tp>
      static constexpr size_t _S_tuple_size = _SimdMember<_Tp>::_S_tuple_size;

    template <typename _Tp>
      using _Simd = simd<_Tp, simd_abi::fixed_size<_Np>>;

    template <typename _Tp>
      using _TypeTag = _Tp*;

    // broadcast {{{2
    template <typename _Tp>
      static constexpr inline _SimdMember<_Tp> _S_broadcast(_Tp __x) noexcept
      {
	return _SimdMember<_Tp>::_S_generate([&](auto __meta) constexpr {
	  return __meta._S_broadcast(__x);
	});
      }

    // _S_generator {{{2
    template <typename _Fp, typename _Tp>
      static constexpr inline _SimdMember<_Tp> _S_generator(_Fp&& __gen,
							    _TypeTag<_Tp>)
      {
	return _SimdMember<_Tp>::_S_generate([&__gen](auto __meta) constexpr {
	  return __meta._S_generator(
	    [&](auto __i) constexpr {
	      return __i < _Np ? __gen(_SizeConstant<__meta._S_offset + __i>())
			       : 0;
	    },
	    _TypeTag<_Tp>());
	});
      }

    // _S_load {{{2
    template <typename _Tp, typename _Up>
      static inline _SimdMember<_Tp> _S_load(const _Up* __mem,
					     _TypeTag<_Tp>) noexcept
      {
	return _SimdMember<_Tp>::_S_generate([&](auto __meta) {
	  return __meta._S_load(&__mem[__meta._S_offset], _TypeTag<_Tp>());
	});
      }

    // _S_masked_load {{{2
    template <typename _Tp, typename... _As, typename _Up>
      static inline _SimdTuple<_Tp, _As...>
      _S_masked_load(const _SimdTuple<_Tp, _As...>& __old,
		     const _MaskMember __bits, const _Up* __mem) noexcept
      {
	auto __merge = __old;
	__for_each(__merge, [&](auto __meta, auto& __native) {
	  if (__meta._S_submask(__bits).any())
#pragma GCC diagnostic push
	  // __mem + __mem._S_offset could be UB ([expr.add]/4.3, but it punts
	  // the responsibility for avoiding UB to the caller of the masked load
	  // via the mask. Consequently, the compiler may assume this branch is
	  // unreachable, if the pointer arithmetic is UB.
#pragma GCC diagnostic ignored "-Warray-bounds"
	    __native
	      = __meta._S_masked_load(__native, __meta._S_make_mask(__bits),
				      __mem + __meta._S_offset);
#pragma GCC diagnostic pop
	});
	return __merge;
      }

    // _S_store {{{2
    template <typename _Tp, typename _Up>
      static inline void _S_store(const _SimdMember<_Tp>& __v, _Up* __mem,
				  _TypeTag<_Tp>) noexcept
      {
	__for_each(__v, [&](auto __meta, auto __native) {
	  __meta._S_store(__native, &__mem[__meta._S_offset], _TypeTag<_Tp>());
	});
      }

    // _S_masked_store {{{2
    template <typename _Tp, typename... _As, typename _Up>
      static inline void _S_masked_store(const _SimdTuple<_Tp, _As...>& __v,
					 _Up* __mem,
					 const _MaskMember __bits) noexcept
      {
	__for_each(__v, [&](auto __meta, auto __native) {
	  if (__meta._S_submask(__bits).any())
#pragma GCC diagnostic push
	  // __mem + __mem._S_offset could be UB ([expr.add]/4.3, but it punts
	  // the responsibility for avoiding UB to the caller of the masked
	  // store via the mask. Consequently, the compiler may assume this
	  // branch is unreachable, if the pointer arithmetic is UB.
#pragma GCC diagnostic ignored "-Warray-bounds"
	    __meta._S_masked_store(__native, __mem + __meta._S_offset,
				   __meta._S_make_mask(__bits));
#pragma GCC diagnostic pop
	});
      }

    // negation {{{2
    template <typename _Tp, typename... _As>
      static inline _MaskMember
      _S_negate(const _SimdTuple<_Tp, _As...>& __x) noexcept
      {
	_MaskMember __bits = 0;
	__for_each(
	  __x, [&__bits](auto __meta, auto __native) constexpr {
	    __bits
	      |= __meta._S_mask_to_shifted_ullong(__meta._S_negate(__native));
	  });
	return __bits;
      }

    // reductions {{{2
    template <typename _Tp, typename _BinaryOperation>
      static constexpr inline _Tp _S_reduce(const _Simd<_Tp>& __x,
					    const _BinaryOperation& __binary_op)
      {
	using _Tup = _SimdMember<_Tp>;
	const _Tup& __tup = __data(__x);
	if constexpr (_Tup::_S_tuple_size == 1)
	  return _Tup::_FirstAbi::_SimdImpl::_S_reduce(
	    __tup.template _M_simd_at<0>(), __binary_op);
	else if constexpr (_Tup::_S_tuple_size == 2 && _Tup::_S_size() > 2
			   && _Tup::_SecondType::_S_size() == 1)
	  {
	    return __binary_op(simd<_Tp, simd_abi::scalar>(
				 reduce(__tup.template _M_simd_at<0>(),
					__binary_op)),
			       __tup.template _M_simd_at<1>())[0];
	  }
	else if constexpr (_Tup::_S_tuple_size == 2 && _Tup::_S_size() > 4
			   && _Tup::_SecondType::_S_size() == 2)
	  {
	    return __binary_op(
	      simd<_Tp, simd_abi::scalar>(
		reduce(__tup.template _M_simd_at<0>(), __binary_op)),
	      simd<_Tp, simd_abi::scalar>(
		reduce(__tup.template _M_simd_at<1>(), __binary_op)))[0];
	  }
	else
	  {
	    const auto& __x2 = __call_with_n_evaluations<
	      __div_roundup(_Tup::_S_tuple_size, 2)>(
	      [](auto __first_simd, auto... __remaining) {
		if constexpr (sizeof...(__remaining) == 0)
		  return __first_simd;
		else
		  {
		    using _Tup2
		      = _SimdTuple<_Tp,
				   typename decltype(__first_simd)::abi_type,
				   typename decltype(__remaining)::abi_type...>;
		    return fixed_size_simd<_Tp, _Tup2::_S_size()>(
		      __private_init,
		      __make_simd_tuple(__first_simd, __remaining...));
		  }
	      },
	      [&](auto __i) {
		auto __left = __tup.template _M_simd_at<2 * __i>();
		if constexpr (2 * __i + 1 == _Tup::_S_tuple_size)
		  return __left;
		else
		  {
		    auto __right = __tup.template _M_simd_at<2 * __i + 1>();
		    using _LT = decltype(__left);
		    using _RT = decltype(__right);
		    if constexpr (_LT::size() == _RT::size())
		      return __binary_op(__left, __right);
		    else
		      {
			_GLIBCXX_SIMD_USE_CONSTEXPR_API
			typename _LT::mask_type __k(
			  __private_init,
			  [](auto __j) constexpr { return __j < _RT::size(); });
			_LT __ext_right = __left;
			where(__k, __ext_right)
			  = __proposed::resizing_simd_cast<_LT>(__right);
			where(__k, __left) = __binary_op(__left, __ext_right);
			return __left;
		      }
		  }
	      });
	    return reduce(__x2, __binary_op);
	  }
      }

    // _S_min, _S_max {{{2
    template <typename _Tp, typename... _As>
      static inline constexpr _SimdTuple<_Tp, _As...>
      _S_min(const _SimdTuple<_Tp, _As...>& __a,
	     const _SimdTuple<_Tp, _As...>& __b)
      {
	return __a._M_apply_per_chunk(
	  [](auto __impl, auto __aa, auto __bb) constexpr {
	    return __impl._S_min(__aa, __bb);
	  },
	  __b);
      }

    template <typename _Tp, typename... _As>
      static inline constexpr _SimdTuple<_Tp, _As...>
      _S_max(const _SimdTuple<_Tp, _As...>& __a,
	     const _SimdTuple<_Tp, _As...>& __b)
      {
	return __a._M_apply_per_chunk(
	  [](auto __impl, auto __aa, auto __bb) constexpr {
	    return __impl._S_max(__aa, __bb);
	  },
	  __b);
      }

    // _S_complement {{{2
    template <typename _Tp, typename... _As>
      static inline constexpr _SimdTuple<_Tp, _As...>
      _S_complement(const _SimdTuple<_Tp, _As...>& __x) noexcept
      {
	return __x._M_apply_per_chunk([](auto __impl, auto __xx) constexpr {
	  return __impl._S_complement(__xx);
	});
      }

    // _S_unary_minus {{{2
    template <typename _Tp, typename... _As>
      static inline constexpr _SimdTuple<_Tp, _As...>
      _S_unary_minus(const _SimdTuple<_Tp, _As...>& __x) noexcept
      {
	return __x._M_apply_per_chunk([](auto __impl, auto __xx) constexpr {
	  return __impl._S_unary_minus(__xx);
	});
      }

    // arithmetic operators {{{2

#define _GLIBCXX_SIMD_FIXED_OP(name_, op_)                                     \
    template <typename _Tp, typename... _As>                                   \
      static inline constexpr _SimdTuple<_Tp, _As...> name_(                   \
	const _SimdTuple<_Tp, _As...>& __x, const _SimdTuple<_Tp, _As...>& __y)\
      {                                                                        \
	return __x._M_apply_per_chunk(                                         \
	  [](auto __impl, auto __xx, auto __yy) constexpr {                    \
	    return __impl.name_(__xx, __yy);                                   \
	  },                                                                   \
	  __y);                                                                \
      }

    _GLIBCXX_SIMD_FIXED_OP(_S_plus, +)
    _GLIBCXX_SIMD_FIXED_OP(_S_minus, -)
    _GLIBCXX_SIMD_FIXED_OP(_S_multiplies, *)
    _GLIBCXX_SIMD_FIXED_OP(_S_divides, /)
    _GLIBCXX_SIMD_FIXED_OP(_S_modulus, %)
    _GLIBCXX_SIMD_FIXED_OP(_S_bit_and, &)
    _GLIBCXX_SIMD_FIXED_OP(_S_bit_or, |)
    _GLIBCXX_SIMD_FIXED_OP(_S_bit_xor, ^)
    _GLIBCXX_SIMD_FIXED_OP(_S_bit_shift_left, <<)
    _GLIBCXX_SIMD_FIXED_OP(_S_bit_shift_right, >>)
#undef _GLIBCXX_SIMD_FIXED_OP

    template <typename _Tp, typename... _As>
      static inline constexpr _SimdTuple<_Tp, _As...>
      _S_bit_shift_left(const _SimdTuple<_Tp, _As...>& __x, int __y)
      {
	return __x._M_apply_per_chunk([__y](auto __impl, auto __xx) constexpr {
	  return __impl._S_bit_shift_left(__xx, __y);
	});
      }

    template <typename _Tp, typename... _As>
      static inline constexpr _SimdTuple<_Tp, _As...>
      _S_bit_shift_right(const _SimdTuple<_Tp, _As...>& __x, int __y)
      {
	return __x._M_apply_per_chunk([__y](auto __impl, auto __xx) constexpr {
	  return __impl._S_bit_shift_right(__xx, __y);
	});
      }

  // math {{{2
#define _GLIBCXX_SIMD_APPLY_ON_TUPLE(_RetTp, __name)                           \
    template <typename _Tp, typename... _As, typename... _More>                \
      static inline __fixed_size_storage_t<_RetTp, _Np>                        \
	_S_##__name(const _SimdTuple<_Tp, _As...>& __x,                        \
		    const _More&... __more)                                    \
      {                                                                        \
	if constexpr (sizeof...(_More) == 0)                                   \
	  {                                                                    \
	    if constexpr (is_same_v<_Tp, _RetTp>)                              \
	      return __x._M_apply_per_chunk(                                   \
		[](auto __impl, auto __xx) constexpr {                         \
		  using _V = typename decltype(__impl)::simd_type;             \
		  return __data(__name(_V(__private_init, __xx)));             \
		});                                                            \
	    else                                                               \
	      return __optimize_simd_tuple(                                    \
		__x.template _M_apply_r<_RetTp>([](auto __impl, auto __xx) {   \
		  return __impl._S_##__name(__xx);                             \
		}));                                                           \
	  }                                                                    \
	else if constexpr (                                                    \
	  is_same_v<                                                           \
	    _Tp,                                                               \
	    _RetTp> && (... && is_same_v<_SimdTuple<_Tp, _As...>, _More>) )    \
	  return __x._M_apply_per_chunk(                                       \
	    [](auto __impl, auto __xx, auto... __pack) constexpr {             \
	      using _V = typename decltype(__impl)::simd_type;                 \
	      return __data(__name(_V(__private_init, __xx),                   \
				   _V(__private_init, __pack)...));            \
	    },                                                                 \
	    __more...);                                                        \
	else if constexpr (is_same_v<_Tp, _RetTp>)                             \
	  return __x._M_apply_per_chunk(                                       \
	    [](auto __impl, auto __xx, auto... __pack) constexpr {             \
	      using _V = typename decltype(__impl)::simd_type;                 \
	      return __data(__name(_V(__private_init, __xx),                   \
				   __autocvt_to_simd(__pack)...));             \
	    },                                                                 \
	    __more...);                                                        \
	else                                                                   \
	  __assert_unreachable<_Tp>();                                         \
      }

    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, acos)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, asin)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, atan)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, atan2)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, cos)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, sin)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, tan)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, acosh)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, asinh)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, atanh)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, cosh)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, sinh)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, tanh)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, exp)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, exp2)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, expm1)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(int, ilogb)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, log)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, log10)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, log1p)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, log2)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, logb)
    // modf implemented in simd_math.h
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp,
				 scalbn) // double scalbn(double x, int exp);
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, scalbln)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, cbrt)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, abs)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, fabs)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, pow)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, sqrt)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, erf)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, erfc)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, lgamma)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, tgamma)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, trunc)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, ceil)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, floor)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, nearbyint)

    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, rint)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(long, lrint)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(long long, llrint)

    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, round)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(long, lround)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(long long, llround)

    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, ldexp)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, fmod)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, remainder)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, copysign)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, nextafter)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, fdim)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, fmax)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, fmin)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(_Tp, fma)
    _GLIBCXX_SIMD_APPLY_ON_TUPLE(int, fpclassify)
#undef _GLIBCXX_SIMD_APPLY_ON_TUPLE

    template <typename _Tp, typename... _Abis>
      static _SimdTuple<_Tp, _Abis...> _S_remquo(
	const _SimdTuple<_Tp, _Abis...>& __x,
	const _SimdTuple<_Tp, _Abis...>& __y,
	__fixed_size_storage_t<int, _SimdTuple<_Tp, _Abis...>::_S_size()>* __z)
      {
	return __x._M_apply_per_chunk(
	  [](auto __impl, const auto __xx, const auto __yy, auto& __zz) {
	    return __impl._S_remquo(__xx, __yy, &__zz);
	  },
	  __y, *__z);
      }

    template <typename _Tp, typename... _As>
      static inline _SimdTuple<_Tp, _As...>
      _S_frexp(const _SimdTuple<_Tp, _As...>& __x,
	       __fixed_size_storage_t<int, _Np>& __exp) noexcept
      {
	return __x._M_apply_per_chunk(
	  [](auto __impl, const auto& __a, auto& __b) {
	    return __data(
	      frexp(typename decltype(__impl)::simd_type(__private_init, __a),
		    __autocvt_to_simd(__b)));
	  },
	  __exp);
      }

#define _GLIBCXX_SIMD_TEST_ON_TUPLE_(name_)                                    \
    template <typename _Tp, typename... _As>                                   \
      static inline _MaskMember                                                \
	_S_##name_(const _SimdTuple<_Tp, _As...>& __x) noexcept                \
      {                                                                        \
	return _M_test([](auto __impl,                                         \
			  auto __xx) { return __impl._S_##name_(__xx); },      \
		       __x);                                                   \
      }

    _GLIBCXX_SIMD_TEST_ON_TUPLE_(isinf)
    _GLIBCXX_SIMD_TEST_ON_TUPLE_(isfinite)
    _GLIBCXX_SIMD_TEST_ON_TUPLE_(isnan)
    _GLIBCXX_SIMD_TEST_ON_TUPLE_(isnormal)
    _GLIBCXX_SIMD_TEST_ON_TUPLE_(signbit)
#undef _GLIBCXX_SIMD_TEST_ON_TUPLE_

    // _S_increment & _S_decrement{{{2
    template <typename... _Ts>
      _GLIBCXX_SIMD_INTRINSIC static constexpr void
      _S_increment(_SimdTuple<_Ts...>& __x)
      {
	__for_each(
	  __x, [](auto __meta, auto& native) constexpr {
	    __meta._S_increment(native);
	  });
      }

    template <typename... _Ts>
      _GLIBCXX_SIMD_INTRINSIC static constexpr void
      _S_decrement(_SimdTuple<_Ts...>& __x)
      {
	__for_each(
	  __x, [](auto __meta, auto& native) constexpr {
	    __meta._S_decrement(native);
	  });
      }

    // compares {{{2
#define _GLIBCXX_SIMD_CMP_OPERATIONS(__cmp)                                    \
    template <typename _Tp, typename... _As>                                   \
      _GLIBCXX_SIMD_INTRINSIC constexpr static _MaskMember                     \
      __cmp(const _SimdTuple<_Tp, _As...>& __x,                                \
	    const _SimdTuple<_Tp, _As...>& __y)                                \
      {                                                                        \
	return _M_test(                                                        \
	  [](auto __impl, auto __xx, auto __yy) constexpr {                    \
	    return __impl.__cmp(__xx, __yy);                                   \
	  },                                                                   \
	  __x, __y);                                                           \
      }

    _GLIBCXX_SIMD_CMP_OPERATIONS(_S_equal_to)
    _GLIBCXX_SIMD_CMP_OPERATIONS(_S_not_equal_to)
    _GLIBCXX_SIMD_CMP_OPERATIONS(_S_less)
    _GLIBCXX_SIMD_CMP_OPERATIONS(_S_less_equal)
    _GLIBCXX_SIMD_CMP_OPERATIONS(_S_isless)
    _GLIBCXX_SIMD_CMP_OPERATIONS(_S_islessequal)
    _GLIBCXX_SIMD_CMP_OPERATIONS(_S_isgreater)
    _GLIBCXX_SIMD_CMP_OPERATIONS(_S_isgreaterequal)
    _GLIBCXX_SIMD_CMP_OPERATIONS(_S_islessgreater)
    _GLIBCXX_SIMD_CMP_OPERATIONS(_S_isunordered)
#undef _GLIBCXX_SIMD_CMP_OPERATIONS

    // smart_reference access {{{2
    template <typename _Tp, typename... _As, typename _Up>
      _GLIBCXX_SIMD_INTRINSIC static void _S_set(_SimdTuple<_Tp, _As...>& __v,
						 int __i, _Up&& __x) noexcept
      { __v._M_set(__i, static_cast<_Up&&>(__x)); }

    // _S_masked_assign {{{2
    template <typename _Tp, typename... _As>
      _GLIBCXX_SIMD_INTRINSIC static void
      _S_masked_assign(const _MaskMember __bits, _SimdTuple<_Tp, _As...>& __lhs,
		       const __type_identity_t<_SimdTuple<_Tp, _As...>>& __rhs)
      {
	__for_each(
	  __lhs, __rhs,
	  [&](auto __meta, auto& __native_lhs, auto __native_rhs) constexpr {
	    __meta._S_masked_assign(__meta._S_make_mask(__bits), __native_lhs,
				    __native_rhs);
	  });
      }

    // Optimization for the case where the RHS is a scalar. No need to broadcast
    // the scalar to a simd first.
    template <typename _Tp, typename... _As>
      _GLIBCXX_SIMD_INTRINSIC static void
      _S_masked_assign(const _MaskMember __bits, _SimdTuple<_Tp, _As...>& __lhs,
		       const __type_identity_t<_Tp> __rhs)
      {
	__for_each(
	  __lhs, [&](auto __meta, auto& __native_lhs) constexpr {
	    __meta._S_masked_assign(__meta._S_make_mask(__bits), __native_lhs,
				    __rhs);
	  });
      }

    // _S_masked_cassign {{{2
    template <typename _Op, typename _Tp, typename... _As>
      static inline void _S_masked_cassign(const _MaskMember __bits,
					   _SimdTuple<_Tp, _As...>& __lhs,
					   const _SimdTuple<_Tp, _As...>& __rhs,
					   _Op __op)
      {
	__for_each(
	  __lhs, __rhs,
	  [&](auto __meta, auto& __native_lhs, auto __native_rhs) constexpr {
	    __meta.template _S_masked_cassign(__meta._S_make_mask(__bits),
					      __native_lhs, __native_rhs, __op);
	  });
      }

    // Optimization for the case where the RHS is a scalar. No need to broadcast
    // the scalar to a simd first.
    template <typename _Op, typename _Tp, typename... _As>
      static inline void _S_masked_cassign(const _MaskMember __bits,
					   _SimdTuple<_Tp, _As...>& __lhs,
					   const _Tp& __rhs, _Op __op)
      {
	__for_each(
	  __lhs, [&](auto __meta, auto& __native_lhs) constexpr {
	    __meta.template _S_masked_cassign(__meta._S_make_mask(__bits),
					      __native_lhs, __rhs, __op);
	  });
      }

    // _S_masked_unary {{{2
    template <template <typename> class _Op, typename _Tp, typename... _As>
      static inline _SimdTuple<_Tp, _As...>
      _S_masked_unary(const _MaskMember __bits, const _SimdTuple<_Tp, _As...>& __v)
      {
	return __v._M_apply_wrapped([&__bits](auto __meta,
					      auto __native) constexpr {
	  return __meta.template _S_masked_unary<_Op>(__meta._S_make_mask(
							__bits),
						      __native);
	});
      }

    // }}}2
  };

// _MaskImplFixedSize {{{1
template <int _Np, typename>
  struct _MaskImplFixedSize
  {
    static_assert(
      sizeof(_ULLong) * __CHAR_BIT__ >= _Np,
      "The fixed_size implementation relies on one _ULLong being able to store "
      "all boolean elements."); // required in load & store

    // member types {{{
    using _Abi = simd_abi::fixed_size<_Np>;

    using _MaskMember = _SanitizedBitMask<_Np>;

    template <typename _Tp>
      using _FirstAbi = typename __fixed_size_storage_t<_Tp, _Np>::_FirstAbi;

    template <typename _Tp>
      using _TypeTag = _Tp*;

    // }}}
    // _S_broadcast {{{
    template <typename>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _MaskMember
      _S_broadcast(bool __x)
      { return __x ? ~_MaskMember() : _MaskMember(); }

    // }}}
    // _S_load {{{
    template <typename>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _MaskMember
      _S_load(const bool* __mem)
      {
	using _Ip = __int_for_sizeof_t<bool>;
	// the following load uses element_aligned and relies on __mem already
	// carrying alignment information from when this load function was
	// called.
	const simd<_Ip, _Abi> __bools(reinterpret_cast<const __may_alias<_Ip>*>(
					__mem),
				      element_aligned);
	return __data(__bools != 0);
      }

    // }}}
    // _S_to_bits {{{
    template <bool _Sanitized>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _SanitizedBitMask<_Np>
      _S_to_bits(_BitMask<_Np, _Sanitized> __x)
      {
	if constexpr (_Sanitized)
	  return __x;
	else
	  return __x._M_sanitized();
      }

    // }}}
    // _S_convert {{{
    template <typename _Tp, typename _Up, typename _UAbi>
      _GLIBCXX_SIMD_INTRINSIC static constexpr _MaskMember
      _S_convert(simd_mask<_Up, _UAbi> __x)
      {
	return _UAbi::_MaskImpl::_S_to_bits(__data(__x))
	  .template _M_extract<0, _Np>();
      }

    // }}}
    // _S_from_bitmask {{{2
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static _MaskMember
      _S_from_bitmask(_MaskMember __bits, _TypeTag<_Tp>) noexcept
      { return __bits; }

    // _S_load {{{2
    static inline _MaskMember _S_load(const bool* __mem) noexcept
    {
      // TODO: _UChar is not necessarily the best type to use here. For smaller
      // _Np _UShort, _UInt, _ULLong, float, and double can be more efficient.
      _ULLong __r = 0;
      using _Vs = __fixed_size_storage_t<_UChar, _Np>;
      __for_each(_Vs{}, [&](auto __meta, auto) {
	__r |= __meta._S_mask_to_shifted_ullong(
	  __meta._S_mask_impl._S_load(&__mem[__meta._S_offset],
				      _SizeConstant<__meta._S_size()>()));
      });
      return __r;
    }

    // _S_masked_load {{{2
    static inline _MaskMember _S_masked_load(_MaskMember __merge,
					     _MaskMember __mask,
					     const bool* __mem) noexcept
    {
      _BitOps::_S_bit_iteration(__mask.to_ullong(), [&](auto __i) {
	__merge.set(__i, __mem[__i]);
      });
      return __merge;
    }

    // _S_store {{{2
    static inline void _S_store(const _MaskMember __bitmask,
				bool* __mem) noexcept
    {
      if constexpr (_Np == 1)
	__mem[0] = __bitmask[0];
      else
	_FirstAbi<_UChar>::_CommonImpl::_S_store_bool_array(__bitmask, __mem);
    }

    // _S_masked_store {{{2
    static inline void _S_masked_store(const _MaskMember __v, bool* __mem,
				       const _MaskMember __k) noexcept
    {
      _BitOps::_S_bit_iteration(__k, [&](auto __i) { __mem[__i] = __v[__i]; });
    }

    // logical and bitwise operators {{{2
    _GLIBCXX_SIMD_INTRINSIC static _MaskMember
    _S_logical_and(const _MaskMember& __x, const _MaskMember& __y) noexcept
    { return __x & __y; }

    _GLIBCXX_SIMD_INTRINSIC static _MaskMember
    _S_logical_or(const _MaskMember& __x, const _MaskMember& __y) noexcept
    { return __x | __y; }

    _GLIBCXX_SIMD_INTRINSIC static constexpr _MaskMember
    _S_bit_not(const _MaskMember& __x) noexcept
    { return ~__x; }

    _GLIBCXX_SIMD_INTRINSIC static _MaskMember
    _S_bit_and(const _MaskMember& __x, const _MaskMember& __y) noexcept
    { return __x & __y; }

    _GLIBCXX_SIMD_INTRINSIC static _MaskMember
    _S_bit_or(const _MaskMember& __x, const _MaskMember& __y) noexcept
    { return __x | __y; }

    _GLIBCXX_SIMD_INTRINSIC static _MaskMember
    _S_bit_xor(const _MaskMember& __x, const _MaskMember& __y) noexcept
    { return __x ^ __y; }

    // smart_reference access {{{2
    _GLIBCXX_SIMD_INTRINSIC static void _S_set(_MaskMember& __k, int __i,
					       bool __x) noexcept
    { __k.set(__i, __x); }

    // _S_masked_assign {{{2
    _GLIBCXX_SIMD_INTRINSIC static void
    _S_masked_assign(const _MaskMember __k, _MaskMember& __lhs,
		     const _MaskMember __rhs)
    { __lhs = (__lhs & ~__k) | (__rhs & __k); }

    // Optimization for the case where the RHS is a scalar.
    _GLIBCXX_SIMD_INTRINSIC static void _S_masked_assign(const _MaskMember __k,
							 _MaskMember& __lhs,
							 const bool __rhs)
    {
      if (__rhs)
	__lhs |= __k;
      else
	__lhs &= ~__k;
    }

    // }}}2
    // _S_all_of {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static bool _S_all_of(simd_mask<_Tp, _Abi> __k)
      { return __data(__k).all(); }

    // }}}
    // _S_any_of {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static bool _S_any_of(simd_mask<_Tp, _Abi> __k)
      { return __data(__k).any(); }

    // }}}
    // _S_none_of {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static bool _S_none_of(simd_mask<_Tp, _Abi> __k)
      { return __data(__k).none(); }

    // }}}
    // _S_some_of {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static bool
      _S_some_of([[maybe_unused]] simd_mask<_Tp, _Abi> __k)
      {
	if constexpr (_Np == 1)
	  return false;
	else
	  return __data(__k).any() && !__data(__k).all();
      }

    // }}}
    // _S_popcount {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static int _S_popcount(simd_mask<_Tp, _Abi> __k)
      { return __data(__k).count(); }

    // }}}
    // _S_find_first_set {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static int
      _S_find_first_set(simd_mask<_Tp, _Abi> __k)
      { return std::__countr_zero(__data(__k).to_ullong()); }

    // }}}
    // _S_find_last_set {{{
    template <typename _Tp>
      _GLIBCXX_SIMD_INTRINSIC static int
      _S_find_last_set(simd_mask<_Tp, _Abi> __k)
      { return std::__bit_width(__data(__k).to_ullong()) - 1; }

    // }}}
  };
// }}}1

_GLIBCXX_SIMD_END_NAMESPACE
#endif // __cplusplus >= 201703L
#endif // _GLIBCXX_EXPERIMENTAL_SIMD_FIXED_SIZE_H_

// vim: foldmethod=marker sw=2 noet ts=8 sts=2 tw=80
