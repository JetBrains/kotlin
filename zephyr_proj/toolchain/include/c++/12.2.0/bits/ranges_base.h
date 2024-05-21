// Core concepts and definitions for <ranges> -*- C++ -*-

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

/** @file bits/ranges_base.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{ranges}
 */

#ifndef _GLIBCXX_RANGES_BASE_H
#define _GLIBCXX_RANGES_BASE_H 1

#pragma GCC system_header

#if __cplusplus > 201703L
#include <bits/iterator_concepts.h>
#include <ext/numeric_traits.h>
#include <bits/max_size_type.h>

#ifdef __cpp_lib_concepts
namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION
namespace ranges
{
  template<typename>
    inline constexpr bool disable_sized_range = false;

  template<typename _Tp>
    inline constexpr bool enable_borrowed_range = false;

  namespace __detail
  {
    constexpr __max_size_type
    __to_unsigned_like(__max_size_type __t) noexcept
    { return __t; }

    constexpr __max_size_type
    __to_unsigned_like(__max_diff_type __t) noexcept
    { return __max_size_type(__t); }

    template<integral _Tp>
      constexpr auto
      __to_unsigned_like(_Tp __t) noexcept
      { return static_cast<make_unsigned_t<_Tp>>(__t); }

#if defined __STRICT_ANSI__ && defined __SIZEOF_INT128__
    constexpr unsigned __int128
    __to_unsigned_like(__int128 __t) noexcept
    { return __t; }

    constexpr unsigned __int128
    __to_unsigned_like(unsigned __int128 __t) noexcept
    { return __t; }
#endif

    template<typename _Tp>
      using __make_unsigned_like_t
	= decltype(__detail::__to_unsigned_like(std::declval<_Tp>()));

    // Part of the constraints of ranges::borrowed_range
    template<typename _Tp>
      concept __maybe_borrowed_range
	= is_lvalue_reference_v<_Tp>
	  || enable_borrowed_range<remove_cvref_t<_Tp>>;

  } // namespace __detail

  namespace __cust_access
  {
    using std::ranges::__detail::__maybe_borrowed_range;
    using std::__detail::__range_iter_t;

    struct _Begin
    {
    private:
      template<typename _Tp>
	static constexpr bool
	_S_noexcept()
	{
	  if constexpr (is_array_v<remove_reference_t<_Tp>>)
	    return true;
	  else if constexpr (__member_begin<_Tp>)
	    return noexcept(__decay_copy(std::declval<_Tp&>().begin()));
	  else
	    return noexcept(__decay_copy(begin(std::declval<_Tp&>())));
	}

    public:
      template<__maybe_borrowed_range _Tp>
	requires is_array_v<remove_reference_t<_Tp>> || __member_begin<_Tp>
	  || __adl_begin<_Tp>
	constexpr auto
	operator()[[nodiscard]](_Tp&& __t) const noexcept(_S_noexcept<_Tp&>())
	{
	  if constexpr (is_array_v<remove_reference_t<_Tp>>)
	    {
	      static_assert(is_lvalue_reference_v<_Tp>);
	      return __t + 0;
	    }
	  else if constexpr (__member_begin<_Tp>)
	    return __t.begin();
	  else
	    return begin(__t);
	}
    };

    template<typename _Tp>
      concept __member_end = requires(_Tp& __t)
	{
	  { __decay_copy(__t.end()) } -> sentinel_for<__range_iter_t<_Tp>>;
	};

    // Poison pills so that unqualified lookup doesn't find std::end.
    void end(auto&) = delete;
    void end(const auto&) = delete;

    template<typename _Tp>
      concept __adl_end = __class_or_enum<remove_reference_t<_Tp>>
	&& requires(_Tp& __t)
	{
	  { __decay_copy(end(__t)) } -> sentinel_for<__range_iter_t<_Tp>>;
	};

    struct _End
    {
    private:
      template<typename _Tp>
	static constexpr bool
	_S_noexcept()
	{
	  if constexpr (is_bounded_array_v<remove_reference_t<_Tp>>)
	    return true;
	  else if constexpr (__member_end<_Tp>)
	    return noexcept(__decay_copy(std::declval<_Tp&>().end()));
	  else
	    return noexcept(__decay_copy(end(std::declval<_Tp&>())));
	}

    public:
      template<__maybe_borrowed_range _Tp>
	requires is_bounded_array_v<remove_reference_t<_Tp>>
	  || __member_end<_Tp> || __adl_end<_Tp>
	constexpr auto
	operator()[[nodiscard]](_Tp&& __t) const noexcept(_S_noexcept<_Tp&>())
	{
	  if constexpr (is_bounded_array_v<remove_reference_t<_Tp>>)
	    {
	      static_assert(is_lvalue_reference_v<_Tp>);
	      return __t + extent_v<remove_reference_t<_Tp>>;
	    }
	  else if constexpr (__member_end<_Tp>)
	    return __t.end();
	  else
	    return end(__t);
	}
    };

    // If _To is an lvalue-reference, return const _Tp&, otherwise const _Tp&&.
    template<typename _To, typename _Tp>
      constexpr decltype(auto)
      __as_const(_Tp& __t) noexcept
      {
	static_assert(std::is_same_v<_To&, _Tp&>);

	if constexpr (is_lvalue_reference_v<_To>)
	  return const_cast<const _Tp&>(__t);
	else
	  return static_cast<const _Tp&&>(__t);
      }

    struct _CBegin
    {
      template<typename _Tp>
	[[nodiscard]]
	constexpr auto
	operator()(_Tp&& __e) const
	noexcept(noexcept(_Begin{}(__cust_access::__as_const<_Tp>(__e))))
	requires requires { _Begin{}(__cust_access::__as_const<_Tp>(__e)); }
	{
	  return _Begin{}(__cust_access::__as_const<_Tp>(__e));
	}
    };

    struct _CEnd final
    {
      template<typename _Tp>
	[[nodiscard]]
	constexpr auto
	operator()(_Tp&& __e) const
	noexcept(noexcept(_End{}(__cust_access::__as_const<_Tp>(__e))))
	requires requires { _End{}(__cust_access::__as_const<_Tp>(__e)); }
	{
	  return _End{}(__cust_access::__as_const<_Tp>(__e));
	}
    };

    template<typename _Tp>
      concept __member_rbegin = requires(_Tp& __t)
	{
	  { __decay_copy(__t.rbegin()) } -> input_or_output_iterator;
	};

    void rbegin(auto&) = delete;
    void rbegin(const auto&) = delete;

    template<typename _Tp>
      concept __adl_rbegin = __class_or_enum<remove_reference_t<_Tp>>
	&& requires(_Tp& __t)
	{
	  { __decay_copy(rbegin(__t)) } -> input_or_output_iterator;
	};

    template<typename _Tp>
      concept __reversable = requires(_Tp& __t)
	{
	  { _Begin{}(__t) } -> bidirectional_iterator;
	  { _End{}(__t) } -> same_as<decltype(_Begin{}(__t))>;
	};

    struct _RBegin
    {
    private:
      template<typename _Tp>
	static constexpr bool
	_S_noexcept()
	{
	  if constexpr (__member_rbegin<_Tp>)
	    return noexcept(__decay_copy(std::declval<_Tp&>().rbegin()));
	  else if constexpr (__adl_rbegin<_Tp>)
	    return noexcept(__decay_copy(rbegin(std::declval<_Tp&>())));
	  else
	    {
	      if constexpr (noexcept(_End{}(std::declval<_Tp&>())))
		{
		  using _It = decltype(_End{}(std::declval<_Tp&>()));
		  // std::reverse_iterator copy-initializes its member.
		  return is_nothrow_copy_constructible_v<_It>;
		}
	      else
		return false;
	    }
	}

    public:
      template<__maybe_borrowed_range _Tp>
	requires __member_rbegin<_Tp> || __adl_rbegin<_Tp> || __reversable<_Tp>
	constexpr auto
	operator()[[nodiscard]](_Tp&& __t) const
	noexcept(_S_noexcept<_Tp&>())
	{
	  if constexpr (__member_rbegin<_Tp>)
	    return __t.rbegin();
	  else if constexpr (__adl_rbegin<_Tp>)
	    return rbegin(__t);
	  else
	    return std::make_reverse_iterator(_End{}(__t));
	}
    };

    template<typename _Tp>
      concept __member_rend = requires(_Tp& __t)
	{
	  { __decay_copy(__t.rend()) }
	    -> sentinel_for<decltype(_RBegin{}(std::forward<_Tp>(__t)))>;
	};

    void rend(auto&) = delete;
    void rend(const auto&) = delete;

    template<typename _Tp>
      concept __adl_rend = __class_or_enum<remove_reference_t<_Tp>>
	&& requires(_Tp& __t)
	{
	  { __decay_copy(rend(__t)) }
	    -> sentinel_for<decltype(_RBegin{}(std::forward<_Tp>(__t)))>;
	};

    struct _REnd
    {
    private:
      template<typename _Tp>
	static constexpr bool
	_S_noexcept()
	{
	  if constexpr (__member_rend<_Tp>)
	    return noexcept(__decay_copy(std::declval<_Tp&>().rend()));
	  else if constexpr (__adl_rend<_Tp>)
	    return noexcept(__decay_copy(rend(std::declval<_Tp&>())));
	  else
	    {
	      if constexpr (noexcept(_Begin{}(std::declval<_Tp&>())))
		{
		  using _It = decltype(_Begin{}(std::declval<_Tp&>()));
		  // std::reverse_iterator copy-initializes its member.
		  return is_nothrow_copy_constructible_v<_It>;
		}
	      else
		return false;
	    }
	}

    public:
      template<__maybe_borrowed_range _Tp>
	requires __member_rend<_Tp> || __adl_rend<_Tp> || __reversable<_Tp>
	constexpr auto
	operator()[[nodiscard]](_Tp&& __t) const
	noexcept(_S_noexcept<_Tp&>())
	{
	  if constexpr (__member_rend<_Tp>)
	    return __t.rend();
	  else if constexpr (__adl_rend<_Tp>)
	    return rend(__t);
	  else
	    return std::make_reverse_iterator(_Begin{}(__t));
	}
    };

    struct _CRBegin
    {
      template<typename _Tp>
	[[nodiscard]]
	constexpr auto
	operator()(_Tp&& __e) const
	noexcept(noexcept(_RBegin{}(__cust_access::__as_const<_Tp>(__e))))
	requires requires { _RBegin{}(__cust_access::__as_const<_Tp>(__e)); }
	{
	  return _RBegin{}(__cust_access::__as_const<_Tp>(__e));
	}
    };

    struct _CREnd
    {
      template<typename _Tp>
	[[nodiscard]]
	constexpr auto
	operator()(_Tp&& __e) const
	noexcept(noexcept(_REnd{}(__cust_access::__as_const<_Tp>(__e))))
	requires requires { _REnd{}(__cust_access::__as_const<_Tp>(__e)); }
	{
	  return _REnd{}(__cust_access::__as_const<_Tp>(__e));
	}
    };

    template<typename _Tp>
      concept __member_size = !disable_sized_range<remove_cvref_t<_Tp>>
	&& requires(_Tp& __t)
	{
	  { __decay_copy(__t.size()) } -> __detail::__is_integer_like;
	};

    void size(auto&) = delete;
    void size(const auto&) = delete;

    template<typename _Tp>
      concept __adl_size = __class_or_enum<remove_reference_t<_Tp>>
	&& !disable_sized_range<remove_cvref_t<_Tp>>
	&& requires(_Tp& __t)
	{
	  { __decay_copy(size(__t)) } -> __detail::__is_integer_like;
	};

    template<typename _Tp>
      concept __sentinel_size = requires(_Tp& __t)
	{
	  requires (!is_unbounded_array_v<remove_reference_t<_Tp>>);

	  { _Begin{}(__t) } -> forward_iterator;

	  { _End{}(__t) } -> sized_sentinel_for<decltype(_Begin{}(__t))>;

	  __detail::__to_unsigned_like(_End{}(__t) - _Begin{}(__t));
	};

    struct _Size
    {
    private:
      template<typename _Tp>
	static constexpr bool
	_S_noexcept()
	{
	  if constexpr (is_bounded_array_v<remove_reference_t<_Tp>>)
	    return true;
	  else if constexpr (__member_size<_Tp>)
	    return noexcept(__decay_copy(std::declval<_Tp&>().size()));
	  else if constexpr (__adl_size<_Tp>)
	    return noexcept(__decay_copy(size(std::declval<_Tp&>())));
	  else if constexpr (__sentinel_size<_Tp>)
	    return noexcept(_End{}(std::declval<_Tp&>())
			    - _Begin{}(std::declval<_Tp&>()));
	}

    public:
      template<typename _Tp>
	requires is_bounded_array_v<remove_reference_t<_Tp>>
	  || __member_size<_Tp> || __adl_size<_Tp> || __sentinel_size<_Tp>
	constexpr auto
	operator()[[nodiscard]](_Tp&& __t) const noexcept(_S_noexcept<_Tp&>())
	{
	  if constexpr (is_bounded_array_v<remove_reference_t<_Tp>>)
	    return extent_v<remove_reference_t<_Tp>>;
	  else if constexpr (__member_size<_Tp>)
	    return __t.size();
	  else if constexpr (__adl_size<_Tp>)
	    return size(__t);
	  else if constexpr (__sentinel_size<_Tp>)
	    return __detail::__to_unsigned_like(_End{}(__t) - _Begin{}(__t));
	}
    };

    struct _SSize
    {
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 3403. Domain of ranges::ssize(E) doesn't match ranges::size(E)
      template<typename _Tp>
	requires requires (_Tp& __t) { _Size{}(__t); }
	constexpr auto
	operator()[[nodiscard]](_Tp&& __t) const noexcept(noexcept(_Size{}(__t)))
	{
	  auto __size = _Size{}(__t);
	  using __size_type = decltype(__size);
	  // Return the wider of ptrdiff_t and make-signed-like-t<__size_type>.
	  if constexpr (integral<__size_type>)
	    {
	      using __gnu_cxx::__int_traits;
	      if constexpr (__int_traits<__size_type>::__digits
			    < __int_traits<ptrdiff_t>::__digits)
		return static_cast<ptrdiff_t>(__size);
	      else
		return static_cast<make_signed_t<__size_type>>(__size);
	    }
#if defined __STRICT_ANSI__ && defined __SIZEOF_INT128__
	  // For strict-ansi modes integral<__int128> is false
	  else if constexpr (__detail::__is_int128<__size_type>)
	    return static_cast<__int128>(__size);
#endif
	  else // Must be one of __max_diff_type or __max_size_type.
	    return __detail::__max_diff_type(__size);
	}
    };

    template<typename _Tp>
      concept __member_empty = requires(_Tp& __t) { bool(__t.empty()); };

    template<typename _Tp>
      concept __size0_empty = requires(_Tp& __t) { _Size{}(__t) == 0; };

    template<typename _Tp>
      concept __eq_iter_empty = requires(_Tp& __t)
	{
	  requires (!is_unbounded_array_v<remove_reference_t<_Tp>>);

	  { _Begin{}(__t) } -> forward_iterator;

	  bool(_Begin{}(__t) == _End{}(__t));
	};

    struct _Empty
    {
    private:
      template<typename _Tp>
	static constexpr bool
	_S_noexcept()
	{
	  if constexpr (__member_empty<_Tp>)
	    return noexcept(bool(std::declval<_Tp&>().empty()));
	  else if constexpr (__size0_empty<_Tp>)
	    return noexcept(_Size{}(std::declval<_Tp&>()) == 0);
	  else
	    return noexcept(bool(_Begin{}(std::declval<_Tp&>())
		== _End{}(std::declval<_Tp&>())));
	}

    public:
      template<typename _Tp>
	requires __member_empty<_Tp> || __size0_empty<_Tp>
	  || __eq_iter_empty<_Tp>
	constexpr bool
	operator()[[nodiscard]](_Tp&& __t) const noexcept(_S_noexcept<_Tp&>())
	{
	  if constexpr (__member_empty<_Tp>)
	    return bool(__t.empty());
	  else if constexpr (__size0_empty<_Tp>)
	    return _Size{}(__t) == 0;
	  else
	    return bool(_Begin{}(__t) == _End{}(__t));
	}
    };

    template<typename _Tp>
      concept __pointer_to_object = is_pointer_v<_Tp>
				    && is_object_v<remove_pointer_t<_Tp>>;

    template<typename _Tp>
      concept __member_data = requires(_Tp& __t)
	{
	  { __decay_copy(__t.data()) } -> __pointer_to_object;
	};

    template<typename _Tp>
      concept __begin_data = contiguous_iterator<__range_iter_t<_Tp>>;

    struct _Data
    {
    private:
      template<typename _Tp>
	static constexpr bool
	_S_noexcept()
	{
	  if constexpr (__member_data<_Tp>)
	    return noexcept(__decay_copy(std::declval<_Tp&>().data()));
	  else
	    return noexcept(_Begin{}(std::declval<_Tp&>()));
	}

    public:
      template<__maybe_borrowed_range _Tp>
	requires __member_data<_Tp> || __begin_data<_Tp>
	constexpr auto
	operator()[[nodiscard]](_Tp&& __t) const noexcept(_S_noexcept<_Tp>())
	{
	  if constexpr (__member_data<_Tp>)
	    return __t.data();
	  else
	    return std::to_address(_Begin{}(__t));
	}
    };

    struct _CData
    {
      template<typename _Tp>
	[[nodiscard]]
	constexpr auto
	operator()(_Tp&& __e) const
	noexcept(noexcept(_Data{}(__cust_access::__as_const<_Tp>(__e))))
	requires requires { _Data{}(__cust_access::__as_const<_Tp>(__e)); }
	{
	  return _Data{}(__cust_access::__as_const<_Tp>(__e));
	}
    };

  } // namespace __cust_access

  inline namespace __cust
  {
    inline constexpr __cust_access::_Begin begin{};
    inline constexpr __cust_access::_End end{};
    inline constexpr __cust_access::_CBegin cbegin{};
    inline constexpr __cust_access::_CEnd cend{};
    inline constexpr __cust_access::_RBegin rbegin{};
    inline constexpr __cust_access::_REnd rend{};
    inline constexpr __cust_access::_CRBegin crbegin{};
    inline constexpr __cust_access::_CREnd crend{};
    inline constexpr __cust_access::_Size size{};
    inline constexpr __cust_access::_SSize ssize{};
    inline constexpr __cust_access::_Empty empty{};
    inline constexpr __cust_access::_Data data{};
    inline constexpr __cust_access::_CData cdata{};
  }

  /// [range.range] The range concept.
  template<typename _Tp>
    concept range = requires(_Tp& __t)
      {
	ranges::begin(__t);
	ranges::end(__t);
      };

  /// [range.range] The borrowed_range concept.
  template<typename _Tp>
    concept borrowed_range
      = range<_Tp> && __detail::__maybe_borrowed_range<_Tp>;

  template<typename _Tp>
    using iterator_t = std::__detail::__range_iter_t<_Tp>;

  template<range _Range>
    using sentinel_t = decltype(ranges::end(std::declval<_Range&>()));

  template<range _Range>
    using range_difference_t = iter_difference_t<iterator_t<_Range>>;

  template<range _Range>
    using range_value_t = iter_value_t<iterator_t<_Range>>;

  template<range _Range>
    using range_reference_t = iter_reference_t<iterator_t<_Range>>;

  template<range _Range>
    using range_rvalue_reference_t
      = iter_rvalue_reference_t<iterator_t<_Range>>;

  /// [range.sized] The sized_range concept.
  template<typename _Tp>
    concept sized_range = range<_Tp>
      && requires(_Tp& __t) { ranges::size(__t); };

  template<sized_range _Range>
    using range_size_t = decltype(ranges::size(std::declval<_Range&>()));

  template<typename _Derived>
    requires is_class_v<_Derived> && same_as<_Derived, remove_cv_t<_Derived>>
    class view_interface; // defined in <bits/ranges_util.h>

  namespace __detail
  {
    template<typename _Tp, typename _Up>
      requires (!same_as<_Tp, view_interface<_Up>>)
      void __is_derived_from_view_interface_fn(const _Tp&,
					       const view_interface<_Up>&); // not defined

    // Returns true iff _Tp has exactly one public base class that's a
    // specialization of view_interface.
    template<typename _Tp>
      concept __is_derived_from_view_interface
	= requires (_Tp __t) { __is_derived_from_view_interface_fn(__t, __t); };
  } // namespace __detail

  /// [range.view] The ranges::view_base type.
  struct view_base { };

  /// [range.view] The ranges::enable_view boolean.
  template<typename _Tp>
    inline constexpr bool enable_view = derived_from<_Tp, view_base>
      || __detail::__is_derived_from_view_interface<_Tp>;

  /// [range.view] The ranges::view concept.
  template<typename _Tp>
    concept view
      = range<_Tp> && movable<_Tp> && enable_view<_Tp>;

  // [range.refinements]

  /// A range for which ranges::begin returns an output iterator.
  template<typename _Range, typename _Tp>
    concept output_range
      = range<_Range> && output_iterator<iterator_t<_Range>, _Tp>;

  /// A range for which ranges::begin returns an input iterator.
  template<typename _Tp>
    concept input_range = range<_Tp> && input_iterator<iterator_t<_Tp>>;

  /// A range for which ranges::begin returns a forward iterator.
  template<typename _Tp>
    concept forward_range
      = input_range<_Tp> && forward_iterator<iterator_t<_Tp>>;

  /// A range for which ranges::begin returns a bidirectional iterator.
  template<typename _Tp>
    concept bidirectional_range
      = forward_range<_Tp> && bidirectional_iterator<iterator_t<_Tp>>;

  /// A range for which ranges::begin returns a random access iterator.
  template<typename _Tp>
    concept random_access_range
      = bidirectional_range<_Tp> && random_access_iterator<iterator_t<_Tp>>;

  /// A range for which ranges::begin returns a contiguous iterator.
  template<typename _Tp>
    concept contiguous_range
      = random_access_range<_Tp> && contiguous_iterator<iterator_t<_Tp>>
      && requires(_Tp& __t)
      {
	{ ranges::data(__t) } -> same_as<add_pointer_t<range_reference_t<_Tp>>>;
      };

  /// A range for which ranges::begin and ranges::end return the same type.
  template<typename _Tp>
    concept common_range
      = range<_Tp> && same_as<iterator_t<_Tp>, sentinel_t<_Tp>>;

  namespace __detail
  {
    template<typename _Tp>
      inline constexpr bool __is_initializer_list = false;

    template<typename _Tp>
      inline constexpr bool __is_initializer_list<initializer_list<_Tp>> = true;
  } // namespace __detail

  /// A range which can be safely converted to a view.
  template<typename _Tp>
    concept viewable_range = range<_Tp>
      && ((view<remove_cvref_t<_Tp>> && constructible_from<remove_cvref_t<_Tp>, _Tp>)
	  || (!view<remove_cvref_t<_Tp>>
	      && (is_lvalue_reference_v<_Tp>
		  || (movable<remove_reference_t<_Tp>>
		      && !__detail::__is_initializer_list<remove_cvref_t<_Tp>>))));

  // [range.iter.ops] range iterator operations

  struct __advance_fn final
  {
    template<input_or_output_iterator _It>
      constexpr void
      operator()(_It& __it, iter_difference_t<_It> __n) const
      {
	if constexpr (random_access_iterator<_It>)
	  __it += __n;
	else if constexpr (bidirectional_iterator<_It>)
	  {
	    if (__n > 0)
	      {
		do
		  {
		    ++__it;
		  }
		while (--__n);
	      }
	    else if (__n < 0)
	      {
		do
		  {
		    --__it;
		  }
		while (++__n);
	      }
	  }
	else
	  {
	    // cannot decrement a non-bidirectional iterator
	    __glibcxx_assert(__n >= 0);
	    while (__n-- > 0)
	      ++__it;
	  }
      }

    template<input_or_output_iterator _It, sentinel_for<_It> _Sent>
      constexpr void
      operator()(_It& __it, _Sent __bound) const
      {
	if constexpr (assignable_from<_It&, _Sent>)
	  __it = std::move(__bound);
	else if constexpr (sized_sentinel_for<_Sent, _It>)
	  (*this)(__it, __bound - __it);
	else
	  {
	    while (__it != __bound)
	      ++__it;
	  }
      }

    template<input_or_output_iterator _It, sentinel_for<_It> _Sent>
      constexpr iter_difference_t<_It>
      operator()(_It& __it, iter_difference_t<_It> __n, _Sent __bound) const
      {
	if constexpr (sized_sentinel_for<_Sent, _It>)
	  {
	    const auto __diff = __bound - __it;

	    if (__diff == 0)
	      return __n;
	    else if (__diff > 0 ? __n >= __diff : __n <= __diff)
	      {
		(*this)(__it, __bound);
		return __n - __diff;
	      }
	    else if (__n != 0) [[likely]]
	      {
		// n and bound must not lead in opposite directions:
		__glibcxx_assert(__n < 0 == __diff < 0);

		(*this)(__it, __n);
		return 0;
	      }
	    else
	      return 0;
	  }
	else if (__it == __bound || __n == 0)
	  return __n;
	else if (__n > 0)
	  {
	    iter_difference_t<_It> __m = 0;
	    do
	      {
		++__it;
		++__m;
	      }
	    while (__m != __n && __it != __bound);
	    return __n - __m;
	  }
	else if constexpr (bidirectional_iterator<_It> && same_as<_It, _Sent>)
	  {
	    iter_difference_t<_It> __m = 0;
	    do
	      {
		--__it;
		--__m;
	      }
	    while (__m != __n && __it != __bound);
	    return __n - __m;
	  }
	else
	  {
	    // cannot decrement a non-bidirectional iterator
	    __glibcxx_assert(__n >= 0);
	    return __n;
	  }
      }

    void operator&() const = delete;
  };

  inline constexpr __advance_fn advance{};

  struct __distance_fn final
  {
    template<input_or_output_iterator _It, sentinel_for<_It> _Sent>
      requires (!sized_sentinel_for<_Sent, _It>)
      constexpr iter_difference_t<_It>
      operator()[[nodiscard]](_It __first, _Sent __last) const
      {
	iter_difference_t<_It> __n = 0;
	while (__first != __last)
	  {
	    ++__first;
	    ++__n;
	  }
	return __n;
      }

    template<input_or_output_iterator _It, sized_sentinel_for<_It> _Sent>
      [[nodiscard]]
      constexpr iter_difference_t<_It>
      operator()(const _It& __first, const _Sent& __last) const
      {
	return __last - __first;
      }

    template<range _Range>
      [[nodiscard]]
      constexpr range_difference_t<_Range>
      operator()(_Range&& __r) const
      {
	if constexpr (sized_range<_Range>)
	  return static_cast<range_difference_t<_Range>>(ranges::size(__r));
	else
	  return (*this)(ranges::begin(__r), ranges::end(__r));
      }

    void operator&() const = delete;
  };

  inline constexpr __distance_fn distance{};

  struct __next_fn final
  {
    template<input_or_output_iterator _It>
      [[nodiscard]]
      constexpr _It
      operator()(_It __x) const
      {
	++__x;
	return __x;
      }

    template<input_or_output_iterator _It>
      [[nodiscard]]
      constexpr _It
      operator()(_It __x, iter_difference_t<_It> __n) const
      {
	ranges::advance(__x, __n);
	return __x;
      }

    template<input_or_output_iterator _It, sentinel_for<_It> _Sent>
      [[nodiscard]]
      constexpr _It
      operator()(_It __x, _Sent __bound) const
      {
	ranges::advance(__x, __bound);
	return __x;
      }

    template<input_or_output_iterator _It, sentinel_for<_It> _Sent>
      [[nodiscard]]
      constexpr _It
      operator()(_It __x, iter_difference_t<_It> __n, _Sent __bound) const
      {
	ranges::advance(__x, __n, __bound);
	return __x;
      }

    void operator&() const = delete;
  };

  inline constexpr __next_fn next{};

  struct __prev_fn final
  {
    template<bidirectional_iterator _It>
      [[nodiscard]]
      constexpr _It
      operator()(_It __x) const
      {
	--__x;
	return __x;
      }

    template<bidirectional_iterator _It>
      [[nodiscard]]
      constexpr _It
      operator()(_It __x, iter_difference_t<_It> __n) const
      {
	ranges::advance(__x, -__n);
	return __x;
      }

    template<bidirectional_iterator _It>
      [[nodiscard]]
      constexpr _It
      operator()(_It __x, iter_difference_t<_It> __n, _It __bound) const
      {
	ranges::advance(__x, -__n, __bound);
	return __x;
      }

    void operator&() const = delete;
  };

  inline constexpr __prev_fn prev{};

  /// Type returned by algorithms instead of a dangling iterator or subrange.
  struct dangling
  {
    constexpr dangling() noexcept = default;
    template<typename... _Args>
      constexpr dangling(_Args&&...) noexcept { }
  };

  template<range _Range>
    using borrowed_iterator_t = __conditional_t<borrowed_range<_Range>,
						iterator_t<_Range>,
						dangling>;

} // namespace ranges
_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std
#endif // library concepts
#endif // C++20
#endif // _GLIBCXX_RANGES_BASE_H
