// Vector implementation -*- C++ -*-

// Copyright (C) 2001-2022 Free Software Foundation, Inc.
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
 *
 * Copyright (c) 1994
 * Hewlett-Packard Company
 *
 * Permission to use, copy, modify, distribute and sell this software
 * and its documentation for any purpose is hereby granted without fee,
 * provided that the above copyright notice appear in all copies and
 * that both that copyright notice and this permission notice appear
 * in supporting documentation.  Hewlett-Packard Company makes no
 * representations about the suitability of this software for any
 * purpose.  It is provided "as is" without express or implied warranty.
 *
 *
 * Copyright (c) 1996
 * Silicon Graphics Computer Systems, Inc.
 *
 * Permission to use, copy, modify, distribute and sell this software
 * and its documentation for any purpose is hereby granted without fee,
 * provided that the above copyright notice appear in all copies and
 * that both that copyright notice and this permission notice appear
 * in supporting documentation.  Silicon Graphics makes no
 * representations about the suitability of this  software for any
 * purpose.  It is provided "as is" without express or implied warranty.
 */

/** @file bits/stl_vector.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{vector}
 */

#ifndef _STL_VECTOR_H
#define _STL_VECTOR_H 1

#include <bits/stl_iterator_base_funcs.h>
#include <bits/functexcept.h>
#include <bits/concept_check.h>
#if __cplusplus >= 201103L
#include <initializer_list>
#endif
#if __cplusplus >= 202002L
# include <compare>
#define __cpp_lib_constexpr_vector 201907L
#endif

#include <debug/assertions.h>

#if _GLIBCXX_SANITIZE_STD_ALLOCATOR && _GLIBCXX_SANITIZE_VECTOR
extern "C" void
__sanitizer_annotate_contiguous_container(const void*, const void*,
					  const void*, const void*);
#endif

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION
_GLIBCXX_BEGIN_NAMESPACE_CONTAINER

  /// See bits/stl_deque.h's _Deque_base for an explanation.
  template<typename _Tp, typename _Alloc>
    struct _Vector_base
    {
      typedef typename __gnu_cxx::__alloc_traits<_Alloc>::template
	rebind<_Tp>::other _Tp_alloc_type;
      typedef typename __gnu_cxx::__alloc_traits<_Tp_alloc_type>::pointer
       	pointer;

      struct _Vector_impl_data
      {
	pointer _M_start;
	pointer _M_finish;
	pointer _M_end_of_storage;

	_GLIBCXX20_CONSTEXPR
	_Vector_impl_data() _GLIBCXX_NOEXCEPT
	: _M_start(), _M_finish(), _M_end_of_storage()
	{ }

#if __cplusplus >= 201103L
	_GLIBCXX20_CONSTEXPR
	_Vector_impl_data(_Vector_impl_data&& __x) noexcept
	: _M_start(__x._M_start), _M_finish(__x._M_finish),
	  _M_end_of_storage(__x._M_end_of_storage)
	{ __x._M_start = __x._M_finish = __x._M_end_of_storage = pointer(); }
#endif

	_GLIBCXX20_CONSTEXPR
	void
	_M_copy_data(_Vector_impl_data const& __x) _GLIBCXX_NOEXCEPT
	{
	  _M_start = __x._M_start;
	  _M_finish = __x._M_finish;
	  _M_end_of_storage = __x._M_end_of_storage;
	}

	_GLIBCXX20_CONSTEXPR
	void
	_M_swap_data(_Vector_impl_data& __x) _GLIBCXX_NOEXCEPT
	{
	  // Do not use std::swap(_M_start, __x._M_start), etc as it loses
	  // information used by TBAA.
	  _Vector_impl_data __tmp;
	  __tmp._M_copy_data(*this);
	  _M_copy_data(__x);
	  __x._M_copy_data(__tmp);
	}
      };

      struct _Vector_impl
	: public _Tp_alloc_type, public _Vector_impl_data
      {
	_GLIBCXX20_CONSTEXPR
	_Vector_impl() _GLIBCXX_NOEXCEPT_IF(
	    is_nothrow_default_constructible<_Tp_alloc_type>::value)
	: _Tp_alloc_type()
	{ }

	_GLIBCXX20_CONSTEXPR
	_Vector_impl(_Tp_alloc_type const& __a) _GLIBCXX_NOEXCEPT
	: _Tp_alloc_type(__a)
	{ }

#if __cplusplus >= 201103L
	// Not defaulted, to enforce noexcept(true) even when
	// !is_nothrow_move_constructible<_Tp_alloc_type>.
	_GLIBCXX20_CONSTEXPR
	_Vector_impl(_Vector_impl&& __x) noexcept
	: _Tp_alloc_type(std::move(__x)), _Vector_impl_data(std::move(__x))
	{ }

	_GLIBCXX20_CONSTEXPR
	_Vector_impl(_Tp_alloc_type&& __a) noexcept
	: _Tp_alloc_type(std::move(__a))
	{ }

	_GLIBCXX20_CONSTEXPR
	_Vector_impl(_Tp_alloc_type&& __a, _Vector_impl&& __rv) noexcept
	: _Tp_alloc_type(std::move(__a)), _Vector_impl_data(std::move(__rv))
	{ }
#endif

#if _GLIBCXX_SANITIZE_STD_ALLOCATOR && _GLIBCXX_SANITIZE_VECTOR
	template<typename = _Tp_alloc_type>
	  struct _Asan
	  {
	    typedef typename __gnu_cxx::__alloc_traits<_Tp_alloc_type>
	      ::size_type size_type;

	    static _GLIBCXX20_CONSTEXPR void
	    _S_shrink(_Vector_impl&, size_type) { }
	    static _GLIBCXX20_CONSTEXPR void
	    _S_on_dealloc(_Vector_impl&) { }

	    typedef _Vector_impl& _Reinit;

	    struct _Grow
	    {
	      _GLIBCXX20_CONSTEXPR _Grow(_Vector_impl&, size_type) { }
	      _GLIBCXX20_CONSTEXPR void _M_grew(size_type) { }
	    };
	  };

	// Enable ASan annotations for memory obtained from std::allocator.
	template<typename _Up>
	  struct _Asan<allocator<_Up> >
	  {
	    typedef typename __gnu_cxx::__alloc_traits<_Tp_alloc_type>
	      ::size_type size_type;

	    // Adjust ASan annotation for [_M_start, _M_end_of_storage) to
	    // mark end of valid region as __curr instead of __prev.
	    static _GLIBCXX20_CONSTEXPR void
	    _S_adjust(_Vector_impl& __impl, pointer __prev, pointer __curr)
	    {
#if __cpp_lib_is_constant_evaluated
	      if (std::is_constant_evaluated())
		return;
#endif
	      __sanitizer_annotate_contiguous_container(__impl._M_start,
		  __impl._M_end_of_storage, __prev, __curr);
	    }

	    static _GLIBCXX20_CONSTEXPR void
	    _S_grow(_Vector_impl& __impl, size_type __n)
	    { _S_adjust(__impl, __impl._M_finish, __impl._M_finish + __n); }

	    static _GLIBCXX20_CONSTEXPR void
	    _S_shrink(_Vector_impl& __impl, size_type __n)
	    { _S_adjust(__impl, __impl._M_finish + __n, __impl._M_finish); }

	    static _GLIBCXX20_CONSTEXPR void
	    _S_on_dealloc(_Vector_impl& __impl)
	    {
	      if (__impl._M_start)
		_S_adjust(__impl, __impl._M_finish, __impl._M_end_of_storage);
	    }

	    // Used on reallocation to tell ASan unused capacity is invalid.
	    struct _Reinit
	    {
	      explicit _GLIBCXX20_CONSTEXPR
	      _Reinit(_Vector_impl& __impl) : _M_impl(__impl)
	      {
		// Mark unused capacity as valid again before deallocating it.
		_S_on_dealloc(_M_impl);
	      }

	      _GLIBCXX20_CONSTEXPR
	      ~_Reinit()
	      {
		// Mark unused capacity as invalid after reallocation.
		if (_M_impl._M_start)
		  _S_adjust(_M_impl, _M_impl._M_end_of_storage,
			    _M_impl._M_finish);
	      }

	      _Vector_impl& _M_impl;

#if __cplusplus >= 201103L
	      _Reinit(const _Reinit&) = delete;
	      _Reinit& operator=(const _Reinit&) = delete;
#endif
	    };

	    // Tell ASan when unused capacity is initialized to be valid.
	    struct _Grow
	    {
	      _GLIBCXX20_CONSTEXPR
	      _Grow(_Vector_impl& __impl, size_type __n)
	      : _M_impl(__impl), _M_n(__n)
	      { _S_grow(_M_impl, __n); }

	      _GLIBCXX20_CONSTEXPR
	      ~_Grow() { if (_M_n) _S_shrink(_M_impl, _M_n); }

	      _GLIBCXX20_CONSTEXPR
	      void _M_grew(size_type __n) { _M_n -= __n; }

#if __cplusplus >= 201103L
	      _Grow(const _Grow&) = delete;
	      _Grow& operator=(const _Grow&) = delete;
#endif
	    private:
	      _Vector_impl& _M_impl;
	      size_type _M_n;
	    };
	  };

#define _GLIBCXX_ASAN_ANNOTATE_REINIT \
  typename _Base::_Vector_impl::template _Asan<>::_Reinit const \
	__attribute__((__unused__)) __reinit_guard(this->_M_impl)
#define _GLIBCXX_ASAN_ANNOTATE_GROW(n) \
  typename _Base::_Vector_impl::template _Asan<>::_Grow \
	__attribute__((__unused__)) __grow_guard(this->_M_impl, (n))
#define _GLIBCXX_ASAN_ANNOTATE_GREW(n) __grow_guard._M_grew(n)
#define _GLIBCXX_ASAN_ANNOTATE_SHRINK(n) \
  _Base::_Vector_impl::template _Asan<>::_S_shrink(this->_M_impl, n)
#define _GLIBCXX_ASAN_ANNOTATE_BEFORE_DEALLOC \
  _Base::_Vector_impl::template _Asan<>::_S_on_dealloc(this->_M_impl)
#else // ! (_GLIBCXX_SANITIZE_STD_ALLOCATOR && _GLIBCXX_SANITIZE_VECTOR)
#define _GLIBCXX_ASAN_ANNOTATE_REINIT
#define _GLIBCXX_ASAN_ANNOTATE_GROW(n)
#define _GLIBCXX_ASAN_ANNOTATE_GREW(n)
#define _GLIBCXX_ASAN_ANNOTATE_SHRINK(n)
#define _GLIBCXX_ASAN_ANNOTATE_BEFORE_DEALLOC
#endif // _GLIBCXX_SANITIZE_STD_ALLOCATOR && _GLIBCXX_SANITIZE_VECTOR
      };

    public:
      typedef _Alloc allocator_type;

      _GLIBCXX20_CONSTEXPR
      _Tp_alloc_type&
      _M_get_Tp_allocator() _GLIBCXX_NOEXCEPT
      { return this->_M_impl; }

      _GLIBCXX20_CONSTEXPR
      const _Tp_alloc_type&
      _M_get_Tp_allocator() const _GLIBCXX_NOEXCEPT
      { return this->_M_impl; }

      _GLIBCXX20_CONSTEXPR
      allocator_type
      get_allocator() const _GLIBCXX_NOEXCEPT
      { return allocator_type(_M_get_Tp_allocator()); }

#if __cplusplus >= 201103L
      _Vector_base() = default;
#else
      _Vector_base() { }
#endif

      _GLIBCXX20_CONSTEXPR
      _Vector_base(const allocator_type& __a) _GLIBCXX_NOEXCEPT
      : _M_impl(__a) { }

      // Kept for ABI compatibility.
#if !_GLIBCXX_INLINE_VERSION
      _GLIBCXX20_CONSTEXPR
      _Vector_base(size_t __n)
      : _M_impl()
      { _M_create_storage(__n); }
#endif

      _GLIBCXX20_CONSTEXPR
      _Vector_base(size_t __n, const allocator_type& __a)
      : _M_impl(__a)
      { _M_create_storage(__n); }

#if __cplusplus >= 201103L
      _Vector_base(_Vector_base&&) = default;

      // Kept for ABI compatibility.
# if !_GLIBCXX_INLINE_VERSION
      _GLIBCXX20_CONSTEXPR
      _Vector_base(_Tp_alloc_type&& __a) noexcept
      : _M_impl(std::move(__a)) { }

      _GLIBCXX20_CONSTEXPR
      _Vector_base(_Vector_base&& __x, const allocator_type& __a)
      : _M_impl(__a)
      {
	if (__x.get_allocator() == __a)
	  this->_M_impl._M_swap_data(__x._M_impl);
	else
	  {
	    size_t __n = __x._M_impl._M_finish - __x._M_impl._M_start;
	    _M_create_storage(__n);
	  }
      }
# endif

      _GLIBCXX20_CONSTEXPR
      _Vector_base(const allocator_type& __a, _Vector_base&& __x)
      : _M_impl(_Tp_alloc_type(__a), std::move(__x._M_impl))
      { }
#endif

      _GLIBCXX20_CONSTEXPR
      ~_Vector_base() _GLIBCXX_NOEXCEPT
      {
	_M_deallocate(_M_impl._M_start,
		      _M_impl._M_end_of_storage - _M_impl._M_start);
      }

    public:
      _Vector_impl _M_impl;

      _GLIBCXX20_CONSTEXPR
      pointer
      _M_allocate(size_t __n)
      {
	typedef __gnu_cxx::__alloc_traits<_Tp_alloc_type> _Tr;
	return __n != 0 ? _Tr::allocate(_M_impl, __n) : pointer();
      }

      _GLIBCXX20_CONSTEXPR
      void
      _M_deallocate(pointer __p, size_t __n)
      {
	typedef __gnu_cxx::__alloc_traits<_Tp_alloc_type> _Tr;
	if (__p)
	  _Tr::deallocate(_M_impl, __p, __n);
      }

    protected:
      _GLIBCXX20_CONSTEXPR
      void
      _M_create_storage(size_t __n)
      {
	this->_M_impl._M_start = this->_M_allocate(__n);
	this->_M_impl._M_finish = this->_M_impl._M_start;
	this->_M_impl._M_end_of_storage = this->_M_impl._M_start + __n;
      }
    };

  /**
   *  @brief A standard container which offers fixed time access to
   *  individual elements in any order.
   *
   *  @ingroup sequences
   *
   *  @tparam _Tp  Type of element.
   *  @tparam _Alloc  Allocator type, defaults to allocator<_Tp>.
   *
   *  Meets the requirements of a <a href="tables.html#65">container</a>, a
   *  <a href="tables.html#66">reversible container</a>, and a
   *  <a href="tables.html#67">sequence</a>, including the
   *  <a href="tables.html#68">optional sequence requirements</a> with the
   *  %exception of @c push_front and @c pop_front.
   *
   *  In some terminology a %vector can be described as a dynamic
   *  C-style array, it offers fast and efficient access to individual
   *  elements in any order and saves the user from worrying about
   *  memory and size allocation.  Subscripting ( @c [] ) access is
   *  also provided as with C-style arrays.
  */
  template<typename _Tp, typename _Alloc = std::allocator<_Tp> >
    class vector : protected _Vector_base<_Tp, _Alloc>
    {
#ifdef _GLIBCXX_CONCEPT_CHECKS
      // Concept requirements.
      typedef typename _Alloc::value_type		_Alloc_value_type;
# if __cplusplus < 201103L
      __glibcxx_class_requires(_Tp, _SGIAssignableConcept)
# endif
      __glibcxx_class_requires2(_Tp, _Alloc_value_type, _SameTypeConcept)
#endif

#if __cplusplus >= 201103L
      static_assert(is_same<typename remove_cv<_Tp>::type, _Tp>::value,
	  "std::vector must have a non-const, non-volatile value_type");
# if __cplusplus > 201703L || defined __STRICT_ANSI__
      static_assert(is_same<typename _Alloc::value_type, _Tp>::value,
	  "std::vector must have the same value_type as its allocator");
# endif
#endif

      typedef _Vector_base<_Tp, _Alloc>			_Base;
      typedef typename _Base::_Tp_alloc_type		_Tp_alloc_type;
      typedef __gnu_cxx::__alloc_traits<_Tp_alloc_type>	_Alloc_traits;

    public:
      typedef _Tp					value_type;
      typedef typename _Base::pointer			pointer;
      typedef typename _Alloc_traits::const_pointer	const_pointer;
      typedef typename _Alloc_traits::reference		reference;
      typedef typename _Alloc_traits::const_reference	const_reference;
      typedef __gnu_cxx::__normal_iterator<pointer, vector> iterator;
      typedef __gnu_cxx::__normal_iterator<const_pointer, vector>
      const_iterator;
      typedef std::reverse_iterator<const_iterator>	const_reverse_iterator;
      typedef std::reverse_iterator<iterator>		reverse_iterator;
      typedef size_t					size_type;
      typedef ptrdiff_t					difference_type;
      typedef _Alloc					allocator_type;

    private:
#if __cplusplus >= 201103L
      static constexpr bool
      _S_nothrow_relocate(true_type)
      {
	return noexcept(std::__relocate_a(std::declval<pointer>(),
					  std::declval<pointer>(),
					  std::declval<pointer>(),
					  std::declval<_Tp_alloc_type&>()));
      }

      static constexpr bool
      _S_nothrow_relocate(false_type)
      { return false; }

      static constexpr bool
      _S_use_relocate()
      {
	// Instantiating std::__relocate_a might cause an error outside the
	// immediate context (in __relocate_object_a's noexcept-specifier),
	// so only do it if we know the type can be move-inserted into *this.
	return _S_nothrow_relocate(__is_move_insertable<_Tp_alloc_type>{});
      }

      static pointer
      _S_do_relocate(pointer __first, pointer __last, pointer __result,
		     _Tp_alloc_type& __alloc, true_type) noexcept
      {
	return std::__relocate_a(__first, __last, __result, __alloc);
      }

      static pointer
      _S_do_relocate(pointer, pointer, pointer __result,
		     _Tp_alloc_type&, false_type) noexcept
      { return __result; }

      static _GLIBCXX20_CONSTEXPR pointer
      _S_relocate(pointer __first, pointer __last, pointer __result,
		  _Tp_alloc_type& __alloc) noexcept
      {
#if __cpp_if_constexpr
	// All callers have already checked _S_use_relocate() so just do it.
	return std::__relocate_a(__first, __last, __result, __alloc);
#else
	using __do_it = __bool_constant<_S_use_relocate()>;
	return _S_do_relocate(__first, __last, __result, __alloc, __do_it{});
#endif
      }
#endif // C++11

    protected:
      using _Base::_M_allocate;
      using _Base::_M_deallocate;
      using _Base::_M_impl;
      using _Base::_M_get_Tp_allocator;

    public:
      // [23.2.4.1] construct/copy/destroy
      // (assign() and get_allocator() are also listed in this section)

      /**
       *  @brief  Creates a %vector with no elements.
       */
#if __cplusplus >= 201103L
      vector() = default;
#else
      vector() { }
#endif

      /**
       *  @brief  Creates a %vector with no elements.
       *  @param  __a  An allocator object.
       */
      explicit
      _GLIBCXX20_CONSTEXPR
      vector(const allocator_type& __a) _GLIBCXX_NOEXCEPT
      : _Base(__a) { }

#if __cplusplus >= 201103L
      /**
       *  @brief  Creates a %vector with default constructed elements.
       *  @param  __n  The number of elements to initially create.
       *  @param  __a  An allocator.
       *
       *  This constructor fills the %vector with @a __n default
       *  constructed elements.
       */
      explicit
      _GLIBCXX20_CONSTEXPR
      vector(size_type __n, const allocator_type& __a = allocator_type())
      : _Base(_S_check_init_len(__n, __a), __a)
      { _M_default_initialize(__n); }

      /**
       *  @brief  Creates a %vector with copies of an exemplar element.
       *  @param  __n  The number of elements to initially create.
       *  @param  __value  An element to copy.
       *  @param  __a  An allocator.
       *
       *  This constructor fills the %vector with @a __n copies of @a __value.
       */
      _GLIBCXX20_CONSTEXPR
      vector(size_type __n, const value_type& __value,
	     const allocator_type& __a = allocator_type())
      : _Base(_S_check_init_len(__n, __a), __a)
      { _M_fill_initialize(__n, __value); }
#else
      /**
       *  @brief  Creates a %vector with copies of an exemplar element.
       *  @param  __n  The number of elements to initially create.
       *  @param  __value  An element to copy.
       *  @param  __a  An allocator.
       *
       *  This constructor fills the %vector with @a __n copies of @a __value.
       */
      explicit
      vector(size_type __n, const value_type& __value = value_type(),
	     const allocator_type& __a = allocator_type())
      : _Base(_S_check_init_len(__n, __a), __a)
      { _M_fill_initialize(__n, __value); }
#endif

      /**
       *  @brief  %Vector copy constructor.
       *  @param  __x  A %vector of identical element and allocator types.
       *
       *  All the elements of @a __x are copied, but any unused capacity in
       *  @a __x  will not be copied
       *  (i.e. capacity() == size() in the new %vector).
       *
       *  The newly-created %vector uses a copy of the allocator object used
       *  by @a __x (unless the allocator traits dictate a different object).
       */
      _GLIBCXX20_CONSTEXPR
      vector(const vector& __x)
      : _Base(__x.size(),
	_Alloc_traits::_S_select_on_copy(__x._M_get_Tp_allocator()))
      {
	this->_M_impl._M_finish =
	  std::__uninitialized_copy_a(__x.begin(), __x.end(),
				      this->_M_impl._M_start,
				      _M_get_Tp_allocator());
      }

#if __cplusplus >= 201103L
      /**
       *  @brief  %Vector move constructor.
       *
       *  The newly-created %vector contains the exact contents of the
       *  moved instance.
       *  The contents of the moved instance are a valid, but unspecified
       *  %vector.
       */
      vector(vector&&) noexcept = default;

      /// Copy constructor with alternative allocator
      _GLIBCXX20_CONSTEXPR
      vector(const vector& __x, const __type_identity_t<allocator_type>& __a)
      : _Base(__x.size(), __a)
      {
	this->_M_impl._M_finish =
	  std::__uninitialized_copy_a(__x.begin(), __x.end(),
				      this->_M_impl._M_start,
				      _M_get_Tp_allocator());
      }

    private:
      _GLIBCXX20_CONSTEXPR
      vector(vector&& __rv, const allocator_type& __m, true_type) noexcept
      : _Base(__m, std::move(__rv))
      { }

      _GLIBCXX20_CONSTEXPR
      vector(vector&& __rv, const allocator_type& __m, false_type)
      : _Base(__m)
      {
	if (__rv.get_allocator() == __m)
	  this->_M_impl._M_swap_data(__rv._M_impl);
	else if (!__rv.empty())
	  {
	    this->_M_create_storage(__rv.size());
	    this->_M_impl._M_finish =
	      std::__uninitialized_move_a(__rv.begin(), __rv.end(),
					  this->_M_impl._M_start,
					  _M_get_Tp_allocator());
	    __rv.clear();
	  }
      }

    public:
      /// Move constructor with alternative allocator
      _GLIBCXX20_CONSTEXPR
      vector(vector&& __rv, const __type_identity_t<allocator_type>& __m)
      noexcept( noexcept(
	vector(std::declval<vector&&>(), std::declval<const allocator_type&>(),
	       std::declval<typename _Alloc_traits::is_always_equal>())) )
      : vector(std::move(__rv), __m, typename _Alloc_traits::is_always_equal{})
      { }

      /**
       *  @brief  Builds a %vector from an initializer list.
       *  @param  __l  An initializer_list.
       *  @param  __a  An allocator.
       *
       *  Create a %vector consisting of copies of the elements in the
       *  initializer_list @a __l.
       *
       *  This will call the element type's copy constructor N times
       *  (where N is @a __l.size()) and do no memory reallocation.
       */
      _GLIBCXX20_CONSTEXPR
      vector(initializer_list<value_type> __l,
	     const allocator_type& __a = allocator_type())
      : _Base(__a)
      {
	_M_range_initialize(__l.begin(), __l.end(),
			    random_access_iterator_tag());
      }
#endif

      /**
       *  @brief  Builds a %vector from a range.
       *  @param  __first  An input iterator.
       *  @param  __last  An input iterator.
       *  @param  __a  An allocator.
       *
       *  Create a %vector consisting of copies of the elements from
       *  [first,last).
       *
       *  If the iterators are forward, bidirectional, or
       *  random-access, then this will call the elements' copy
       *  constructor N times (where N is distance(first,last)) and do
       *  no memory reallocation.  But if only input iterators are
       *  used, then this will do at most 2N calls to the copy
       *  constructor, and logN memory reallocations.
       */
#if __cplusplus >= 201103L
      template<typename _InputIterator,
	       typename = std::_RequireInputIter<_InputIterator>>
	_GLIBCXX20_CONSTEXPR
	vector(_InputIterator __first, _InputIterator __last,
	       const allocator_type& __a = allocator_type())
	: _Base(__a)
	{
	  _M_range_initialize(__first, __last,
			      std::__iterator_category(__first));
	}
#else
      template<typename _InputIterator>
	vector(_InputIterator __first, _InputIterator __last,
	       const allocator_type& __a = allocator_type())
	: _Base(__a)
	{
	  // Check whether it's an integral type.  If so, it's not an iterator.
	  typedef typename std::__is_integer<_InputIterator>::__type _Integral;
	  _M_initialize_dispatch(__first, __last, _Integral());
	}
#endif

      /**
       *  The dtor only erases the elements, and note that if the
       *  elements themselves are pointers, the pointed-to memory is
       *  not touched in any way.  Managing the pointer is the user's
       *  responsibility.
       */
      _GLIBCXX20_CONSTEXPR
      ~vector() _GLIBCXX_NOEXCEPT
      {
	std::_Destroy(this->_M_impl._M_start, this->_M_impl._M_finish,
		      _M_get_Tp_allocator());
	_GLIBCXX_ASAN_ANNOTATE_BEFORE_DEALLOC;
      }

      /**
       *  @brief  %Vector assignment operator.
       *  @param  __x  A %vector of identical element and allocator types.
       *
       *  All the elements of @a __x are copied, but any unused capacity in
       *  @a __x will not be copied.
       *
       *  Whether the allocator is copied depends on the allocator traits.
       */
      _GLIBCXX20_CONSTEXPR
      vector&
      operator=(const vector& __x);

#if __cplusplus >= 201103L
      /**
       *  @brief  %Vector move assignment operator.
       *  @param  __x  A %vector of identical element and allocator types.
       *
       *  The contents of @a __x are moved into this %vector (without copying,
       *  if the allocators permit it).
       *  Afterwards @a __x is a valid, but unspecified %vector.
       *
       *  Whether the allocator is moved depends on the allocator traits.
       */
      _GLIBCXX20_CONSTEXPR
      vector&
      operator=(vector&& __x) noexcept(_Alloc_traits::_S_nothrow_move())
      {
	constexpr bool __move_storage =
	  _Alloc_traits::_S_propagate_on_move_assign()
	  || _Alloc_traits::_S_always_equal();
	_M_move_assign(std::move(__x), __bool_constant<__move_storage>());
	return *this;
      }

      /**
       *  @brief  %Vector list assignment operator.
       *  @param  __l  An initializer_list.
       *
       *  This function fills a %vector with copies of the elements in the
       *  initializer list @a __l.
       *
       *  Note that the assignment completely changes the %vector and
       *  that the resulting %vector's size is the same as the number
       *  of elements assigned.
       */
      _GLIBCXX20_CONSTEXPR
      vector&
      operator=(initializer_list<value_type> __l)
      {
	this->_M_assign_aux(__l.begin(), __l.end(),
			    random_access_iterator_tag());
	return *this;
      }
#endif

      /**
       *  @brief  Assigns a given value to a %vector.
       *  @param  __n  Number of elements to be assigned.
       *  @param  __val  Value to be assigned.
       *
       *  This function fills a %vector with @a __n copies of the given
       *  value.  Note that the assignment completely changes the
       *  %vector and that the resulting %vector's size is the same as
       *  the number of elements assigned.
       */
      _GLIBCXX20_CONSTEXPR
      void
      assign(size_type __n, const value_type& __val)
      { _M_fill_assign(__n, __val); }

      /**
       *  @brief  Assigns a range to a %vector.
       *  @param  __first  An input iterator.
       *  @param  __last   An input iterator.
       *
       *  This function fills a %vector with copies of the elements in the
       *  range [__first,__last).
       *
       *  Note that the assignment completely changes the %vector and
       *  that the resulting %vector's size is the same as the number
       *  of elements assigned.
       */
#if __cplusplus >= 201103L
      template<typename _InputIterator,
	       typename = std::_RequireInputIter<_InputIterator>>
	_GLIBCXX20_CONSTEXPR
	void
	assign(_InputIterator __first, _InputIterator __last)
	{ _M_assign_dispatch(__first, __last, __false_type()); }
#else
      template<typename _InputIterator>
	void
	assign(_InputIterator __first, _InputIterator __last)
	{
	  // Check whether it's an integral type.  If so, it's not an iterator.
	  typedef typename std::__is_integer<_InputIterator>::__type _Integral;
	  _M_assign_dispatch(__first, __last, _Integral());
	}
#endif

#if __cplusplus >= 201103L
      /**
       *  @brief  Assigns an initializer list to a %vector.
       *  @param  __l  An initializer_list.
       *
       *  This function fills a %vector with copies of the elements in the
       *  initializer list @a __l.
       *
       *  Note that the assignment completely changes the %vector and
       *  that the resulting %vector's size is the same as the number
       *  of elements assigned.
       */
      _GLIBCXX20_CONSTEXPR
      void
      assign(initializer_list<value_type> __l)
      {
	this->_M_assign_aux(__l.begin(), __l.end(),
			    random_access_iterator_tag());
      }
#endif

      /// Get a copy of the memory allocation object.
      using _Base::get_allocator;

      // iterators
      /**
       *  Returns a read/write iterator that points to the first
       *  element in the %vector.  Iteration is done in ordinary
       *  element order.
       */
      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      iterator
      begin() _GLIBCXX_NOEXCEPT
      { return iterator(this->_M_impl._M_start); }

      /**
       *  Returns a read-only (constant) iterator that points to the
       *  first element in the %vector.  Iteration is done in ordinary
       *  element order.
       */
      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      const_iterator
      begin() const _GLIBCXX_NOEXCEPT
      { return const_iterator(this->_M_impl._M_start); }

      /**
       *  Returns a read/write iterator that points one past the last
       *  element in the %vector.  Iteration is done in ordinary
       *  element order.
       */
      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      iterator
      end() _GLIBCXX_NOEXCEPT
      { return iterator(this->_M_impl._M_finish); }

      /**
       *  Returns a read-only (constant) iterator that points one past
       *  the last element in the %vector.  Iteration is done in
       *  ordinary element order.
       */
      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      const_iterator
      end() const _GLIBCXX_NOEXCEPT
      { return const_iterator(this->_M_impl._M_finish); }

      /**
       *  Returns a read/write reverse iterator that points to the
       *  last element in the %vector.  Iteration is done in reverse
       *  element order.
       */
      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      reverse_iterator
      rbegin() _GLIBCXX_NOEXCEPT
      { return reverse_iterator(end()); }

      /**
       *  Returns a read-only (constant) reverse iterator that points
       *  to the last element in the %vector.  Iteration is done in
       *  reverse element order.
       */
      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      const_reverse_iterator
      rbegin() const _GLIBCXX_NOEXCEPT
      { return const_reverse_iterator(end()); }

      /**
       *  Returns a read/write reverse iterator that points to one
       *  before the first element in the %vector.  Iteration is done
       *  in reverse element order.
       */
      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      reverse_iterator
      rend() _GLIBCXX_NOEXCEPT
      { return reverse_iterator(begin()); }

      /**
       *  Returns a read-only (constant) reverse iterator that points
       *  to one before the first element in the %vector.  Iteration
       *  is done in reverse element order.
       */
      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      const_reverse_iterator
      rend() const _GLIBCXX_NOEXCEPT
      { return const_reverse_iterator(begin()); }

#if __cplusplus >= 201103L
      /**
       *  Returns a read-only (constant) iterator that points to the
       *  first element in the %vector.  Iteration is done in ordinary
       *  element order.
       */
      [[__nodiscard__]] _GLIBCXX20_CONSTEXPR
      const_iterator
      cbegin() const noexcept
      { return const_iterator(this->_M_impl._M_start); }

      /**
       *  Returns a read-only (constant) iterator that points one past
       *  the last element in the %vector.  Iteration is done in
       *  ordinary element order.
       */
      [[__nodiscard__]] _GLIBCXX20_CONSTEXPR
      const_iterator
      cend() const noexcept
      { return const_iterator(this->_M_impl._M_finish); }

      /**
       *  Returns a read-only (constant) reverse iterator that points
       *  to the last element in the %vector.  Iteration is done in
       *  reverse element order.
       */
      [[__nodiscard__]] _GLIBCXX20_CONSTEXPR
      const_reverse_iterator
      crbegin() const noexcept
      { return const_reverse_iterator(end()); }

      /**
       *  Returns a read-only (constant) reverse iterator that points
       *  to one before the first element in the %vector.  Iteration
       *  is done in reverse element order.
       */
      [[__nodiscard__]] _GLIBCXX20_CONSTEXPR
      const_reverse_iterator
      crend() const noexcept
      { return const_reverse_iterator(begin()); }
#endif

      // [23.2.4.2] capacity
      /**  Returns the number of elements in the %vector.  */
      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      size_type
      size() const _GLIBCXX_NOEXCEPT
      { return size_type(this->_M_impl._M_finish - this->_M_impl._M_start); }

      /**  Returns the size() of the largest possible %vector.  */
      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      size_type
      max_size() const _GLIBCXX_NOEXCEPT
      { return _S_max_size(_M_get_Tp_allocator()); }

#if __cplusplus >= 201103L
      /**
       *  @brief  Resizes the %vector to the specified number of elements.
       *  @param  __new_size  Number of elements the %vector should contain.
       *
       *  This function will %resize the %vector to the specified
       *  number of elements.  If the number is smaller than the
       *  %vector's current size the %vector is truncated, otherwise
       *  default constructed elements are appended.
       */
      _GLIBCXX20_CONSTEXPR
      void
      resize(size_type __new_size)
      {
	if (__new_size > size())
	  _M_default_append(__new_size - size());
	else if (__new_size < size())
	  _M_erase_at_end(this->_M_impl._M_start + __new_size);
      }

      /**
       *  @brief  Resizes the %vector to the specified number of elements.
       *  @param  __new_size  Number of elements the %vector should contain.
       *  @param  __x  Data with which new elements should be populated.
       *
       *  This function will %resize the %vector to the specified
       *  number of elements.  If the number is smaller than the
       *  %vector's current size the %vector is truncated, otherwise
       *  the %vector is extended and new elements are populated with
       *  given data.
       */
      _GLIBCXX20_CONSTEXPR
      void
      resize(size_type __new_size, const value_type& __x)
      {
	if (__new_size > size())
	  _M_fill_insert(end(), __new_size - size(), __x);
	else if (__new_size < size())
	  _M_erase_at_end(this->_M_impl._M_start + __new_size);
      }
#else
      /**
       *  @brief  Resizes the %vector to the specified number of elements.
       *  @param  __new_size  Number of elements the %vector should contain.
       *  @param  __x  Data with which new elements should be populated.
       *
       *  This function will %resize the %vector to the specified
       *  number of elements.  If the number is smaller than the
       *  %vector's current size the %vector is truncated, otherwise
       *  the %vector is extended and new elements are populated with
       *  given data.
       */
      _GLIBCXX20_CONSTEXPR
      void
      resize(size_type __new_size, value_type __x = value_type())
      {
	if (__new_size > size())
	  _M_fill_insert(end(), __new_size - size(), __x);
	else if (__new_size < size())
	  _M_erase_at_end(this->_M_impl._M_start + __new_size);
      }
#endif

#if __cplusplus >= 201103L
      /**  A non-binding request to reduce capacity() to size().  */
      _GLIBCXX20_CONSTEXPR
      void
      shrink_to_fit()
      { _M_shrink_to_fit(); }
#endif

      /**
       *  Returns the total number of elements that the %vector can
       *  hold before needing to allocate more memory.
       */
      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      size_type
      capacity() const _GLIBCXX_NOEXCEPT
      { return size_type(this->_M_impl._M_end_of_storage
			 - this->_M_impl._M_start); }

      /**
       *  Returns true if the %vector is empty.  (Thus begin() would
       *  equal end().)
       */
      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      bool
      empty() const _GLIBCXX_NOEXCEPT
      { return begin() == end(); }

      /**
       *  @brief  Attempt to preallocate enough memory for specified number of
       *          elements.
       *  @param  __n  Number of elements required.
       *  @throw  std::length_error  If @a n exceeds @c max_size().
       *
       *  This function attempts to reserve enough memory for the
       *  %vector to hold the specified number of elements.  If the
       *  number requested is more than max_size(), length_error is
       *  thrown.
       *
       *  The advantage of this function is that if optimal code is a
       *  necessity and the user can determine the number of elements
       *  that will be required, the user can reserve the memory in
       *  %advance, and thus prevent a possible reallocation of memory
       *  and copying of %vector data.
       */
      _GLIBCXX20_CONSTEXPR
      void
      reserve(size_type __n);

      // element access
      /**
       *  @brief  Subscript access to the data contained in the %vector.
       *  @param __n The index of the element for which data should be
       *  accessed.
       *  @return  Read/write reference to data.
       *
       *  This operator allows for easy, array-style, data access.
       *  Note that data access with this operator is unchecked and
       *  out_of_range lookups are not defined. (For checked lookups
       *  see at().)
       */
      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      reference
      operator[](size_type __n) _GLIBCXX_NOEXCEPT
      {
	__glibcxx_requires_subscript(__n);
	return *(this->_M_impl._M_start + __n);
      }

      /**
       *  @brief  Subscript access to the data contained in the %vector.
       *  @param __n The index of the element for which data should be
       *  accessed.
       *  @return  Read-only (constant) reference to data.
       *
       *  This operator allows for easy, array-style, data access.
       *  Note that data access with this operator is unchecked and
       *  out_of_range lookups are not defined. (For checked lookups
       *  see at().)
       */
      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      const_reference
      operator[](size_type __n) const _GLIBCXX_NOEXCEPT
      {
	__glibcxx_requires_subscript(__n);
	return *(this->_M_impl._M_start + __n);
      }

    protected:
      /// Safety check used only from at().
      _GLIBCXX20_CONSTEXPR
      void
      _M_range_check(size_type __n) const
      {
	if (__n >= this->size())
	  __throw_out_of_range_fmt(__N("vector::_M_range_check: __n "
				       "(which is %zu) >= this->size() "
				       "(which is %zu)"),
				   __n, this->size());
      }

    public:
      /**
       *  @brief  Provides access to the data contained in the %vector.
       *  @param __n The index of the element for which data should be
       *  accessed.
       *  @return  Read/write reference to data.
       *  @throw  std::out_of_range  If @a __n is an invalid index.
       *
       *  This function provides for safer data access.  The parameter
       *  is first checked that it is in the range of the vector.  The
       *  function throws out_of_range if the check fails.
       */
      _GLIBCXX20_CONSTEXPR
      reference
      at(size_type __n)
      {
	_M_range_check(__n);
	return (*this)[__n];
      }

      /**
       *  @brief  Provides access to the data contained in the %vector.
       *  @param __n The index of the element for which data should be
       *  accessed.
       *  @return  Read-only (constant) reference to data.
       *  @throw  std::out_of_range  If @a __n is an invalid index.
       *
       *  This function provides for safer data access.  The parameter
       *  is first checked that it is in the range of the vector.  The
       *  function throws out_of_range if the check fails.
       */
      _GLIBCXX20_CONSTEXPR
      const_reference
      at(size_type __n) const
      {
	_M_range_check(__n);
	return (*this)[__n];
      }

      /**
       *  Returns a read/write reference to the data at the first
       *  element of the %vector.
       */
      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      reference
      front() _GLIBCXX_NOEXCEPT
      {
	__glibcxx_requires_nonempty();
	return *begin();
      }

      /**
       *  Returns a read-only (constant) reference to the data at the first
       *  element of the %vector.
       */
      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      const_reference
      front() const _GLIBCXX_NOEXCEPT
      {
	__glibcxx_requires_nonempty();
	return *begin();
      }

      /**
       *  Returns a read/write reference to the data at the last
       *  element of the %vector.
       */
      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      reference
      back() _GLIBCXX_NOEXCEPT
      {
	__glibcxx_requires_nonempty();
	return *(end() - 1);
      }

      /**
       *  Returns a read-only (constant) reference to the data at the
       *  last element of the %vector.
       */
      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      const_reference
      back() const _GLIBCXX_NOEXCEPT
      {
	__glibcxx_requires_nonempty();
	return *(end() - 1);
      }

      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // DR 464. Suggestion for new member functions in standard containers.
      // data access
      /**
       *   Returns a pointer such that [data(), data() + size()) is a valid
       *   range.  For a non-empty %vector, data() == &front().
       */
      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      _Tp*
      data() _GLIBCXX_NOEXCEPT
      { return _M_data_ptr(this->_M_impl._M_start); }

      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      const _Tp*
      data() const _GLIBCXX_NOEXCEPT
      { return _M_data_ptr(this->_M_impl._M_start); }

      // [23.2.4.3] modifiers
      /**
       *  @brief  Add data to the end of the %vector.
       *  @param  __x  Data to be added.
       *
       *  This is a typical stack operation.  The function creates an
       *  element at the end of the %vector and assigns the given data
       *  to it.  Due to the nature of a %vector this operation can be
       *  done in constant time if the %vector has preallocated space
       *  available.
       */
      _GLIBCXX20_CONSTEXPR
      void
      push_back(const value_type& __x)
      {
	if (this->_M_impl._M_finish != this->_M_impl._M_end_of_storage)
	  {
	    _GLIBCXX_ASAN_ANNOTATE_GROW(1);
	    _Alloc_traits::construct(this->_M_impl, this->_M_impl._M_finish,
				     __x);
	    ++this->_M_impl._M_finish;
	    _GLIBCXX_ASAN_ANNOTATE_GREW(1);
	  }
	else
	  _M_realloc_insert(end(), __x);
      }

#if __cplusplus >= 201103L
      _GLIBCXX20_CONSTEXPR
      void
      push_back(value_type&& __x)
      { emplace_back(std::move(__x)); }

      template<typename... _Args>
#if __cplusplus > 201402L
	_GLIBCXX20_CONSTEXPR
	reference
#else
	void
#endif
	emplace_back(_Args&&... __args);
#endif

      /**
       *  @brief  Removes last element.
       *
       *  This is a typical stack operation. It shrinks the %vector by one.
       *
       *  Note that no data is returned, and if the last element's
       *  data is needed, it should be retrieved before pop_back() is
       *  called.
       */
      _GLIBCXX20_CONSTEXPR
      void
      pop_back() _GLIBCXX_NOEXCEPT
      {
	__glibcxx_requires_nonempty();
	--this->_M_impl._M_finish;
	_Alloc_traits::destroy(this->_M_impl, this->_M_impl._M_finish);
	_GLIBCXX_ASAN_ANNOTATE_SHRINK(1);
      }

#if __cplusplus >= 201103L
      /**
       *  @brief  Inserts an object in %vector before specified iterator.
       *  @param  __position  A const_iterator into the %vector.
       *  @param  __args  Arguments.
       *  @return  An iterator that points to the inserted data.
       *
       *  This function will insert an object of type T constructed
       *  with T(std::forward<Args>(args)...) before the specified location.
       *  Note that this kind of operation could be expensive for a %vector
       *  and if it is frequently used the user should consider using
       *  std::list.
       */
      template<typename... _Args>
	_GLIBCXX20_CONSTEXPR
	iterator
	emplace(const_iterator __position, _Args&&... __args)
	{ return _M_emplace_aux(__position, std::forward<_Args>(__args)...); }

      /**
       *  @brief  Inserts given value into %vector before specified iterator.
       *  @param  __position  A const_iterator into the %vector.
       *  @param  __x  Data to be inserted.
       *  @return  An iterator that points to the inserted data.
       *
       *  This function will insert a copy of the given value before
       *  the specified location.  Note that this kind of operation
       *  could be expensive for a %vector and if it is frequently
       *  used the user should consider using std::list.
       */
      _GLIBCXX20_CONSTEXPR
      iterator
      insert(const_iterator __position, const value_type& __x);
#else
      /**
       *  @brief  Inserts given value into %vector before specified iterator.
       *  @param  __position  An iterator into the %vector.
       *  @param  __x  Data to be inserted.
       *  @return  An iterator that points to the inserted data.
       *
       *  This function will insert a copy of the given value before
       *  the specified location.  Note that this kind of operation
       *  could be expensive for a %vector and if it is frequently
       *  used the user should consider using std::list.
       */
      iterator
      insert(iterator __position, const value_type& __x);
#endif

#if __cplusplus >= 201103L
      /**
       *  @brief  Inserts given rvalue into %vector before specified iterator.
       *  @param  __position  A const_iterator into the %vector.
       *  @param  __x  Data to be inserted.
       *  @return  An iterator that points to the inserted data.
       *
       *  This function will insert a copy of the given rvalue before
       *  the specified location.  Note that this kind of operation
       *  could be expensive for a %vector and if it is frequently
       *  used the user should consider using std::list.
       */
      _GLIBCXX20_CONSTEXPR
      iterator
      insert(const_iterator __position, value_type&& __x)
      { return _M_insert_rval(__position, std::move(__x)); }

      /**
       *  @brief  Inserts an initializer_list into the %vector.
       *  @param  __position  An iterator into the %vector.
       *  @param  __l  An initializer_list.
       *
       *  This function will insert copies of the data in the
       *  initializer_list @a l into the %vector before the location
       *  specified by @a position.
       *
       *  Note that this kind of operation could be expensive for a
       *  %vector and if it is frequently used the user should
       *  consider using std::list.
       */
      _GLIBCXX20_CONSTEXPR
      iterator
      insert(const_iterator __position, initializer_list<value_type> __l)
      {
	auto __offset = __position - cbegin();
	_M_range_insert(begin() + __offset, __l.begin(), __l.end(),
			std::random_access_iterator_tag());
	return begin() + __offset;
      }
#endif

#if __cplusplus >= 201103L
      /**
       *  @brief  Inserts a number of copies of given data into the %vector.
       *  @param  __position  A const_iterator into the %vector.
       *  @param  __n  Number of elements to be inserted.
       *  @param  __x  Data to be inserted.
       *  @return  An iterator that points to the inserted data.
       *
       *  This function will insert a specified number of copies of
       *  the given data before the location specified by @a position.
       *
       *  Note that this kind of operation could be expensive for a
       *  %vector and if it is frequently used the user should
       *  consider using std::list.
       */
      _GLIBCXX20_CONSTEXPR
      iterator
      insert(const_iterator __position, size_type __n, const value_type& __x)
      {
	difference_type __offset = __position - cbegin();
	_M_fill_insert(begin() + __offset, __n, __x);
	return begin() + __offset;
      }
#else
      /**
       *  @brief  Inserts a number of copies of given data into the %vector.
       *  @param  __position  An iterator into the %vector.
       *  @param  __n  Number of elements to be inserted.
       *  @param  __x  Data to be inserted.
       *
       *  This function will insert a specified number of copies of
       *  the given data before the location specified by @a position.
       *
       *  Note that this kind of operation could be expensive for a
       *  %vector and if it is frequently used the user should
       *  consider using std::list.
       */
      void
      insert(iterator __position, size_type __n, const value_type& __x)
      { _M_fill_insert(__position, __n, __x); }
#endif

#if __cplusplus >= 201103L
      /**
       *  @brief  Inserts a range into the %vector.
       *  @param  __position  A const_iterator into the %vector.
       *  @param  __first  An input iterator.
       *  @param  __last   An input iterator.
       *  @return  An iterator that points to the inserted data.
       *
       *  This function will insert copies of the data in the range
       *  [__first,__last) into the %vector before the location specified
       *  by @a pos.
       *
       *  Note that this kind of operation could be expensive for a
       *  %vector and if it is frequently used the user should
       *  consider using std::list.
       */
      template<typename _InputIterator,
	       typename = std::_RequireInputIter<_InputIterator>>
	_GLIBCXX20_CONSTEXPR
	iterator
	insert(const_iterator __position, _InputIterator __first,
	       _InputIterator __last)
	{
	  difference_type __offset = __position - cbegin();
	  _M_insert_dispatch(begin() + __offset,
			     __first, __last, __false_type());
	  return begin() + __offset;
	}
#else
      /**
       *  @brief  Inserts a range into the %vector.
       *  @param  __position  An iterator into the %vector.
       *  @param  __first  An input iterator.
       *  @param  __last   An input iterator.
       *
       *  This function will insert copies of the data in the range
       *  [__first,__last) into the %vector before the location specified
       *  by @a pos.
       *
       *  Note that this kind of operation could be expensive for a
       *  %vector and if it is frequently used the user should
       *  consider using std::list.
       */
      template<typename _InputIterator>
	void
	insert(iterator __position, _InputIterator __first,
	       _InputIterator __last)
	{
	  // Check whether it's an integral type.  If so, it's not an iterator.
	  typedef typename std::__is_integer<_InputIterator>::__type _Integral;
	  _M_insert_dispatch(__position, __first, __last, _Integral());
	}
#endif

      /**
       *  @brief  Remove element at given position.
       *  @param  __position  Iterator pointing to element to be erased.
       *  @return  An iterator pointing to the next element (or end()).
       *
       *  This function will erase the element at the given position and thus
       *  shorten the %vector by one.
       *
       *  Note This operation could be expensive and if it is
       *  frequently used the user should consider using std::list.
       *  The user is also cautioned that this function only erases
       *  the element, and that if the element is itself a pointer,
       *  the pointed-to memory is not touched in any way.  Managing
       *  the pointer is the user's responsibility.
       */
      _GLIBCXX20_CONSTEXPR
      iterator
#if __cplusplus >= 201103L
      erase(const_iterator __position)
      { return _M_erase(begin() + (__position - cbegin())); }
#else
      erase(iterator __position)
      { return _M_erase(__position); }
#endif

      /**
       *  @brief  Remove a range of elements.
       *  @param  __first  Iterator pointing to the first element to be erased.
       *  @param  __last  Iterator pointing to one past the last element to be
       *                  erased.
       *  @return  An iterator pointing to the element pointed to by @a __last
       *           prior to erasing (or end()).
       *
       *  This function will erase the elements in the range
       *  [__first,__last) and shorten the %vector accordingly.
       *
       *  Note This operation could be expensive and if it is
       *  frequently used the user should consider using std::list.
       *  The user is also cautioned that this function only erases
       *  the elements, and that if the elements themselves are
       *  pointers, the pointed-to memory is not touched in any way.
       *  Managing the pointer is the user's responsibility.
       */
      _GLIBCXX20_CONSTEXPR
      iterator
#if __cplusplus >= 201103L
      erase(const_iterator __first, const_iterator __last)
      {
	const auto __beg = begin();
	const auto __cbeg = cbegin();
	return _M_erase(__beg + (__first - __cbeg), __beg + (__last - __cbeg));
      }
#else
      erase(iterator __first, iterator __last)
      { return _M_erase(__first, __last); }
#endif

      /**
       *  @brief  Swaps data with another %vector.
       *  @param  __x  A %vector of the same element and allocator types.
       *
       *  This exchanges the elements between two vectors in constant time.
       *  (Three pointers, so it should be quite fast.)
       *  Note that the global std::swap() function is specialized such that
       *  std::swap(v1,v2) will feed to this function.
       *
       *  Whether the allocators are swapped depends on the allocator traits.
       */
      _GLIBCXX20_CONSTEXPR
      void
      swap(vector& __x) _GLIBCXX_NOEXCEPT
      {
#if __cplusplus >= 201103L
	__glibcxx_assert(_Alloc_traits::propagate_on_container_swap::value
			 || _M_get_Tp_allocator() == __x._M_get_Tp_allocator());
#endif
	this->_M_impl._M_swap_data(__x._M_impl);
	_Alloc_traits::_S_on_swap(_M_get_Tp_allocator(),
				  __x._M_get_Tp_allocator());
      }

      /**
       *  Erases all the elements.  Note that this function only erases the
       *  elements, and that if the elements themselves are pointers, the
       *  pointed-to memory is not touched in any way.  Managing the pointer is
       *  the user's responsibility.
       */
      _GLIBCXX20_CONSTEXPR
      void
      clear() _GLIBCXX_NOEXCEPT
      { _M_erase_at_end(this->_M_impl._M_start); }

    protected:
      /**
       *  Memory expansion handler.  Uses the member allocation function to
       *  obtain @a n bytes of memory, and then copies [first,last) into it.
       */
      template<typename _ForwardIterator>
	_GLIBCXX20_CONSTEXPR
	pointer
	_M_allocate_and_copy(size_type __n,
			     _ForwardIterator __first, _ForwardIterator __last)
	{
	  pointer __result = this->_M_allocate(__n);
	  __try
	    {
	      std::__uninitialized_copy_a(__first, __last, __result,
					  _M_get_Tp_allocator());
	      return __result;
	    }
	  __catch(...)
	    {
	      _M_deallocate(__result, __n);
	      __throw_exception_again;
	    }
	}


      // Internal constructor functions follow.

      // Called by the range constructor to implement [23.1.1]/9

#if __cplusplus < 201103L
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 438. Ambiguity in the "do the right thing" clause
      template<typename _Integer>
	void
	_M_initialize_dispatch(_Integer __n, _Integer __value, __true_type)
	{
	  this->_M_impl._M_start = _M_allocate(_S_check_init_len(
		static_cast<size_type>(__n), _M_get_Tp_allocator()));
	  this->_M_impl._M_end_of_storage =
	    this->_M_impl._M_start + static_cast<size_type>(__n);
	  _M_fill_initialize(static_cast<size_type>(__n), __value);
	}

      // Called by the range constructor to implement [23.1.1]/9
      template<typename _InputIterator>
	void
	_M_initialize_dispatch(_InputIterator __first, _InputIterator __last,
			       __false_type)
	{
	  _M_range_initialize(__first, __last,
			      std::__iterator_category(__first));
	}
#endif

      // Called by the second initialize_dispatch above
      template<typename _InputIterator>
	_GLIBCXX20_CONSTEXPR
	void
	_M_range_initialize(_InputIterator __first, _InputIterator __last,
			    std::input_iterator_tag)
	{
	  __try {
	    for (; __first != __last; ++__first)
#if __cplusplus >= 201103L
	      emplace_back(*__first);
#else
	      push_back(*__first);
#endif
	  } __catch(...) {
	    clear();
	    __throw_exception_again;
	  }
	}

      // Called by the second initialize_dispatch above
      template<typename _ForwardIterator>
	_GLIBCXX20_CONSTEXPR
	void
	_M_range_initialize(_ForwardIterator __first, _ForwardIterator __last,
			    std::forward_iterator_tag)
	{
	  const size_type __n = std::distance(__first, __last);
	  this->_M_impl._M_start
	    = this->_M_allocate(_S_check_init_len(__n, _M_get_Tp_allocator()));
	  this->_M_impl._M_end_of_storage = this->_M_impl._M_start + __n;
	  this->_M_impl._M_finish =
	    std::__uninitialized_copy_a(__first, __last,
					this->_M_impl._M_start,
					_M_get_Tp_allocator());
	}

      // Called by the first initialize_dispatch above and by the
      // vector(n,value,a) constructor.
      _GLIBCXX20_CONSTEXPR
      void
      _M_fill_initialize(size_type __n, const value_type& __value)
      {
	this->_M_impl._M_finish =
	  std::__uninitialized_fill_n_a(this->_M_impl._M_start, __n, __value,
					_M_get_Tp_allocator());
      }

#if __cplusplus >= 201103L
      // Called by the vector(n) constructor.
      _GLIBCXX20_CONSTEXPR
      void
      _M_default_initialize(size_type __n)
      {
	this->_M_impl._M_finish =
	  std::__uninitialized_default_n_a(this->_M_impl._M_start, __n,
					   _M_get_Tp_allocator());
      }
#endif

      // Internal assign functions follow.  The *_aux functions do the actual
      // assignment work for the range versions.

      // Called by the range assign to implement [23.1.1]/9

      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 438. Ambiguity in the "do the right thing" clause
      template<typename _Integer>
	_GLIBCXX20_CONSTEXPR
	void
	_M_assign_dispatch(_Integer __n, _Integer __val, __true_type)
	{ _M_fill_assign(__n, __val); }

      // Called by the range assign to implement [23.1.1]/9
      template<typename _InputIterator>
	_GLIBCXX20_CONSTEXPR
	void
	_M_assign_dispatch(_InputIterator __first, _InputIterator __last,
			   __false_type)
	{ _M_assign_aux(__first, __last, std::__iterator_category(__first)); }

      // Called by the second assign_dispatch above
      template<typename _InputIterator>
	_GLIBCXX20_CONSTEXPR
	void
	_M_assign_aux(_InputIterator __first, _InputIterator __last,
		      std::input_iterator_tag);

      // Called by the second assign_dispatch above
      template<typename _ForwardIterator>
	_GLIBCXX20_CONSTEXPR
	void
	_M_assign_aux(_ForwardIterator __first, _ForwardIterator __last,
		      std::forward_iterator_tag);

      // Called by assign(n,t), and the range assign when it turns out
      // to be the same thing.
      _GLIBCXX20_CONSTEXPR
      void
      _M_fill_assign(size_type __n, const value_type& __val);

      // Internal insert functions follow.

      // Called by the range insert to implement [23.1.1]/9

      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 438. Ambiguity in the "do the right thing" clause
      template<typename _Integer>
	_GLIBCXX20_CONSTEXPR
	void
	_M_insert_dispatch(iterator __pos, _Integer __n, _Integer __val,
			   __true_type)
	{ _M_fill_insert(__pos, __n, __val); }

      // Called by the range insert to implement [23.1.1]/9
      template<typename _InputIterator>
	_GLIBCXX20_CONSTEXPR
	void
	_M_insert_dispatch(iterator __pos, _InputIterator __first,
			   _InputIterator __last, __false_type)
	{
	  _M_range_insert(__pos, __first, __last,
			  std::__iterator_category(__first));
	}

      // Called by the second insert_dispatch above
      template<typename _InputIterator>
	_GLIBCXX20_CONSTEXPR
	void
	_M_range_insert(iterator __pos, _InputIterator __first,
			_InputIterator __last, std::input_iterator_tag);

      // Called by the second insert_dispatch above
      template<typename _ForwardIterator>
	_GLIBCXX20_CONSTEXPR
	void
	_M_range_insert(iterator __pos, _ForwardIterator __first,
			_ForwardIterator __last, std::forward_iterator_tag);

      // Called by insert(p,n,x), and the range insert when it turns out to be
      // the same thing.
      _GLIBCXX20_CONSTEXPR
      void
      _M_fill_insert(iterator __pos, size_type __n, const value_type& __x);

#if __cplusplus >= 201103L
      // Called by resize(n).
      _GLIBCXX20_CONSTEXPR
      void
      _M_default_append(size_type __n);

      _GLIBCXX20_CONSTEXPR
      bool
      _M_shrink_to_fit();
#endif

#if __cplusplus < 201103L
      // Called by insert(p,x)
      void
      _M_insert_aux(iterator __position, const value_type& __x);

      void
      _M_realloc_insert(iterator __position, const value_type& __x);
#else
      // A value_type object constructed with _Alloc_traits::construct()
      // and destroyed with _Alloc_traits::destroy().
      struct _Temporary_value
      {
	template<typename... _Args>
	  _GLIBCXX20_CONSTEXPR explicit
	  _Temporary_value(vector* __vec, _Args&&... __args) : _M_this(__vec)
	  {
	    _Alloc_traits::construct(_M_this->_M_impl, _M_ptr(),
				     std::forward<_Args>(__args)...);
	  }

	_GLIBCXX20_CONSTEXPR
	~_Temporary_value()
	{ _Alloc_traits::destroy(_M_this->_M_impl, _M_ptr()); }

	_GLIBCXX20_CONSTEXPR value_type&
	_M_val() noexcept { return _M_storage._M_val; }

      private:
	_GLIBCXX20_CONSTEXPR _Tp*
	_M_ptr() noexcept { return std::__addressof(_M_storage._M_val); }

	union _Storage
	{
	  constexpr _Storage() : _M_byte() { }
	  _GLIBCXX20_CONSTEXPR ~_Storage() { }
	  _Storage& operator=(const _Storage&) = delete;
	  unsigned char _M_byte;
	  _Tp _M_val;
	};

	vector*  _M_this;
	_Storage _M_storage;
      };

      // Called by insert(p,x) and other functions when insertion needs to
      // reallocate or move existing elements. _Arg is either _Tp& or _Tp.
      template<typename _Arg>
	_GLIBCXX20_CONSTEXPR
	void
	_M_insert_aux(iterator __position, _Arg&& __arg);

      template<typename... _Args>
	_GLIBCXX20_CONSTEXPR
	void
	_M_realloc_insert(iterator __position, _Args&&... __args);

      // Either move-construct at the end, or forward to _M_insert_aux.
      _GLIBCXX20_CONSTEXPR
      iterator
      _M_insert_rval(const_iterator __position, value_type&& __v);

      // Try to emplace at the end, otherwise forward to _M_insert_aux.
      template<typename... _Args>
	_GLIBCXX20_CONSTEXPR
	iterator
	_M_emplace_aux(const_iterator __position, _Args&&... __args);

      // Emplacing an rvalue of the correct type can use _M_insert_rval.
      _GLIBCXX20_CONSTEXPR
      iterator
      _M_emplace_aux(const_iterator __position, value_type&& __v)
      { return _M_insert_rval(__position, std::move(__v)); }
#endif

      // Called by _M_fill_insert, _M_insert_aux etc.
      _GLIBCXX20_CONSTEXPR
      size_type
      _M_check_len(size_type __n, const char* __s) const
      {
	if (max_size() - size() < __n)
	  __throw_length_error(__N(__s));

	const size_type __len = size() + (std::max)(size(), __n);
	return (__len < size() || __len > max_size()) ? max_size() : __len;
      }

      // Called by constructors to check initial size.
      static _GLIBCXX20_CONSTEXPR size_type
      _S_check_init_len(size_type __n, const allocator_type& __a)
      {
	if (__n > _S_max_size(_Tp_alloc_type(__a)))
	  __throw_length_error(
	      __N("cannot create std::vector larger than max_size()"));
	return __n;
      }

      static _GLIBCXX20_CONSTEXPR size_type
      _S_max_size(const _Tp_alloc_type& __a) _GLIBCXX_NOEXCEPT
      {
	// std::distance(begin(), end()) cannot be greater than PTRDIFF_MAX,
	// and realistically we can't store more than PTRDIFF_MAX/sizeof(T)
	// (even if std::allocator_traits::max_size says we can).
	const size_t __diffmax
	  = __gnu_cxx::__numeric_traits<ptrdiff_t>::__max / sizeof(_Tp);
	const size_t __allocmax = _Alloc_traits::max_size(__a);
	return (std::min)(__diffmax, __allocmax);
      }

      // Internal erase functions follow.

      // Called by erase(q1,q2), clear(), resize(), _M_fill_assign,
      // _M_assign_aux.
      _GLIBCXX20_CONSTEXPR
      void
      _M_erase_at_end(pointer __pos) _GLIBCXX_NOEXCEPT
      {
	if (size_type __n = this->_M_impl._M_finish - __pos)
	  {
	    std::_Destroy(__pos, this->_M_impl._M_finish,
			  _M_get_Tp_allocator());
	    this->_M_impl._M_finish = __pos;
	    _GLIBCXX_ASAN_ANNOTATE_SHRINK(__n);
	  }
      }

      _GLIBCXX20_CONSTEXPR
      iterator
      _M_erase(iterator __position);

      _GLIBCXX20_CONSTEXPR
      iterator
      _M_erase(iterator __first, iterator __last);

#if __cplusplus >= 201103L
    private:
      // Constant-time move assignment when source object's memory can be
      // moved, either because the source's allocator will move too
      // or because the allocators are equal.
      _GLIBCXX20_CONSTEXPR
      void
      _M_move_assign(vector&& __x, true_type) noexcept
      {
	vector __tmp(get_allocator());
	this->_M_impl._M_swap_data(__x._M_impl);
	__tmp._M_impl._M_swap_data(__x._M_impl);
	std::__alloc_on_move(_M_get_Tp_allocator(), __x._M_get_Tp_allocator());
      }

      // Do move assignment when it might not be possible to move source
      // object's memory, resulting in a linear-time operation.
      _GLIBCXX20_CONSTEXPR
      void
      _M_move_assign(vector&& __x, false_type)
      {
	if (__x._M_get_Tp_allocator() == this->_M_get_Tp_allocator())
	  _M_move_assign(std::move(__x), true_type());
	else
	  {
	    // The rvalue's allocator cannot be moved and is not equal,
	    // so we need to individually move each element.
	    this->_M_assign_aux(std::make_move_iterator(__x.begin()),
			        std::make_move_iterator(__x.end()),
				std::random_access_iterator_tag());
	    __x.clear();
	  }
      }
#endif

      template<typename _Up>
	_GLIBCXX20_CONSTEXPR
	_Up*
	_M_data_ptr(_Up* __ptr) const _GLIBCXX_NOEXCEPT
	{ return __ptr; }

#if __cplusplus >= 201103L
      template<typename _Ptr>
	_GLIBCXX20_CONSTEXPR
	typename std::pointer_traits<_Ptr>::element_type*
	_M_data_ptr(_Ptr __ptr) const
	{ return empty() ? nullptr : std::__to_address(__ptr); }
#else
      template<typename _Up>
	_Up*
	_M_data_ptr(_Up* __ptr) _GLIBCXX_NOEXCEPT
	{ return __ptr; }

      template<typename _Ptr>
	value_type*
	_M_data_ptr(_Ptr __ptr)
	{ return empty() ? (value_type*)0 : __ptr.operator->(); }

      template<typename _Ptr>
	const value_type*
	_M_data_ptr(_Ptr __ptr) const
	{ return empty() ? (const value_type*)0 : __ptr.operator->(); }
#endif
    };

#if __cpp_deduction_guides >= 201606
  template<typename _InputIterator, typename _ValT
	     = typename iterator_traits<_InputIterator>::value_type,
	   typename _Allocator = allocator<_ValT>,
	   typename = _RequireInputIter<_InputIterator>,
	   typename = _RequireAllocator<_Allocator>>
    vector(_InputIterator, _InputIterator, _Allocator = _Allocator())
      -> vector<_ValT, _Allocator>;
#endif

  /**
   *  @brief  Vector equality comparison.
   *  @param  __x  A %vector.
   *  @param  __y  A %vector of the same type as @a __x.
   *  @return  True iff the size and elements of the vectors are equal.
   *
   *  This is an equivalence relation.  It is linear in the size of the
   *  vectors.  Vectors are considered equivalent if their sizes are equal,
   *  and if corresponding elements compare equal.
  */
  template<typename _Tp, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    inline bool
    operator==(const vector<_Tp, _Alloc>& __x, const vector<_Tp, _Alloc>& __y)
    { return (__x.size() == __y.size()
	      && std::equal(__x.begin(), __x.end(), __y.begin())); }

#if __cpp_lib_three_way_comparison
  /**
   *  @brief  Vector ordering relation.
   *  @param  __x  A `vector`.
   *  @param  __y  A `vector` of the same type as `__x`.
   *  @return  A value indicating whether `__x` is less than, equal to,
   *           greater than, or incomparable with `__y`.
   *
   *  See `std::lexicographical_compare_three_way()` for how the determination
   *  is made. This operator is used to synthesize relational operators like
   *  `<` and `>=` etc.
  */
  template<typename _Tp, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    inline __detail::__synth3way_t<_Tp>
    operator<=>(const vector<_Tp, _Alloc>& __x, const vector<_Tp, _Alloc>& __y)
    {
      return std::lexicographical_compare_three_way(__x.begin(), __x.end(),
						    __y.begin(), __y.end(),
						    __detail::__synth3way);
    }
#else
  /**
   *  @brief  Vector ordering relation.
   *  @param  __x  A %vector.
   *  @param  __y  A %vector of the same type as @a __x.
   *  @return  True iff @a __x is lexicographically less than @a __y.
   *
   *  This is a total ordering relation.  It is linear in the size of the
   *  vectors.  The elements must be comparable with @c <.
   *
   *  See std::lexicographical_compare() for how the determination is made.
  */
  template<typename _Tp, typename _Alloc>
    inline bool
    operator<(const vector<_Tp, _Alloc>& __x, const vector<_Tp, _Alloc>& __y)
    { return std::lexicographical_compare(__x.begin(), __x.end(),
					  __y.begin(), __y.end()); }

  /// Based on operator==
  template<typename _Tp, typename _Alloc>
    inline bool
    operator!=(const vector<_Tp, _Alloc>& __x, const vector<_Tp, _Alloc>& __y)
    { return !(__x == __y); }

  /// Based on operator<
  template<typename _Tp, typename _Alloc>
    inline bool
    operator>(const vector<_Tp, _Alloc>& __x, const vector<_Tp, _Alloc>& __y)
    { return __y < __x; }

  /// Based on operator<
  template<typename _Tp, typename _Alloc>
    inline bool
    operator<=(const vector<_Tp, _Alloc>& __x, const vector<_Tp, _Alloc>& __y)
    { return !(__y < __x); }

  /// Based on operator<
  template<typename _Tp, typename _Alloc>
    inline bool
    operator>=(const vector<_Tp, _Alloc>& __x, const vector<_Tp, _Alloc>& __y)
    { return !(__x < __y); }
#endif // three-way comparison

  /// See std::vector::swap().
  template<typename _Tp, typename _Alloc>
    _GLIBCXX20_CONSTEXPR
    inline void
    swap(vector<_Tp, _Alloc>& __x, vector<_Tp, _Alloc>& __y)
    _GLIBCXX_NOEXCEPT_IF(noexcept(__x.swap(__y)))
    { __x.swap(__y); }

_GLIBCXX_END_NAMESPACE_CONTAINER

#if __cplusplus >= 201703L
  namespace __detail::__variant
  {
    template<typename> struct _Never_valueless_alt; // see <variant>

    // Provide the strong exception-safety guarantee when emplacing a
    // vector into a variant, but only if move assignment cannot throw.
    template<typename _Tp, typename _Alloc>
      struct _Never_valueless_alt<_GLIBCXX_STD_C::vector<_Tp, _Alloc>>
      : std::is_nothrow_move_assignable<_GLIBCXX_STD_C::vector<_Tp, _Alloc>>
      { };
  }  // namespace __detail::__variant
#endif // C++17

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std

#endif /* _STL_VECTOR_H */
