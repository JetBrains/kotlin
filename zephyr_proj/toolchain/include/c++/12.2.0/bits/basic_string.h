// Components for manipulating sequences of characters -*- C++ -*-

// Copyright (C) 1997-2022 Free Software Foundation, Inc.
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

/** @file bits/basic_string.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{string}
 */

//
// ISO C++ 14882: 21 Strings library
//

#ifndef _BASIC_STRING_H
#define _BASIC_STRING_H 1

#pragma GCC system_header

#include <ext/alloc_traits.h>
#include <debug/debug.h>

#if __cplusplus >= 201103L
#include <initializer_list>
#endif

#if __cplusplus >= 201703L
# include <string_view>
#endif

#if ! _GLIBCXX_USE_CXX11_ABI
# include "cow_string.h"
#else
namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION
_GLIBCXX_BEGIN_NAMESPACE_CXX11

#ifdef __cpp_lib_is_constant_evaluated
// Support P0980R1 in C++20.
# define __cpp_lib_constexpr_string 201907L
#elif __cplusplus >= 201703L && _GLIBCXX_HAVE_IS_CONSTANT_EVALUATED
// Support P0426R1 changes to char_traits in C++17.
# define __cpp_lib_constexpr_string 201611L
#endif

  /**
   *  @class basic_string basic_string.h <string>
   *  @brief  Managing sequences of characters and character-like objects.
   *
   *  @ingroup strings
   *  @ingroup sequences
   *
   *  @tparam _CharT  Type of character
   *  @tparam _Traits  Traits for character type, defaults to
   *                   char_traits<_CharT>.
   *  @tparam _Alloc  Allocator type, defaults to allocator<_CharT>.
   *
   *  Meets the requirements of a <a href="tables.html#65">container</a>, a
   *  <a href="tables.html#66">reversible container</a>, and a
   *  <a href="tables.html#67">sequence</a>.  Of the
   *  <a href="tables.html#68">optional sequence requirements</a>, only
   *  @c push_back, @c at, and @c %array access are supported.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    class basic_string
    {
      typedef typename __gnu_cxx::__alloc_traits<_Alloc>::template
	rebind<_CharT>::other _Char_alloc_type;

#if __cpp_lib_constexpr_string < 201907L
      typedef __gnu_cxx::__alloc_traits<_Char_alloc_type> _Alloc_traits;
#else
      template<typename _Traits2, typename _Dummy_for_PR85282>
	struct _Alloc_traits_impl : __gnu_cxx::__alloc_traits<_Char_alloc_type>
	{
	  typedef __gnu_cxx::__alloc_traits<_Char_alloc_type> _Base;

	  [[__gnu__::__always_inline__]]
	  static constexpr typename _Base::pointer
	  allocate(_Char_alloc_type& __a, typename _Base::size_type __n)
	  {
	    pointer __p = _Base::allocate(__a, __n);
	    if (std::is_constant_evaluated())
	      // Begin the lifetime of characters in allocated storage.
	      for (size_type __i = 0; __i < __n; ++__i)
		std::construct_at(__builtin_addressof(__p[__i]));
	    return __p;
	  }
	};

      template<typename _Dummy_for_PR85282>
	struct _Alloc_traits_impl<char_traits<_CharT>, _Dummy_for_PR85282>
	: __gnu_cxx::__alloc_traits<_Char_alloc_type>
	{
	  // std::char_traits begins the lifetime of characters.
	};

      using _Alloc_traits = _Alloc_traits_impl<_Traits, void>;
#endif

      // Types:
    public:
      typedef _Traits					traits_type;
      typedef typename _Traits::char_type		value_type;
      typedef _Char_alloc_type				allocator_type;
      typedef typename _Alloc_traits::size_type		size_type;
      typedef typename _Alloc_traits::difference_type	difference_type;
      typedef typename _Alloc_traits::reference		reference;
      typedef typename _Alloc_traits::const_reference	const_reference;
      typedef typename _Alloc_traits::pointer		pointer;
      typedef typename _Alloc_traits::const_pointer	const_pointer;
      typedef __gnu_cxx::__normal_iterator<pointer, basic_string>  iterator;
      typedef __gnu_cxx::__normal_iterator<const_pointer, basic_string>
							const_iterator;
      typedef std::reverse_iterator<const_iterator>	const_reverse_iterator;
      typedef std::reverse_iterator<iterator>		reverse_iterator;

      ///  Value returned by various member functions when they fail.
      static const size_type	npos = static_cast<size_type>(-1);

    protected:
      // type used for positions in insert, erase etc.
#if __cplusplus < 201103L
      typedef iterator __const_iterator;
#else
      typedef const_iterator __const_iterator;
#endif

    private:
#if __cplusplus >= 201703L
      // A helper type for avoiding boiler-plate.
      typedef basic_string_view<_CharT, _Traits> __sv_type;

      template<typename _Tp, typename _Res>
	using _If_sv = enable_if_t<
	  __and_<is_convertible<const _Tp&, __sv_type>,
		 __not_<is_convertible<const _Tp*, const basic_string*>>,
		 __not_<is_convertible<const _Tp&, const _CharT*>>>::value,
	  _Res>;

      // Allows an implicit conversion to __sv_type.
      _GLIBCXX20_CONSTEXPR
      static __sv_type
      _S_to_string_view(__sv_type __svt) noexcept
      { return __svt; }

      // Wraps a string_view by explicit conversion and thus
      // allows to add an internal constructor that does not
      // participate in overload resolution when a string_view
      // is provided.
      struct __sv_wrapper
      {
	_GLIBCXX20_CONSTEXPR explicit
	__sv_wrapper(__sv_type __sv) noexcept : _M_sv(__sv) { }

	__sv_type _M_sv;
      };

      /**
       *  @brief  Only internally used: Construct string from a string view
       *          wrapper.
       *  @param  __svw  string view wrapper.
       *  @param  __a  Allocator to use.
       */
      _GLIBCXX20_CONSTEXPR
      explicit
      basic_string(__sv_wrapper __svw, const _Alloc& __a)
      : basic_string(__svw._M_sv.data(), __svw._M_sv.size(), __a) { }
#endif

      // Use empty-base optimization: http://www.cantrip.org/emptyopt.html
      struct _Alloc_hider : allocator_type // TODO check __is_final
      {
#if __cplusplus < 201103L
	_Alloc_hider(pointer __dat, const _Alloc& __a = _Alloc())
	: allocator_type(__a), _M_p(__dat) { }
#else
	_GLIBCXX20_CONSTEXPR
	_Alloc_hider(pointer __dat, const _Alloc& __a)
	: allocator_type(__a), _M_p(__dat) { }

	_GLIBCXX20_CONSTEXPR
	_Alloc_hider(pointer __dat, _Alloc&& __a = _Alloc())
	: allocator_type(std::move(__a)), _M_p(__dat) { }
#endif

	pointer _M_p; // The actual data.
      };

      _Alloc_hider	_M_dataplus;
      size_type		_M_string_length;

      enum { _S_local_capacity = 15 / sizeof(_CharT) };

      union
      {
	_CharT           _M_local_buf[_S_local_capacity + 1];
	size_type        _M_allocated_capacity;
      };

      _GLIBCXX20_CONSTEXPR
      void
      _M_data(pointer __p)
      { _M_dataplus._M_p = __p; }

      _GLIBCXX20_CONSTEXPR
      void
      _M_length(size_type __length)
      { _M_string_length = __length; }

      _GLIBCXX20_CONSTEXPR
      pointer
      _M_data() const
      { return _M_dataplus._M_p; }

      _GLIBCXX20_CONSTEXPR
      pointer
      _M_local_data()
      {
#if __cplusplus >= 201103L
	return std::pointer_traits<pointer>::pointer_to(*_M_local_buf);
#else
	return pointer(_M_local_buf);
#endif
      }

      _GLIBCXX20_CONSTEXPR
      const_pointer
      _M_local_data() const
      {
#if __cplusplus >= 201103L
	return std::pointer_traits<const_pointer>::pointer_to(*_M_local_buf);
#else
	return const_pointer(_M_local_buf);
#endif
      }

      _GLIBCXX20_CONSTEXPR
      void
      _M_capacity(size_type __capacity)
      { _M_allocated_capacity = __capacity; }

      _GLIBCXX20_CONSTEXPR
      void
      _M_set_length(size_type __n)
      {
	_M_length(__n);
	traits_type::assign(_M_data()[__n], _CharT());
      }

      _GLIBCXX20_CONSTEXPR
      bool
      _M_is_local() const
      { return _M_data() == _M_local_data(); }

      // Create & Destroy
      _GLIBCXX20_CONSTEXPR
      pointer
      _M_create(size_type&, size_type);

      _GLIBCXX20_CONSTEXPR
      void
      _M_dispose()
      {
	if (!_M_is_local())
	  _M_destroy(_M_allocated_capacity);
      }

      _GLIBCXX20_CONSTEXPR
      void
      _M_destroy(size_type __size) throw()
      { _Alloc_traits::deallocate(_M_get_allocator(), _M_data(), __size + 1); }

#if __cplusplus < 201103L || defined _GLIBCXX_DEFINING_STRING_INSTANTIATIONS
      // _M_construct_aux is used to implement the 21.3.1 para 15 which
      // requires special behaviour if _InIterator is an integral type
      template<typename _InIterator>
        void
        _M_construct_aux(_InIterator __beg, _InIterator __end,
			 std::__false_type)
	{
          typedef typename iterator_traits<_InIterator>::iterator_category _Tag;
          _M_construct(__beg, __end, _Tag());
	}

      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 438. Ambiguity in the "do the right thing" clause
      template<typename _Integer>
        void
        _M_construct_aux(_Integer __beg, _Integer __end, std::__true_type)
	{ _M_construct_aux_2(static_cast<size_type>(__beg), __end); }

      void
      _M_construct_aux_2(size_type __req, _CharT __c)
      { _M_construct(__req, __c); }
#endif

      // For Input Iterators, used in istreambuf_iterators, etc.
      template<typename _InIterator>
	_GLIBCXX20_CONSTEXPR
        void
        _M_construct(_InIterator __beg, _InIterator __end,
		     std::input_iterator_tag);

      // For forward_iterators up to random_access_iterators, used for
      // string::iterator, _CharT*, etc.
      template<typename _FwdIterator>
	_GLIBCXX20_CONSTEXPR
        void
        _M_construct(_FwdIterator __beg, _FwdIterator __end,
		     std::forward_iterator_tag);

      _GLIBCXX20_CONSTEXPR
      void
      _M_construct(size_type __req, _CharT __c);

      _GLIBCXX20_CONSTEXPR
      allocator_type&
      _M_get_allocator()
      { return _M_dataplus; }

      _GLIBCXX20_CONSTEXPR
      const allocator_type&
      _M_get_allocator() const
      { return _M_dataplus; }

      // Ensure that _M_local_buf is the active member of the union.
      __attribute__((__always_inline__))
      _GLIBCXX14_CONSTEXPR
      pointer
      _M_use_local_data() _GLIBCXX_NOEXCEPT
      {
#if __cpp_lib_is_constant_evaluated
	if (std::is_constant_evaluated())
	  for (_CharT& __c : _M_local_buf)
	    __c = _CharT();
#endif
	return _M_local_data();
      }

    private:

#ifdef _GLIBCXX_DISAMBIGUATE_REPLACE_INST
      // The explicit instantiations in misc-inst.cc require this due to
      // https://gcc.gnu.org/bugzilla/show_bug.cgi?id=64063
      template<typename _Tp, bool _Requires =
	       !__are_same<_Tp, _CharT*>::__value
	       && !__are_same<_Tp, const _CharT*>::__value
	       && !__are_same<_Tp, iterator>::__value
	       && !__are_same<_Tp, const_iterator>::__value>
	struct __enable_if_not_native_iterator
	{ typedef basic_string& __type; };
      template<typename _Tp>
	struct __enable_if_not_native_iterator<_Tp, false> { };
#endif

      _GLIBCXX20_CONSTEXPR
      size_type
      _M_check(size_type __pos, const char* __s) const
      {
	if (__pos > this->size())
	  __throw_out_of_range_fmt(__N("%s: __pos (which is %zu) > "
				       "this->size() (which is %zu)"),
				   __s, __pos, this->size());
	return __pos;
      }

      _GLIBCXX20_CONSTEXPR
      void
      _M_check_length(size_type __n1, size_type __n2, const char* __s) const
      {
	if (this->max_size() - (this->size() - __n1) < __n2)
	  __throw_length_error(__N(__s));
      }


      // NB: _M_limit doesn't check for a bad __pos value.
      _GLIBCXX20_CONSTEXPR
      size_type
      _M_limit(size_type __pos, size_type __off) const _GLIBCXX_NOEXCEPT
      {
	const bool __testoff =  __off < this->size() - __pos;
	return __testoff ? __off : this->size() - __pos;
      }

      // True if _Rep and source do not overlap.
      bool
      _M_disjunct(const _CharT* __s) const _GLIBCXX_NOEXCEPT
      {
	return (less<const _CharT*>()(__s, _M_data())
		|| less<const _CharT*>()(_M_data() + this->size(), __s));
      }

      // When __n = 1 way faster than the general multichar
      // traits_type::copy/move/assign.
      _GLIBCXX20_CONSTEXPR
      static void
      _S_copy(_CharT* __d, const _CharT* __s, size_type __n)
      {
	if (__n == 1)
	  traits_type::assign(*__d, *__s);
	else
	  traits_type::copy(__d, __s, __n);
      }

      _GLIBCXX20_CONSTEXPR
      static void
      _S_move(_CharT* __d, const _CharT* __s, size_type __n)
      {
	if (__n == 1)
	  traits_type::assign(*__d, *__s);
	else
	  traits_type::move(__d, __s, __n);
      }

      _GLIBCXX20_CONSTEXPR
      static void
      _S_assign(_CharT* __d, size_type __n, _CharT __c)
      {
	if (__n == 1)
	  traits_type::assign(*__d, __c);
	else
	  traits_type::assign(__d, __n, __c);
      }

      // _S_copy_chars is a separate template to permit specialization
      // to optimize for the common case of pointers as iterators.
      template<class _Iterator>
	_GLIBCXX20_CONSTEXPR
        static void
        _S_copy_chars(_CharT* __p, _Iterator __k1, _Iterator __k2)
        {
	  for (; __k1 != __k2; ++__k1, (void)++__p)
	    traits_type::assign(*__p, *__k1); // These types are off.
	}

      _GLIBCXX20_CONSTEXPR
      static void
      _S_copy_chars(_CharT* __p, iterator __k1, iterator __k2) _GLIBCXX_NOEXCEPT
      { _S_copy_chars(__p, __k1.base(), __k2.base()); }

      _GLIBCXX20_CONSTEXPR
      static void
      _S_copy_chars(_CharT* __p, const_iterator __k1, const_iterator __k2)
      _GLIBCXX_NOEXCEPT
      { _S_copy_chars(__p, __k1.base(), __k2.base()); }

      _GLIBCXX20_CONSTEXPR
      static void
      _S_copy_chars(_CharT* __p, _CharT* __k1, _CharT* __k2) _GLIBCXX_NOEXCEPT
      { _S_copy(__p, __k1, __k2 - __k1); }

      _GLIBCXX20_CONSTEXPR
      static void
      _S_copy_chars(_CharT* __p, const _CharT* __k1, const _CharT* __k2)
      _GLIBCXX_NOEXCEPT
      { _S_copy(__p, __k1, __k2 - __k1); }

      _GLIBCXX20_CONSTEXPR
      static int
      _S_compare(size_type __n1, size_type __n2) _GLIBCXX_NOEXCEPT
      {
	const difference_type __d = difference_type(__n1 - __n2);

	if (__d > __gnu_cxx::__numeric_traits<int>::__max)
	  return __gnu_cxx::__numeric_traits<int>::__max;
	else if (__d < __gnu_cxx::__numeric_traits<int>::__min)
	  return __gnu_cxx::__numeric_traits<int>::__min;
	else
	  return int(__d);
      }

      _GLIBCXX20_CONSTEXPR
      void
      _M_assign(const basic_string&);

      _GLIBCXX20_CONSTEXPR
      void
      _M_mutate(size_type __pos, size_type __len1, const _CharT* __s,
		size_type __len2);

      _GLIBCXX20_CONSTEXPR
      void
      _M_erase(size_type __pos, size_type __n);

    public:
      // Construct/copy/destroy:
      // NB: We overload ctors in some cases instead of using default
      // arguments, per 17.4.4.4 para. 2 item 2.

      /**
       *  @brief  Default constructor creates an empty string.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string()
      _GLIBCXX_NOEXCEPT_IF(is_nothrow_default_constructible<_Alloc>::value)
      : _M_dataplus(_M_local_data())
      {
	_M_use_local_data();
	_M_set_length(0);
      }

      /**
       *  @brief  Construct an empty string using allocator @a a.
       */
      _GLIBCXX20_CONSTEXPR
      explicit
      basic_string(const _Alloc& __a) _GLIBCXX_NOEXCEPT
      : _M_dataplus(_M_local_data(), __a)
      {
	_M_use_local_data();
	_M_set_length(0);
      }

      /**
       *  @brief  Construct string with copy of value of @a __str.
       *  @param  __str  Source string.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string(const basic_string& __str)
      : _M_dataplus(_M_local_data(),
		    _Alloc_traits::_S_select_on_copy(__str._M_get_allocator()))
      {
	_M_construct(__str._M_data(), __str._M_data() + __str.length(),
		     std::forward_iterator_tag());
      }

      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 2583. no way to supply an allocator for basic_string(str, pos)
      /**
       *  @brief  Construct string as copy of a substring.
       *  @param  __str  Source string.
       *  @param  __pos  Index of first character to copy from.
       *  @param  __a  Allocator to use.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string(const basic_string& __str, size_type __pos,
		   const _Alloc& __a = _Alloc())
      : _M_dataplus(_M_local_data(), __a)
      {
	const _CharT* __start = __str._M_data()
	  + __str._M_check(__pos, "basic_string::basic_string");
	_M_construct(__start, __start + __str._M_limit(__pos, npos),
		     std::forward_iterator_tag());
      }

      /**
       *  @brief  Construct string as copy of a substring.
       *  @param  __str  Source string.
       *  @param  __pos  Index of first character to copy from.
       *  @param  __n  Number of characters to copy.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string(const basic_string& __str, size_type __pos,
		   size_type __n)
      : _M_dataplus(_M_local_data())
      {
	const _CharT* __start = __str._M_data()
	  + __str._M_check(__pos, "basic_string::basic_string");
	_M_construct(__start, __start + __str._M_limit(__pos, __n),
		     std::forward_iterator_tag());
      }

      /**
       *  @brief  Construct string as copy of a substring.
       *  @param  __str  Source string.
       *  @param  __pos  Index of first character to copy from.
       *  @param  __n  Number of characters to copy.
       *  @param  __a  Allocator to use.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string(const basic_string& __str, size_type __pos,
		   size_type __n, const _Alloc& __a)
      : _M_dataplus(_M_local_data(), __a)
      {
	const _CharT* __start
	  = __str._M_data() + __str._M_check(__pos, "string::string");
	_M_construct(__start, __start + __str._M_limit(__pos, __n),
		     std::forward_iterator_tag());
      }

      /**
       *  @brief  Construct string initialized by a character %array.
       *  @param  __s  Source character %array.
       *  @param  __n  Number of characters to copy.
       *  @param  __a  Allocator to use (default is default allocator).
       *
       *  NB: @a __s must have at least @a __n characters, &apos;\\0&apos;
       *  has no special meaning.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string(const _CharT* __s, size_type __n,
		   const _Alloc& __a = _Alloc())
      : _M_dataplus(_M_local_data(), __a)
      {
	// NB: Not required, but considered best practice.
	if (__s == 0 && __n > 0)
	  std::__throw_logic_error(__N("basic_string: "
				       "construction from null is not valid"));
	_M_construct(__s, __s + __n, std::forward_iterator_tag());
      }

      /**
       *  @brief  Construct string as copy of a C string.
       *  @param  __s  Source C string.
       *  @param  __a  Allocator to use (default is default allocator).
       */
#if __cpp_deduction_guides && ! defined _GLIBCXX_DEFINING_STRING_INSTANTIATIONS
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 3076. basic_string CTAD ambiguity
      template<typename = _RequireAllocator<_Alloc>>
#endif
      _GLIBCXX20_CONSTEXPR
      basic_string(const _CharT* __s, const _Alloc& __a = _Alloc())
      : _M_dataplus(_M_local_data(), __a)
      {
	// NB: Not required, but considered best practice.
	if (__s == 0)
	  std::__throw_logic_error(__N("basic_string: "
				       "construction from null is not valid"));
	const _CharT* __end = __s + traits_type::length(__s);
	_M_construct(__s, __end, forward_iterator_tag());
      }

      /**
       *  @brief  Construct string as multiple characters.
       *  @param  __n  Number of characters.
       *  @param  __c  Character to use.
       *  @param  __a  Allocator to use (default is default allocator).
       */
#if __cpp_deduction_guides && ! defined _GLIBCXX_DEFINING_STRING_INSTANTIATIONS
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 3076. basic_string CTAD ambiguity
      template<typename = _RequireAllocator<_Alloc>>
#endif
      _GLIBCXX20_CONSTEXPR
      basic_string(size_type __n, _CharT __c, const _Alloc& __a = _Alloc())
      : _M_dataplus(_M_local_data(), __a)
      { _M_construct(__n, __c); }

#if __cplusplus >= 201103L
      /**
       *  @brief  Move construct string.
       *  @param  __str  Source string.
       *
       *  The newly-created string contains the exact contents of @a __str.
       *  @a __str is a valid, but unspecified string.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string(basic_string&& __str) noexcept
      : _M_dataplus(_M_local_data(), std::move(__str._M_get_allocator()))
      {
	if (__str._M_is_local())
	  {
	    traits_type::copy(_M_local_buf, __str._M_local_buf,
			      __str.length() + 1);
	  }
	else
	  {
	    _M_data(__str._M_data());
	    _M_capacity(__str._M_allocated_capacity);
	  }

	// Must use _M_length() here not _M_set_length() because
	// basic_stringbuf relies on writing into unallocated capacity so
	// we mess up the contents if we put a '\0' in the string.
	_M_length(__str.length());
	__str._M_data(__str._M_local_data());
	__str._M_set_length(0);
      }

      /**
       *  @brief  Construct string from an initializer %list.
       *  @param  __l  std::initializer_list of characters.
       *  @param  __a  Allocator to use (default is default allocator).
       */
      _GLIBCXX20_CONSTEXPR
      basic_string(initializer_list<_CharT> __l, const _Alloc& __a = _Alloc())
      : _M_dataplus(_M_local_data(), __a)
      { _M_construct(__l.begin(), __l.end(), std::forward_iterator_tag()); }

      _GLIBCXX20_CONSTEXPR
      basic_string(const basic_string& __str, const _Alloc& __a)
      : _M_dataplus(_M_local_data(), __a)
      { _M_construct(__str.begin(), __str.end(), std::forward_iterator_tag()); }

      _GLIBCXX20_CONSTEXPR
      basic_string(basic_string&& __str, const _Alloc& __a)
      noexcept(_Alloc_traits::_S_always_equal())
      : _M_dataplus(_M_local_data(), __a)
      {
	if (__str._M_is_local())
	  {
	    traits_type::copy(_M_local_buf, __str._M_local_buf,
			      __str.length() + 1);
	    _M_length(__str.length());
	    __str._M_set_length(0);
	  }
	else if (_Alloc_traits::_S_always_equal()
	    || __str.get_allocator() == __a)
	  {
	    _M_data(__str._M_data());
	    _M_length(__str.length());
	    _M_capacity(__str._M_allocated_capacity);
	    __str._M_data(__str._M_local_buf);
	    __str._M_set_length(0);
	  }
	else
	  _M_construct(__str.begin(), __str.end(), std::forward_iterator_tag());
      }
#endif // C++11

#if __cplusplus >= 202100L
      basic_string(nullptr_t) = delete;
      basic_string& operator=(nullptr_t) = delete;
#endif // C++23

      /**
       *  @brief  Construct string as copy of a range.
       *  @param  __beg  Start of range.
       *  @param  __end  End of range.
       *  @param  __a  Allocator to use (default is default allocator).
       */
#if __cplusplus >= 201103L
      template<typename _InputIterator,
	       typename = std::_RequireInputIter<_InputIterator>>
#else
      template<typename _InputIterator>
#endif
	_GLIBCXX20_CONSTEXPR
        basic_string(_InputIterator __beg, _InputIterator __end,
		     const _Alloc& __a = _Alloc())
	: _M_dataplus(_M_local_data(), __a)
	{
#if __cplusplus >= 201103L
	  _M_construct(__beg, __end, std::__iterator_category(__beg));
#else
	  typedef typename std::__is_integer<_InputIterator>::__type _Integral;
	  _M_construct_aux(__beg, __end, _Integral());
#endif
	}

#if __cplusplus >= 201703L
      /**
       *  @brief  Construct string from a substring of a string_view.
       *  @param  __t   Source object convertible to string view.
       *  @param  __pos The index of the first character to copy from __t.
       *  @param  __n   The number of characters to copy from __t.
       *  @param  __a   Allocator to use.
       */
      template<typename _Tp,
	       typename = enable_if_t<is_convertible_v<const _Tp&, __sv_type>>>
	_GLIBCXX20_CONSTEXPR
	basic_string(const _Tp& __t, size_type __pos, size_type __n,
		     const _Alloc& __a = _Alloc())
	: basic_string(_S_to_string_view(__t).substr(__pos, __n), __a) { }

      /**
       *  @brief  Construct string from a string_view.
       *  @param  __t  Source object convertible to string view.
       *  @param  __a  Allocator to use (default is default allocator).
       */
      template<typename _Tp, typename = _If_sv<_Tp, void>>
	_GLIBCXX20_CONSTEXPR
	explicit
	basic_string(const _Tp& __t, const _Alloc& __a = _Alloc())
	: basic_string(__sv_wrapper(_S_to_string_view(__t)), __a) { }
#endif // C++17

      /**
       *  @brief  Destroy the string instance.
       */
      _GLIBCXX20_CONSTEXPR
      ~basic_string()
      { _M_dispose(); }

      /**
       *  @brief  Assign the value of @a str to this string.
       *  @param  __str  Source string.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      operator=(const basic_string& __str)
      {
	return this->assign(__str);
      }

      /**
       *  @brief  Copy contents of @a s into this string.
       *  @param  __s  Source null-terminated string.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      operator=(const _CharT* __s)
      { return this->assign(__s); }

      /**
       *  @brief  Set value to string of length 1.
       *  @param  __c  Source character.
       *
       *  Assigning to a character makes this string length 1 and
       *  (*this)[0] == @a c.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      operator=(_CharT __c)
      {
	this->assign(1, __c);
	return *this;
      }

#if __cplusplus >= 201103L
      /**
       *  @brief  Move assign the value of @a str to this string.
       *  @param  __str  Source string.
       *
       *  The contents of @a str are moved into this string (without copying).
       *  @a str is a valid, but unspecified string.
       */
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 2063. Contradictory requirements for string move assignment
      _GLIBCXX20_CONSTEXPR
      basic_string&
      operator=(basic_string&& __str)
      noexcept(_Alloc_traits::_S_nothrow_move())
      {
	if (!_M_is_local() && _Alloc_traits::_S_propagate_on_move_assign()
	    && !_Alloc_traits::_S_always_equal()
	    && _M_get_allocator() != __str._M_get_allocator())
	  {
	    // Destroy existing storage before replacing allocator.
	    _M_destroy(_M_allocated_capacity);
	    _M_data(_M_local_data());
	    _M_set_length(0);
	  }
	// Replace allocator if POCMA is true.
	std::__alloc_on_move(_M_get_allocator(), __str._M_get_allocator());

	if (__str._M_is_local())
	  {
	    // We've always got room for a short string, just copy it
	    // (unless this is a self-move, because that would violate the
	    // char_traits::copy precondition that the ranges don't overlap).
	    if (__builtin_expect(std::__addressof(__str) != this, true))
	      {
		if (__str.size())
		  this->_S_copy(_M_data(), __str._M_data(), __str.size());
		_M_set_length(__str.size());
	      }
	  }
	else if (_Alloc_traits::_S_propagate_on_move_assign()
	    || _Alloc_traits::_S_always_equal()
	    || _M_get_allocator() == __str._M_get_allocator())
	  {
	    // Just move the allocated pointer, our allocator can free it.
	    pointer __data = nullptr;
	    size_type __capacity;
	    if (!_M_is_local())
	      {
		if (_Alloc_traits::_S_always_equal())
		  {
		    // __str can reuse our existing storage.
		    __data = _M_data();
		    __capacity = _M_allocated_capacity;
		  }
		else // __str can't use it, so free it.
		  _M_destroy(_M_allocated_capacity);
	      }

	    _M_data(__str._M_data());
	    _M_length(__str.length());
	    _M_capacity(__str._M_allocated_capacity);
	    if (__data)
	      {
		__str._M_data(__data);
		__str._M_capacity(__capacity);
	      }
	    else
	      __str._M_data(__str._M_local_buf);
	  }
	else // Need to do a deep copy
	  assign(__str);
	__str.clear();
	return *this;
      }

      /**
       *  @brief  Set value to string constructed from initializer %list.
       *  @param  __l  std::initializer_list.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      operator=(initializer_list<_CharT> __l)
      {
	this->assign(__l.begin(), __l.size());
	return *this;
      }
#endif // C++11

#if __cplusplus >= 201703L
      /**
       *  @brief  Set value to string constructed from a string_view.
       *  @param  __svt  An object convertible to string_view.
       */
     template<typename _Tp>
       _GLIBCXX20_CONSTEXPR
       _If_sv<_Tp, basic_string&>
       operator=(const _Tp& __svt)
       { return this->assign(__svt); }

      /**
       *  @brief  Convert to a string_view.
       *  @return A string_view.
       */
      _GLIBCXX20_CONSTEXPR
      operator __sv_type() const noexcept
      { return __sv_type(data(), size()); }
#endif // C++17

      // Iterators:
      /**
       *  Returns a read/write iterator that points to the first character in
       *  the %string.
       */
      _GLIBCXX20_CONSTEXPR
      iterator
      begin() _GLIBCXX_NOEXCEPT
      { return iterator(_M_data()); }

      /**
       *  Returns a read-only (constant) iterator that points to the first
       *  character in the %string.
       */
      _GLIBCXX20_CONSTEXPR
      const_iterator
      begin() const _GLIBCXX_NOEXCEPT
      { return const_iterator(_M_data()); }

      /**
       *  Returns a read/write iterator that points one past the last
       *  character in the %string.
       */
      _GLIBCXX20_CONSTEXPR
      iterator
      end() _GLIBCXX_NOEXCEPT
      { return iterator(_M_data() + this->size()); }

      /**
       *  Returns a read-only (constant) iterator that points one past the
       *  last character in the %string.
       */
      _GLIBCXX20_CONSTEXPR
      const_iterator
      end() const _GLIBCXX_NOEXCEPT
      { return const_iterator(_M_data() + this->size()); }

      /**
       *  Returns a read/write reverse iterator that points to the last
       *  character in the %string.  Iteration is done in reverse element
       *  order.
       */
      _GLIBCXX20_CONSTEXPR
      reverse_iterator
      rbegin() _GLIBCXX_NOEXCEPT
      { return reverse_iterator(this->end()); }

      /**
       *  Returns a read-only (constant) reverse iterator that points
       *  to the last character in the %string.  Iteration is done in
       *  reverse element order.
       */
      _GLIBCXX20_CONSTEXPR
      const_reverse_iterator
      rbegin() const _GLIBCXX_NOEXCEPT
      { return const_reverse_iterator(this->end()); }

      /**
       *  Returns a read/write reverse iterator that points to one before the
       *  first character in the %string.  Iteration is done in reverse
       *  element order.
       */
      _GLIBCXX20_CONSTEXPR
      reverse_iterator
      rend() _GLIBCXX_NOEXCEPT
      { return reverse_iterator(this->begin()); }

      /**
       *  Returns a read-only (constant) reverse iterator that points
       *  to one before the first character in the %string.  Iteration
       *  is done in reverse element order.
       */
      _GLIBCXX20_CONSTEXPR
      const_reverse_iterator
      rend() const _GLIBCXX_NOEXCEPT
      { return const_reverse_iterator(this->begin()); }

#if __cplusplus >= 201103L
      /**
       *  Returns a read-only (constant) iterator that points to the first
       *  character in the %string.
       */
      _GLIBCXX20_CONSTEXPR
      const_iterator
      cbegin() const noexcept
      { return const_iterator(this->_M_data()); }

      /**
       *  Returns a read-only (constant) iterator that points one past the
       *  last character in the %string.
       */
      _GLIBCXX20_CONSTEXPR
      const_iterator
      cend() const noexcept
      { return const_iterator(this->_M_data() + this->size()); }

      /**
       *  Returns a read-only (constant) reverse iterator that points
       *  to the last character in the %string.  Iteration is done in
       *  reverse element order.
       */
      _GLIBCXX20_CONSTEXPR
      const_reverse_iterator
      crbegin() const noexcept
      { return const_reverse_iterator(this->end()); }

      /**
       *  Returns a read-only (constant) reverse iterator that points
       *  to one before the first character in the %string.  Iteration
       *  is done in reverse element order.
       */
      _GLIBCXX20_CONSTEXPR
      const_reverse_iterator
      crend() const noexcept
      { return const_reverse_iterator(this->begin()); }
#endif

    public:
      // Capacity:
      ///  Returns the number of characters in the string, not including any
      ///  null-termination.
      _GLIBCXX20_CONSTEXPR
      size_type
      size() const _GLIBCXX_NOEXCEPT
      { return _M_string_length; }

      ///  Returns the number of characters in the string, not including any
      ///  null-termination.
      _GLIBCXX20_CONSTEXPR
      size_type
      length() const _GLIBCXX_NOEXCEPT
      { return _M_string_length; }

      ///  Returns the size() of the largest possible %string.
      _GLIBCXX20_CONSTEXPR
      size_type
      max_size() const _GLIBCXX_NOEXCEPT
      { return (_Alloc_traits::max_size(_M_get_allocator()) - 1) / 2; }

      /**
       *  @brief  Resizes the %string to the specified number of characters.
       *  @param  __n  Number of characters the %string should contain.
       *  @param  __c  Character to fill any new elements.
       *
       *  This function will %resize the %string to the specified
       *  number of characters.  If the number is smaller than the
       *  %string's current size the %string is truncated, otherwise
       *  the %string is extended and new elements are %set to @a __c.
       */
      _GLIBCXX20_CONSTEXPR
      void
      resize(size_type __n, _CharT __c);

      /**
       *  @brief  Resizes the %string to the specified number of characters.
       *  @param  __n  Number of characters the %string should contain.
       *
       *  This function will resize the %string to the specified length.  If
       *  the new size is smaller than the %string's current size the %string
       *  is truncated, otherwise the %string is extended and new characters
       *  are default-constructed.  For basic types such as char, this means
       *  setting them to 0.
       */
      _GLIBCXX20_CONSTEXPR
      void
      resize(size_type __n)
      { this->resize(__n, _CharT()); }

#if __cplusplus >= 201103L
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"
      ///  A non-binding request to reduce capacity() to size().
      _GLIBCXX20_CONSTEXPR
      void
      shrink_to_fit() noexcept
      { reserve(); }
#pragma GCC diagnostic pop
#endif

#if __cplusplus > 202002L
#define __cpp_lib_string_resize_and_overwrite 202110L
      template<typename _Operation>
	constexpr void
	resize_and_overwrite(size_type __n, _Operation __op);
#endif

      /**
       *  Returns the total number of characters that the %string can hold
       *  before needing to allocate more memory.
       */
      _GLIBCXX20_CONSTEXPR
      size_type
      capacity() const _GLIBCXX_NOEXCEPT
      {
	return _M_is_local() ? size_type(_S_local_capacity)
	                     : _M_allocated_capacity;
      }

      /**
       *  @brief  Attempt to preallocate enough memory for specified number of
       *          characters.
       *  @param  __res_arg  Number of characters required.
       *  @throw  std::length_error  If @a __res_arg exceeds @c max_size().
       *
       *  This function attempts to reserve enough memory for the
       *  %string to hold the specified number of characters.  If the
       *  number requested is more than max_size(), length_error is
       *  thrown.
       *
       *  The advantage of this function is that if optimal code is a
       *  necessity and the user can determine the string length that will be
       *  required, the user can reserve the memory in %advance, and thus
       *  prevent a possible reallocation of memory and copying of %string
       *  data.
       */
      _GLIBCXX20_CONSTEXPR
      void
      reserve(size_type __res_arg);

      /**
       *  Equivalent to shrink_to_fit().
       */
#if __cplusplus > 201703L
      [[deprecated("use shrink_to_fit() instead")]]
#endif
      _GLIBCXX20_CONSTEXPR
      void
      reserve();

      /**
       *  Erases the string, making it empty.
       */
      _GLIBCXX20_CONSTEXPR
      void
      clear() _GLIBCXX_NOEXCEPT
      { _M_set_length(0); }

      /**
       *  Returns true if the %string is empty.  Equivalent to 
       *  <code>*this == ""</code>.
       */
      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      bool
      empty() const _GLIBCXX_NOEXCEPT
      { return this->size() == 0; }

      // Element access:
      /**
       *  @brief  Subscript access to the data contained in the %string.
       *  @param  __pos  The index of the character to access.
       *  @return  Read-only (constant) reference to the character.
       *
       *  This operator allows for easy, array-style, data access.
       *  Note that data access with this operator is unchecked and
       *  out_of_range lookups are not defined. (For checked lookups
       *  see at().)
       */
      _GLIBCXX20_CONSTEXPR
      const_reference
      operator[] (size_type __pos) const _GLIBCXX_NOEXCEPT
      {
	__glibcxx_assert(__pos <= size());
	return _M_data()[__pos];
      }

      /**
       *  @brief  Subscript access to the data contained in the %string.
       *  @param  __pos  The index of the character to access.
       *  @return  Read/write reference to the character.
       *
       *  This operator allows for easy, array-style, data access.
       *  Note that data access with this operator is unchecked and
       *  out_of_range lookups are not defined. (For checked lookups
       *  see at().)
       */
      _GLIBCXX20_CONSTEXPR
      reference
      operator[](size_type __pos)
      {
        // Allow pos == size() both in C++98 mode, as v3 extension,
	// and in C++11 mode.
	__glibcxx_assert(__pos <= size());
        // In pedantic mode be strict in C++98 mode.
	_GLIBCXX_DEBUG_PEDASSERT(__cplusplus >= 201103L || __pos < size());
	return _M_data()[__pos];
      }

      /**
       *  @brief  Provides access to the data contained in the %string.
       *  @param __n The index of the character to access.
       *  @return  Read-only (const) reference to the character.
       *  @throw  std::out_of_range  If @a n is an invalid index.
       *
       *  This function provides for safer data access.  The parameter is
       *  first checked that it is in the range of the string.  The function
       *  throws out_of_range if the check fails.
       */
      _GLIBCXX20_CONSTEXPR
      const_reference
      at(size_type __n) const
      {
	if (__n >= this->size())
	  __throw_out_of_range_fmt(__N("basic_string::at: __n "
				       "(which is %zu) >= this->size() "
				       "(which is %zu)"),
				   __n, this->size());
	return _M_data()[__n];
      }

      /**
       *  @brief  Provides access to the data contained in the %string.
       *  @param __n The index of the character to access.
       *  @return  Read/write reference to the character.
       *  @throw  std::out_of_range  If @a n is an invalid index.
       *
       *  This function provides for safer data access.  The parameter is
       *  first checked that it is in the range of the string.  The function
       *  throws out_of_range if the check fails.
       */
      _GLIBCXX20_CONSTEXPR
      reference
      at(size_type __n)
      {
	if (__n >= size())
	  __throw_out_of_range_fmt(__N("basic_string::at: __n "
				       "(which is %zu) >= this->size() "
				       "(which is %zu)"),
				   __n, this->size());
	return _M_data()[__n];
      }

#if __cplusplus >= 201103L
      /**
       *  Returns a read/write reference to the data at the first
       *  element of the %string.
       */
      _GLIBCXX20_CONSTEXPR
      reference
      front() noexcept
      {
	__glibcxx_assert(!empty());
	return operator[](0);
      }

      /**
       *  Returns a read-only (constant) reference to the data at the first
       *  element of the %string.
       */
      _GLIBCXX20_CONSTEXPR
      const_reference
      front() const noexcept
      {
	__glibcxx_assert(!empty());
	return operator[](0);
      }

      /**
       *  Returns a read/write reference to the data at the last
       *  element of the %string.
       */
      _GLIBCXX20_CONSTEXPR
      reference
      back() noexcept
      {
	__glibcxx_assert(!empty());
	return operator[](this->size() - 1);
      }

      /**
       *  Returns a read-only (constant) reference to the data at the
       *  last element of the %string.
       */
      _GLIBCXX20_CONSTEXPR
      const_reference
      back() const noexcept
      {
	__glibcxx_assert(!empty());
	return operator[](this->size() - 1);
      }
#endif

      // Modifiers:
      /**
       *  @brief  Append a string to this string.
       *  @param __str  The string to append.
       *  @return  Reference to this string.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      operator+=(const basic_string& __str)
      { return this->append(__str); }

      /**
       *  @brief  Append a C string.
       *  @param __s  The C string to append.
       *  @return  Reference to this string.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      operator+=(const _CharT* __s)
      { return this->append(__s); }

      /**
       *  @brief  Append a character.
       *  @param __c  The character to append.
       *  @return  Reference to this string.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      operator+=(_CharT __c)
      {
	this->push_back(__c);
	return *this;
      }

#if __cplusplus >= 201103L
      /**
       *  @brief  Append an initializer_list of characters.
       *  @param __l  The initializer_list of characters to be appended.
       *  @return  Reference to this string.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      operator+=(initializer_list<_CharT> __l)
      { return this->append(__l.begin(), __l.size()); }
#endif // C++11

#if __cplusplus >= 201703L
      /**
       *  @brief  Append a string_view.
       *  @param __svt  An object convertible to string_view to be appended.
       *  @return  Reference to this string.
       */
      template<typename _Tp>
	_GLIBCXX20_CONSTEXPR
	_If_sv<_Tp, basic_string&>
	operator+=(const _Tp& __svt)
	{ return this->append(__svt); }
#endif // C++17

      /**
       *  @brief  Append a string to this string.
       *  @param __str  The string to append.
       *  @return  Reference to this string.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      append(const basic_string& __str)
      { return this->append(__str._M_data(), __str.size()); }

      /**
       *  @brief  Append a substring.
       *  @param __str  The string to append.
       *  @param __pos  Index of the first character of str to append.
       *  @param __n  The number of characters to append.
       *  @return  Reference to this string.
       *  @throw  std::out_of_range if @a __pos is not a valid index.
       *
       *  This function appends @a __n characters from @a __str
       *  starting at @a __pos to this string.  If @a __n is is larger
       *  than the number of available characters in @a __str, the
       *  remainder of @a __str is appended.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      append(const basic_string& __str, size_type __pos, size_type __n = npos)
      { return this->append(__str._M_data()
			    + __str._M_check(__pos, "basic_string::append"),
			    __str._M_limit(__pos, __n)); }

      /**
       *  @brief  Append a C substring.
       *  @param __s  The C string to append.
       *  @param __n  The number of characters to append.
       *  @return  Reference to this string.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      append(const _CharT* __s, size_type __n)
      {
	__glibcxx_requires_string_len(__s, __n);
	_M_check_length(size_type(0), __n, "basic_string::append");
	return _M_append(__s, __n);
      }

      /**
       *  @brief  Append a C string.
       *  @param __s  The C string to append.
       *  @return  Reference to this string.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      append(const _CharT* __s)
      {
	__glibcxx_requires_string(__s);
	const size_type __n = traits_type::length(__s);
	_M_check_length(size_type(0), __n, "basic_string::append");
	return _M_append(__s, __n);
      }

      /**
       *  @brief  Append multiple characters.
       *  @param __n  The number of characters to append.
       *  @param __c  The character to use.
       *  @return  Reference to this string.
       *
       *  Appends __n copies of __c to this string.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      append(size_type __n, _CharT __c)
      { return _M_replace_aux(this->size(), size_type(0), __n, __c); }

#if __cplusplus >= 201103L
      /**
       *  @brief  Append an initializer_list of characters.
       *  @param __l  The initializer_list of characters to append.
       *  @return  Reference to this string.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      append(initializer_list<_CharT> __l)
      { return this->append(__l.begin(), __l.size()); }
#endif // C++11

      /**
       *  @brief  Append a range of characters.
       *  @param __first  Iterator referencing the first character to append.
       *  @param __last  Iterator marking the end of the range.
       *  @return  Reference to this string.
       *
       *  Appends characters in the range [__first,__last) to this string.
       */
#if __cplusplus >= 201103L
      template<class _InputIterator,
	       typename = std::_RequireInputIter<_InputIterator>>
	_GLIBCXX20_CONSTEXPR
#else
      template<class _InputIterator>
#endif
        basic_string&
        append(_InputIterator __first, _InputIterator __last)
        { return this->replace(end(), end(), __first, __last); }

#if __cplusplus >= 201703L
      /**
       *  @brief  Append a string_view.
       *  @param __svt  An object convertible to string_view to be appended.
       *  @return  Reference to this string.
       */
      template<typename _Tp>
	_GLIBCXX20_CONSTEXPR
        _If_sv<_Tp, basic_string&>
        append(const _Tp& __svt)
        {
          __sv_type __sv = __svt;
          return this->append(__sv.data(), __sv.size());
        }

      /**
       *  @brief  Append a range of characters from a string_view.
       *  @param __svt  An object convertible to string_view to be appended from.
       *  @param __pos The position in the string_view to append from.
       *  @param __n   The number of characters to append from the string_view.
       *  @return  Reference to this string.
       */
      template<typename _Tp>
	_GLIBCXX20_CONSTEXPR
        _If_sv<_Tp, basic_string&>
	append(const _Tp& __svt, size_type __pos, size_type __n = npos)
	{
	  __sv_type __sv = __svt;
	  return _M_append(__sv.data()
	      + std::__sv_check(__sv.size(), __pos, "basic_string::append"),
	      std::__sv_limit(__sv.size(), __pos, __n));
	}
#endif // C++17

      /**
       *  @brief  Append a single character.
       *  @param __c  Character to append.
       */
      _GLIBCXX20_CONSTEXPR
      void
      push_back(_CharT __c)
      {
	const size_type __size = this->size();
	if (__size + 1 > this->capacity())
	  this->_M_mutate(__size, size_type(0), 0, size_type(1));
	traits_type::assign(this->_M_data()[__size], __c);
	this->_M_set_length(__size + 1);
      }

      /**
       *  @brief  Set value to contents of another string.
       *  @param  __str  Source string to use.
       *  @return  Reference to this string.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      assign(const basic_string& __str)
      {
#if __cplusplus >= 201103L
	if (_Alloc_traits::_S_propagate_on_copy_assign())
	  {
	    if (!_Alloc_traits::_S_always_equal() && !_M_is_local()
		&& _M_get_allocator() != __str._M_get_allocator())
	      {
		// Propagating allocator cannot free existing storage so must
		// deallocate it before replacing current allocator.
		if (__str.size() <= _S_local_capacity)
		  {
		    _M_destroy(_M_allocated_capacity);
		    _M_data(_M_use_local_data());
		    _M_set_length(0);
		  }
		else
		  {
		    const auto __len = __str.size();
		    auto __alloc = __str._M_get_allocator();
		    // If this allocation throws there are no effects:
		    auto __ptr = _Alloc_traits::allocate(__alloc, __len + 1);
		    _M_destroy(_M_allocated_capacity);
		    _M_data(__ptr);
		    _M_capacity(__len);
		    _M_set_length(__len);
		  }
	      }
	    std::__alloc_on_copy(_M_get_allocator(), __str._M_get_allocator());
	  }
#endif
	this->_M_assign(__str);
	return *this;
      }

#if __cplusplus >= 201103L
      /**
       *  @brief  Set value to contents of another string.
       *  @param  __str  Source string to use.
       *  @return  Reference to this string.
       *
       *  This function sets this string to the exact contents of @a __str.
       *  @a __str is a valid, but unspecified string.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      assign(basic_string&& __str)
      noexcept(_Alloc_traits::_S_nothrow_move())
      {
	// _GLIBCXX_RESOLVE_LIB_DEFECTS
	// 2063. Contradictory requirements for string move assignment
	return *this = std::move(__str);
      }
#endif // C++11

      /**
       *  @brief  Set value to a substring of a string.
       *  @param __str  The string to use.
       *  @param __pos  Index of the first character of str.
       *  @param __n  Number of characters to use.
       *  @return  Reference to this string.
       *  @throw  std::out_of_range if @a pos is not a valid index.
       *
       *  This function sets this string to the substring of @a __str
       *  consisting of @a __n characters at @a __pos.  If @a __n is
       *  is larger than the number of available characters in @a
       *  __str, the remainder of @a __str is used.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      assign(const basic_string& __str, size_type __pos, size_type __n = npos)
      { return _M_replace(size_type(0), this->size(), __str._M_data()
			  + __str._M_check(__pos, "basic_string::assign"),
			  __str._M_limit(__pos, __n)); }

      /**
       *  @brief  Set value to a C substring.
       *  @param __s  The C string to use.
       *  @param __n  Number of characters to use.
       *  @return  Reference to this string.
       *
       *  This function sets the value of this string to the first @a __n
       *  characters of @a __s.  If @a __n is is larger than the number of
       *  available characters in @a __s, the remainder of @a __s is used.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      assign(const _CharT* __s, size_type __n)
      {
	__glibcxx_requires_string_len(__s, __n);
	return _M_replace(size_type(0), this->size(), __s, __n);
      }

      /**
       *  @brief  Set value to contents of a C string.
       *  @param __s  The C string to use.
       *  @return  Reference to this string.
       *
       *  This function sets the value of this string to the value of @a __s.
       *  The data is copied, so there is no dependence on @a __s once the
       *  function returns.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      assign(const _CharT* __s)
      {
	__glibcxx_requires_string(__s);
	return _M_replace(size_type(0), this->size(), __s,
			  traits_type::length(__s));
      }

      /**
       *  @brief  Set value to multiple characters.
       *  @param __n  Length of the resulting string.
       *  @param __c  The character to use.
       *  @return  Reference to this string.
       *
       *  This function sets the value of this string to @a __n copies of
       *  character @a __c.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      assign(size_type __n, _CharT __c)
      { return _M_replace_aux(size_type(0), this->size(), __n, __c); }

      /**
       *  @brief  Set value to a range of characters.
       *  @param __first  Iterator referencing the first character to append.
       *  @param __last  Iterator marking the end of the range.
       *  @return  Reference to this string.
       *
       *  Sets value of string to characters in the range [__first,__last).
      */
#if __cplusplus >= 201103L
      template<class _InputIterator,
	       typename = std::_RequireInputIter<_InputIterator>>
	_GLIBCXX20_CONSTEXPR
#else
      template<class _InputIterator>
#endif
        basic_string&
        assign(_InputIterator __first, _InputIterator __last)
        { return this->replace(begin(), end(), __first, __last); }

#if __cplusplus >= 201103L
      /**
       *  @brief  Set value to an initializer_list of characters.
       *  @param __l  The initializer_list of characters to assign.
       *  @return  Reference to this string.
       */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      assign(initializer_list<_CharT> __l)
      { return this->assign(__l.begin(), __l.size()); }
#endif // C++11

#if __cplusplus >= 201703L
      /**
       *  @brief  Set value from a string_view.
       *  @param __svt  The source object convertible to string_view.
       *  @return  Reference to this string.
       */
      template<typename _Tp>
	_GLIBCXX20_CONSTEXPR
	_If_sv<_Tp, basic_string&>
	assign(const _Tp& __svt)
	{
	  __sv_type __sv = __svt;
	  return this->assign(__sv.data(), __sv.size());
	}

      /**
       *  @brief  Set value from a range of characters in a string_view.
       *  @param __svt  The source object convertible to string_view.
       *  @param __pos  The position in the string_view to assign from.
       *  @param __n  The number of characters to assign.
       *  @return  Reference to this string.
       */
      template<typename _Tp>
	_GLIBCXX20_CONSTEXPR
	_If_sv<_Tp, basic_string&>
	assign(const _Tp& __svt, size_type __pos, size_type __n = npos)
	{
	  __sv_type __sv = __svt;
	  return _M_replace(size_type(0), this->size(),
	      __sv.data()
	      + std::__sv_check(__sv.size(), __pos, "basic_string::assign"),
	      std::__sv_limit(__sv.size(), __pos, __n));
	}
#endif // C++17

#if __cplusplus >= 201103L
      /**
       *  @brief  Insert multiple characters.
       *  @param __p  Const_iterator referencing location in string to
       *              insert at.
       *  @param __n  Number of characters to insert
       *  @param __c  The character to insert.
       *  @return  Iterator referencing the first inserted char.
       *  @throw  std::length_error  If new length exceeds @c max_size().
       *
       *  Inserts @a __n copies of character @a __c starting at the
       *  position referenced by iterator @a __p.  If adding
       *  characters causes the length to exceed max_size(),
       *  length_error is thrown.  The value of the string doesn't
       *  change if an error is thrown.
      */
      _GLIBCXX20_CONSTEXPR
      iterator
      insert(const_iterator __p, size_type __n, _CharT __c)
      {
	_GLIBCXX_DEBUG_PEDASSERT(__p >= begin() && __p <= end());
	const size_type __pos = __p - begin();
	this->replace(__p, __p, __n, __c);
	return iterator(this->_M_data() + __pos);
      }
#else
      /**
       *  @brief  Insert multiple characters.
       *  @param __p  Iterator referencing location in string to insert at.
       *  @param __n  Number of characters to insert
       *  @param __c  The character to insert.
       *  @throw  std::length_error  If new length exceeds @c max_size().
       *
       *  Inserts @a __n copies of character @a __c starting at the
       *  position referenced by iterator @a __p.  If adding
       *  characters causes the length to exceed max_size(),
       *  length_error is thrown.  The value of the string doesn't
       *  change if an error is thrown.
      */
      void
      insert(iterator __p, size_type __n, _CharT __c)
      {	this->replace(__p, __p, __n, __c);  }
#endif

#if __cplusplus >= 201103L
      /**
       *  @brief  Insert a range of characters.
       *  @param __p  Const_iterator referencing location in string to
       *              insert at.
       *  @param __beg  Start of range.
       *  @param __end  End of range.
       *  @return  Iterator referencing the first inserted char.
       *  @throw  std::length_error  If new length exceeds @c max_size().
       *
       *  Inserts characters in range [beg,end).  If adding characters
       *  causes the length to exceed max_size(), length_error is
       *  thrown.  The value of the string doesn't change if an error
       *  is thrown.
      */
      template<class _InputIterator,
	       typename = std::_RequireInputIter<_InputIterator>>
	_GLIBCXX20_CONSTEXPR
	iterator
        insert(const_iterator __p, _InputIterator __beg, _InputIterator __end)
        {
	  _GLIBCXX_DEBUG_PEDASSERT(__p >= begin() && __p <= end());
	  const size_type __pos = __p - begin();
	  this->replace(__p, __p, __beg, __end);
	  return iterator(this->_M_data() + __pos);
	}
#else
      /**
       *  @brief  Insert a range of characters.
       *  @param __p  Iterator referencing location in string to insert at.
       *  @param __beg  Start of range.
       *  @param __end  End of range.
       *  @throw  std::length_error  If new length exceeds @c max_size().
       *
       *  Inserts characters in range [__beg,__end).  If adding
       *  characters causes the length to exceed max_size(),
       *  length_error is thrown.  The value of the string doesn't
       *  change if an error is thrown.
      */
      template<class _InputIterator>
        void
        insert(iterator __p, _InputIterator __beg, _InputIterator __end)
        { this->replace(__p, __p, __beg, __end); }
#endif

#if __cplusplus >= 201103L
      /**
       *  @brief  Insert an initializer_list of characters.
       *  @param __p  Iterator referencing location in string to insert at.
       *  @param __l  The initializer_list of characters to insert.
       *  @throw  std::length_error  If new length exceeds @c max_size().
       */
      _GLIBCXX20_CONSTEXPR
      iterator
      insert(const_iterator __p, initializer_list<_CharT> __l)
      { return this->insert(__p, __l.begin(), __l.end()); }

#ifdef _GLIBCXX_DEFINING_STRING_INSTANTIATIONS
      // See PR libstdc++/83328
      void
      insert(iterator __p, initializer_list<_CharT> __l)
      {
	_GLIBCXX_DEBUG_PEDASSERT(__p >= begin() && __p <= end());
	this->insert(__p - begin(), __l.begin(), __l.size());
      }
#endif
#endif // C++11

      /**
       *  @brief  Insert value of a string.
       *  @param __pos1 Position in string to insert at.
       *  @param __str  The string to insert.
       *  @return  Reference to this string.
       *  @throw  std::length_error  If new length exceeds @c max_size().
       *
       *  Inserts value of @a __str starting at @a __pos1.  If adding
       *  characters causes the length to exceed max_size(),
       *  length_error is thrown.  The value of the string doesn't
       *  change if an error is thrown.
      */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      insert(size_type __pos1, const basic_string& __str)
      { return this->replace(__pos1, size_type(0),
			     __str._M_data(), __str.size()); }

      /**
       *  @brief  Insert a substring.
       *  @param __pos1  Position in string to insert at.
       *  @param __str   The string to insert.
       *  @param __pos2  Start of characters in str to insert.
       *  @param __n  Number of characters to insert.
       *  @return  Reference to this string.
       *  @throw  std::length_error  If new length exceeds @c max_size().
       *  @throw  std::out_of_range  If @a pos1 > size() or
       *  @a __pos2 > @a str.size().
       *
       *  Starting at @a pos1, insert @a __n character of @a __str
       *  beginning with @a __pos2.  If adding characters causes the
       *  length to exceed max_size(), length_error is thrown.  If @a
       *  __pos1 is beyond the end of this string or @a __pos2 is
       *  beyond the end of @a __str, out_of_range is thrown.  The
       *  value of the string doesn't change if an error is thrown.
      */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      insert(size_type __pos1, const basic_string& __str,
	     size_type __pos2, size_type __n = npos)
      { return this->replace(__pos1, size_type(0), __str._M_data()
			     + __str._M_check(__pos2, "basic_string::insert"),
			     __str._M_limit(__pos2, __n)); }

      /**
       *  @brief  Insert a C substring.
       *  @param __pos  Position in string to insert at.
       *  @param __s  The C string to insert.
       *  @param __n  The number of characters to insert.
       *  @return  Reference to this string.
       *  @throw  std::length_error  If new length exceeds @c max_size().
       *  @throw  std::out_of_range  If @a __pos is beyond the end of this
       *  string.
       *
       *  Inserts the first @a __n characters of @a __s starting at @a
       *  __pos.  If adding characters causes the length to exceed
       *  max_size(), length_error is thrown.  If @a __pos is beyond
       *  end(), out_of_range is thrown.  The value of the string
       *  doesn't change if an error is thrown.
      */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      insert(size_type __pos, const _CharT* __s, size_type __n)
      { return this->replace(__pos, size_type(0), __s, __n); }

      /**
       *  @brief  Insert a C string.
       *  @param __pos  Position in string to insert at.
       *  @param __s  The C string to insert.
       *  @return  Reference to this string.
       *  @throw  std::length_error  If new length exceeds @c max_size().
       *  @throw  std::out_of_range  If @a pos is beyond the end of this
       *  string.
       *
       *  Inserts the first @a n characters of @a __s starting at @a __pos.  If
       *  adding characters causes the length to exceed max_size(),
       *  length_error is thrown.  If @a __pos is beyond end(), out_of_range is
       *  thrown.  The value of the string doesn't change if an error is
       *  thrown.
      */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      insert(size_type __pos, const _CharT* __s)
      {
	__glibcxx_requires_string(__s);
	return this->replace(__pos, size_type(0), __s,
			     traits_type::length(__s));
      }

      /**
       *  @brief  Insert multiple characters.
       *  @param __pos  Index in string to insert at.
       *  @param __n  Number of characters to insert
       *  @param __c  The character to insert.
       *  @return  Reference to this string.
       *  @throw  std::length_error  If new length exceeds @c max_size().
       *  @throw  std::out_of_range  If @a __pos is beyond the end of this
       *  string.
       *
       *  Inserts @a __n copies of character @a __c starting at index
       *  @a __pos.  If adding characters causes the length to exceed
       *  max_size(), length_error is thrown.  If @a __pos > length(),
       *  out_of_range is thrown.  The value of the string doesn't
       *  change if an error is thrown.
      */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      insert(size_type __pos, size_type __n, _CharT __c)
      { return _M_replace_aux(_M_check(__pos, "basic_string::insert"),
			      size_type(0), __n, __c); }

      /**
       *  @brief  Insert one character.
       *  @param __p  Iterator referencing position in string to insert at.
       *  @param __c  The character to insert.
       *  @return  Iterator referencing newly inserted char.
       *  @throw  std::length_error  If new length exceeds @c max_size().
       *
       *  Inserts character @a __c at position referenced by @a __p.
       *  If adding character causes the length to exceed max_size(),
       *  length_error is thrown.  If @a __p is beyond end of string,
       *  out_of_range is thrown.  The value of the string doesn't
       *  change if an error is thrown.
      */
      _GLIBCXX20_CONSTEXPR
      iterator
      insert(__const_iterator __p, _CharT __c)
      {
	_GLIBCXX_DEBUG_PEDASSERT(__p >= begin() && __p <= end());
	const size_type __pos = __p - begin();
	_M_replace_aux(__pos, size_type(0), size_type(1), __c);
	return iterator(_M_data() + __pos);
      }

#if __cplusplus >= 201703L
      /**
       *  @brief  Insert a string_view.
       *  @param __pos  Position in string to insert at.
       *  @param __svt  The object convertible to string_view to insert.
       *  @return  Reference to this string.
      */
      template<typename _Tp>
	_GLIBCXX20_CONSTEXPR
	_If_sv<_Tp, basic_string&>
	insert(size_type __pos, const _Tp& __svt)
	{
	  __sv_type __sv = __svt;
	  return this->insert(__pos, __sv.data(), __sv.size());
	}

      /**
       *  @brief  Insert a string_view.
       *  @param __pos1  Position in string to insert at.
       *  @param __svt   The object convertible to string_view to insert from.
       *  @param __pos2  Start of characters in str to insert.
       *  @param __n    The number of characters to insert.
       *  @return  Reference to this string.
      */
      template<typename _Tp>
	_GLIBCXX20_CONSTEXPR
	_If_sv<_Tp, basic_string&>
	insert(size_type __pos1, const _Tp& __svt,
	       size_type __pos2, size_type __n = npos)
	{
	  __sv_type __sv = __svt;
	  return this->replace(__pos1, size_type(0),
	      __sv.data()
	      + std::__sv_check(__sv.size(), __pos2, "basic_string::insert"),
	      std::__sv_limit(__sv.size(), __pos2, __n));
	}
#endif // C++17

      /**
       *  @brief  Remove characters.
       *  @param __pos  Index of first character to remove (default 0).
       *  @param __n  Number of characters to remove (default remainder).
       *  @return  Reference to this string.
       *  @throw  std::out_of_range  If @a pos is beyond the end of this
       *  string.
       *
       *  Removes @a __n characters from this string starting at @a
       *  __pos.  The length of the string is reduced by @a __n.  If
       *  there are < @a __n characters to remove, the remainder of
       *  the string is truncated.  If @a __p is beyond end of string,
       *  out_of_range is thrown.  The value of the string doesn't
       *  change if an error is thrown.
      */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      erase(size_type __pos = 0, size_type __n = npos)
      {
	_M_check(__pos, "basic_string::erase");
	if (__n == npos)
	  this->_M_set_length(__pos);
	else if (__n != 0)
	  this->_M_erase(__pos, _M_limit(__pos, __n));
	return *this;
      }

      /**
       *  @brief  Remove one character.
       *  @param __position  Iterator referencing the character to remove.
       *  @return  iterator referencing same location after removal.
       *
       *  Removes the character at @a __position from this string. The value
       *  of the string doesn't change if an error is thrown.
      */
      _GLIBCXX20_CONSTEXPR
      iterator
      erase(__const_iterator __position)
      {
	_GLIBCXX_DEBUG_PEDASSERT(__position >= begin()
				 && __position < end());
	const size_type __pos = __position - begin();
	this->_M_erase(__pos, size_type(1));
	return iterator(_M_data() + __pos);
      }

      /**
       *  @brief  Remove a range of characters.
       *  @param __first  Iterator referencing the first character to remove.
       *  @param __last  Iterator referencing the end of the range.
       *  @return  Iterator referencing location of first after removal.
       *
       *  Removes the characters in the range [first,last) from this string.
       *  The value of the string doesn't change if an error is thrown.
      */
      _GLIBCXX20_CONSTEXPR
      iterator
      erase(__const_iterator __first, __const_iterator __last)
      {
	_GLIBCXX_DEBUG_PEDASSERT(__first >= begin() && __first <= __last
				 && __last <= end());
        const size_type __pos = __first - begin();
	if (__last == end())
	  this->_M_set_length(__pos);
	else
	  this->_M_erase(__pos, __last - __first);
	return iterator(this->_M_data() + __pos);
      }

#if __cplusplus >= 201103L
      /**
       *  @brief  Remove the last character.
       *
       *  The string must be non-empty.
       */
      _GLIBCXX20_CONSTEXPR
      void
      pop_back() noexcept
      {
	__glibcxx_assert(!empty());
	_M_erase(size() - 1, 1);
      }
#endif // C++11

      /**
       *  @brief  Replace characters with value from another string.
       *  @param __pos  Index of first character to replace.
       *  @param __n  Number of characters to be replaced.
       *  @param __str  String to insert.
       *  @return  Reference to this string.
       *  @throw  std::out_of_range  If @a pos is beyond the end of this
       *  string.
       *  @throw  std::length_error  If new length exceeds @c max_size().
       *
       *  Removes the characters in the range [__pos,__pos+__n) from
       *  this string.  In place, the value of @a __str is inserted.
       *  If @a __pos is beyond end of string, out_of_range is thrown.
       *  If the length of the result exceeds max_size(), length_error
       *  is thrown.  The value of the string doesn't change if an
       *  error is thrown.
      */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      replace(size_type __pos, size_type __n, const basic_string& __str)
      { return this->replace(__pos, __n, __str._M_data(), __str.size()); }

      /**
       *  @brief  Replace characters with value from another string.
       *  @param __pos1  Index of first character to replace.
       *  @param __n1  Number of characters to be replaced.
       *  @param __str  String to insert.
       *  @param __pos2  Index of first character of str to use.
       *  @param __n2  Number of characters from str to use.
       *  @return  Reference to this string.
       *  @throw  std::out_of_range  If @a __pos1 > size() or @a __pos2 >
       *  __str.size().
       *  @throw  std::length_error  If new length exceeds @c max_size().
       *
       *  Removes the characters in the range [__pos1,__pos1 + n) from this
       *  string.  In place, the value of @a __str is inserted.  If @a __pos is
       *  beyond end of string, out_of_range is thrown.  If the length of the
       *  result exceeds max_size(), length_error is thrown.  The value of the
       *  string doesn't change if an error is thrown.
      */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      replace(size_type __pos1, size_type __n1, const basic_string& __str,
	      size_type __pos2, size_type __n2 = npos)
      { return this->replace(__pos1, __n1, __str._M_data()
			     + __str._M_check(__pos2, "basic_string::replace"),
			     __str._M_limit(__pos2, __n2)); }

      /**
       *  @brief  Replace characters with value of a C substring.
       *  @param __pos  Index of first character to replace.
       *  @param __n1  Number of characters to be replaced.
       *  @param __s  C string to insert.
       *  @param __n2  Number of characters from @a s to use.
       *  @return  Reference to this string.
       *  @throw  std::out_of_range  If @a pos1 > size().
       *  @throw  std::length_error  If new length exceeds @c max_size().
       *
       *  Removes the characters in the range [__pos,__pos + __n1)
       *  from this string.  In place, the first @a __n2 characters of
       *  @a __s are inserted, or all of @a __s if @a __n2 is too large.  If
       *  @a __pos is beyond end of string, out_of_range is thrown.  If
       *  the length of result exceeds max_size(), length_error is
       *  thrown.  The value of the string doesn't change if an error
       *  is thrown.
      */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      replace(size_type __pos, size_type __n1, const _CharT* __s,
	      size_type __n2)
      {
	__glibcxx_requires_string_len(__s, __n2);
	return _M_replace(_M_check(__pos, "basic_string::replace"),
			  _M_limit(__pos, __n1), __s, __n2);
      }

      /**
       *  @brief  Replace characters with value of a C string.
       *  @param __pos  Index of first character to replace.
       *  @param __n1  Number of characters to be replaced.
       *  @param __s  C string to insert.
       *  @return  Reference to this string.
       *  @throw  std::out_of_range  If @a pos > size().
       *  @throw  std::length_error  If new length exceeds @c max_size().
       *
       *  Removes the characters in the range [__pos,__pos + __n1)
       *  from this string.  In place, the characters of @a __s are
       *  inserted.  If @a __pos is beyond end of string, out_of_range
       *  is thrown.  If the length of result exceeds max_size(),
       *  length_error is thrown.  The value of the string doesn't
       *  change if an error is thrown.
      */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      replace(size_type __pos, size_type __n1, const _CharT* __s)
      {
	__glibcxx_requires_string(__s);
	return this->replace(__pos, __n1, __s, traits_type::length(__s));
      }

      /**
       *  @brief  Replace characters with multiple characters.
       *  @param __pos  Index of first character to replace.
       *  @param __n1  Number of characters to be replaced.
       *  @param __n2  Number of characters to insert.
       *  @param __c  Character to insert.
       *  @return  Reference to this string.
       *  @throw  std::out_of_range  If @a __pos > size().
       *  @throw  std::length_error  If new length exceeds @c max_size().
       *
       *  Removes the characters in the range [pos,pos + n1) from this
       *  string.  In place, @a __n2 copies of @a __c are inserted.
       *  If @a __pos is beyond end of string, out_of_range is thrown.
       *  If the length of result exceeds max_size(), length_error is
       *  thrown.  The value of the string doesn't change if an error
       *  is thrown.
      */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      replace(size_type __pos, size_type __n1, size_type __n2, _CharT __c)
      { return _M_replace_aux(_M_check(__pos, "basic_string::replace"),
			      _M_limit(__pos, __n1), __n2, __c); }

      /**
       *  @brief  Replace range of characters with string.
       *  @param __i1  Iterator referencing start of range to replace.
       *  @param __i2  Iterator referencing end of range to replace.
       *  @param __str  String value to insert.
       *  @return  Reference to this string.
       *  @throw  std::length_error  If new length exceeds @c max_size().
       *
       *  Removes the characters in the range [__i1,__i2).  In place,
       *  the value of @a __str is inserted.  If the length of result
       *  exceeds max_size(), length_error is thrown.  The value of
       *  the string doesn't change if an error is thrown.
      */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      replace(__const_iterator __i1, __const_iterator __i2,
	      const basic_string& __str)
      { return this->replace(__i1, __i2, __str._M_data(), __str.size()); }

      /**
       *  @brief  Replace range of characters with C substring.
       *  @param __i1  Iterator referencing start of range to replace.
       *  @param __i2  Iterator referencing end of range to replace.
       *  @param __s  C string value to insert.
       *  @param __n  Number of characters from s to insert.
       *  @return  Reference to this string.
       *  @throw  std::length_error  If new length exceeds @c max_size().
       *
       *  Removes the characters in the range [__i1,__i2).  In place,
       *  the first @a __n characters of @a __s are inserted.  If the
       *  length of result exceeds max_size(), length_error is thrown.
       *  The value of the string doesn't change if an error is
       *  thrown.
      */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      replace(__const_iterator __i1, __const_iterator __i2,
	      const _CharT* __s, size_type __n)
      {
	_GLIBCXX_DEBUG_PEDASSERT(begin() <= __i1 && __i1 <= __i2
				 && __i2 <= end());
	return this->replace(__i1 - begin(), __i2 - __i1, __s, __n);
      }

      /**
       *  @brief  Replace range of characters with C string.
       *  @param __i1  Iterator referencing start of range to replace.
       *  @param __i2  Iterator referencing end of range to replace.
       *  @param __s  C string value to insert.
       *  @return  Reference to this string.
       *  @throw  std::length_error  If new length exceeds @c max_size().
       *
       *  Removes the characters in the range [__i1,__i2).  In place,
       *  the characters of @a __s are inserted.  If the length of
       *  result exceeds max_size(), length_error is thrown.  The
       *  value of the string doesn't change if an error is thrown.
      */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      replace(__const_iterator __i1, __const_iterator __i2, const _CharT* __s)
      {
	__glibcxx_requires_string(__s);
	return this->replace(__i1, __i2, __s, traits_type::length(__s));
      }

      /**
       *  @brief  Replace range of characters with multiple characters
       *  @param __i1  Iterator referencing start of range to replace.
       *  @param __i2  Iterator referencing end of range to replace.
       *  @param __n  Number of characters to insert.
       *  @param __c  Character to insert.
       *  @return  Reference to this string.
       *  @throw  std::length_error  If new length exceeds @c max_size().
       *
       *  Removes the characters in the range [__i1,__i2).  In place,
       *  @a __n copies of @a __c are inserted.  If the length of
       *  result exceeds max_size(), length_error is thrown.  The
       *  value of the string doesn't change if an error is thrown.
      */
      _GLIBCXX20_CONSTEXPR
      basic_string&
      replace(__const_iterator __i1, __const_iterator __i2, size_type __n,
	      _CharT __c)
      {
	_GLIBCXX_DEBUG_PEDASSERT(begin() <= __i1 && __i1 <= __i2
				 && __i2 <= end());
	return _M_replace_aux(__i1 - begin(), __i2 - __i1, __n, __c);
      }

      /**
       *  @brief  Replace range of characters with range.
       *  @param __i1  Iterator referencing start of range to replace.
       *  @param __i2  Iterator referencing end of range to replace.
       *  @param __k1  Iterator referencing start of range to insert.
       *  @param __k2  Iterator referencing end of range to insert.
       *  @return  Reference to this string.
       *  @throw  std::length_error  If new length exceeds @c max_size().
       *
       *  Removes the characters in the range [__i1,__i2).  In place,
       *  characters in the range [__k1,__k2) are inserted.  If the
       *  length of result exceeds max_size(), length_error is thrown.
       *  The value of the string doesn't change if an error is
       *  thrown.
      */
#if __cplusplus >= 201103L
      template<class _InputIterator,
	       typename = std::_RequireInputIter<_InputIterator>>
	_GLIBCXX20_CONSTEXPR
        basic_string&
        replace(const_iterator __i1, const_iterator __i2,
		_InputIterator __k1, _InputIterator __k2)
        {
	  _GLIBCXX_DEBUG_PEDASSERT(begin() <= __i1 && __i1 <= __i2
				   && __i2 <= end());
	  __glibcxx_requires_valid_range(__k1, __k2);
	  return this->_M_replace_dispatch(__i1, __i2, __k1, __k2,
					   std::__false_type());
	}
#else
      template<class _InputIterator>
#ifdef _GLIBCXX_DISAMBIGUATE_REPLACE_INST
        typename __enable_if_not_native_iterator<_InputIterator>::__type
#else
        basic_string&
#endif
        replace(iterator __i1, iterator __i2,
		_InputIterator __k1, _InputIterator __k2)
        {
	  _GLIBCXX_DEBUG_PEDASSERT(begin() <= __i1 && __i1 <= __i2
				   && __i2 <= end());
	  __glibcxx_requires_valid_range(__k1, __k2);
	  typedef typename std::__is_integer<_InputIterator>::__type _Integral;
	  return _M_replace_dispatch(__i1, __i2, __k1, __k2, _Integral());
	}
#endif

      // Specializations for the common case of pointer and iterator:
      // useful to avoid the overhead of temporary buffering in _M_replace.
      _GLIBCXX20_CONSTEXPR
      basic_string&
      replace(__const_iterator __i1, __const_iterator __i2,
	      _CharT* __k1, _CharT* __k2)
      {
	_GLIBCXX_DEBUG_PEDASSERT(begin() <= __i1 && __i1 <= __i2
				 && __i2 <= end());
	__glibcxx_requires_valid_range(__k1, __k2);
	return this->replace(__i1 - begin(), __i2 - __i1,
			     __k1, __k2 - __k1);
      }

      _GLIBCXX20_CONSTEXPR
      basic_string&
      replace(__const_iterator __i1, __const_iterator __i2,
	      const _CharT* __k1, const _CharT* __k2)
      {
	_GLIBCXX_DEBUG_PEDASSERT(begin() <= __i1 && __i1 <= __i2
				 && __i2 <= end());
	__glibcxx_requires_valid_range(__k1, __k2);
	return this->replace(__i1 - begin(), __i2 - __i1,
			     __k1, __k2 - __k1);
      }

      _GLIBCXX20_CONSTEXPR
      basic_string&
      replace(__const_iterator __i1, __const_iterator __i2,
	      iterator __k1, iterator __k2)
      {
	_GLIBCXX_DEBUG_PEDASSERT(begin() <= __i1 && __i1 <= __i2
				 && __i2 <= end());
	__glibcxx_requires_valid_range(__k1, __k2);
	return this->replace(__i1 - begin(), __i2 - __i1,
			     __k1.base(), __k2 - __k1);
      }

      _GLIBCXX20_CONSTEXPR
      basic_string&
      replace(__const_iterator __i1, __const_iterator __i2,
	      const_iterator __k1, const_iterator __k2)
      {
	_GLIBCXX_DEBUG_PEDASSERT(begin() <= __i1 && __i1 <= __i2
				 && __i2 <= end());
	__glibcxx_requires_valid_range(__k1, __k2);
	return this->replace(__i1 - begin(), __i2 - __i1,
			     __k1.base(), __k2 - __k1);
      }

#if __cplusplus >= 201103L
      /**
       *  @brief  Replace range of characters with initializer_list.
       *  @param __i1  Iterator referencing start of range to replace.
       *  @param __i2  Iterator referencing end of range to replace.
       *  @param __l  The initializer_list of characters to insert.
       *  @return  Reference to this string.
       *  @throw  std::length_error  If new length exceeds @c max_size().
       *
       *  Removes the characters in the range [__i1,__i2).  In place,
       *  characters in the range [__k1,__k2) are inserted.  If the
       *  length of result exceeds max_size(), length_error is thrown.
       *  The value of the string doesn't change if an error is
       *  thrown.
      */
      _GLIBCXX20_CONSTEXPR
      basic_string& replace(const_iterator __i1, const_iterator __i2,
			    initializer_list<_CharT> __l)
      { return this->replace(__i1, __i2, __l.begin(), __l.size()); }
#endif // C++11

#if __cplusplus >= 201703L
      /**
       *  @brief  Replace range of characters with string_view.
       *  @param __pos  The position to replace at.
       *  @param __n    The number of characters to replace.
       *  @param __svt  The object convertible to string_view to insert.
       *  @return  Reference to this string.
      */
      template<typename _Tp>
	_GLIBCXX20_CONSTEXPR
	_If_sv<_Tp, basic_string&>
	replace(size_type __pos, size_type __n, const _Tp& __svt)
	{
	  __sv_type __sv = __svt;
	  return this->replace(__pos, __n, __sv.data(), __sv.size());
	}

      /**
       *  @brief  Replace range of characters with string_view.
       *  @param __pos1  The position to replace at.
       *  @param __n1    The number of characters to replace.
       *  @param __svt   The object convertible to string_view to insert from.
       *  @param __pos2  The position in the string_view to insert from.
       *  @param __n2    The number of characters to insert.
       *  @return  Reference to this string.
      */
      template<typename _Tp>
	_GLIBCXX20_CONSTEXPR
	_If_sv<_Tp, basic_string&>
	replace(size_type __pos1, size_type __n1, const _Tp& __svt,
		size_type __pos2, size_type __n2 = npos)
	{
	  __sv_type __sv = __svt;
	  return this->replace(__pos1, __n1,
	      __sv.data()
	      + std::__sv_check(__sv.size(), __pos2, "basic_string::replace"),
	      std::__sv_limit(__sv.size(), __pos2, __n2));
	}

      /**
       *  @brief  Replace range of characters with string_view.
       *  @param __i1    An iterator referencing the start position
          to replace at.
       *  @param __i2    An iterator referencing the end position
          for the replace.
       *  @param __svt   The object convertible to string_view to insert from.
       *  @return  Reference to this string.
      */
      template<typename _Tp>
	_GLIBCXX20_CONSTEXPR
	_If_sv<_Tp, basic_string&>
	replace(const_iterator __i1, const_iterator __i2, const _Tp& __svt)
	{
	  __sv_type __sv = __svt;
	  return this->replace(__i1 - begin(), __i2 - __i1, __sv);
	}
#endif // C++17

    private:
      template<class _Integer>
	_GLIBCXX20_CONSTEXPR
	basic_string&
	_M_replace_dispatch(const_iterator __i1, const_iterator __i2,
			    _Integer __n, _Integer __val, __true_type)
        { return _M_replace_aux(__i1 - begin(), __i2 - __i1, __n, __val); }

      template<class _InputIterator>
	_GLIBCXX20_CONSTEXPR
	basic_string&
	_M_replace_dispatch(const_iterator __i1, const_iterator __i2,
			    _InputIterator __k1, _InputIterator __k2,
			    __false_type);

      _GLIBCXX20_CONSTEXPR
      basic_string&
      _M_replace_aux(size_type __pos1, size_type __n1, size_type __n2,
		     _CharT __c);

      _GLIBCXX20_CONSTEXPR
      basic_string&
      _M_replace(size_type __pos, size_type __len1, const _CharT* __s,
		 const size_type __len2);

      _GLIBCXX20_CONSTEXPR
      basic_string&
      _M_append(const _CharT* __s, size_type __n);

    public:

      /**
       *  @brief  Copy substring into C string.
       *  @param __s  C string to copy value into.
       *  @param __n  Number of characters to copy.
       *  @param __pos  Index of first character to copy.
       *  @return  Number of characters actually copied
       *  @throw  std::out_of_range  If __pos > size().
       *
       *  Copies up to @a __n characters starting at @a __pos into the
       *  C string @a __s.  If @a __pos is %greater than size(),
       *  out_of_range is thrown.
      */
      _GLIBCXX20_CONSTEXPR
      size_type
      copy(_CharT* __s, size_type __n, size_type __pos = 0) const;

      /**
       *  @brief  Swap contents with another string.
       *  @param __s  String to swap with.
       *
       *  Exchanges the contents of this string with that of @a __s in constant
       *  time.
      */
      _GLIBCXX20_CONSTEXPR
      void
      swap(basic_string& __s) _GLIBCXX_NOEXCEPT;

      // String operations:
      /**
       *  @brief  Return const pointer to null-terminated contents.
       *
       *  This is a handle to internal data.  Do not modify or dire things may
       *  happen.
      */
      _GLIBCXX20_CONSTEXPR
      const _CharT*
      c_str() const _GLIBCXX_NOEXCEPT
      { return _M_data(); }

      /**
       *  @brief  Return const pointer to contents.
       *
       *  This is a pointer to internal data.  It is undefined to modify
       *  the contents through the returned pointer. To get a pointer that
       *  allows modifying the contents use @c &str[0] instead,
       *  (or in C++17 the non-const @c str.data() overload).
      */
      _GLIBCXX20_CONSTEXPR
      const _CharT*
      data() const _GLIBCXX_NOEXCEPT
      { return _M_data(); }

#if __cplusplus >= 201703L
      /**
       *  @brief  Return non-const pointer to contents.
       *
       *  This is a pointer to the character sequence held by the string.
       *  Modifying the characters in the sequence is allowed.
      */
      _GLIBCXX20_CONSTEXPR
      _CharT*
      data() noexcept
      { return _M_data(); }
#endif

      /**
       *  @brief  Return copy of allocator used to construct this string.
      */
      _GLIBCXX20_CONSTEXPR
      allocator_type
      get_allocator() const _GLIBCXX_NOEXCEPT
      { return _M_get_allocator(); }

      /**
       *  @brief  Find position of a C substring.
       *  @param __s  C string to locate.
       *  @param __pos  Index of character to search from.
       *  @param __n  Number of characters from @a s to search for.
       *  @return  Index of start of first occurrence.
       *
       *  Starting from @a __pos, searches forward for the first @a
       *  __n characters in @a __s within this string.  If found,
       *  returns the index where it begins.  If not found, returns
       *  npos.
      */
      _GLIBCXX20_CONSTEXPR
      size_type
      find(const _CharT* __s, size_type __pos, size_type __n) const
      _GLIBCXX_NOEXCEPT;

      /**
       *  @brief  Find position of a string.
       *  @param __str  String to locate.
       *  @param __pos  Index of character to search from (default 0).
       *  @return  Index of start of first occurrence.
       *
       *  Starting from @a __pos, searches forward for value of @a __str within
       *  this string.  If found, returns the index where it begins.  If not
       *  found, returns npos.
      */
      _GLIBCXX20_CONSTEXPR
      size_type
      find(const basic_string& __str, size_type __pos = 0) const
      _GLIBCXX_NOEXCEPT
      { return this->find(__str.data(), __pos, __str.size()); }

#if __cplusplus >= 201703L
      /**
       *  @brief  Find position of a string_view.
       *  @param __svt  The object convertible to string_view to locate.
       *  @param __pos  Index of character to search from (default 0).
       *  @return  Index of start of first occurrence.
      */
      template<typename _Tp>
	_GLIBCXX20_CONSTEXPR
	_If_sv<_Tp, size_type>
	find(const _Tp& __svt, size_type __pos = 0) const
	noexcept(is_same<_Tp, __sv_type>::value)
	{
	  __sv_type __sv = __svt;
	  return this->find(__sv.data(), __pos, __sv.size());
	}
#endif // C++17

      /**
       *  @brief  Find position of a C string.
       *  @param __s  C string to locate.
       *  @param __pos  Index of character to search from (default 0).
       *  @return  Index of start of first occurrence.
       *
       *  Starting from @a __pos, searches forward for the value of @a
       *  __s within this string.  If found, returns the index where
       *  it begins.  If not found, returns npos.
      */
      _GLIBCXX20_CONSTEXPR
      size_type
      find(const _CharT* __s, size_type __pos = 0) const _GLIBCXX_NOEXCEPT
      {
	__glibcxx_requires_string(__s);
	return this->find(__s, __pos, traits_type::length(__s));
      }

      /**
       *  @brief  Find position of a character.
       *  @param __c  Character to locate.
       *  @param __pos  Index of character to search from (default 0).
       *  @return  Index of first occurrence.
       *
       *  Starting from @a __pos, searches forward for @a __c within
       *  this string.  If found, returns the index where it was
       *  found.  If not found, returns npos.
      */
      _GLIBCXX20_CONSTEXPR
      size_type
      find(_CharT __c, size_type __pos = 0) const _GLIBCXX_NOEXCEPT;

      /**
       *  @brief  Find last position of a string.
       *  @param __str  String to locate.
       *  @param __pos  Index of character to search back from (default end).
       *  @return  Index of start of last occurrence.
       *
       *  Starting from @a __pos, searches backward for value of @a
       *  __str within this string.  If found, returns the index where
       *  it begins.  If not found, returns npos.
      */
      _GLIBCXX20_CONSTEXPR
      size_type
      rfind(const basic_string& __str, size_type __pos = npos) const
      _GLIBCXX_NOEXCEPT
      { return this->rfind(__str.data(), __pos, __str.size()); }

#if __cplusplus >= 201703L
      /**
       *  @brief  Find last position of a string_view.
       *  @param __svt  The object convertible to string_view to locate.
       *  @param __pos  Index of character to search back from (default end).
       *  @return  Index of start of last occurrence.
      */
      template<typename _Tp>
	_GLIBCXX20_CONSTEXPR
	_If_sv<_Tp, size_type>
	rfind(const _Tp& __svt, size_type __pos = npos) const
	noexcept(is_same<_Tp, __sv_type>::value)
	{
	  __sv_type __sv = __svt;
	  return this->rfind(__sv.data(), __pos, __sv.size());
	}
#endif // C++17

      /**
       *  @brief  Find last position of a C substring.
       *  @param __s  C string to locate.
       *  @param __pos  Index of character to search back from.
       *  @param __n  Number of characters from s to search for.
       *  @return  Index of start of last occurrence.
       *
       *  Starting from @a __pos, searches backward for the first @a
       *  __n characters in @a __s within this string.  If found,
       *  returns the index where it begins.  If not found, returns
       *  npos.
      */
      _GLIBCXX20_CONSTEXPR
      size_type
      rfind(const _CharT* __s, size_type __pos, size_type __n) const
      _GLIBCXX_NOEXCEPT;

      /**
       *  @brief  Find last position of a C string.
       *  @param __s  C string to locate.
       *  @param __pos  Index of character to start search at (default end).
       *  @return  Index of start of  last occurrence.
       *
       *  Starting from @a __pos, searches backward for the value of
       *  @a __s within this string.  If found, returns the index
       *  where it begins.  If not found, returns npos.
      */
      _GLIBCXX20_CONSTEXPR
      size_type
      rfind(const _CharT* __s, size_type __pos = npos) const
      {
	__glibcxx_requires_string(__s);
	return this->rfind(__s, __pos, traits_type::length(__s));
      }

      /**
       *  @brief  Find last position of a character.
       *  @param __c  Character to locate.
       *  @param __pos  Index of character to search back from (default end).
       *  @return  Index of last occurrence.
       *
       *  Starting from @a __pos, searches backward for @a __c within
       *  this string.  If found, returns the index where it was
       *  found.  If not found, returns npos.
      */
      _GLIBCXX20_CONSTEXPR
      size_type
      rfind(_CharT __c, size_type __pos = npos) const _GLIBCXX_NOEXCEPT;

      /**
       *  @brief  Find position of a character of string.
       *  @param __str  String containing characters to locate.
       *  @param __pos  Index of character to search from (default 0).
       *  @return  Index of first occurrence.
       *
       *  Starting from @a __pos, searches forward for one of the
       *  characters of @a __str within this string.  If found,
       *  returns the index where it was found.  If not found, returns
       *  npos.
      */
      _GLIBCXX20_CONSTEXPR
      size_type
      find_first_of(const basic_string& __str, size_type __pos = 0) const
      _GLIBCXX_NOEXCEPT
      { return this->find_first_of(__str.data(), __pos, __str.size()); }

#if __cplusplus >= 201703L
      /**
       *  @brief  Find position of a character of a string_view.
       *  @param __svt  An object convertible to string_view containing
       *                characters to locate.
       *  @param __pos  Index of character to search from (default 0).
       *  @return  Index of first occurrence.
      */
      template<typename _Tp>
	_GLIBCXX20_CONSTEXPR
	_If_sv<_Tp, size_type>
	find_first_of(const _Tp& __svt, size_type __pos = 0) const
	noexcept(is_same<_Tp, __sv_type>::value)
	{
	  __sv_type __sv = __svt;
	  return this->find_first_of(__sv.data(), __pos, __sv.size());
	}
#endif // C++17

      /**
       *  @brief  Find position of a character of C substring.
       *  @param __s  String containing characters to locate.
       *  @param __pos  Index of character to search from.
       *  @param __n  Number of characters from s to search for.
       *  @return  Index of first occurrence.
       *
       *  Starting from @a __pos, searches forward for one of the
       *  first @a __n characters of @a __s within this string.  If
       *  found, returns the index where it was found.  If not found,
       *  returns npos.
      */
      _GLIBCXX20_CONSTEXPR
      size_type
      find_first_of(const _CharT* __s, size_type __pos, size_type __n) const
      _GLIBCXX_NOEXCEPT;

      /**
       *  @brief  Find position of a character of C string.
       *  @param __s  String containing characters to locate.
       *  @param __pos  Index of character to search from (default 0).
       *  @return  Index of first occurrence.
       *
       *  Starting from @a __pos, searches forward for one of the
       *  characters of @a __s within this string.  If found, returns
       *  the index where it was found.  If not found, returns npos.
      */
      _GLIBCXX20_CONSTEXPR
      size_type
      find_first_of(const _CharT* __s, size_type __pos = 0) const
      _GLIBCXX_NOEXCEPT
      {
	__glibcxx_requires_string(__s);
	return this->find_first_of(__s, __pos, traits_type::length(__s));
      }

      /**
       *  @brief  Find position of a character.
       *  @param __c  Character to locate.
       *  @param __pos  Index of character to search from (default 0).
       *  @return  Index of first occurrence.
       *
       *  Starting from @a __pos, searches forward for the character
       *  @a __c within this string.  If found, returns the index
       *  where it was found.  If not found, returns npos.
       *
       *  Note: equivalent to find(__c, __pos).
      */
      _GLIBCXX20_CONSTEXPR
      size_type
      find_first_of(_CharT __c, size_type __pos = 0) const _GLIBCXX_NOEXCEPT
      { return this->find(__c, __pos); }

      /**
       *  @brief  Find last position of a character of string.
       *  @param __str  String containing characters to locate.
       *  @param __pos  Index of character to search back from (default end).
       *  @return  Index of last occurrence.
       *
       *  Starting from @a __pos, searches backward for one of the
       *  characters of @a __str within this string.  If found,
       *  returns the index where it was found.  If not found, returns
       *  npos.
      */
      _GLIBCXX20_CONSTEXPR
      size_type
      find_last_of(const basic_string& __str, size_type __pos = npos) const
      _GLIBCXX_NOEXCEPT
      { return this->find_last_of(__str.data(), __pos, __str.size()); }

#if __cplusplus >= 201703L
      /**
       *  @brief  Find last position of a character of string.
       *  @param __svt  An object convertible to string_view containing
       *                characters to locate.
       *  @param __pos  Index of character to search back from (default end).
       *  @return  Index of last occurrence.
      */
      template<typename _Tp>
	_GLIBCXX20_CONSTEXPR
	_If_sv<_Tp, size_type>
	find_last_of(const _Tp& __svt, size_type __pos = npos) const
	noexcept(is_same<_Tp, __sv_type>::value)
	{
	  __sv_type __sv = __svt;
	  return this->find_last_of(__sv.data(), __pos, __sv.size());
	}
#endif // C++17

      /**
       *  @brief  Find last position of a character of C substring.
       *  @param __s  C string containing characters to locate.
       *  @param __pos  Index of character to search back from.
       *  @param __n  Number of characters from s to search for.
       *  @return  Index of last occurrence.
       *
       *  Starting from @a __pos, searches backward for one of the
       *  first @a __n characters of @a __s within this string.  If
       *  found, returns the index where it was found.  If not found,
       *  returns npos.
      */
      _GLIBCXX20_CONSTEXPR
      size_type
      find_last_of(const _CharT* __s, size_type __pos, size_type __n) const
      _GLIBCXX_NOEXCEPT;

      /**
       *  @brief  Find last position of a character of C string.
       *  @param __s  C string containing characters to locate.
       *  @param __pos  Index of character to search back from (default end).
       *  @return  Index of last occurrence.
       *
       *  Starting from @a __pos, searches backward for one of the
       *  characters of @a __s within this string.  If found, returns
       *  the index where it was found.  If not found, returns npos.
      */
      _GLIBCXX20_CONSTEXPR
      size_type
      find_last_of(const _CharT* __s, size_type __pos = npos) const
      _GLIBCXX_NOEXCEPT
      {
	__glibcxx_requires_string(__s);
	return this->find_last_of(__s, __pos, traits_type::length(__s));
      }

      /**
       *  @brief  Find last position of a character.
       *  @param __c  Character to locate.
       *  @param __pos  Index of character to search back from (default end).
       *  @return  Index of last occurrence.
       *
       *  Starting from @a __pos, searches backward for @a __c within
       *  this string.  If found, returns the index where it was
       *  found.  If not found, returns npos.
       *
       *  Note: equivalent to rfind(__c, __pos).
      */
      _GLIBCXX20_CONSTEXPR
      size_type
      find_last_of(_CharT __c, size_type __pos = npos) const _GLIBCXX_NOEXCEPT
      { return this->rfind(__c, __pos); }

      /**
       *  @brief  Find position of a character not in string.
       *  @param __str  String containing characters to avoid.
       *  @param __pos  Index of character to search from (default 0).
       *  @return  Index of first occurrence.
       *
       *  Starting from @a __pos, searches forward for a character not contained
       *  in @a __str within this string.  If found, returns the index where it
       *  was found.  If not found, returns npos.
      */
      _GLIBCXX20_CONSTEXPR
      size_type
      find_first_not_of(const basic_string& __str, size_type __pos = 0) const
      _GLIBCXX_NOEXCEPT
      { return this->find_first_not_of(__str.data(), __pos, __str.size()); }

#if __cplusplus >= 201703L
      /**
       *  @brief  Find position of a character not in a string_view.
       *  @param __svt  A object convertible to string_view containing
       *                characters to avoid.
       *  @param __pos  Index of character to search from (default 0).
       *  @return  Index of first occurrence.
       */
      template<typename _Tp>
	_If_sv<_Tp, size_type>
	_GLIBCXX20_CONSTEXPR
	find_first_not_of(const _Tp& __svt, size_type __pos = 0) const
	noexcept(is_same<_Tp, __sv_type>::value)
	{
	  __sv_type __sv = __svt;
	  return this->find_first_not_of(__sv.data(), __pos, __sv.size());
	}
#endif // C++17

      /**
       *  @brief  Find position of a character not in C substring.
       *  @param __s  C string containing characters to avoid.
       *  @param __pos  Index of character to search from.
       *  @param __n  Number of characters from __s to consider.
       *  @return  Index of first occurrence.
       *
       *  Starting from @a __pos, searches forward for a character not
       *  contained in the first @a __n characters of @a __s within
       *  this string.  If found, returns the index where it was
       *  found.  If not found, returns npos.
      */
      _GLIBCXX20_CONSTEXPR
      size_type
      find_first_not_of(const _CharT* __s, size_type __pos,
			size_type __n) const _GLIBCXX_NOEXCEPT;

      /**
       *  @brief  Find position of a character not in C string.
       *  @param __s  C string containing characters to avoid.
       *  @param __pos  Index of character to search from (default 0).
       *  @return  Index of first occurrence.
       *
       *  Starting from @a __pos, searches forward for a character not
       *  contained in @a __s within this string.  If found, returns
       *  the index where it was found.  If not found, returns npos.
      */
      _GLIBCXX20_CONSTEXPR
      size_type
      find_first_not_of(const _CharT* __s, size_type __pos = 0) const
      _GLIBCXX_NOEXCEPT
      {
	__glibcxx_requires_string(__s);
	return this->find_first_not_of(__s, __pos, traits_type::length(__s));
      }

      /**
       *  @brief  Find position of a different character.
       *  @param __c  Character to avoid.
       *  @param __pos  Index of character to search from (default 0).
       *  @return  Index of first occurrence.
       *
       *  Starting from @a __pos, searches forward for a character
       *  other than @a __c within this string.  If found, returns the
       *  index where it was found.  If not found, returns npos.
      */
      _GLIBCXX20_CONSTEXPR
      size_type
      find_first_not_of(_CharT __c, size_type __pos = 0) const
      _GLIBCXX_NOEXCEPT;

      /**
       *  @brief  Find last position of a character not in string.
       *  @param __str  String containing characters to avoid.
       *  @param __pos  Index of character to search back from (default end).
       *  @return  Index of last occurrence.
       *
       *  Starting from @a __pos, searches backward for a character
       *  not contained in @a __str within this string.  If found,
       *  returns the index where it was found.  If not found, returns
       *  npos.
      */
      _GLIBCXX20_CONSTEXPR
      size_type
      find_last_not_of(const basic_string& __str, size_type __pos = npos) const
      _GLIBCXX_NOEXCEPT
      { return this->find_last_not_of(__str.data(), __pos, __str.size()); }

#if __cplusplus >= 201703L
      /**
       *  @brief  Find last position of a character not in a string_view.
       *  @param __svt  An object convertible to string_view containing
       *                characters to avoid.
       *  @param __pos  Index of character to search back from (default end).
       *  @return  Index of last occurrence.
       */
      template<typename _Tp>
	_GLIBCXX20_CONSTEXPR
	_If_sv<_Tp, size_type>
	find_last_not_of(const _Tp& __svt, size_type __pos = npos) const
	noexcept(is_same<_Tp, __sv_type>::value)
	{
	  __sv_type __sv = __svt;
	  return this->find_last_not_of(__sv.data(), __pos, __sv.size());
	}
#endif // C++17

      /**
       *  @brief  Find last position of a character not in C substring.
       *  @param __s  C string containing characters to avoid.
       *  @param __pos  Index of character to search back from.
       *  @param __n  Number of characters from s to consider.
       *  @return  Index of last occurrence.
       *
       *  Starting from @a __pos, searches backward for a character not
       *  contained in the first @a __n characters of @a __s within this string.
       *  If found, returns the index where it was found.  If not found,
       *  returns npos.
      */
      _GLIBCXX20_CONSTEXPR
      size_type
      find_last_not_of(const _CharT* __s, size_type __pos,
		       size_type __n) const _GLIBCXX_NOEXCEPT;
      /**
       *  @brief  Find last position of a character not in C string.
       *  @param __s  C string containing characters to avoid.
       *  @param __pos  Index of character to search back from (default end).
       *  @return  Index of last occurrence.
       *
       *  Starting from @a __pos, searches backward for a character
       *  not contained in @a __s within this string.  If found,
       *  returns the index where it was found.  If not found, returns
       *  npos.
      */
      _GLIBCXX20_CONSTEXPR
      size_type
      find_last_not_of(const _CharT* __s, size_type __pos = npos) const
      _GLIBCXX_NOEXCEPT
      {
	__glibcxx_requires_string(__s);
	return this->find_last_not_of(__s, __pos, traits_type::length(__s));
      }

      /**
       *  @brief  Find last position of a different character.
       *  @param __c  Character to avoid.
       *  @param __pos  Index of character to search back from (default end).
       *  @return  Index of last occurrence.
       *
       *  Starting from @a __pos, searches backward for a character other than
       *  @a __c within this string.  If found, returns the index where it was
       *  found.  If not found, returns npos.
      */
      _GLIBCXX20_CONSTEXPR
      size_type
      find_last_not_of(_CharT __c, size_type __pos = npos) const
      _GLIBCXX_NOEXCEPT;

      /**
       *  @brief  Get a substring.
       *  @param __pos  Index of first character (default 0).
       *  @param __n  Number of characters in substring (default remainder).
       *  @return  The new string.
       *  @throw  std::out_of_range  If __pos > size().
       *
       *  Construct and return a new string using the @a __n
       *  characters starting at @a __pos.  If the string is too
       *  short, use the remainder of the characters.  If @a __pos is
       *  beyond the end of the string, out_of_range is thrown.
      */
      _GLIBCXX20_CONSTEXPR
      basic_string
      substr(size_type __pos = 0, size_type __n = npos) const
      { return basic_string(*this,
			    _M_check(__pos, "basic_string::substr"), __n); }

      /**
       *  @brief  Compare to a string.
       *  @param __str  String to compare against.
       *  @return  Integer < 0, 0, or > 0.
       *
       *  Returns an integer < 0 if this string is ordered before @a
       *  __str, 0 if their values are equivalent, or > 0 if this
       *  string is ordered after @a __str.  Determines the effective
       *  length rlen of the strings to compare as the smallest of
       *  size() and str.size().  The function then compares the two
       *  strings by calling traits::compare(data(), str.data(),rlen).
       *  If the result of the comparison is nonzero returns it,
       *  otherwise the shorter one is ordered first.
      */
      _GLIBCXX20_CONSTEXPR
      int
      compare(const basic_string& __str) const
      {
	const size_type __size = this->size();
	const size_type __osize = __str.size();
	const size_type __len = std::min(__size, __osize);

	int __r = traits_type::compare(_M_data(), __str.data(), __len);
	if (!__r)
	  __r = _S_compare(__size, __osize);
	return __r;
      }

#if __cplusplus >= 201703L
      /**
       *  @brief  Compare to a string_view.
       *  @param __svt An object convertible to string_view to compare against.
       *  @return  Integer < 0, 0, or > 0.
       */
      template<typename _Tp>
	_GLIBCXX20_CONSTEXPR
	_If_sv<_Tp, int>
	compare(const _Tp& __svt) const
	noexcept(is_same<_Tp, __sv_type>::value)
	{
	  __sv_type __sv = __svt;
	  const size_type __size = this->size();
	  const size_type __osize = __sv.size();
	  const size_type __len = std::min(__size, __osize);

	  int __r = traits_type::compare(_M_data(), __sv.data(), __len);
	  if (!__r)
	    __r = _S_compare(__size, __osize);
	  return __r;
	}

      /**
       *  @brief  Compare to a string_view.
       *  @param __pos  A position in the string to start comparing from.
       *  @param __n  The number of characters to compare.
       *  @param __svt  An object convertible to string_view to compare
       *                against.
       *  @return  Integer < 0, 0, or > 0.
       */
      template<typename _Tp>
	_GLIBCXX20_CONSTEXPR
	_If_sv<_Tp, int>
	compare(size_type __pos, size_type __n, const _Tp& __svt) const
	noexcept(is_same<_Tp, __sv_type>::value)
	{
	  __sv_type __sv = __svt;
	  return __sv_type(*this).substr(__pos, __n).compare(__sv);
	}

      /**
       *  @brief  Compare to a string_view.
       *  @param __pos1  A position in the string to start comparing from.
       *  @param __n1  The number of characters to compare.
       *  @param __svt  An object convertible to string_view to compare
       *                against.
       *  @param __pos2  A position in the string_view to start comparing from.
       *  @param __n2  The number of characters to compare.
       *  @return  Integer < 0, 0, or > 0.
       */
      template<typename _Tp>
	_GLIBCXX20_CONSTEXPR
	_If_sv<_Tp, int>
	compare(size_type __pos1, size_type __n1, const _Tp& __svt,
		size_type __pos2, size_type __n2 = npos) const
	noexcept(is_same<_Tp, __sv_type>::value)
	{
	  __sv_type __sv = __svt;
	  return __sv_type(*this)
	    .substr(__pos1, __n1).compare(__sv.substr(__pos2, __n2));
	}
#endif // C++17

      /**
       *  @brief  Compare substring to a string.
       *  @param __pos  Index of first character of substring.
       *  @param __n  Number of characters in substring.
       *  @param __str  String to compare against.
       *  @return  Integer < 0, 0, or > 0.
       *
       *  Form the substring of this string from the @a __n characters
       *  starting at @a __pos.  Returns an integer < 0 if the
       *  substring is ordered before @a __str, 0 if their values are
       *  equivalent, or > 0 if the substring is ordered after @a
       *  __str.  Determines the effective length rlen of the strings
       *  to compare as the smallest of the length of the substring
       *  and @a __str.size().  The function then compares the two
       *  strings by calling
       *  traits::compare(substring.data(),str.data(),rlen).  If the
       *  result of the comparison is nonzero returns it, otherwise
       *  the shorter one is ordered first.
      */
      _GLIBCXX20_CONSTEXPR
      int
      compare(size_type __pos, size_type __n, const basic_string& __str) const;

      /**
       *  @brief  Compare substring to a substring.
       *  @param __pos1  Index of first character of substring.
       *  @param __n1  Number of characters in substring.
       *  @param __str  String to compare against.
       *  @param __pos2  Index of first character of substring of str.
       *  @param __n2  Number of characters in substring of str.
       *  @return  Integer < 0, 0, or > 0.
       *
       *  Form the substring of this string from the @a __n1
       *  characters starting at @a __pos1.  Form the substring of @a
       *  __str from the @a __n2 characters starting at @a __pos2.
       *  Returns an integer < 0 if this substring is ordered before
       *  the substring of @a __str, 0 if their values are equivalent,
       *  or > 0 if this substring is ordered after the substring of
       *  @a __str.  Determines the effective length rlen of the
       *  strings to compare as the smallest of the lengths of the
       *  substrings.  The function then compares the two strings by
       *  calling
       *  traits::compare(substring.data(),str.substr(pos2,n2).data(),rlen).
       *  If the result of the comparison is nonzero returns it,
       *  otherwise the shorter one is ordered first.
      */
      _GLIBCXX20_CONSTEXPR
      int
      compare(size_type __pos1, size_type __n1, const basic_string& __str,
	      size_type __pos2, size_type __n2 = npos) const;

      /**
       *  @brief  Compare to a C string.
       *  @param __s  C string to compare against.
       *  @return  Integer < 0, 0, or > 0.
       *
       *  Returns an integer < 0 if this string is ordered before @a __s, 0 if
       *  their values are equivalent, or > 0 if this string is ordered after
       *  @a __s.  Determines the effective length rlen of the strings to
       *  compare as the smallest of size() and the length of a string
       *  constructed from @a __s.  The function then compares the two strings
       *  by calling traits::compare(data(),s,rlen).  If the result of the
       *  comparison is nonzero returns it, otherwise the shorter one is
       *  ordered first.
      */
      _GLIBCXX20_CONSTEXPR
      int
      compare(const _CharT* __s) const _GLIBCXX_NOEXCEPT;

      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 5 String::compare specification questionable
      /**
       *  @brief  Compare substring to a C string.
       *  @param __pos  Index of first character of substring.
       *  @param __n1  Number of characters in substring.
       *  @param __s  C string to compare against.
       *  @return  Integer < 0, 0, or > 0.
       *
       *  Form the substring of this string from the @a __n1
       *  characters starting at @a pos.  Returns an integer < 0 if
       *  the substring is ordered before @a __s, 0 if their values
       *  are equivalent, or > 0 if the substring is ordered after @a
       *  __s.  Determines the effective length rlen of the strings to
       *  compare as the smallest of the length of the substring and
       *  the length of a string constructed from @a __s.  The
       *  function then compares the two string by calling
       *  traits::compare(substring.data(),__s,rlen).  If the result of
       *  the comparison is nonzero returns it, otherwise the shorter
       *  one is ordered first.
      */
      _GLIBCXX20_CONSTEXPR
      int
      compare(size_type __pos, size_type __n1, const _CharT* __s) const;

      /**
       *  @brief  Compare substring against a character %array.
       *  @param __pos  Index of first character of substring.
       *  @param __n1  Number of characters in substring.
       *  @param __s  character %array to compare against.
       *  @param __n2  Number of characters of s.
       *  @return  Integer < 0, 0, or > 0.
       *
       *  Form the substring of this string from the @a __n1
       *  characters starting at @a __pos.  Form a string from the
       *  first @a __n2 characters of @a __s.  Returns an integer < 0
       *  if this substring is ordered before the string from @a __s,
       *  0 if their values are equivalent, or > 0 if this substring
       *  is ordered after the string from @a __s.  Determines the
       *  effective length rlen of the strings to compare as the
       *  smallest of the length of the substring and @a __n2.  The
       *  function then compares the two strings by calling
       *  traits::compare(substring.data(),s,rlen).  If the result of
       *  the comparison is nonzero returns it, otherwise the shorter
       *  one is ordered first.
       *
       *  NB: s must have at least n2 characters, &apos;\\0&apos; has
       *  no special meaning.
      */
      _GLIBCXX20_CONSTEXPR
      int
      compare(size_type __pos, size_type __n1, const _CharT* __s,
	      size_type __n2) const;

#if __cplusplus >= 202002L
      constexpr bool
      starts_with(basic_string_view<_CharT, _Traits> __x) const noexcept
      { return __sv_type(this->data(), this->size()).starts_with(__x); }

      constexpr bool
      starts_with(_CharT __x) const noexcept
      { return __sv_type(this->data(), this->size()).starts_with(__x); }

      constexpr bool
      starts_with(const _CharT* __x) const noexcept
      { return __sv_type(this->data(), this->size()).starts_with(__x); }

      constexpr bool
      ends_with(basic_string_view<_CharT, _Traits> __x) const noexcept
      { return __sv_type(this->data(), this->size()).ends_with(__x); }

      constexpr bool
      ends_with(_CharT __x) const noexcept
      { return __sv_type(this->data(), this->size()).ends_with(__x); }

      constexpr bool
      ends_with(const _CharT* __x) const noexcept
      { return __sv_type(this->data(), this->size()).ends_with(__x); }
#endif // C++20

#if __cplusplus > 202002L
      constexpr bool
      contains(basic_string_view<_CharT, _Traits> __x) const noexcept
      { return __sv_type(this->data(), this->size()).contains(__x); }

      constexpr bool
      contains(_CharT __x) const noexcept
      { return __sv_type(this->data(), this->size()).contains(__x); }

      constexpr bool
      contains(const _CharT* __x) const noexcept
      { return __sv_type(this->data(), this->size()).contains(__x); }
#endif // C++23

      // Allow basic_stringbuf::__xfer_bufptrs to call _M_length:
      template<typename, typename, typename> friend class basic_stringbuf;
    };
_GLIBCXX_END_NAMESPACE_CXX11
_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std
#endif  // _GLIBCXX_USE_CXX11_ABI

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

#if __cpp_deduction_guides >= 201606
_GLIBCXX_BEGIN_NAMESPACE_CXX11
  template<typename _InputIterator, typename _CharT
	     = typename iterator_traits<_InputIterator>::value_type,
	   typename _Allocator = allocator<_CharT>,
	   typename = _RequireInputIter<_InputIterator>,
	   typename = _RequireAllocator<_Allocator>>
    basic_string(_InputIterator, _InputIterator, _Allocator = _Allocator())
      -> basic_string<_CharT, char_traits<_CharT>, _Allocator>;

  // _GLIBCXX_RESOLVE_LIB_DEFECTS
  // 3075. basic_string needs deduction guides from basic_string_view
  template<typename _CharT, typename _Traits,
	   typename _Allocator = allocator<_CharT>,
	   typename = _RequireAllocator<_Allocator>>
    basic_string(basic_string_view<_CharT, _Traits>, const _Allocator& = _Allocator())
      -> basic_string<_CharT, _Traits, _Allocator>;

  template<typename _CharT, typename _Traits,
	   typename _Allocator = allocator<_CharT>,
	   typename = _RequireAllocator<_Allocator>>
    basic_string(basic_string_view<_CharT, _Traits>,
		 typename basic_string<_CharT, _Traits, _Allocator>::size_type,
		 typename basic_string<_CharT, _Traits, _Allocator>::size_type,
		 const _Allocator& = _Allocator())
      -> basic_string<_CharT, _Traits, _Allocator>;
_GLIBCXX_END_NAMESPACE_CXX11
#endif

  // operator+
  /**
   *  @brief  Concatenate two strings.
   *  @param __lhs  First string.
   *  @param __rhs  Last string.
   *  @return  New string with value of @a __lhs followed by @a __rhs.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    basic_string<_CharT, _Traits, _Alloc>
    operator+(const basic_string<_CharT, _Traits, _Alloc>& __lhs,
	      const basic_string<_CharT, _Traits, _Alloc>& __rhs)
    {
      basic_string<_CharT, _Traits, _Alloc> __str(__lhs);
      __str.append(__rhs);
      return __str;
    }

  /**
   *  @brief  Concatenate C string and string.
   *  @param __lhs  First string.
   *  @param __rhs  Last string.
   *  @return  New string with value of @a __lhs followed by @a __rhs.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    basic_string<_CharT,_Traits,_Alloc>
    operator+(const _CharT* __lhs,
	      const basic_string<_CharT,_Traits,_Alloc>& __rhs);

  /**
   *  @brief  Concatenate character and string.
   *  @param __lhs  First string.
   *  @param __rhs  Last string.
   *  @return  New string with @a __lhs followed by @a __rhs.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    basic_string<_CharT,_Traits,_Alloc>
    operator+(_CharT __lhs, const basic_string<_CharT,_Traits,_Alloc>& __rhs);

  /**
   *  @brief  Concatenate string and C string.
   *  @param __lhs  First string.
   *  @param __rhs  Last string.
   *  @return  New string with @a __lhs followed by @a __rhs.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    inline basic_string<_CharT, _Traits, _Alloc>
    operator+(const basic_string<_CharT, _Traits, _Alloc>& __lhs,
	      const _CharT* __rhs)
    {
      basic_string<_CharT, _Traits, _Alloc> __str(__lhs);
      __str.append(__rhs);
      return __str;
    }

  /**
   *  @brief  Concatenate string and character.
   *  @param __lhs  First string.
   *  @param __rhs  Last string.
   *  @return  New string with @a __lhs followed by @a __rhs.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    inline basic_string<_CharT, _Traits, _Alloc>
    operator+(const basic_string<_CharT, _Traits, _Alloc>& __lhs, _CharT __rhs)
    {
      typedef basic_string<_CharT, _Traits, _Alloc>	__string_type;
      typedef typename __string_type::size_type		__size_type;
      __string_type __str(__lhs);
      __str.append(__size_type(1), __rhs);
      return __str;
    }

#if __cplusplus >= 201103L
  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    inline basic_string<_CharT, _Traits, _Alloc>
    operator+(basic_string<_CharT, _Traits, _Alloc>&& __lhs,
	      const basic_string<_CharT, _Traits, _Alloc>& __rhs)
    { return std::move(__lhs.append(__rhs)); }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    inline basic_string<_CharT, _Traits, _Alloc>
    operator+(const basic_string<_CharT, _Traits, _Alloc>& __lhs,
	      basic_string<_CharT, _Traits, _Alloc>&& __rhs)
    { return std::move(__rhs.insert(0, __lhs)); }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    inline basic_string<_CharT, _Traits, _Alloc>
    operator+(basic_string<_CharT, _Traits, _Alloc>&& __lhs,
	      basic_string<_CharT, _Traits, _Alloc>&& __rhs)
    {
#if _GLIBCXX_USE_CXX11_ABI
      using _Alloc_traits = allocator_traits<_Alloc>;
      bool __use_rhs = false;
      if _GLIBCXX17_CONSTEXPR (typename _Alloc_traits::is_always_equal{})
	__use_rhs = true;
      else if (__lhs.get_allocator() == __rhs.get_allocator())
	__use_rhs = true;
      if (__use_rhs)
#endif
	{
	  const auto __size = __lhs.size() + __rhs.size();
	  if (__size > __lhs.capacity() && __size <= __rhs.capacity())
	    return std::move(__rhs.insert(0, __lhs));
	}
      return std::move(__lhs.append(__rhs));
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    inline basic_string<_CharT, _Traits, _Alloc>
    operator+(const _CharT* __lhs,
	      basic_string<_CharT, _Traits, _Alloc>&& __rhs)
    { return std::move(__rhs.insert(0, __lhs)); }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    inline basic_string<_CharT, _Traits, _Alloc>
    operator+(_CharT __lhs,
	      basic_string<_CharT, _Traits, _Alloc>&& __rhs)
    { return std::move(__rhs.insert(0, 1, __lhs)); }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    inline basic_string<_CharT, _Traits, _Alloc>
    operator+(basic_string<_CharT, _Traits, _Alloc>&& __lhs,
	      const _CharT* __rhs)
    { return std::move(__lhs.append(__rhs)); }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    inline basic_string<_CharT, _Traits, _Alloc>
    operator+(basic_string<_CharT, _Traits, _Alloc>&& __lhs,
	      _CharT __rhs)
    { return std::move(__lhs.append(1, __rhs)); }
#endif

  // operator ==
  /**
   *  @brief  Test equivalence of two strings.
   *  @param __lhs  First string.
   *  @param __rhs  Second string.
   *  @return  True if @a __lhs.compare(@a __rhs) == 0.  False otherwise.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    inline bool
    operator==(const basic_string<_CharT, _Traits, _Alloc>& __lhs,
	       const basic_string<_CharT, _Traits, _Alloc>& __rhs)
    _GLIBCXX_NOEXCEPT
    { return __lhs.compare(__rhs) == 0; }

  template<typename _CharT>
    _GLIBCXX20_CONSTEXPR
    inline
    typename __gnu_cxx::__enable_if<__is_char<_CharT>::__value, bool>::__type
    operator==(const basic_string<_CharT>& __lhs,
	       const basic_string<_CharT>& __rhs) _GLIBCXX_NOEXCEPT
    { return (__lhs.size() == __rhs.size()
	      && !std::char_traits<_CharT>::compare(__lhs.data(), __rhs.data(),
						    __lhs.size())); }

  /**
   *  @brief  Test equivalence of string and C string.
   *  @param __lhs  String.
   *  @param __rhs  C string.
   *  @return  True if @a __lhs.compare(@a __rhs) == 0.  False otherwise.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    inline bool
    operator==(const basic_string<_CharT, _Traits, _Alloc>& __lhs,
	       const _CharT* __rhs)
    { return __lhs.compare(__rhs) == 0; }

#if __cpp_lib_three_way_comparison
  /**
   *  @brief  Three-way comparison of a string and a C string.
   *  @param __lhs  A string.
   *  @param __rhs  A null-terminated string.
   *  @return  A value indicating whether `__lhs` is less than, equal to,
   *	       greater than, or incomparable with `__rhs`.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    constexpr auto
    operator<=>(const basic_string<_CharT, _Traits, _Alloc>& __lhs,
		const basic_string<_CharT, _Traits, _Alloc>& __rhs) noexcept
    -> decltype(__detail::__char_traits_cmp_cat<_Traits>(0))
    { return __detail::__char_traits_cmp_cat<_Traits>(__lhs.compare(__rhs)); }

  /**
   *  @brief  Three-way comparison of a string and a C string.
   *  @param __lhs  A string.
   *  @param __rhs  A null-terminated string.
   *  @return  A value indicating whether `__lhs` is less than, equal to,
   *	       greater than, or incomparable with `__rhs`.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    constexpr auto
    operator<=>(const basic_string<_CharT, _Traits, _Alloc>& __lhs,
		const _CharT* __rhs) noexcept
    -> decltype(__detail::__char_traits_cmp_cat<_Traits>(0))
    { return __detail::__char_traits_cmp_cat<_Traits>(__lhs.compare(__rhs)); }
#else
  /**
   *  @brief  Test equivalence of C string and string.
   *  @param __lhs  C string.
   *  @param __rhs  String.
   *  @return  True if @a __rhs.compare(@a __lhs) == 0.  False otherwise.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    inline bool
    operator==(const _CharT* __lhs,
	       const basic_string<_CharT, _Traits, _Alloc>& __rhs)
    { return __rhs.compare(__lhs) == 0; }

  // operator !=
  /**
   *  @brief  Test difference of two strings.
   *  @param __lhs  First string.
   *  @param __rhs  Second string.
   *  @return  True if @a __lhs.compare(@a __rhs) != 0.  False otherwise.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    inline bool
    operator!=(const basic_string<_CharT, _Traits, _Alloc>& __lhs,
	       const basic_string<_CharT, _Traits, _Alloc>& __rhs)
    _GLIBCXX_NOEXCEPT
    { return !(__lhs == __rhs); }

  /**
   *  @brief  Test difference of C string and string.
   *  @param __lhs  C string.
   *  @param __rhs  String.
   *  @return  True if @a __rhs.compare(@a __lhs) != 0.  False otherwise.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    inline bool
    operator!=(const _CharT* __lhs,
	       const basic_string<_CharT, _Traits, _Alloc>& __rhs)
    { return !(__lhs == __rhs); }

  /**
   *  @brief  Test difference of string and C string.
   *  @param __lhs  String.
   *  @param __rhs  C string.
   *  @return  True if @a __lhs.compare(@a __rhs) != 0.  False otherwise.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    inline bool
    operator!=(const basic_string<_CharT, _Traits, _Alloc>& __lhs,
	       const _CharT* __rhs)
    { return !(__lhs == __rhs); }

  // operator <
  /**
   *  @brief  Test if string precedes string.
   *  @param __lhs  First string.
   *  @param __rhs  Second string.
   *  @return  True if @a __lhs precedes @a __rhs.  False otherwise.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    inline bool
    operator<(const basic_string<_CharT, _Traits, _Alloc>& __lhs,
	      const basic_string<_CharT, _Traits, _Alloc>& __rhs)
    _GLIBCXX_NOEXCEPT
    { return __lhs.compare(__rhs) < 0; }

  /**
   *  @brief  Test if string precedes C string.
   *  @param __lhs  String.
   *  @param __rhs  C string.
   *  @return  True if @a __lhs precedes @a __rhs.  False otherwise.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    inline bool
    operator<(const basic_string<_CharT, _Traits, _Alloc>& __lhs,
	      const _CharT* __rhs)
    { return __lhs.compare(__rhs) < 0; }

  /**
   *  @brief  Test if C string precedes string.
   *  @param __lhs  C string.
   *  @param __rhs  String.
   *  @return  True if @a __lhs precedes @a __rhs.  False otherwise.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    inline bool
    operator<(const _CharT* __lhs,
	      const basic_string<_CharT, _Traits, _Alloc>& __rhs)
    { return __rhs.compare(__lhs) > 0; }

  // operator >
  /**
   *  @brief  Test if string follows string.
   *  @param __lhs  First string.
   *  @param __rhs  Second string.
   *  @return  True if @a __lhs follows @a __rhs.  False otherwise.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    inline bool
    operator>(const basic_string<_CharT, _Traits, _Alloc>& __lhs,
	      const basic_string<_CharT, _Traits, _Alloc>& __rhs)
    _GLIBCXX_NOEXCEPT
    { return __lhs.compare(__rhs) > 0; }

  /**
   *  @brief  Test if string follows C string.
   *  @param __lhs  String.
   *  @param __rhs  C string.
   *  @return  True if @a __lhs follows @a __rhs.  False otherwise.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    inline bool
    operator>(const basic_string<_CharT, _Traits, _Alloc>& __lhs,
	      const _CharT* __rhs)
    { return __lhs.compare(__rhs) > 0; }

  /**
   *  @brief  Test if C string follows string.
   *  @param __lhs  C string.
   *  @param __rhs  String.
   *  @return  True if @a __lhs follows @a __rhs.  False otherwise.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    inline bool
    operator>(const _CharT* __lhs,
	      const basic_string<_CharT, _Traits, _Alloc>& __rhs)
    { return __rhs.compare(__lhs) < 0; }

  // operator <=
  /**
   *  @brief  Test if string doesn't follow string.
   *  @param __lhs  First string.
   *  @param __rhs  Second string.
   *  @return  True if @a __lhs doesn't follow @a __rhs.  False otherwise.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    inline bool
    operator<=(const basic_string<_CharT, _Traits, _Alloc>& __lhs,
	       const basic_string<_CharT, _Traits, _Alloc>& __rhs)
    _GLIBCXX_NOEXCEPT
    { return __lhs.compare(__rhs) <= 0; }

  /**
   *  @brief  Test if string doesn't follow C string.
   *  @param __lhs  String.
   *  @param __rhs  C string.
   *  @return  True if @a __lhs doesn't follow @a __rhs.  False otherwise.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    inline bool
    operator<=(const basic_string<_CharT, _Traits, _Alloc>& __lhs,
	       const _CharT* __rhs)
    { return __lhs.compare(__rhs) <= 0; }

  /**
   *  @brief  Test if C string doesn't follow string.
   *  @param __lhs  C string.
   *  @param __rhs  String.
   *  @return  True if @a __lhs doesn't follow @a __rhs.  False otherwise.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    inline bool
    operator<=(const _CharT* __lhs,
	       const basic_string<_CharT, _Traits, _Alloc>& __rhs)
    { return __rhs.compare(__lhs) >= 0; }

  // operator >=
  /**
   *  @brief  Test if string doesn't precede string.
   *  @param __lhs  First string.
   *  @param __rhs  Second string.
   *  @return  True if @a __lhs doesn't precede @a __rhs.  False otherwise.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    inline bool
    operator>=(const basic_string<_CharT, _Traits, _Alloc>& __lhs,
	       const basic_string<_CharT, _Traits, _Alloc>& __rhs)
    _GLIBCXX_NOEXCEPT
    { return __lhs.compare(__rhs) >= 0; }

  /**
   *  @brief  Test if string doesn't precede C string.
   *  @param __lhs  String.
   *  @param __rhs  C string.
   *  @return  True if @a __lhs doesn't precede @a __rhs.  False otherwise.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    inline bool
    operator>=(const basic_string<_CharT, _Traits, _Alloc>& __lhs,
	       const _CharT* __rhs)
    { return __lhs.compare(__rhs) >= 0; }

  /**
   *  @brief  Test if C string doesn't precede string.
   *  @param __lhs  C string.
   *  @param __rhs  String.
   *  @return  True if @a __lhs doesn't precede @a __rhs.  False otherwise.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    inline bool
    operator>=(const _CharT* __lhs,
	     const basic_string<_CharT, _Traits, _Alloc>& __rhs)
    { return __rhs.compare(__lhs) <= 0; }
#endif // three-way comparison

  /**
   *  @brief  Swap contents of two strings.
   *  @param __lhs  First string.
   *  @param __rhs  Second string.
   *
   *  Exchanges the contents of @a __lhs and @a __rhs in constant time.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    inline void
    swap(basic_string<_CharT, _Traits, _Alloc>& __lhs,
	 basic_string<_CharT, _Traits, _Alloc>& __rhs)
    _GLIBCXX_NOEXCEPT_IF(noexcept(__lhs.swap(__rhs)))
    { __lhs.swap(__rhs); }


  /**
   *  @brief  Read stream into a string.
   *  @param __is  Input stream.
   *  @param __str  Buffer to store into.
   *  @return  Reference to the input stream.
   *
   *  Stores characters from @a __is into @a __str until whitespace is
   *  found, the end of the stream is encountered, or str.max_size()
   *  is reached.  If is.width() is non-zero, that is the limit on the
   *  number of characters stored into @a __str.  Any previous
   *  contents of @a __str are erased.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    basic_istream<_CharT, _Traits>&
    operator>>(basic_istream<_CharT, _Traits>& __is,
	       basic_string<_CharT, _Traits, _Alloc>& __str);

  template<>
    basic_istream<char>&
    operator>>(basic_istream<char>& __is, basic_string<char>& __str);

  /**
   *  @brief  Write string to a stream.
   *  @param __os  Output stream.
   *  @param __str  String to write out.
   *  @return  Reference to the output stream.
   *
   *  Output characters of @a __str into os following the same rules as for
   *  writing a C string.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    inline basic_ostream<_CharT, _Traits>&
    operator<<(basic_ostream<_CharT, _Traits>& __os,
	       const basic_string<_CharT, _Traits, _Alloc>& __str)
    {
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 586. string inserter not a formatted function
      return __ostream_insert(__os, __str.data(), __str.size());
    }

  /**
   *  @brief  Read a line from stream into a string.
   *  @param __is  Input stream.
   *  @param __str  Buffer to store into.
   *  @param __delim  Character marking end of line.
   *  @return  Reference to the input stream.
   *
   *  Stores characters from @a __is into @a __str until @a __delim is
   *  found, the end of the stream is encountered, or str.max_size()
   *  is reached.  Any previous contents of @a __str are erased.  If
   *  @a __delim is encountered, it is extracted but not stored into
   *  @a __str.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    basic_istream<_CharT, _Traits>&
    getline(basic_istream<_CharT, _Traits>& __is,
	    basic_string<_CharT, _Traits, _Alloc>& __str, _CharT __delim);

  /**
   *  @brief  Read a line from stream into a string.
   *  @param __is  Input stream.
   *  @param __str  Buffer to store into.
   *  @return  Reference to the input stream.
   *
   *  Stores characters from is into @a __str until &apos;\n&apos; is
   *  found, the end of the stream is encountered, or str.max_size()
   *  is reached.  Any previous contents of @a __str are erased.  If
   *  end of line is encountered, it is extracted but not stored into
   *  @a __str.
   */
  template<typename _CharT, typename _Traits, typename _Alloc>
    inline basic_istream<_CharT, _Traits>&
    getline(basic_istream<_CharT, _Traits>& __is,
	    basic_string<_CharT, _Traits, _Alloc>& __str)
    { return std::getline(__is, __str, __is.widen('\n')); }

#if __cplusplus >= 201103L
  /// Read a line from an rvalue stream into a string.
  template<typename _CharT, typename _Traits, typename _Alloc>
    inline basic_istream<_CharT, _Traits>&
    getline(basic_istream<_CharT, _Traits>&& __is,
	    basic_string<_CharT, _Traits, _Alloc>& __str, _CharT __delim)
    { return std::getline(__is, __str, __delim); }

  /// Read a line from an rvalue stream into a string.
  template<typename _CharT, typename _Traits, typename _Alloc>
    inline basic_istream<_CharT, _Traits>&
    getline(basic_istream<_CharT, _Traits>&& __is,
	    basic_string<_CharT, _Traits, _Alloc>& __str)
    { return std::getline(__is, __str); }
#endif

  template<>
    basic_istream<char>&
    getline(basic_istream<char>& __in, basic_string<char>& __str,
	    char __delim);

#ifdef _GLIBCXX_USE_WCHAR_T
  template<>
    basic_istream<wchar_t>&
    getline(basic_istream<wchar_t>& __in, basic_string<wchar_t>& __str,
	    wchar_t __delim);
#endif  

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace

#if __cplusplus >= 201103L

#include <ext/string_conversions.h>
#include <bits/charconv.h>

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION
_GLIBCXX_BEGIN_NAMESPACE_CXX11

#if _GLIBCXX_USE_C99_STDLIB
  // 21.4 Numeric Conversions [string.conversions].
  inline int
  stoi(const string& __str, size_t* __idx = 0, int __base = 10)
  { return __gnu_cxx::__stoa<long, int>(&std::strtol, "stoi", __str.c_str(),
					__idx, __base); }

  inline long
  stol(const string& __str, size_t* __idx = 0, int __base = 10)
  { return __gnu_cxx::__stoa(&std::strtol, "stol", __str.c_str(),
			     __idx, __base); }

  inline unsigned long
  stoul(const string& __str, size_t* __idx = 0, int __base = 10)
  { return __gnu_cxx::__stoa(&std::strtoul, "stoul", __str.c_str(),
			     __idx, __base); }

  inline long long
  stoll(const string& __str, size_t* __idx = 0, int __base = 10)
  { return __gnu_cxx::__stoa(&std::strtoll, "stoll", __str.c_str(),
			     __idx, __base); }

  inline unsigned long long
  stoull(const string& __str, size_t* __idx = 0, int __base = 10)
  { return __gnu_cxx::__stoa(&std::strtoull, "stoull", __str.c_str(),
			     __idx, __base); }

  // NB: strtof vs strtod.
  inline float
  stof(const string& __str, size_t* __idx = 0)
  { return __gnu_cxx::__stoa(&std::strtof, "stof", __str.c_str(), __idx); }

  inline double
  stod(const string& __str, size_t* __idx = 0)
  { return __gnu_cxx::__stoa(&std::strtod, "stod", __str.c_str(), __idx); }

  inline long double
  stold(const string& __str, size_t* __idx = 0)
  { return __gnu_cxx::__stoa(&std::strtold, "stold", __str.c_str(), __idx); }
#endif // _GLIBCXX_USE_C99_STDLIB

  // DR 1261. Insufficent overloads for to_string / to_wstring

  inline string
  to_string(int __val)
#if _GLIBCXX_USE_CXX11_ABI && (__CHAR_BIT__ * __SIZEOF_INT__) <= 32
  noexcept // any 32-bit value fits in the SSO buffer
#endif
  {
    const bool __neg = __val < 0;
    const unsigned __uval = __neg ? (unsigned)~__val + 1u : __val;
    const auto __len = __detail::__to_chars_len(__uval);
    string __str(__neg + __len, '-');
    __detail::__to_chars_10_impl(&__str[__neg], __len, __uval);
    return __str;
  }

  inline string
  to_string(unsigned __val)
#if _GLIBCXX_USE_CXX11_ABI && (__CHAR_BIT__ * __SIZEOF_INT__) <= 32
  noexcept // any 32-bit value fits in the SSO buffer
#endif
  {
    string __str(__detail::__to_chars_len(__val), '\0');
    __detail::__to_chars_10_impl(&__str[0], __str.size(), __val);
    return __str;
  }

  inline string
  to_string(long __val)
#if _GLIBCXX_USE_CXX11_ABI && (__CHAR_BIT__ * __SIZEOF_LONG__) <= 32
  noexcept // any 32-bit value fits in the SSO buffer
#endif
  {
    const bool __neg = __val < 0;
    const unsigned long __uval = __neg ? (unsigned long)~__val + 1ul : __val;
    const auto __len = __detail::__to_chars_len(__uval);
    string __str(__neg + __len, '-');
    __detail::__to_chars_10_impl(&__str[__neg], __len, __uval);
    return __str;
  }

  inline string
  to_string(unsigned long __val)
#if _GLIBCXX_USE_CXX11_ABI && (__CHAR_BIT__ * __SIZEOF_LONG__) <= 32
  noexcept // any 32-bit value fits in the SSO buffer
#endif
  {
    string __str(__detail::__to_chars_len(__val), '\0');
    __detail::__to_chars_10_impl(&__str[0], __str.size(), __val);
    return __str;
  }

  inline string
  to_string(long long __val)
  {
    const bool __neg = __val < 0;
    const unsigned long long __uval
      = __neg ? (unsigned long long)~__val + 1ull : __val;
    const auto __len = __detail::__to_chars_len(__uval);
    string __str(__neg + __len, '-');
    __detail::__to_chars_10_impl(&__str[__neg], __len, __uval);
    return __str;
  }

  inline string
  to_string(unsigned long long __val)
  {
    string __str(__detail::__to_chars_len(__val), '\0');
    __detail::__to_chars_10_impl(&__str[0], __str.size(), __val);
    return __str;
  }

#if _GLIBCXX_USE_C99_STDIO
  // NB: (v)snprintf vs sprintf.

  inline string
  to_string(float __val)
  {
    const int __n = 
      __gnu_cxx::__numeric_traits<float>::__max_exponent10 + 20;
    return __gnu_cxx::__to_xstring<string>(&std::vsnprintf, __n,
					   "%f", __val);
  }

  inline string
  to_string(double __val)
  {
    const int __n = 
      __gnu_cxx::__numeric_traits<double>::__max_exponent10 + 20;
    return __gnu_cxx::__to_xstring<string>(&std::vsnprintf, __n,
					   "%f", __val);
  }

  inline string
  to_string(long double __val)
  {
    const int __n = 
      __gnu_cxx::__numeric_traits<long double>::__max_exponent10 + 20;
    return __gnu_cxx::__to_xstring<string>(&std::vsnprintf, __n,
					   "%Lf", __val);
  }
#endif // _GLIBCXX_USE_C99_STDIO

#if defined(_GLIBCXX_USE_WCHAR_T) && _GLIBCXX_USE_C99_WCHAR
  inline int 
  stoi(const wstring& __str, size_t* __idx = 0, int __base = 10)
  { return __gnu_cxx::__stoa<long, int>(&std::wcstol, "stoi", __str.c_str(),
					__idx, __base); }

  inline long 
  stol(const wstring& __str, size_t* __idx = 0, int __base = 10)
  { return __gnu_cxx::__stoa(&std::wcstol, "stol", __str.c_str(),
			     __idx, __base); }

  inline unsigned long
  stoul(const wstring& __str, size_t* __idx = 0, int __base = 10)
  { return __gnu_cxx::__stoa(&std::wcstoul, "stoul", __str.c_str(),
			     __idx, __base); }

  inline long long
  stoll(const wstring& __str, size_t* __idx = 0, int __base = 10)
  { return __gnu_cxx::__stoa(&std::wcstoll, "stoll", __str.c_str(),
			     __idx, __base); }

  inline unsigned long long
  stoull(const wstring& __str, size_t* __idx = 0, int __base = 10)
  { return __gnu_cxx::__stoa(&std::wcstoull, "stoull", __str.c_str(),
			     __idx, __base); }

  // NB: wcstof vs wcstod.
  inline float
  stof(const wstring& __str, size_t* __idx = 0)
  { return __gnu_cxx::__stoa(&std::wcstof, "stof", __str.c_str(), __idx); }

  inline double
  stod(const wstring& __str, size_t* __idx = 0)
  { return __gnu_cxx::__stoa(&std::wcstod, "stod", __str.c_str(), __idx); }

  inline long double
  stold(const wstring& __str, size_t* __idx = 0)
  { return __gnu_cxx::__stoa(&std::wcstold, "stold", __str.c_str(), __idx); }

#ifndef _GLIBCXX_HAVE_BROKEN_VSWPRINTF
  // DR 1261.
  inline wstring
  to_wstring(int __val)
  { return __gnu_cxx::__to_xstring<wstring>(&std::vswprintf, 4 * sizeof(int),
					    L"%d", __val); }

  inline wstring
  to_wstring(unsigned __val)
  { return __gnu_cxx::__to_xstring<wstring>(&std::vswprintf,
					    4 * sizeof(unsigned),
					    L"%u", __val); }

  inline wstring
  to_wstring(long __val)
  { return __gnu_cxx::__to_xstring<wstring>(&std::vswprintf, 4 * sizeof(long),
					    L"%ld", __val); }

  inline wstring
  to_wstring(unsigned long __val)
  { return __gnu_cxx::__to_xstring<wstring>(&std::vswprintf,
					    4 * sizeof(unsigned long),
					    L"%lu", __val); }

  inline wstring
  to_wstring(long long __val)
  { return __gnu_cxx::__to_xstring<wstring>(&std::vswprintf,
					    4 * sizeof(long long),
					    L"%lld", __val); }

  inline wstring
  to_wstring(unsigned long long __val)
  { return __gnu_cxx::__to_xstring<wstring>(&std::vswprintf,
					    4 * sizeof(unsigned long long),
					    L"%llu", __val); }

  inline wstring
  to_wstring(float __val)
  {
    const int __n =
      __gnu_cxx::__numeric_traits<float>::__max_exponent10 + 20;
    return __gnu_cxx::__to_xstring<wstring>(&std::vswprintf, __n,
					    L"%f", __val);
  }

  inline wstring
  to_wstring(double __val)
  {
    const int __n =
      __gnu_cxx::__numeric_traits<double>::__max_exponent10 + 20;
    return __gnu_cxx::__to_xstring<wstring>(&std::vswprintf, __n,
					    L"%f", __val);
  }

  inline wstring
  to_wstring(long double __val)
  {
    const int __n =
      __gnu_cxx::__numeric_traits<long double>::__max_exponent10 + 20;
    return __gnu_cxx::__to_xstring<wstring>(&std::vswprintf, __n,
					    L"%Lf", __val);
  }
#endif // _GLIBCXX_HAVE_BROKEN_VSWPRINTF
#endif // _GLIBCXX_USE_WCHAR_T && _GLIBCXX_USE_C99_WCHAR

_GLIBCXX_END_NAMESPACE_CXX11
_GLIBCXX_END_NAMESPACE_VERSION
} // namespace

#endif /* C++11 */

#if __cplusplus >= 201103L

#include <bits/functional_hash.h>

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

  // DR 1182.

#ifndef _GLIBCXX_COMPATIBILITY_CXX0X
  /// std::hash specialization for string.
  template<>
    struct hash<string>
    : public __hash_base<size_t, string>
    {
      size_t
      operator()(const string& __s) const noexcept
      { return std::_Hash_impl::hash(__s.data(), __s.length()); }
    };

  template<>
    struct __is_fast_hash<hash<string>> : std::false_type
    { };

  /// std::hash specialization for wstring.
  template<>
    struct hash<wstring>
    : public __hash_base<size_t, wstring>
    {
      size_t
      operator()(const wstring& __s) const noexcept
      { return std::_Hash_impl::hash(__s.data(),
                                     __s.length() * sizeof(wchar_t)); }
    };

  template<>
    struct __is_fast_hash<hash<wstring>> : std::false_type
    { };
#endif /* _GLIBCXX_COMPATIBILITY_CXX0X */

#ifdef _GLIBCXX_USE_CHAR8_T
  /// std::hash specialization for u8string.
  template<>
    struct hash<u8string>
    : public __hash_base<size_t, u8string>
    {
      size_t
      operator()(const u8string& __s) const noexcept
      { return std::_Hash_impl::hash(__s.data(),
                                     __s.length() * sizeof(char8_t)); }
    };

  template<>
    struct __is_fast_hash<hash<u8string>> : std::false_type
    { };
#endif

  /// std::hash specialization for u16string.
  template<>
    struct hash<u16string>
    : public __hash_base<size_t, u16string>
    {
      size_t
      operator()(const u16string& __s) const noexcept
      { return std::_Hash_impl::hash(__s.data(),
                                     __s.length() * sizeof(char16_t)); }
    };

  template<>
    struct __is_fast_hash<hash<u16string>> : std::false_type
    { };

  /// std::hash specialization for u32string.
  template<>
    struct hash<u32string>
    : public __hash_base<size_t, u32string>
    {
      size_t
      operator()(const u32string& __s) const noexcept
      { return std::_Hash_impl::hash(__s.data(),
                                     __s.length() * sizeof(char32_t)); }
    };

  template<>
    struct __is_fast_hash<hash<u32string>> : std::false_type
    { };

#if __cplusplus >= 201402L

#define __cpp_lib_string_udls 201304L

  inline namespace literals
  {
  inline namespace string_literals
  {
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wliteral-suffix"

#if __cpp_lib_constexpr_string >= 201907L
# define _GLIBCXX_STRING_CONSTEXPR constexpr
#else
# define _GLIBCXX_STRING_CONSTEXPR
#endif

    _GLIBCXX_DEFAULT_ABI_TAG _GLIBCXX_STRING_CONSTEXPR
    inline basic_string<char>
    operator""s(const char* __str, size_t __len)
    { return basic_string<char>{__str, __len}; }

    _GLIBCXX_DEFAULT_ABI_TAG _GLIBCXX_STRING_CONSTEXPR
    inline basic_string<wchar_t>
    operator""s(const wchar_t* __str, size_t __len)
    { return basic_string<wchar_t>{__str, __len}; }

#ifdef _GLIBCXX_USE_CHAR8_T
    _GLIBCXX_DEFAULT_ABI_TAG _GLIBCXX_STRING_CONSTEXPR
    inline basic_string<char8_t>
    operator""s(const char8_t* __str, size_t __len)
    { return basic_string<char8_t>{__str, __len}; }
#endif

    _GLIBCXX_DEFAULT_ABI_TAG _GLIBCXX_STRING_CONSTEXPR
    inline basic_string<char16_t>
    operator""s(const char16_t* __str, size_t __len)
    { return basic_string<char16_t>{__str, __len}; }

    _GLIBCXX_DEFAULT_ABI_TAG _GLIBCXX_STRING_CONSTEXPR
    inline basic_string<char32_t>
    operator""s(const char32_t* __str, size_t __len)
    { return basic_string<char32_t>{__str, __len}; }

#undef _GLIBCXX_STRING_CONSTEXPR
#pragma GCC diagnostic pop
  } // inline namespace string_literals
  } // inline namespace literals

#if __cplusplus >= 201703L
  namespace __detail::__variant
  {
    template<typename> struct _Never_valueless_alt; // see <variant>

    // Provide the strong exception-safety guarantee when emplacing a
    // basic_string into a variant, but only if moving the string cannot throw.
    template<typename _Tp, typename _Traits, typename _Alloc>
      struct _Never_valueless_alt<std::basic_string<_Tp, _Traits, _Alloc>>
      : __and_<
	is_nothrow_move_constructible<std::basic_string<_Tp, _Traits, _Alloc>>,
	is_nothrow_move_assignable<std::basic_string<_Tp, _Traits, _Alloc>>
	>::type
      { };
  }  // namespace __detail::__variant
#endif // C++17
#endif // C++14

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std

#endif // C++11

#endif /* _BASIC_STRING_H */
