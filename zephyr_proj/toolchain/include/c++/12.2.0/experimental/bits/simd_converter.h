// Generic simd conversions -*- C++ -*-

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

#ifndef _GLIBCXX_EXPERIMENTAL_SIMD_CONVERTER_H_
#define _GLIBCXX_EXPERIMENTAL_SIMD_CONVERTER_H_

#if __cplusplus >= 201703L

_GLIBCXX_SIMD_BEGIN_NAMESPACE
// _SimdConverter scalar -> scalar {{{
template <typename _From, typename _To>
  struct _SimdConverter<_From, simd_abi::scalar, _To, simd_abi::scalar,
			enable_if_t<!is_same_v<_From, _To>>>
  {
    _GLIBCXX_SIMD_INTRINSIC constexpr _To operator()(_From __a) const noexcept
    { return static_cast<_To>(__a); }
  };

// }}}
// _SimdConverter scalar -> "native" {{{
template <typename _From, typename _To, typename _Abi>
  struct _SimdConverter<_From, simd_abi::scalar, _To, _Abi,
			enable_if_t<!is_same_v<_Abi, simd_abi::scalar>>>
  {
    using _Ret = typename _Abi::template __traits<_To>::_SimdMember;

    template <typename... _More>
      _GLIBCXX_SIMD_INTRINSIC constexpr _Ret
      operator()(_From __a, _More... __more) const noexcept
      {
	static_assert(sizeof...(_More) + 1 == _Abi::template _S_size<_To>);
	static_assert(conjunction_v<is_same<_From, _More>...>);
	return __make_vector<_To>(__a, __more...);
      }
  };

// }}}
// _SimdConverter "native 1" -> "native 2" {{{
template <typename _From, typename _To, typename _AFrom, typename _ATo>
  struct _SimdConverter<
    _From, _AFrom, _To, _ATo,
    enable_if_t<!disjunction_v<
      __is_fixed_size_abi<_AFrom>, __is_fixed_size_abi<_ATo>,
      is_same<_AFrom, simd_abi::scalar>, is_same<_ATo, simd_abi::scalar>,
      conjunction<is_same<_From, _To>, is_same<_AFrom, _ATo>>>>>
  {
    using _Arg = typename _AFrom::template __traits<_From>::_SimdMember;
    using _Ret = typename _ATo::template __traits<_To>::_SimdMember;
    using _V = __vector_type_t<_To, simd_size_v<_To, _ATo>>;

    template <typename... _More>
      _GLIBCXX_SIMD_INTRINSIC constexpr _Ret
      operator()(_Arg __a, _More... __more) const noexcept
      { return __vector_convert<_V>(__a, __more...); }
  };

// }}}
// _SimdConverter scalar -> fixed_size<1> {{{1
template <typename _From, typename _To>
  struct _SimdConverter<_From, simd_abi::scalar, _To, simd_abi::fixed_size<1>,
			void>
  {
    _GLIBCXX_SIMD_INTRINSIC constexpr _SimdTuple<_To, simd_abi::scalar>
    operator()(_From __x) const noexcept
    { return {static_cast<_To>(__x)}; }
  };

// _SimdConverter fixed_size<1> -> scalar {{{1
template <typename _From, typename _To>
  struct _SimdConverter<_From, simd_abi::fixed_size<1>, _To, simd_abi::scalar,
			void>
  {
    _GLIBCXX_SIMD_INTRINSIC constexpr _To
    operator()(_SimdTuple<_From, simd_abi::scalar> __x) const noexcept
    { return {static_cast<_To>(__x.first)}; }
  };

// _SimdConverter fixed_size<_Np> -> fixed_size<_Np> {{{1
template <typename _From, typename _To, int _Np>
  struct _SimdConverter<_From, simd_abi::fixed_size<_Np>, _To,
			simd_abi::fixed_size<_Np>,
			enable_if_t<!is_same_v<_From, _To>>>
  {
    using _Ret = __fixed_size_storage_t<_To, _Np>;
    using _Arg = __fixed_size_storage_t<_From, _Np>;

    _GLIBCXX_SIMD_INTRINSIC constexpr _Ret
    operator()(const _Arg& __x) const noexcept
    {
      if constexpr (is_same_v<_From, _To>)
	return __x;

      // special case (optimize) int signedness casts
      else if constexpr (sizeof(_From) == sizeof(_To)
			 && is_integral_v<_From> && is_integral_v<_To>)
	return __bit_cast<_Ret>(__x);

      // special case if all ABI tags in _Ret are scalar
      else if constexpr (__is_scalar_abi<typename _Ret::_FirstAbi>())
	{
	  return __call_with_subscripts(
	    __x, make_index_sequence<_Np>(),
	    [](auto... __values) constexpr->_Ret {
	      return __make_simd_tuple<_To, decltype((void) __values,
						     simd_abi::scalar())...>(
		static_cast<_To>(__values)...);
	    });
	}

      // from one vector to one vector
      else if constexpr (_Arg::_S_first_size == _Ret::_S_first_size)
	{
	  _SimdConverter<_From, typename _Arg::_FirstAbi, _To,
			 typename _Ret::_FirstAbi>
	    __native_cvt;
	  if constexpr (_Arg::_S_tuple_size == 1)
	    return {__native_cvt(__x.first)};
	  else
	    {
	      constexpr size_t _NRemain = _Np - _Arg::_S_first_size;
	      _SimdConverter<_From, simd_abi::fixed_size<_NRemain>, _To,
			     simd_abi::fixed_size<_NRemain>>
		__remainder_cvt;
	      return {__native_cvt(__x.first), __remainder_cvt(__x.second)};
	    }
	}

      // from one vector to multiple vectors
      else if constexpr (_Arg::_S_first_size > _Ret::_S_first_size)
	{
	  const auto __multiple_return_chunks
	    = __convert_all<__vector_type_t<_To, _Ret::_S_first_size>>(
	      __x.first);
	  constexpr auto __converted = __multiple_return_chunks.size()
				       * _Ret::_FirstAbi::template _S_size<_To>;
	  constexpr auto __remaining = _Np - __converted;
	  if constexpr (_Arg::_S_tuple_size == 1 && __remaining == 0)
	    return __to_simd_tuple<_To, _Np>(__multiple_return_chunks);
	  else if constexpr (_Arg::_S_tuple_size == 1)
	    { // e.g. <int, 3> -> <double, 2, 1> or <short, 7> -> <double, 4, 2,
	      // 1>
	      using _RetRem
		= __remove_cvref_t<decltype(__simd_tuple_pop_front<__converted>(
		  _Ret()))>;
	      const auto __return_chunks2
		= __convert_all<__vector_type_t<_To, _RetRem::_S_first_size>, 0,
				__converted>(__x.first);
	      constexpr auto __converted2
		= __converted
		  + __return_chunks2.size() * _RetRem::_S_first_size;
	      if constexpr (__converted2 == _Np)
		return __to_simd_tuple<_To, _Np>(__multiple_return_chunks,
						 __return_chunks2);
	      else
		{
		  using _RetRem2 = __remove_cvref_t<
		    decltype(__simd_tuple_pop_front<__return_chunks2.size()
						    * _RetRem::_S_first_size>(
		      _RetRem()))>;
		  const auto __return_chunks3 = __convert_all<
		    __vector_type_t<_To, _RetRem2::_S_first_size>, 0,
		    __converted2>(__x.first);
		  constexpr auto __converted3
		    = __converted2
		      + __return_chunks3.size() * _RetRem2::_S_first_size;
		  if constexpr (__converted3 == _Np)
		    return __to_simd_tuple<_To, _Np>(__multiple_return_chunks,
						     __return_chunks2,
						     __return_chunks3);
		  else
		    {
		      using _RetRem3
			= __remove_cvref_t<decltype(__simd_tuple_pop_front<
						    __return_chunks3.size()
						    * _RetRem2::_S_first_size>(
			  _RetRem2()))>;
		      const auto __return_chunks4 = __convert_all<
			__vector_type_t<_To, _RetRem3::_S_first_size>, 0,
			__converted3>(__x.first);
		      constexpr auto __converted4
			= __converted3
			  + __return_chunks4.size() * _RetRem3::_S_first_size;
		      if constexpr (__converted4 == _Np)
			return __to_simd_tuple<_To, _Np>(
			  __multiple_return_chunks, __return_chunks2,
			  __return_chunks3, __return_chunks4);
		      else
			__assert_unreachable<_To>();
		    }
		}
	    }
	  else
	    {
	      constexpr size_t _NRemain = _Np - _Arg::_S_first_size;
	      _SimdConverter<_From, simd_abi::fixed_size<_NRemain>, _To,
			     simd_abi::fixed_size<_NRemain>>
		__remainder_cvt;
	      return __simd_tuple_concat(
		__to_simd_tuple<_To, _Arg::_S_first_size>(
		  __multiple_return_chunks),
		__remainder_cvt(__x.second));
	    }
	}

      // from multiple vectors to one vector
      // _Arg::_S_first_size < _Ret::_S_first_size
      // a) heterogeneous input at the end of the tuple (possible with partial
      //    native registers in _Ret)
      else if constexpr (_Ret::_S_tuple_size == 1
			 && _Np % _Arg::_S_first_size != 0)
	{
	  static_assert(_Ret::_FirstAbi::template _S_is_partial<_To>);
	  return _Ret{__generate_from_n_evaluations<
	    _Np, typename _VectorTraits<typename _Ret::_FirstType>::type>(
	    [&](auto __i) { return static_cast<_To>(__x[__i]); })};
	}
      else
	{
	  static_assert(_Arg::_S_tuple_size > 1);
	  constexpr auto __n
	    = __div_roundup(_Ret::_S_first_size, _Arg::_S_first_size);
	  return __call_with_n_evaluations<__n>(
	    [&__x](auto... __uncvted) {
	      // assuming _Arg Abi tags for all __i are _Arg::_FirstAbi
	      _SimdConverter<_From, typename _Arg::_FirstAbi, _To,
			     typename _Ret::_FirstAbi>
		__native_cvt;
	      if constexpr (_Ret::_S_tuple_size == 1)
		return _Ret{__native_cvt(__uncvted...)};
	      else
		return _Ret{
		  __native_cvt(__uncvted...),
		  _SimdConverter<
		    _From, simd_abi::fixed_size<_Np - _Ret::_S_first_size>, _To,
		    simd_abi::fixed_size<_Np - _Ret::_S_first_size>>()(
		    __simd_tuple_pop_front<_Ret::_S_first_size>(__x))};
	    },
	    [&__x](auto __i) { return __get_tuple_at<__i>(__x); });
	}
    }
  };

// _SimdConverter "native" -> fixed_size<_Np> {{{1
// i.e. 1 register to ? registers
template <typename _From, typename _Ap, typename _To, int _Np>
  struct _SimdConverter<_From, _Ap, _To, simd_abi::fixed_size<_Np>,
			enable_if_t<!__is_fixed_size_abi_v<_Ap>>>
  {
    static_assert(
      _Np == simd_size_v<_From, _Ap>,
      "_SimdConverter to fixed_size only works for equal element counts");

    using _Ret = __fixed_size_storage_t<_To, _Np>;

    _GLIBCXX_SIMD_INTRINSIC constexpr _Ret
    operator()(typename _SimdTraits<_From, _Ap>::_SimdMember __x) const noexcept
    {
      if constexpr (_Ret::_S_tuple_size == 1)
	return {__vector_convert<typename _Ret::_FirstType::_BuiltinType>(__x)};
      else
	{
	  using _FixedNp = simd_abi::fixed_size<_Np>;
	  _SimdConverter<_From, _FixedNp, _To, _FixedNp> __fixed_cvt;
	  using _FromFixedStorage = __fixed_size_storage_t<_From, _Np>;
	  if constexpr (_FromFixedStorage::_S_tuple_size == 1)
	    return __fixed_cvt(_FromFixedStorage{__x});
	  else if constexpr (_FromFixedStorage::_S_tuple_size == 2)
	    {
	      _FromFixedStorage __tmp;
	      static_assert(sizeof(__tmp) <= sizeof(__x));
	      __builtin_memcpy(&__tmp.first, &__x, sizeof(__tmp.first));
	      __builtin_memcpy(&__tmp.second.first,
			       reinterpret_cast<const char*>(&__x)
				 + sizeof(__tmp.first),
			       sizeof(__tmp.second.first));
	      return __fixed_cvt(__tmp);
	    }
	  else
	    __assert_unreachable<_From>();
	}
    }
  };

// _SimdConverter fixed_size<_Np> -> "native" {{{1
// i.e. ? register to 1 registers
template <typename _From, int _Np, typename _To, typename _Ap>
  struct _SimdConverter<_From, simd_abi::fixed_size<_Np>, _To, _Ap,
			enable_if_t<!__is_fixed_size_abi_v<_Ap>>>
  {
    static_assert(
      _Np == simd_size_v<_To, _Ap>,
      "_SimdConverter to fixed_size only works for equal element counts");

    using _Arg = __fixed_size_storage_t<_From, _Np>;

    _GLIBCXX_SIMD_INTRINSIC constexpr
      typename _SimdTraits<_To, _Ap>::_SimdMember
      operator()(const _Arg& __x) const noexcept
    {
      if constexpr (_Arg::_S_tuple_size == 1)
	return __vector_convert<__vector_type_t<_To, _Np>>(__x.first);
      else if constexpr (_Arg::_S_is_homogeneous)
	return __call_with_n_evaluations<_Arg::_S_tuple_size>(
	  [](auto... __members) {
	    if constexpr ((is_convertible_v<decltype(__members), _To> && ...))
	      return __vector_type_t<_To, _Np>{static_cast<_To>(__members)...};
	    else
	      return __vector_convert<__vector_type_t<_To, _Np>>(__members...);
	  },
	  [&](auto __i) { return __get_tuple_at<__i>(__x); });
      else if constexpr (__fixed_size_storage_t<_To, _Np>::_S_tuple_size == 1)
	{
	  _SimdConverter<_From, simd_abi::fixed_size<_Np>, _To,
			 simd_abi::fixed_size<_Np>>
	    __fixed_cvt;
	  return __fixed_cvt(__x).first;
	}
      else
	{
	  const _SimdWrapper<_From, _Np> __xv
	    = __generate_from_n_evaluations<_Np, __vector_type_t<_From, _Np>>(
	      [&](auto __i) { return __x[__i]; });
	  return __vector_convert<__vector_type_t<_To, _Np>>(__xv);
	}
    }
  };

// }}}1
_GLIBCXX_SIMD_END_NAMESPACE
#endif // __cplusplus >= 201703L
#endif // _GLIBCXX_EXPERIMENTAL_SIMD_CONVERTER_H_

// vim: foldmethod=marker sw=2 noet ts=8 sts=2 tw=80
