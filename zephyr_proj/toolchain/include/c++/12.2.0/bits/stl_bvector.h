// vector<bool> specialization -*- C++ -*-

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
 * Copyright (c) 1996-1999
 * Silicon Graphics Computer Systems, Inc.
 *
 * Permission to use, copy, modify, distribute and sell this software
 * and its documentation for any purpose is hereby granted without fee,
 * provided that the above copyright notice appear in all copies and
 * that both that copyright notice and this permission notice appear
 * in supporting documentation.  Silicon Graphics makes no
 * representations about the suitability of this software for any
 * purpose.  It is provided "as is" without express or implied warranty.
 */

/** @file bits/stl_bvector.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{vector}
 */

#ifndef _STL_BVECTOR_H
#define _STL_BVECTOR_H 1

#if __cplusplus >= 201103L
#include <initializer_list>
#include <bits/functional_hash.h>
#endif

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

  typedef unsigned long _Bit_type;
  enum { _S_word_bit = int(__CHAR_BIT__ * sizeof(_Bit_type)) };

  __attribute__((__nonnull__))
  _GLIBCXX20_CONSTEXPR
  void
  __fill_bvector_n(_Bit_type*, size_t, bool) _GLIBCXX_NOEXCEPT;

_GLIBCXX_BEGIN_NAMESPACE_CONTAINER

  struct _Bit_reference
  {
    _Bit_type * _M_p;
    _Bit_type _M_mask;

    _GLIBCXX20_CONSTEXPR
    _Bit_reference(_Bit_type * __x, _Bit_type __y)
    : _M_p(__x), _M_mask(__y) { }

    _GLIBCXX20_CONSTEXPR
    _Bit_reference() _GLIBCXX_NOEXCEPT : _M_p(0), _M_mask(0) { }

#if __cplusplus >= 201103L
    _Bit_reference(const _Bit_reference&) = default;
#endif

    _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
    operator bool() const _GLIBCXX_NOEXCEPT
    { return !!(*_M_p & _M_mask); }

    _GLIBCXX20_CONSTEXPR
    _Bit_reference&
    operator=(bool __x) _GLIBCXX_NOEXCEPT
    {
      if (__x)
	*_M_p |= _M_mask;
      else
	*_M_p &= ~_M_mask;
      return *this;
    }

    _GLIBCXX20_CONSTEXPR
    _Bit_reference&
    operator=(const _Bit_reference& __x) _GLIBCXX_NOEXCEPT
    { return *this = bool(__x); }

    _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
    bool
    operator==(const _Bit_reference& __x) const
    { return bool(*this) == bool(__x); }

    _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
    bool
    operator<(const _Bit_reference& __x) const
    { return !bool(*this) && bool(__x); }

    _GLIBCXX20_CONSTEXPR
    void
    flip() _GLIBCXX_NOEXCEPT
    { *_M_p ^= _M_mask; }

#if __cplusplus >= 201103L
    _GLIBCXX20_CONSTEXPR
    friend void
    swap(_Bit_reference __x, _Bit_reference __y) noexcept
    {
      bool __tmp = __x;
      __x = __y;
      __y = __tmp;
    }

    _GLIBCXX20_CONSTEXPR
    friend void
    swap(_Bit_reference __x, bool& __y) noexcept
    {
      bool __tmp = __x;
      __x = __y;
      __y = __tmp;
    }

    _GLIBCXX20_CONSTEXPR
    friend void
    swap(bool& __x, _Bit_reference __y) noexcept
    {
      bool __tmp = __x;
      __x = __y;
      __y = __tmp;
    }
#endif
  };

// Ignore warnings about std::iterator.
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"
  struct _Bit_iterator_base
  : public std::iterator<std::random_access_iterator_tag, bool>
  {
    _Bit_type * _M_p;
    unsigned int _M_offset;

    _GLIBCXX20_CONSTEXPR
    _Bit_iterator_base(_Bit_type * __x, unsigned int __y)
    : _M_p(__x), _M_offset(__y) { }

    _GLIBCXX20_CONSTEXPR
    void
    _M_bump_up()
    {
      if (_M_offset++ == int(_S_word_bit) - 1)
	{
	  _M_offset = 0;
	  ++_M_p;
	}
    }

    _GLIBCXX20_CONSTEXPR
    void
    _M_bump_down()
    {
      if (_M_offset-- == 0)
	{
	  _M_offset = int(_S_word_bit) - 1;
	  --_M_p;
	}
    }

    _GLIBCXX20_CONSTEXPR
    void
    _M_incr(ptrdiff_t __i)
    {
      difference_type __n = __i + _M_offset;
      _M_p += __n / int(_S_word_bit);
      __n = __n % int(_S_word_bit);
      if (__n < 0)
	{
	  __n += int(_S_word_bit);
	  --_M_p;
	}
      _M_offset = static_cast<unsigned int>(__n);
    }

    _GLIBCXX_NODISCARD
    friend _GLIBCXX20_CONSTEXPR bool
    operator==(const _Bit_iterator_base& __x, const _Bit_iterator_base& __y)
    { return __x._M_p == __y._M_p && __x._M_offset == __y._M_offset; }

#if __cpp_lib_three_way_comparison
    [[nodiscard]]
    friend constexpr strong_ordering
    operator<=>(const _Bit_iterator_base& __x, const _Bit_iterator_base& __y)
    noexcept
    {
      if (const auto __cmp = __x._M_p <=> __y._M_p; __cmp != 0)
	return __cmp;
      return __x._M_offset <=> __y._M_offset;
    }
#else
    _GLIBCXX_NODISCARD
    friend bool
    operator<(const _Bit_iterator_base& __x, const _Bit_iterator_base& __y)
    {
      return __x._M_p < __y._M_p
	    || (__x._M_p == __y._M_p && __x._M_offset < __y._M_offset);
    }

    _GLIBCXX_NODISCARD
    friend bool
    operator!=(const _Bit_iterator_base& __x, const _Bit_iterator_base& __y)
    { return !(__x == __y); }

    _GLIBCXX_NODISCARD
    friend bool
    operator>(const _Bit_iterator_base& __x, const _Bit_iterator_base& __y)
    { return __y < __x; }

    _GLIBCXX_NODISCARD
    friend bool
    operator<=(const _Bit_iterator_base& __x, const _Bit_iterator_base& __y)
    { return !(__y < __x); }

    _GLIBCXX_NODISCARD
    friend bool
    operator>=(const _Bit_iterator_base& __x, const _Bit_iterator_base& __y)
    { return !(__x < __y); }
#endif // three-way comparison

    friend _GLIBCXX20_CONSTEXPR ptrdiff_t
    operator-(const _Bit_iterator_base& __x, const _Bit_iterator_base& __y)
    {
      return (int(_S_word_bit) * (__x._M_p - __y._M_p)
	      + __x._M_offset - __y._M_offset);
    }
  };
#pragma GCC diagnostic pop

  struct _Bit_iterator : public _Bit_iterator_base
  {
    typedef _Bit_reference  reference;
#if __cplusplus > 201703L
    typedef void	    pointer;
#else
    typedef _Bit_reference* pointer;
#endif
    typedef _Bit_iterator   iterator;

    _GLIBCXX20_CONSTEXPR
    _Bit_iterator() : _Bit_iterator_base(0, 0) { }

    _GLIBCXX20_CONSTEXPR
    _Bit_iterator(_Bit_type * __x, unsigned int __y)
    : _Bit_iterator_base(__x, __y) { }

    _GLIBCXX20_CONSTEXPR
    iterator
    _M_const_cast() const
    { return *this; }

    _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
    reference
    operator*() const
    { return reference(_M_p, 1UL << _M_offset); }

    _GLIBCXX20_CONSTEXPR
    iterator&
    operator++()
    {
      _M_bump_up();
      return *this;
    }

    _GLIBCXX20_CONSTEXPR
    iterator
    operator++(int)
    {
      iterator __tmp = *this;
      _M_bump_up();
      return __tmp;
    }

    _GLIBCXX20_CONSTEXPR
    iterator&
    operator--()
    {
      _M_bump_down();
      return *this;
    }

    _GLIBCXX20_CONSTEXPR
    iterator
    operator--(int)
    {
      iterator __tmp = *this;
      _M_bump_down();
      return __tmp;
    }

    _GLIBCXX20_CONSTEXPR
    iterator&
    operator+=(difference_type __i)
    {
      _M_incr(__i);
      return *this;
    }

    _GLIBCXX20_CONSTEXPR
    iterator&
    operator-=(difference_type __i)
    {
      *this += -__i;
      return *this;
    }

    _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
    reference
    operator[](difference_type __i) const
    { return *(*this + __i); }

    _GLIBCXX_NODISCARD
    friend _GLIBCXX20_CONSTEXPR iterator
    operator+(const iterator& __x, difference_type __n)
    {
      iterator __tmp = __x;
      __tmp += __n;
      return __tmp;
    }

    _GLIBCXX_NODISCARD
    friend _GLIBCXX20_CONSTEXPR iterator
    operator+(difference_type __n, const iterator& __x)
    { return __x + __n; }

    _GLIBCXX_NODISCARD
    friend _GLIBCXX20_CONSTEXPR iterator
    operator-(const iterator& __x, difference_type __n)
    {
      iterator __tmp = __x;
      __tmp -= __n;
      return __tmp;
    }
  };

  struct _Bit_const_iterator : public _Bit_iterator_base
  {
    typedef bool                 reference;
    typedef bool                 const_reference;
#if __cplusplus > 201703L
    typedef void	    pointer;
#else
    typedef const bool*          pointer;
#endif
    typedef _Bit_const_iterator  const_iterator;

    _GLIBCXX20_CONSTEXPR
    _Bit_const_iterator() : _Bit_iterator_base(0, 0) { }

    _GLIBCXX20_CONSTEXPR
    _Bit_const_iterator(_Bit_type * __x, unsigned int __y)
    : _Bit_iterator_base(__x, __y) { }

    _GLIBCXX20_CONSTEXPR
    _Bit_const_iterator(const _Bit_iterator& __x)
    : _Bit_iterator_base(__x._M_p, __x._M_offset) { }

    _GLIBCXX20_CONSTEXPR
    _Bit_iterator
    _M_const_cast() const
    { return _Bit_iterator(_M_p, _M_offset); }

    _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
    const_reference
    operator*() const
    { return _Bit_reference(_M_p, 1UL << _M_offset); }

    _GLIBCXX20_CONSTEXPR
    const_iterator&
    operator++()
    {
      _M_bump_up();
      return *this;
    }

    _GLIBCXX20_CONSTEXPR
    const_iterator
    operator++(int)
    {
      const_iterator __tmp = *this;
      _M_bump_up();
      return __tmp;
    }

    _GLIBCXX20_CONSTEXPR
    const_iterator&
    operator--()
    {
      _M_bump_down();
      return *this;
    }

    _GLIBCXX20_CONSTEXPR
    const_iterator
    operator--(int)
    {
      const_iterator __tmp = *this;
      _M_bump_down();
      return __tmp;
    }

    _GLIBCXX20_CONSTEXPR
    const_iterator&
    operator+=(difference_type __i)
    {
      _M_incr(__i);
      return *this;
    }

    _GLIBCXX20_CONSTEXPR
    const_iterator&
    operator-=(difference_type __i)
    {
      *this += -__i;
      return *this;
    }

    _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
    const_reference
    operator[](difference_type __i) const
    { return *(*this + __i); }

    _GLIBCXX_NODISCARD
    friend _GLIBCXX20_CONSTEXPR const_iterator
    operator+(const const_iterator& __x, difference_type __n)
    {
      const_iterator __tmp = __x;
      __tmp += __n;
      return __tmp;
    }

    _GLIBCXX_NODISCARD
    friend _GLIBCXX20_CONSTEXPR const_iterator
    operator-(const const_iterator& __x, difference_type __n)
    {
      const_iterator __tmp = __x;
      __tmp -= __n;
      return __tmp;
    }

    _GLIBCXX_NODISCARD
    friend _GLIBCXX20_CONSTEXPR const_iterator
    operator+(difference_type __n, const const_iterator& __x)
    { return __x + __n; }
  };

  template<typename _Alloc>
    struct _Bvector_base
    {
      typedef typename __gnu_cxx::__alloc_traits<_Alloc>::template
        rebind<_Bit_type>::other _Bit_alloc_type;
      typedef typename __gnu_cxx::__alloc_traits<_Bit_alloc_type>
	_Bit_alloc_traits;
      typedef typename _Bit_alloc_traits::pointer _Bit_pointer;

      struct _Bvector_impl_data
      {
#if !_GLIBCXX_INLINE_VERSION
	_Bit_iterator	_M_start;
#else
	// We don't need the offset field for the start, it's always zero.
	struct {
	  _Bit_type* _M_p;
	  // Allow assignment from iterators (assume offset is zero):
	  _GLIBCXX20_CONSTEXPR
	  void operator=(_Bit_iterator __it) { _M_p = __it._M_p; }
	} _M_start;
#endif
	_Bit_iterator	_M_finish;
	_Bit_pointer	_M_end_of_storage;

	_GLIBCXX20_CONSTEXPR
	_Bvector_impl_data() _GLIBCXX_NOEXCEPT
	: _M_start(), _M_finish(), _M_end_of_storage()
	{ }

#if __cplusplus >= 201103L
	_Bvector_impl_data(const _Bvector_impl_data&) = default;

	_Bvector_impl_data&
	operator=(const _Bvector_impl_data&) = default;

	_GLIBCXX20_CONSTEXPR
	_Bvector_impl_data(_Bvector_impl_data&& __x) noexcept
	: _Bvector_impl_data(__x)
	{ __x._M_reset(); }

	_GLIBCXX20_CONSTEXPR
	void
	_M_move_data(_Bvector_impl_data&& __x) noexcept
	{
	  *this = __x;
	  __x._M_reset();
	}
#endif

	_GLIBCXX20_CONSTEXPR
	void
	_M_reset() _GLIBCXX_NOEXCEPT
	{ *this = _Bvector_impl_data(); }

	_GLIBCXX20_CONSTEXPR
	void
	_M_swap_data(_Bvector_impl_data& __x) _GLIBCXX_NOEXCEPT
	{
	  // Do not use std::swap(_M_start, __x._M_start), etc as it loses
	  // information used by TBAA.
	  std::swap(*this, __x);
	}
      };

      struct _Bvector_impl
	: public _Bit_alloc_type, public _Bvector_impl_data
      {
	_GLIBCXX20_CONSTEXPR
	_Bvector_impl() _GLIBCXX_NOEXCEPT_IF(
	  is_nothrow_default_constructible<_Bit_alloc_type>::value)
	: _Bit_alloc_type()
	{ }

	_GLIBCXX20_CONSTEXPR
	_Bvector_impl(const _Bit_alloc_type& __a) _GLIBCXX_NOEXCEPT
	: _Bit_alloc_type(__a)
	{ }

#if __cplusplus >= 201103L
	// Not defaulted, to enforce noexcept(true) even when
	// !is_nothrow_move_constructible<_Bit_alloc_type>.
	_GLIBCXX20_CONSTEXPR
	_Bvector_impl(_Bvector_impl&& __x) noexcept
	: _Bit_alloc_type(std::move(__x)), _Bvector_impl_data(std::move(__x))
	{ }

	_GLIBCXX20_CONSTEXPR
	_Bvector_impl(_Bit_alloc_type&& __a, _Bvector_impl&& __x) noexcept
	: _Bit_alloc_type(std::move(__a)), _Bvector_impl_data(std::move(__x))
	{ }
#endif

	_GLIBCXX20_CONSTEXPR
	_Bit_type*
	_M_end_addr() const _GLIBCXX_NOEXCEPT
	{
	  if (this->_M_end_of_storage)
	    return std::__addressof(this->_M_end_of_storage[-1]) + 1;
	  return 0;
	}
      };

    public:
      typedef _Alloc allocator_type;

      _GLIBCXX20_CONSTEXPR
      _Bit_alloc_type&
      _M_get_Bit_allocator() _GLIBCXX_NOEXCEPT
      { return this->_M_impl; }

      _GLIBCXX20_CONSTEXPR
      const _Bit_alloc_type&
      _M_get_Bit_allocator() const _GLIBCXX_NOEXCEPT
      { return this->_M_impl; }

      _GLIBCXX20_CONSTEXPR
      allocator_type
      get_allocator() const _GLIBCXX_NOEXCEPT
      { return allocator_type(_M_get_Bit_allocator()); }

#if __cplusplus >= 201103L
      _Bvector_base() = default;
#else
      _Bvector_base() { }
#endif

      _GLIBCXX20_CONSTEXPR
      _Bvector_base(const allocator_type& __a)
      : _M_impl(__a) { }

#if __cplusplus >= 201103L
      _Bvector_base(_Bvector_base&&) = default;

      _GLIBCXX20_CONSTEXPR
      _Bvector_base(_Bvector_base&& __x, const allocator_type& __a) noexcept
      : _M_impl(_Bit_alloc_type(__a), std::move(__x._M_impl))
      { }
#endif

      _GLIBCXX20_CONSTEXPR
      ~_Bvector_base()
      { this->_M_deallocate(); }

    protected:
      _Bvector_impl _M_impl;

      _GLIBCXX20_CONSTEXPR
      _Bit_pointer
      _M_allocate(size_t __n)
      {
	_Bit_pointer __p = _Bit_alloc_traits::allocate(_M_impl, _S_nword(__n));
#if __cpp_lib_is_constant_evaluated
	if (std::is_constant_evaluated())
	{
	  __n = _S_nword(__n);
	  for (size_t __i = 0; __i < __n; ++__i)
	    __p[__i] = 0ul;
	}
#endif
	return __p;
      }

      _GLIBCXX20_CONSTEXPR
      void
      _M_deallocate()
      {
	if (_M_impl._M_start._M_p)
	  {
	    const size_t __n = _M_impl._M_end_addr() - _M_impl._M_start._M_p;
	    _Bit_alloc_traits::deallocate(_M_impl,
					  _M_impl._M_end_of_storage - __n,
					  __n);
	    _M_impl._M_reset();
	  }
      }

#if __cplusplus >= 201103L
      _GLIBCXX20_CONSTEXPR
      void
      _M_move_data(_Bvector_base&& __x) noexcept
      { _M_impl._M_move_data(std::move(__x._M_impl)); }
#endif

      _GLIBCXX_CONSTEXPR
      static size_t
      _S_nword(size_t __n)
      { return (__n + int(_S_word_bit) - 1) / int(_S_word_bit); }
    };

  /**
   *  @brief  A specialization of vector for booleans which offers fixed time
   *  access to individual elements in any order.
   *
   *  @ingroup sequences
   *
   *  @tparam _Alloc  Allocator type.
   *
   *  Note that vector<bool> does not actually meet the requirements for being
   *  a container.  This is because the reference and pointer types are not
   *  really references and pointers to bool.  See DR96 for details.  @see
   *  vector for function documentation.
   *
   *  In some terminology a %vector can be described as a dynamic
   *  C-style array, it offers fast and efficient access to individual
   *  elements in any order and saves the user from worrying about
   *  memory and size allocation.  Subscripting ( @c [] ) access is
   *  also provided as with C-style arrays.
  */
  template<typename _Alloc>
    class vector<bool, _Alloc> : protected _Bvector_base<_Alloc>
    {
      typedef _Bvector_base<_Alloc>			_Base;
      typedef typename _Base::_Bit_pointer		_Bit_pointer;
      typedef typename _Base::_Bit_alloc_traits		_Bit_alloc_traits;

#if __cplusplus >= 201103L
      friend struct std::hash<vector>;
#endif

    public:
      typedef bool					value_type;
      typedef size_t					size_type;
      typedef ptrdiff_t					difference_type;
      typedef _Bit_reference				reference;
      typedef bool					const_reference;
      typedef _Bit_reference*				pointer;
      typedef const bool*				const_pointer;
      typedef _Bit_iterator				iterator;
      typedef _Bit_const_iterator			const_iterator;
      typedef std::reverse_iterator<const_iterator>	const_reverse_iterator;
      typedef std::reverse_iterator<iterator>		reverse_iterator;
      typedef _Alloc					allocator_type;

      _GLIBCXX20_CONSTEXPR
      allocator_type
      get_allocator() const
      { return _Base::get_allocator(); }

    protected:
      using _Base::_M_allocate;
      using _Base::_M_deallocate;
      using _Base::_S_nword;
      using _Base::_M_get_Bit_allocator;

    public:
#if __cplusplus >= 201103L
      vector() = default;
#else
      vector() { }
#endif

      _GLIBCXX20_CONSTEXPR
      explicit
      vector(const allocator_type& __a)
      : _Base(__a) { }

#if __cplusplus >= 201103L
      _GLIBCXX20_CONSTEXPR
      explicit
      vector(size_type __n, const allocator_type& __a = allocator_type())
      : vector(__n, false, __a)
      { }

      _GLIBCXX20_CONSTEXPR
      vector(size_type __n, const bool& __value,
	     const allocator_type& __a = allocator_type())
#else
      explicit
      vector(size_type __n, const bool& __value = bool(),
	     const allocator_type& __a = allocator_type())
#endif
      : _Base(__a)
      {
	_M_initialize(__n);
	_M_initialize_value(__value);
      }

      _GLIBCXX20_CONSTEXPR
      vector(const vector& __x)
      : _Base(_Bit_alloc_traits::_S_select_on_copy(__x._M_get_Bit_allocator()))
      {
	_M_initialize(__x.size());
	_M_copy_aligned(__x.begin(), __x.end(), begin());
      }

#if __cplusplus >= 201103L
      vector(vector&&) = default;

    private:
      _GLIBCXX20_CONSTEXPR
      vector(vector&& __x, const allocator_type& __a, true_type) noexcept
      : _Base(std::move(__x), __a)
      { }

      _GLIBCXX20_CONSTEXPR
      vector(vector&& __x, const allocator_type& __a, false_type)
      : _Base(__a)
      {
	if (__x.get_allocator() == __a)
	  this->_M_move_data(std::move(__x));
	else
	  {
	    _M_initialize(__x.size());
	    _M_copy_aligned(__x.begin(), __x.end(), begin());
	    __x.clear();
	  }
      }

    public:
      _GLIBCXX20_CONSTEXPR
      vector(vector&& __x, const __type_identity_t<allocator_type>& __a)
      noexcept(_Bit_alloc_traits::_S_always_equal())
      : vector(std::move(__x), __a,
	       typename _Bit_alloc_traits::is_always_equal{})
      { }

      _GLIBCXX20_CONSTEXPR
      vector(const vector& __x, const __type_identity_t<allocator_type>& __a)
      : _Base(__a)
      {
	_M_initialize(__x.size());
	_M_copy_aligned(__x.begin(), __x.end(), begin());
      }

      _GLIBCXX20_CONSTEXPR
      vector(initializer_list<bool> __l,
	     const allocator_type& __a = allocator_type())
      : _Base(__a)
      {
	_M_initialize_range(__l.begin(), __l.end(),
			    random_access_iterator_tag());
      }
#endif

#if __cplusplus >= 201103L
      template<typename _InputIterator,
	       typename = std::_RequireInputIter<_InputIterator>>
	_GLIBCXX20_CONSTEXPR
	vector(_InputIterator __first, _InputIterator __last,
	       const allocator_type& __a = allocator_type())
	: _Base(__a)
	{
	  _M_initialize_range(__first, __last,
			      std::__iterator_category(__first));
	}
#else
      template<typename _InputIterator>
	vector(_InputIterator __first, _InputIterator __last,
	       const allocator_type& __a = allocator_type())
	: _Base(__a)
	{
	  // Check whether it's an integral type. If so, it's not an iterator.
	  typedef typename std::__is_integer<_InputIterator>::__type _Integral;
	  _M_initialize_dispatch(__first, __last, _Integral());
	}
#endif

      _GLIBCXX20_CONSTEXPR
      ~vector() _GLIBCXX_NOEXCEPT { }

      _GLIBCXX20_CONSTEXPR
      vector&
      operator=(const vector& __x)
      {
	if (&__x == this)
	  return *this;
#if __cplusplus >= 201103L
	if (_Bit_alloc_traits::_S_propagate_on_copy_assign())
	  {
	    if (this->_M_get_Bit_allocator() != __x._M_get_Bit_allocator())
	      {
		this->_M_deallocate();
		std::__alloc_on_copy(_M_get_Bit_allocator(),
				     __x._M_get_Bit_allocator());
		_M_initialize(__x.size());
	      }
	    else
	      std::__alloc_on_copy(_M_get_Bit_allocator(),
				   __x._M_get_Bit_allocator());
	  }
#endif
	if (__x.size() > capacity())
	  {
	    this->_M_deallocate();
	    _M_initialize(__x.size());
	  }
	this->_M_impl._M_finish = _M_copy_aligned(__x.begin(), __x.end(),
						  begin());
	return *this;
      }

#if __cplusplus >= 201103L
      _GLIBCXX20_CONSTEXPR
      vector&
      operator=(vector&& __x) noexcept(_Bit_alloc_traits::_S_nothrow_move())
      {
	if (_Bit_alloc_traits::_S_propagate_on_move_assign()
	    || this->_M_get_Bit_allocator() == __x._M_get_Bit_allocator())
	  {
	    this->_M_deallocate();
	    this->_M_move_data(std::move(__x));
	    std::__alloc_on_move(_M_get_Bit_allocator(),
				 __x._M_get_Bit_allocator());
	  }
	else
	  {
	    if (__x.size() > capacity())
	      {
		this->_M_deallocate();
		_M_initialize(__x.size());
	      }
	    this->_M_impl._M_finish = _M_copy_aligned(__x.begin(), __x.end(),
						      begin());
	    __x.clear();
	  }
	return *this;
      }

      _GLIBCXX20_CONSTEXPR
      vector&
      operator=(initializer_list<bool> __l)
      {
	this->assign(__l.begin(), __l.end());
	return *this;
      }
#endif

      // assign(), a generalized assignment member function.  Two
      // versions: one that takes a count, and one that takes a range.
      // The range version is a member template, so we dispatch on whether
      // or not the type is an integer.
      _GLIBCXX20_CONSTEXPR
      void
      assign(size_type __n, const bool& __x)
      { _M_fill_assign(__n, __x); }

#if __cplusplus >= 201103L
      template<typename _InputIterator,
	       typename = std::_RequireInputIter<_InputIterator>>
	_GLIBCXX20_CONSTEXPR
	void
	assign(_InputIterator __first, _InputIterator __last)
	{ _M_assign_aux(__first, __last, std::__iterator_category(__first)); }
#else
      template<typename _InputIterator>
	void
	assign(_InputIterator __first, _InputIterator __last)
	{
	  // Check whether it's an integral type. If so, it's not an iterator.
	  typedef typename std::__is_integer<_InputIterator>::__type _Integral;
	  _M_assign_dispatch(__first, __last, _Integral());
	}
#endif

#if __cplusplus >= 201103L
      _GLIBCXX20_CONSTEXPR
      void
      assign(initializer_list<bool> __l)
      { _M_assign_aux(__l.begin(), __l.end(), random_access_iterator_tag()); }
#endif

      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      iterator
      begin() _GLIBCXX_NOEXCEPT
      { return iterator(this->_M_impl._M_start._M_p, 0); }

      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      const_iterator
      begin() const _GLIBCXX_NOEXCEPT
      { return const_iterator(this->_M_impl._M_start._M_p, 0); }

      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      iterator
      end() _GLIBCXX_NOEXCEPT
      { return this->_M_impl._M_finish; }

      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      const_iterator
      end() const _GLIBCXX_NOEXCEPT
      { return this->_M_impl._M_finish; }

      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      reverse_iterator
      rbegin() _GLIBCXX_NOEXCEPT
      { return reverse_iterator(end()); }

      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      const_reverse_iterator
      rbegin() const _GLIBCXX_NOEXCEPT
      { return const_reverse_iterator(end()); }

      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      reverse_iterator
      rend() _GLIBCXX_NOEXCEPT
      { return reverse_iterator(begin()); }

      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      const_reverse_iterator
      rend() const _GLIBCXX_NOEXCEPT
      { return const_reverse_iterator(begin()); }

#if __cplusplus >= 201103L
      [[__nodiscard__]] _GLIBCXX20_CONSTEXPR
      const_iterator
      cbegin() const noexcept
      { return const_iterator(this->_M_impl._M_start._M_p, 0); }

      [[__nodiscard__]] _GLIBCXX20_CONSTEXPR
      const_iterator
      cend() const noexcept
      { return this->_M_impl._M_finish; }

      [[__nodiscard__]] _GLIBCXX20_CONSTEXPR
      const_reverse_iterator
      crbegin() const noexcept
      { return const_reverse_iterator(end()); }

      [[__nodiscard__]] _GLIBCXX20_CONSTEXPR
      const_reverse_iterator
      crend() const noexcept
      { return const_reverse_iterator(begin()); }
#endif

      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      size_type
      size() const _GLIBCXX_NOEXCEPT
      { return size_type(end() - begin()); }

      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      size_type
      max_size() const _GLIBCXX_NOEXCEPT
      {
	const size_type __isize =
	  __gnu_cxx::__numeric_traits<difference_type>::__max
	  - int(_S_word_bit) + 1;
	const size_type __asize
	  = _Bit_alloc_traits::max_size(_M_get_Bit_allocator());
	return (__asize <= __isize / int(_S_word_bit)
		? __asize * int(_S_word_bit) : __isize);
      }

      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      size_type
      capacity() const _GLIBCXX_NOEXCEPT
      { return size_type(const_iterator(this->_M_impl._M_end_addr(), 0)
			 - begin()); }

      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      bool
      empty() const _GLIBCXX_NOEXCEPT
      { return begin() == end(); }

      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      reference
      operator[](size_type __n)
      { return begin()[__n]; }

      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      const_reference
      operator[](size_type __n) const
      { return begin()[__n]; }

    protected:
      _GLIBCXX20_CONSTEXPR
      void
      _M_range_check(size_type __n) const
      {
	if (__n >= this->size())
	  __throw_out_of_range_fmt(__N("vector<bool>::_M_range_check: __n "
				       "(which is %zu) >= this->size() "
				       "(which is %zu)"),
				   __n, this->size());
      }

    public:
      _GLIBCXX20_CONSTEXPR
      reference
      at(size_type __n)
      {
	_M_range_check(__n);
	return (*this)[__n];
      }

      _GLIBCXX20_CONSTEXPR
      const_reference
      at(size_type __n) const
      {
	_M_range_check(__n);
	return (*this)[__n];
      }

      _GLIBCXX20_CONSTEXPR
      void
      reserve(size_type __n)
      {
	if (__n > max_size())
	  __throw_length_error(__N("vector::reserve"));
	if (capacity() < __n)
	  _M_reallocate(__n);
      }

      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      reference
      front()
      { return *begin(); }

      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      const_reference
      front() const
      { return *begin(); }

      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      reference
      back()
      { return *(end() - 1); }

      _GLIBCXX_NODISCARD _GLIBCXX20_CONSTEXPR
      const_reference
      back() const
      { return *(end() - 1); }

      _GLIBCXX20_CONSTEXPR
      void
      push_back(bool __x)
      {
	if (this->_M_impl._M_finish._M_p != this->_M_impl._M_end_addr())
	  *this->_M_impl._M_finish++ = __x;
	else
	  _M_insert_aux(end(), __x);
      }

      _GLIBCXX20_CONSTEXPR
      void
      swap(vector& __x) _GLIBCXX_NOEXCEPT
      {
#if __cplusplus >= 201103L
	__glibcxx_assert(_Bit_alloc_traits::propagate_on_container_swap::value
			 || _M_get_Bit_allocator() == __x._M_get_Bit_allocator());
#endif
	this->_M_impl._M_swap_data(__x._M_impl);
	_Bit_alloc_traits::_S_on_swap(_M_get_Bit_allocator(),
				      __x._M_get_Bit_allocator());
      }

      // [23.2.5]/1, third-to-last entry in synopsis listing
      _GLIBCXX20_CONSTEXPR
      static void
      swap(reference __x, reference __y) _GLIBCXX_NOEXCEPT
      {
	bool __tmp = __x;
	__x = __y;
	__y = __tmp;
      }

      _GLIBCXX20_CONSTEXPR
      iterator
#if __cplusplus >= 201103L
      insert(const_iterator __position, const bool& __x)
#else
      insert(iterator __position, const bool& __x)
#endif
      {
	const difference_type __n = __position - begin();
	if (this->_M_impl._M_finish._M_p != this->_M_impl._M_end_addr()
	    && __position == end())
	  *this->_M_impl._M_finish++ = __x;
	else
	  _M_insert_aux(__position._M_const_cast(), __x);
	return begin() + __n;
      }

#if _GLIBCXX_USE_DEPRECATED
      _GLIBCXX_DEPRECATED_SUGGEST("insert(position, false)")
      iterator
      insert(const_iterator __position)
      { return this->insert(__position._M_const_cast(), false); }
#endif

#if __cplusplus >= 201103L
      template<typename _InputIterator,
	       typename = std::_RequireInputIter<_InputIterator>>
	_GLIBCXX20_CONSTEXPR
	iterator
	insert(const_iterator __position,
	       _InputIterator __first, _InputIterator __last)
	{
	  difference_type __offset = __position - cbegin();
	  _M_insert_range(__position._M_const_cast(),
			  __first, __last,
			  std::__iterator_category(__first));
	  return begin() + __offset;
	}
#else
      template<typename _InputIterator>
	void
	insert(iterator __position,
	       _InputIterator __first, _InputIterator __last)
	{
	  // Check whether it's an integral type. If so, it's not an iterator.
	  typedef typename std::__is_integer<_InputIterator>::__type _Integral;
	  _M_insert_dispatch(__position, __first, __last, _Integral());
	}
#endif

#if __cplusplus >= 201103L
      _GLIBCXX20_CONSTEXPR
      iterator
      insert(const_iterator __position, size_type __n, const bool& __x)
      {
	difference_type __offset = __position - cbegin();
	_M_fill_insert(__position._M_const_cast(), __n, __x);
	return begin() + __offset;
      }
#else
      void
      insert(iterator __position, size_type __n, const bool& __x)
      { _M_fill_insert(__position, __n, __x); }
#endif

#if __cplusplus >= 201103L
      _GLIBCXX20_CONSTEXPR
      iterator
      insert(const_iterator __p, initializer_list<bool> __l)
      { return this->insert(__p, __l.begin(), __l.end()); }
#endif

      _GLIBCXX20_CONSTEXPR
      void
      pop_back()
      { --this->_M_impl._M_finish; }

      _GLIBCXX20_CONSTEXPR
      iterator
#if __cplusplus >= 201103L
      erase(const_iterator __position)
#else
      erase(iterator __position)
#endif
      { return _M_erase(__position._M_const_cast()); }

      _GLIBCXX20_CONSTEXPR
      iterator
#if __cplusplus >= 201103L
      erase(const_iterator __first, const_iterator __last)
#else
      erase(iterator __first, iterator __last)
#endif
      { return _M_erase(__first._M_const_cast(), __last._M_const_cast()); }

      _GLIBCXX20_CONSTEXPR
      void
      resize(size_type __new_size, bool __x = bool())
      {
	if (__new_size < size())
	  _M_erase_at_end(begin() + difference_type(__new_size));
	else
	  insert(end(), __new_size - size(), __x);
      }

#if __cplusplus >= 201103L
      _GLIBCXX20_CONSTEXPR
      void
      shrink_to_fit()
      { _M_shrink_to_fit(); }
#endif

      _GLIBCXX20_CONSTEXPR
      void
      flip() _GLIBCXX_NOEXCEPT
      {
	_Bit_type * const __end = this->_M_impl._M_end_addr();
	for (_Bit_type * __p = this->_M_impl._M_start._M_p; __p != __end; ++__p)
	  *__p = ~*__p;
      }

      _GLIBCXX20_CONSTEXPR
      void
      clear() _GLIBCXX_NOEXCEPT
      { _M_erase_at_end(begin()); }

#if __cplusplus >= 201103L
      template<typename... _Args>
#if __cplusplus > 201402L
	_GLIBCXX20_CONSTEXPR
	reference
#else
	void
#endif
	emplace_back(_Args&&... __args)
	{
	  push_back(bool(__args...));
#if __cplusplus > 201402L
	  return back();
#endif
	}

      template<typename... _Args>
	_GLIBCXX20_CONSTEXPR
	iterator
	emplace(const_iterator __pos, _Args&&... __args)
	{ return insert(__pos, bool(__args...)); }
#endif

    protected:
      // Precondition: __first._M_offset == 0 && __result._M_offset == 0.
      _GLIBCXX20_CONSTEXPR
      iterator
      _M_copy_aligned(const_iterator __first, const_iterator __last,
		      iterator __result)
      {
	_Bit_type* __q = std::copy(__first._M_p, __last._M_p, __result._M_p);
	return std::copy(const_iterator(__last._M_p, 0), __last,
			 iterator(__q, 0));
      }

      _GLIBCXX20_CONSTEXPR
      void
      _M_initialize(size_type __n)
      {
	if (__n)
	  {
	    _Bit_pointer __q = this->_M_allocate(__n);
	    this->_M_impl._M_end_of_storage = __q + _S_nword(__n);
	    iterator __start = iterator(std::__addressof(*__q), 0);
	    this->_M_impl._M_start = __start;
	    this->_M_impl._M_finish = __start + difference_type(__n);
	  }
      }

      _GLIBCXX20_CONSTEXPR
      void
      _M_initialize_value(bool __x) _GLIBCXX_NOEXCEPT
      {
	if (_Bit_type* __p = this->_M_impl._M_start._M_p)
	  __fill_bvector_n(__p, this->_M_impl._M_end_addr() - __p, __x);
      }

      _GLIBCXX20_CONSTEXPR
      void
      _M_reallocate(size_type __n);

#if __cplusplus >= 201103L
      _GLIBCXX20_CONSTEXPR
      bool
      _M_shrink_to_fit();
#endif

#if __cplusplus < 201103L
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 438. Ambiguity in the "do the right thing" clause
      template<typename _Integer>
	void
	_M_initialize_dispatch(_Integer __n, _Integer __x, __true_type)
	{
	  _M_initialize(static_cast<size_type>(__n));
	  _M_initialize_value(__x);
	}

      template<typename _InputIterator>
	void
	_M_initialize_dispatch(_InputIterator __first, _InputIterator __last,
			       __false_type)
	{ _M_initialize_range(__first, __last,
			      std::__iterator_category(__first)); }
#endif

      template<typename _InputIterator>
	_GLIBCXX20_CONSTEXPR
	void
	_M_initialize_range(_InputIterator __first, _InputIterator __last,
			    std::input_iterator_tag)
	{
	  for (; __first != __last; ++__first)
	    push_back(*__first);
	}

      template<typename _ForwardIterator>
	_GLIBCXX20_CONSTEXPR
	void
	_M_initialize_range(_ForwardIterator __first, _ForwardIterator __last,
			    std::forward_iterator_tag)
	{
	  const size_type __n = std::distance(__first, __last);
	  _M_initialize(__n);
	  std::copy(__first, __last, begin());
	}

#if __cplusplus < 201103L
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 438. Ambiguity in the "do the right thing" clause
      template<typename _Integer>
	void
	_M_assign_dispatch(_Integer __n, _Integer __val, __true_type)
	{ _M_fill_assign(__n, __val); }

      template<class _InputIterator>
	void
	_M_assign_dispatch(_InputIterator __first, _InputIterator __last,
			   __false_type)
	{ _M_assign_aux(__first, __last, std::__iterator_category(__first)); }
#endif

      _GLIBCXX20_CONSTEXPR
      void
      _M_fill_assign(size_t __n, bool __x)
      {
	if (__n > size())
	  {
	    _M_initialize_value(__x);
	    insert(end(), __n - size(), __x);
	  }
	else
	  {
	    _M_erase_at_end(begin() + __n);
	    _M_initialize_value(__x);
	  }
      }

      template<typename _InputIterator>
	_GLIBCXX20_CONSTEXPR
	void
	_M_assign_aux(_InputIterator __first, _InputIterator __last,
		      std::input_iterator_tag)
	{
	  iterator __cur = begin();
	  for (; __first != __last && __cur != end(); ++__cur, (void)++__first)
	    *__cur = *__first;
	  if (__first == __last)
	    _M_erase_at_end(__cur);
	  else
	    insert(end(), __first, __last);
	}

      template<typename _ForwardIterator>
	_GLIBCXX20_CONSTEXPR
	void
	_M_assign_aux(_ForwardIterator __first, _ForwardIterator __last,
		      std::forward_iterator_tag)
	{
	  const size_type __len = std::distance(__first, __last);
	  if (__len < size())
	    _M_erase_at_end(std::copy(__first, __last, begin()));
	  else
	    {
	      _ForwardIterator __mid = __first;
	      std::advance(__mid, size());
	      std::copy(__first, __mid, begin());
	      insert(end(), __mid, __last);
	    }
	}

#if __cplusplus < 201103L
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 438. Ambiguity in the "do the right thing" clause
      template<typename _Integer>
	void
	_M_insert_dispatch(iterator __pos, _Integer __n, _Integer __x,
			   __true_type)
	{ _M_fill_insert(__pos, __n, __x); }

      template<typename _InputIterator>
	void
	_M_insert_dispatch(iterator __pos,
			   _InputIterator __first, _InputIterator __last,
			   __false_type)
	{ _M_insert_range(__pos, __first, __last,
			  std::__iterator_category(__first)); }
#endif

      _GLIBCXX20_CONSTEXPR
      void
      _M_fill_insert(iterator __position, size_type __n, bool __x);

      template<typename _InputIterator>
	_GLIBCXX20_CONSTEXPR
	void
	_M_insert_range(iterator __pos, _InputIterator __first,
			_InputIterator __last, std::input_iterator_tag)
	{
	  for (; __first != __last; ++__first)
	    {
	      __pos = insert(__pos, *__first);
	      ++__pos;
	    }
	}

      template<typename _ForwardIterator>
	_GLIBCXX20_CONSTEXPR
	void
	_M_insert_range(iterator __position, _ForwardIterator __first,
			_ForwardIterator __last, std::forward_iterator_tag);

      _GLIBCXX20_CONSTEXPR
      void
      _M_insert_aux(iterator __position, bool __x);

      _GLIBCXX20_CONSTEXPR
      size_type
      _M_check_len(size_type __n, const char* __s) const
      {
	if (max_size() - size() < __n)
	  __throw_length_error(__N(__s));

	const size_type __len = size() + std::max(size(), __n);
	return (__len < size() || __len > max_size()) ? max_size() : __len;
      }

      _GLIBCXX20_CONSTEXPR
      void
      _M_erase_at_end(iterator __pos)
      { this->_M_impl._M_finish = __pos; }

      _GLIBCXX20_CONSTEXPR
      iterator
      _M_erase(iterator __pos);

      _GLIBCXX20_CONSTEXPR
      iterator
      _M_erase(iterator __first, iterator __last);

    protected:
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // DR 464. Suggestion for new member functions in standard containers.
      // N.B. DR 464 says nothing about vector<bool> but we need something
      // here due to the using-declaration in __gnu_debug::vector.
      // vector class.
#if __cplusplus >= 201103L
      void data() = delete;
#else
      void data() { }
#endif
    };

_GLIBCXX_END_NAMESPACE_CONTAINER

  // Fill a partial word.
  _GLIBCXX20_CONSTEXPR
  inline void
  __fill_bvector(_Bit_type* __v, unsigned int __first, unsigned int __last,
		 bool __x) _GLIBCXX_NOEXCEPT
  {
    const _Bit_type __fmask = ~0ul << __first;
    const _Bit_type __lmask = ~0ul >> (_S_word_bit - __last);
    const _Bit_type __mask = __fmask & __lmask;

    if (__x)
      *__v |= __mask;
    else
      *__v &= ~__mask;
  }

  // Fill N full words, as if using memset, but usable in constant expressions.
  __attribute__((__nonnull__))
  _GLIBCXX20_CONSTEXPR
  inline void
  __fill_bvector_n(_Bit_type* __p, size_t __n, bool __x) _GLIBCXX_NOEXCEPT
  {
#if __cpp_lib_is_constant_evaluated
    if (std::is_constant_evaluated())
    {
      for (size_t __i = 0; __i < __n; ++__i)
	__p[__i] = __x ? ~0ul : 0ul;
      return;
    }
#endif
    __builtin_memset(__p, __x ? ~0 : 0, __n * sizeof(_Bit_type));
  }


  _GLIBCXX20_CONSTEXPR
  inline void
  __fill_a1(_GLIBCXX_STD_C::_Bit_iterator __first,
	    _GLIBCXX_STD_C::_Bit_iterator __last, const bool& __x)
  {
    if (__first._M_p != __last._M_p)
      {
	_Bit_type* __first_p = __first._M_p;
	if (__first._M_offset != 0)
	  __fill_bvector(__first_p++, __first._M_offset, _S_word_bit, __x);

	__fill_bvector_n(__first_p, __last._M_p - __first_p, __x);

	if (__last._M_offset != 0)
	  __fill_bvector(__last._M_p, 0, __last._M_offset, __x);
      }
    else if (__first._M_offset != __last._M_offset)
      __fill_bvector(__first._M_p, __first._M_offset, __last._M_offset, __x);
  }

#if __cplusplus >= 201103L
  // DR 1182.
  /// std::hash specialization for vector<bool>.
  template<typename _Alloc>
    struct hash<_GLIBCXX_STD_C::vector<bool, _Alloc>>
    : public __hash_base<size_t, _GLIBCXX_STD_C::vector<bool, _Alloc>>
    {
      size_t
      operator()(const _GLIBCXX_STD_C::vector<bool, _Alloc>&) const noexcept;
    };
#endif // C++11

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std

#endif
