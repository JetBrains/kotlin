// Utilities for representing and manipulating ranges -*- C++ -*-

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

/** @file bits/ranges_util.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{ranges}
 */

#ifndef _RANGES_UTIL_H
#define _RANGES_UTIL_H 1

#if __cplusplus > 201703L
# include <bits/ranges_base.h>
# include <bits/utility.h>

#ifdef __cpp_lib_ranges
namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION
namespace ranges
{
  // C++20 24.5 [range.utility] Range utilities

  namespace __detail
  {
    template<typename _Range>
      concept __simple_view = view<_Range> && range<const _Range>
	&& same_as<iterator_t<_Range>, iterator_t<const _Range>>
	&& same_as<sentinel_t<_Range>, sentinel_t<const _Range>>;

    template<typename _It>
      concept __has_arrow = input_iterator<_It>
	&& (is_pointer_v<_It> || requires(_It __it) { __it.operator->(); });

    template<typename _Tp, typename _Up>
      concept __different_from
	= !same_as<remove_cvref_t<_Tp>, remove_cvref_t<_Up>>;
  } // namespace __detail

  /// The ranges::view_interface class template
  template<typename _Derived>
    requires is_class_v<_Derived> && same_as<_Derived, remove_cv_t<_Derived>>
    class view_interface
    {
    private:
      constexpr _Derived& _M_derived() noexcept
      {
	static_assert(derived_from<_Derived, view_interface<_Derived>>);
	static_assert(view<_Derived>);
	return static_cast<_Derived&>(*this);
      }

      constexpr const _Derived& _M_derived() const noexcept
      {
	static_assert(derived_from<_Derived, view_interface<_Derived>>);
	static_assert(view<_Derived>);
	return static_cast<const _Derived&>(*this);
      }

      static constexpr bool
      _S_bool(bool) noexcept; // not defined

      template<typename _Tp>
	static constexpr bool
	_S_empty(_Tp& __t)
	noexcept(noexcept(_S_bool(ranges::begin(__t) == ranges::end(__t))))
	{ return ranges::begin(__t) == ranges::end(__t); }

      template<typename _Tp>
	static constexpr auto
	_S_size(_Tp& __t)
	noexcept(noexcept(ranges::end(__t) - ranges::begin(__t)))
	{ return ranges::end(__t) - ranges::begin(__t); }

    public:
      constexpr bool
      empty()
      noexcept(noexcept(_S_empty(_M_derived())))
      requires forward_range<_Derived>
      { return _S_empty(_M_derived()); }

      constexpr bool
      empty() const
      noexcept(noexcept(_S_empty(_M_derived())))
      requires forward_range<const _Derived>
      { return _S_empty(_M_derived()); }

      constexpr explicit
      operator bool() noexcept(noexcept(ranges::empty(_M_derived())))
      requires requires { ranges::empty(_M_derived()); }
      { return !ranges::empty(_M_derived()); }

      constexpr explicit
      operator bool() const noexcept(noexcept(ranges::empty(_M_derived())))
      requires requires { ranges::empty(_M_derived()); }
      { return !ranges::empty(_M_derived()); }

      constexpr auto
      data() noexcept(noexcept(ranges::begin(_M_derived())))
      requires contiguous_iterator<iterator_t<_Derived>>
      { return std::to_address(ranges::begin(_M_derived())); }

      constexpr auto
      data() const noexcept(noexcept(ranges::begin(_M_derived())))
      requires range<const _Derived>
	&& contiguous_iterator<iterator_t<const _Derived>>
      { return std::to_address(ranges::begin(_M_derived())); }

      constexpr auto
      size() noexcept(noexcept(_S_size(_M_derived())))
      requires forward_range<_Derived>
	&& sized_sentinel_for<sentinel_t<_Derived>, iterator_t<_Derived>>
      { return _S_size(_M_derived()); }

      constexpr auto
      size() const noexcept(noexcept(_S_size(_M_derived())))
      requires forward_range<const _Derived>
	&& sized_sentinel_for<sentinel_t<const _Derived>,
			      iterator_t<const _Derived>>
      { return _S_size(_M_derived()); }

      constexpr decltype(auto)
      front() requires forward_range<_Derived>
      {
	__glibcxx_assert(!empty());
	return *ranges::begin(_M_derived());
      }

      constexpr decltype(auto)
      front() const requires forward_range<const _Derived>
      {
	__glibcxx_assert(!empty());
	return *ranges::begin(_M_derived());
      }

      constexpr decltype(auto)
      back()
      requires bidirectional_range<_Derived> && common_range<_Derived>
      {
	__glibcxx_assert(!empty());
	return *ranges::prev(ranges::end(_M_derived()));
      }

      constexpr decltype(auto)
      back() const
      requires bidirectional_range<const _Derived>
	&& common_range<const _Derived>
      {
	__glibcxx_assert(!empty());
	return *ranges::prev(ranges::end(_M_derived()));
      }

      template<random_access_range _Range = _Derived>
	constexpr decltype(auto)
	operator[](range_difference_t<_Range> __n)
	{ return ranges::begin(_M_derived())[__n]; }

      template<random_access_range _Range = const _Derived>
	constexpr decltype(auto)
	operator[](range_difference_t<_Range> __n) const
	{ return ranges::begin(_M_derived())[__n]; }
    };

  namespace __detail
  {
    template<typename _From, typename _To>
      concept __uses_nonqualification_pointer_conversion
	= is_pointer_v<_From> && is_pointer_v<_To>
	  && !convertible_to<remove_pointer_t<_From>(*)[],
			     remove_pointer_t<_To>(*)[]>;

    template<typename _From, typename _To>
      concept __convertible_to_non_slicing = convertible_to<_From, _To>
	&& !__uses_nonqualification_pointer_conversion<decay_t<_From>,
						       decay_t<_To>>;

    template<typename _Tp>
      concept __pair_like
	= !is_reference_v<_Tp> && requires(_Tp __t)
	{
	  typename tuple_size<_Tp>::type;
	  requires derived_from<tuple_size<_Tp>, integral_constant<size_t, 2>>;
	  typename tuple_element_t<0, remove_const_t<_Tp>>;
	  typename tuple_element_t<1, remove_const_t<_Tp>>;
	  { get<0>(__t) } -> convertible_to<const tuple_element_t<0, _Tp>&>;
	  { get<1>(__t) } -> convertible_to<const tuple_element_t<1, _Tp>&>;
	};

    template<typename _Tp, typename _Up, typename _Vp>
      concept __pair_like_convertible_from
	= !range<_Tp> && __pair_like<_Tp>
	&& constructible_from<_Tp, _Up, _Vp>
	&& __convertible_to_non_slicing<_Up, tuple_element_t<0, _Tp>>
	&& convertible_to<_Vp, tuple_element_t<1, _Tp>>;

  } // namespace __detail

  namespace views { struct _Drop; } // defined in <ranges>

  enum class subrange_kind : bool { unsized, sized };

  /// The ranges::subrange class template
  template<input_or_output_iterator _It, sentinel_for<_It> _Sent = _It,
	   subrange_kind _Kind = sized_sentinel_for<_Sent, _It>
	     ? subrange_kind::sized : subrange_kind::unsized>
    requires (_Kind == subrange_kind::sized || !sized_sentinel_for<_Sent, _It>)
    class subrange : public view_interface<subrange<_It, _Sent, _Kind>>
    {
    private:
      static constexpr bool _S_store_size
	= _Kind == subrange_kind::sized && !sized_sentinel_for<_Sent, _It>;

      friend struct views::_Drop; // Needs to inspect _S_store_size.

      _It _M_begin = _It();
      [[no_unique_address]] _Sent _M_end = _Sent();

      using __size_type
	= __detail::__make_unsigned_like_t<iter_difference_t<_It>>;

      template<typename, bool = _S_store_size>
	struct _Size
	{ };

      template<typename _Tp>
	struct _Size<_Tp, true>
	{ _Tp _M_size; };

      [[no_unique_address]] _Size<__size_type> _M_size = {};

    public:
      subrange() requires default_initializable<_It> = default;

      constexpr
      subrange(__detail::__convertible_to_non_slicing<_It> auto __i, _Sent __s)
      noexcept(is_nothrow_constructible_v<_It, decltype(__i)>
	       && is_nothrow_constructible_v<_Sent, _Sent&>)
	requires (!_S_store_size)
      : _M_begin(std::move(__i)), _M_end(__s)
      { }

      constexpr
      subrange(__detail::__convertible_to_non_slicing<_It> auto __i, _Sent __s,
	       __size_type __n)
      noexcept(is_nothrow_constructible_v<_It, decltype(__i)>
	       && is_nothrow_constructible_v<_Sent, _Sent&>)
	requires (_Kind == subrange_kind::sized)
      : _M_begin(std::move(__i)), _M_end(__s)
      {
	if constexpr (_S_store_size)
	  _M_size._M_size = __n;
      }

      template<__detail::__different_from<subrange> _Rng>
	requires borrowed_range<_Rng>
	  && __detail::__convertible_to_non_slicing<iterator_t<_Rng>, _It>
	  && convertible_to<sentinel_t<_Rng>, _Sent>
	constexpr
	subrange(_Rng&& __r)
	noexcept(noexcept(subrange(__r, ranges::size(__r))))
	requires _S_store_size && sized_range<_Rng>
	: subrange(__r, ranges::size(__r))
	{ }

      template<__detail::__different_from<subrange> _Rng>
	requires borrowed_range<_Rng>
	  && __detail::__convertible_to_non_slicing<iterator_t<_Rng>, _It>
	  && convertible_to<sentinel_t<_Rng>, _Sent>
	constexpr
	subrange(_Rng&& __r)
	noexcept(noexcept(subrange(ranges::begin(__r), ranges::end(__r))))
	requires (!_S_store_size)
	: subrange(ranges::begin(__r), ranges::end(__r))
	{ }

      template<borrowed_range _Rng>
	requires __detail::__convertible_to_non_slicing<iterator_t<_Rng>, _It>
	  && convertible_to<sentinel_t<_Rng>, _Sent>
	constexpr
	subrange(_Rng&& __r, __size_type __n)
	noexcept(noexcept(subrange(ranges::begin(__r), ranges::end(__r), __n)))
	requires (_Kind == subrange_kind::sized)
	: subrange{ranges::begin(__r), ranges::end(__r), __n}
	{ }

      template<__detail::__different_from<subrange> _PairLike>
	requires __detail::__pair_like_convertible_from<_PairLike, const _It&,
							const _Sent&>
	constexpr
	operator _PairLike() const
	{ return _PairLike(_M_begin, _M_end); }

      constexpr _It
      begin() const requires copyable<_It>
      { return _M_begin; }

      [[nodiscard]] constexpr _It
      begin() requires (!copyable<_It>)
      { return std::move(_M_begin); }

      constexpr _Sent end() const { return _M_end; }

      constexpr bool empty() const { return _M_begin == _M_end; }

      constexpr __size_type
      size() const requires (_Kind == subrange_kind::sized)
      {
	if constexpr (_S_store_size)
	  return _M_size._M_size;
	else
	  return __detail::__to_unsigned_like(_M_end - _M_begin);
      }

      [[nodiscard]] constexpr subrange
      next(iter_difference_t<_It> __n = 1) const &
	requires forward_iterator<_It>
      {
	auto __tmp = *this;
	__tmp.advance(__n);
	return __tmp;
      }

      [[nodiscard]] constexpr subrange
      next(iter_difference_t<_It> __n = 1) &&
      {
	advance(__n);
	return std::move(*this);
      }

      [[nodiscard]] constexpr subrange
      prev(iter_difference_t<_It> __n = 1) const
	requires bidirectional_iterator<_It>
      {
	auto __tmp = *this;
	__tmp.advance(-__n);
	return __tmp;
      }

      constexpr subrange&
      advance(iter_difference_t<_It> __n)
      {
	// _GLIBCXX_RESOLVE_LIB_DEFECTS
	// 3433. subrange::advance(n) has UB when n < 0
	if constexpr (bidirectional_iterator<_It>)
	  if (__n < 0)
	    {
	      ranges::advance(_M_begin, __n);
	      if constexpr (_S_store_size)
		_M_size._M_size += __detail::__to_unsigned_like(-__n);
	      return *this;
	    }

	__glibcxx_assert(__n >= 0);
	auto __d = __n - ranges::advance(_M_begin, __n, _M_end);
	if constexpr (_S_store_size)
	  _M_size._M_size -= __detail::__to_unsigned_like(__d);
	return *this;
      }
    };

  template<input_or_output_iterator _It, sentinel_for<_It> _Sent>
    subrange(_It, _Sent) -> subrange<_It, _Sent>;

  template<input_or_output_iterator _It, sentinel_for<_It> _Sent>
    subrange(_It, _Sent,
	     __detail::__make_unsigned_like_t<iter_difference_t<_It>>)
      -> subrange<_It, _Sent, subrange_kind::sized>;

  template<borrowed_range _Rng>
    subrange(_Rng&&)
      -> subrange<iterator_t<_Rng>, sentinel_t<_Rng>,
		 (sized_range<_Rng>
		  || sized_sentinel_for<sentinel_t<_Rng>, iterator_t<_Rng>>)
		 ? subrange_kind::sized : subrange_kind::unsized>;

  template<borrowed_range _Rng>
    subrange(_Rng&&,
	     __detail::__make_unsigned_like_t<range_difference_t<_Rng>>)
      -> subrange<iterator_t<_Rng>, sentinel_t<_Rng>, subrange_kind::sized>;

  template<size_t _Num, class _It, class _Sent, subrange_kind _Kind>
    requires (_Num < 2)
    constexpr auto
    get(const subrange<_It, _Sent, _Kind>& __r)
    {
      if constexpr (_Num == 0)
	return __r.begin();
      else
	return __r.end();
    }

  template<size_t _Num, class _It, class _Sent, subrange_kind _Kind>
    requires (_Num < 2)
    constexpr auto
    get(subrange<_It, _Sent, _Kind>&& __r)
    {
      if constexpr (_Num == 0)
	return __r.begin();
      else
	return __r.end();
    }

  template<typename _It, typename _Sent, subrange_kind _Kind>
    inline constexpr bool
      enable_borrowed_range<subrange<_It, _Sent, _Kind>> = true;

  template<range _Range>
    using borrowed_subrange_t = __conditional_t<borrowed_range<_Range>,
						subrange<iterator_t<_Range>>,
						dangling>;
} // namespace ranges

// The following ranges algorithms are used by <ranges>, and are defined here
// so that <ranges> can avoid including all of <bits/ranges_algo.h>.
namespace ranges
{
  struct __find_fn
  {
    template<input_iterator _Iter, sentinel_for<_Iter> _Sent, typename _Tp,
	     typename _Proj = identity>
      requires indirect_binary_predicate<ranges::equal_to,
					 projected<_Iter, _Proj>, const _Tp*>
      constexpr _Iter
      operator()(_Iter __first, _Sent __last,
		 const _Tp& __value, _Proj __proj = {}) const
      {
	while (__first != __last
	    && !(std::__invoke(__proj, *__first) == __value))
	  ++__first;
	return __first;
      }

    template<input_range _Range, typename _Tp, typename _Proj = identity>
      requires indirect_binary_predicate<ranges::equal_to,
					 projected<iterator_t<_Range>, _Proj>,
					 const _Tp*>
      constexpr borrowed_iterator_t<_Range>
      operator()(_Range&& __r, const _Tp& __value, _Proj __proj = {}) const
      {
	return (*this)(ranges::begin(__r), ranges::end(__r),
		       __value, std::move(__proj));
      }
  };

  inline constexpr __find_fn find{};

  struct __find_if_fn
  {
    template<input_iterator _Iter, sentinel_for<_Iter> _Sent,
	     typename _Proj = identity,
	     indirect_unary_predicate<projected<_Iter, _Proj>> _Pred>
      constexpr _Iter
      operator()(_Iter __first, _Sent __last,
		 _Pred __pred, _Proj __proj = {}) const
      {
	while (__first != __last
	    && !(bool)std::__invoke(__pred, std::__invoke(__proj, *__first)))
	  ++__first;
	return __first;
      }

    template<input_range _Range, typename _Proj = identity,
	     indirect_unary_predicate<projected<iterator_t<_Range>, _Proj>>
	       _Pred>
      constexpr borrowed_iterator_t<_Range>
      operator()(_Range&& __r, _Pred __pred, _Proj __proj = {}) const
      {
	return (*this)(ranges::begin(__r), ranges::end(__r),
		       std::move(__pred), std::move(__proj));
      }
  };

  inline constexpr __find_if_fn find_if{};

  struct __find_if_not_fn
  {
    template<input_iterator _Iter, sentinel_for<_Iter> _Sent,
	     typename _Proj = identity,
	     indirect_unary_predicate<projected<_Iter, _Proj>> _Pred>
      constexpr _Iter
      operator()(_Iter __first, _Sent __last,
		 _Pred __pred, _Proj __proj = {}) const
      {
	while (__first != __last
	    && (bool)std::__invoke(__pred, std::__invoke(__proj, *__first)))
	  ++__first;
	return __first;
      }

    template<input_range _Range, typename _Proj = identity,
	     indirect_unary_predicate<projected<iterator_t<_Range>, _Proj>>
	       _Pred>
      constexpr borrowed_iterator_t<_Range>
      operator()(_Range&& __r, _Pred __pred, _Proj __proj = {}) const
      {
	return (*this)(ranges::begin(__r), ranges::end(__r),
		       std::move(__pred), std::move(__proj));
      }
  };

  inline constexpr __find_if_not_fn find_if_not{};

  template<typename _Iter1, typename _Iter2>
    struct in_in_result
    {
      [[no_unique_address]] _Iter1 in1;
      [[no_unique_address]] _Iter2 in2;

      template<typename _IIter1, typename _IIter2>
	requires convertible_to<const _Iter1&, _IIter1>
	  && convertible_to<const _Iter2&, _IIter2>
	constexpr
	operator in_in_result<_IIter1, _IIter2>() const &
	{ return {in1, in2}; }

      template<typename _IIter1, typename _IIter2>
	requires convertible_to<_Iter1, _IIter1>
	  && convertible_to<_Iter2, _IIter2>
	constexpr
	operator in_in_result<_IIter1, _IIter2>() &&
	{ return {std::move(in1), std::move(in2)}; }
    };

  template<typename _Iter1, typename _Iter2>
    using mismatch_result = in_in_result<_Iter1, _Iter2>;

  struct __mismatch_fn
  {
    template<input_iterator _Iter1, sentinel_for<_Iter1> _Sent1,
	     input_iterator _Iter2, sentinel_for<_Iter2> _Sent2,
	     typename _Pred = ranges::equal_to,
	     typename _Proj1 = identity, typename _Proj2 = identity>
      requires indirectly_comparable<_Iter1, _Iter2, _Pred, _Proj1, _Proj2>
      constexpr mismatch_result<_Iter1, _Iter2>
      operator()(_Iter1 __first1, _Sent1 __last1,
		 _Iter2 __first2, _Sent2 __last2, _Pred __pred = {},
		 _Proj1 __proj1 = {}, _Proj2 __proj2 = {}) const
      {
	while (__first1 != __last1 && __first2 != __last2
	       && (bool)std::__invoke(__pred,
				      std::__invoke(__proj1, *__first1),
				      std::__invoke(__proj2, *__first2)))
	{
	  ++__first1;
	  ++__first2;
	}
	return { std::move(__first1), std::move(__first2) };
      }

    template<input_range _Range1, input_range _Range2,
	     typename _Pred = ranges::equal_to,
	     typename _Proj1 = identity, typename _Proj2 = identity>
      requires indirectly_comparable<iterator_t<_Range1>, iterator_t<_Range2>,
				     _Pred, _Proj1, _Proj2>
      constexpr mismatch_result<iterator_t<_Range1>, iterator_t<_Range2>>
      operator()(_Range1&& __r1, _Range2&& __r2, _Pred __pred = {},
		 _Proj1 __proj1 = {}, _Proj2 __proj2 = {}) const
      {
	return (*this)(ranges::begin(__r1), ranges::end(__r1),
		       ranges::begin(__r2), ranges::end(__r2),
		       std::move(__pred),
		       std::move(__proj1), std::move(__proj2));
      }
  };

  inline constexpr __mismatch_fn mismatch{};

  struct __search_fn
  {
    template<forward_iterator _Iter1, sentinel_for<_Iter1> _Sent1,
	     forward_iterator _Iter2, sentinel_for<_Iter2> _Sent2,
	     typename _Pred = ranges::equal_to,
	     typename _Proj1 = identity, typename _Proj2 = identity>
      requires indirectly_comparable<_Iter1, _Iter2, _Pred, _Proj1, _Proj2>
      constexpr subrange<_Iter1>
      operator()(_Iter1 __first1, _Sent1 __last1,
		 _Iter2 __first2, _Sent2 __last2, _Pred __pred = {},
		 _Proj1 __proj1 = {}, _Proj2 __proj2 = {}) const
      {
	if (__first1 == __last1 || __first2 == __last2)
	  return {__first1, __first1};

	for (;;)
	  {
	    for (;;)
	      {
		if (__first1 == __last1)
		  return {__first1, __first1};
		if (std::__invoke(__pred,
				  std::__invoke(__proj1, *__first1),
				  std::__invoke(__proj2, *__first2)))
		  break;
		++__first1;
	      }
	    auto __cur1 = __first1;
	    auto __cur2 = __first2;
	    for (;;)
	      {
		if (++__cur2 == __last2)
		  return {__first1, ++__cur1};
		if (++__cur1 == __last1)
		  return {__cur1, __cur1};
		if (!(bool)std::__invoke(__pred,
					 std::__invoke(__proj1, *__cur1),
					 std::__invoke(__proj2, *__cur2)))
		  {
		    ++__first1;
		    break;
		  }
	      }
	  }
      }

    template<forward_range _Range1, forward_range _Range2,
	     typename _Pred = ranges::equal_to,
	     typename _Proj1 = identity, typename _Proj2 = identity>
      requires indirectly_comparable<iterator_t<_Range1>, iterator_t<_Range2>,
				     _Pred, _Proj1, _Proj2>
      constexpr borrowed_subrange_t<_Range1>
      operator()(_Range1&& __r1, _Range2&& __r2, _Pred __pred = {},
		 _Proj1 __proj1 = {}, _Proj2 __proj2 = {}) const
      {
	return (*this)(ranges::begin(__r1), ranges::end(__r1),
		       ranges::begin(__r2), ranges::end(__r2),
		       std::move(__pred),
		       std::move(__proj1), std::move(__proj2));
      }
  };

  inline constexpr __search_fn search{};
} // namespace ranges

  using ranges::get;

  template<typename _Iter, typename _Sent, ranges::subrange_kind _Kind>
    struct tuple_size<ranges::subrange<_Iter, _Sent, _Kind>>
    : integral_constant<size_t, 2>
    { };

  template<typename _Iter, typename _Sent, ranges::subrange_kind _Kind>
    struct tuple_element<0, ranges::subrange<_Iter, _Sent, _Kind>>
    { using type = _Iter; };

  template<typename _Iter, typename _Sent, ranges::subrange_kind _Kind>
    struct tuple_element<1, ranges::subrange<_Iter, _Sent, _Kind>>
    { using type = _Sent; };

  template<typename _Iter, typename _Sent, ranges::subrange_kind _Kind>
    struct tuple_element<0, const ranges::subrange<_Iter, _Sent, _Kind>>
    { using type = _Iter; };

  template<typename _Iter, typename _Sent, ranges::subrange_kind _Kind>
    struct tuple_element<1, const ranges::subrange<_Iter, _Sent, _Kind>>
    { using type = _Sent; };

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std
#endif // library concepts
#endif // C++20
#endif // _RANGES_UTIL_H
