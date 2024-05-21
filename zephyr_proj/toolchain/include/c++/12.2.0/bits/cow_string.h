// Definition of gcc4-compatible Copy-on-Write basic_string -*- C++ -*-

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

/** @file bits/cow_string.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{string}
 *
 *  Defines the reference-counted COW string implentation.
 */

#ifndef _COW_STRING_H
#define _COW_STRING_H 1

#if ! _GLIBCXX_USE_CXX11_ABI

#include <ext/atomicity.h> // _Atomic_word, __is_single_threaded

#ifdef __cpp_lib_is_constant_evaluated
// Support P1032R1 in C++20 (but not P0980R1 for COW strings).
# define __cpp_lib_constexpr_string 201811L
#elif __cplusplus >= 201703L && _GLIBCXX_HAVE_IS_CONSTANT_EVALUATED
// Support P0426R1 changes to char_traits in C++17.
# define __cpp_lib_constexpr_string 201611L
#endif

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

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
   *
   *  @doctodo
   *
   *
   *  Documentation?  What's that?
   *  Nathan Myers <ncm@cantrip.org>.
   *
   *  A string looks like this:
   *
   *  @code
   *                                        [_Rep]
   *                                        _M_length
   *   [basic_string<char_type>]            _M_capacity
   *   _M_dataplus                          _M_refcount
   *   _M_p ---------------->               unnamed array of char_type
   *  @endcode
   *
   *  Where the _M_p points to the first character in the string, and
   *  you cast it to a pointer-to-_Rep and subtract 1 to get a
   *  pointer to the header.
   *
   *  This approach has the enormous advantage that a string object
   *  requires only one allocation.  All the ugliness is confined
   *  within a single %pair of inline functions, which each compile to
   *  a single @a add instruction: _Rep::_M_data(), and
   *  string::_M_rep(); and the allocation function which gets a
   *  block of raw bytes and with room enough and constructs a _Rep
   *  object at the front.
   *
   *  The reason you want _M_data pointing to the character %array and
   *  not the _Rep is so that the debugger can see the string
   *  contents. (Probably we should add a non-inline member to get
   *  the _Rep for the debugger to use, so users can check the actual
   *  string length.)
   *
   *  Note that the _Rep object is a POD so that you can have a
   *  static <em>empty string</em> _Rep object already @a constructed before
   *  static constructors have run.  The reference-count encoding is
   *  chosen so that a 0 indicates one reference, so you never try to
   *  destroy the empty-string _Rep object.
   *
   *  All but the last paragraph is considered pretty conventional
   *  for a Copy-On-Write C++ string implementation.
  */
  // 21.3  Template class basic_string
  template<typename _CharT, typename _Traits, typename _Alloc>
    class basic_string
    {
      typedef typename __gnu_cxx::__alloc_traits<_Alloc>::template
	rebind<_CharT>::other _CharT_alloc_type;
      typedef __gnu_cxx::__alloc_traits<_CharT_alloc_type> _CharT_alloc_traits;

      // Types:
    public:
      typedef _Traits					    traits_type;
      typedef typename _Traits::char_type		    value_type;
      typedef _Alloc					    allocator_type;
      typedef typename _CharT_alloc_traits::size_type	    size_type;
      typedef typename _CharT_alloc_traits::difference_type difference_type;
#if __cplusplus < 201103L
      typedef typename _CharT_alloc_type::reference	    reference;
      typedef typename _CharT_alloc_type::const_reference   const_reference;
#else
      typedef value_type&				    reference;
      typedef const value_type&				    const_reference;
#endif
      typedef typename _CharT_alloc_traits::pointer	    pointer;
      typedef typename _CharT_alloc_traits::const_pointer   const_pointer;
      typedef __gnu_cxx::__normal_iterator<pointer, basic_string>  iterator;
      typedef __gnu_cxx::__normal_iterator<const_pointer, basic_string>
							    const_iterator;
      typedef std::reverse_iterator<const_iterator>	const_reverse_iterator;
      typedef std::reverse_iterator<iterator>		    reverse_iterator;

    protected:
      // type used for positions in insert, erase etc.
      typedef iterator __const_iterator;

    private:
      // _Rep: string representation
      //   Invariants:
      //   1. String really contains _M_length + 1 characters: due to 21.3.4
      //      must be kept null-terminated.
      //   2. _M_capacity >= _M_length
      //      Allocated memory is always (_M_capacity + 1) * sizeof(_CharT).
      //   3. _M_refcount has three states:
      //      -1: leaked, one reference, no ref-copies allowed, non-const.
      //       0: one reference, non-const.
      //     n>0: n + 1 references, operations require a lock, const.
      //   4. All fields==0 is an empty string, given the extra storage
      //      beyond-the-end for a null terminator; thus, the shared
      //      empty string representation needs no constructor.

      struct _Rep_base
      {
	size_type		_M_length;
	size_type		_M_capacity;
	_Atomic_word		_M_refcount;
      };

      struct _Rep : _Rep_base
      {
	// Types:
	typedef typename __gnu_cxx::__alloc_traits<_Alloc>::template
	  rebind<char>::other _Raw_bytes_alloc;

	// (Public) Data members:

	// The maximum number of individual char_type elements of an
	// individual string is determined by _S_max_size. This is the
	// value that will be returned by max_size().  (Whereas npos
	// is the maximum number of bytes the allocator can allocate.)
	// If one was to divvy up the theoretical largest size string,
	// with a terminating character and m _CharT elements, it'd
	// look like this:
	// npos = sizeof(_Rep) + (m * sizeof(_CharT)) + sizeof(_CharT)
	// Solving for m:
	// m = ((npos - sizeof(_Rep))/sizeof(CharT)) - 1
	// In addition, this implementation quarters this amount.
	static const size_type	_S_max_size;
	static const _CharT	_S_terminal;

	// The following storage is init'd to 0 by the linker, resulting
	// (carefully) in an empty string with one reference.
	static size_type _S_empty_rep_storage[];

	static _Rep&
	_S_empty_rep() _GLIBCXX_NOEXCEPT
	{
	  // NB: Mild hack to avoid strict-aliasing warnings.  Note that
	  // _S_empty_rep_storage is never modified and the punning should
	  // be reasonably safe in this case.
	  void* __p = reinterpret_cast<void*>(&_S_empty_rep_storage);
	  return *reinterpret_cast<_Rep*>(__p);
	}

	bool
	_M_is_leaked() const _GLIBCXX_NOEXCEPT
	{
#if defined(__GTHREADS)
	  // _M_refcount is mutated concurrently by _M_refcopy/_M_dispose,
	  // so we need to use an atomic load. However, _M_is_leaked
	  // predicate does not change concurrently (i.e. the string is either
	  // leaked or not), so a relaxed load is enough.
	  return __atomic_load_n(&this->_M_refcount, __ATOMIC_RELAXED) < 0;
#else
	  return this->_M_refcount < 0;
#endif
	}

	bool
	_M_is_shared() const _GLIBCXX_NOEXCEPT
	{
#if defined(__GTHREADS)
	  // _M_refcount is mutated concurrently by _M_refcopy/_M_dispose,
	  // so we need to use an atomic load. Another thread can drop last
	  // but one reference concurrently with this check, so we need this
	  // load to be acquire to synchronize with release fetch_and_add in
	  // _M_dispose.
	  if (!__gnu_cxx::__is_single_threaded())
	    return __atomic_load_n(&this->_M_refcount, __ATOMIC_ACQUIRE) > 0;
#endif
	  return this->_M_refcount > 0;
	}

	void
	_M_set_leaked() _GLIBCXX_NOEXCEPT
	{ this->_M_refcount = -1; }

	void
	_M_set_sharable() _GLIBCXX_NOEXCEPT
	{ this->_M_refcount = 0; }

	void
	_M_set_length_and_sharable(size_type __n) _GLIBCXX_NOEXCEPT
	{
#if _GLIBCXX_FULLY_DYNAMIC_STRING == 0
	  if (__builtin_expect(this != &_S_empty_rep(), false))
#endif
	    {
	      this->_M_set_sharable();  // One reference.
	      this->_M_length = __n;
	      traits_type::assign(this->_M_refdata()[__n], _S_terminal);
	      // grrr. (per 21.3.4)
	      // You cannot leave those LWG people alone for a second.
	    }
	}

	_CharT*
	_M_refdata() throw()
	{ return reinterpret_cast<_CharT*>(this + 1); }

	_CharT*
	_M_grab(const _Alloc& __alloc1, const _Alloc& __alloc2)
	{
	  return (!_M_is_leaked() && __alloc1 == __alloc2)
		  ? _M_refcopy() : _M_clone(__alloc1);
	}

	// Create & Destroy
	static _Rep*
	_S_create(size_type, size_type, const _Alloc&);

	void
	_M_dispose(const _Alloc& __a) _GLIBCXX_NOEXCEPT
	{
#if _GLIBCXX_FULLY_DYNAMIC_STRING == 0
	  if (__builtin_expect(this != &_S_empty_rep(), false))
#endif
	    {
	      // Be race-detector-friendly.  For more info see bits/c++config.
	      _GLIBCXX_SYNCHRONIZATION_HAPPENS_BEFORE(&this->_M_refcount);
	      // Decrement of _M_refcount is acq_rel, because:
	      // - all but last decrements need to release to synchronize with
	      //   the last decrement that will delete the object.
	      // - the last decrement needs to acquire to synchronize with
	      //   all the previous decrements.
	      // - last but one decrement needs to release to synchronize with
	      //   the acquire load in _M_is_shared that will conclude that
	      //   the object is not shared anymore.
	      if (__gnu_cxx::__exchange_and_add_dispatch(&this->_M_refcount,
							 -1) <= 0)
		{
		  _GLIBCXX_SYNCHRONIZATION_HAPPENS_AFTER(&this->_M_refcount);
		  _M_destroy(__a);
		}
	    }
	}  // XXX MT

	void
	_M_destroy(const _Alloc&) throw();

	_CharT*
	_M_refcopy() throw()
	{
#if _GLIBCXX_FULLY_DYNAMIC_STRING == 0
	  if (__builtin_expect(this != &_S_empty_rep(), false))
#endif
	    __gnu_cxx::__atomic_add_dispatch(&this->_M_refcount, 1);
	  return _M_refdata();
	}  // XXX MT

	_CharT*
	_M_clone(const _Alloc&, size_type __res = 0);
      };

      // Use empty-base optimization: http://www.cantrip.org/emptyopt.html
      struct _Alloc_hider : _Alloc
      {
	_Alloc_hider(_CharT* __dat, const _Alloc& __a) _GLIBCXX_NOEXCEPT
	: _Alloc(__a), _M_p(__dat) { }

	_CharT* _M_p; // The actual data.
      };

    public:
      // Data Members (public):
      // NB: This is an unsigned type, and thus represents the maximum
      // size that the allocator can hold.
      ///  Value returned by various member functions when they fail.
      static const size_type	npos = static_cast<size_type>(-1);

    private:
      // Data Members (private):
      mutable _Alloc_hider	_M_dataplus;

      _CharT*
      _M_data() const _GLIBCXX_NOEXCEPT
      { return  _M_dataplus._M_p; }

      _CharT*
      _M_data(_CharT* __p) _GLIBCXX_NOEXCEPT
      { return (_M_dataplus._M_p = __p); }

      _Rep*
      _M_rep() const _GLIBCXX_NOEXCEPT
      { return &((reinterpret_cast<_Rep*> (_M_data()))[-1]); }

      // For the internal use we have functions similar to `begin'/`end'
      // but they do not call _M_leak.
      iterator
      _M_ibegin() const _GLIBCXX_NOEXCEPT
      { return iterator(_M_data()); }

      iterator
      _M_iend() const _GLIBCXX_NOEXCEPT
      { return iterator(_M_data() + this->size()); }

      void
      _M_leak()    // for use in begin() & non-const op[]
      {
	if (!_M_rep()->_M_is_leaked())
	  _M_leak_hard();
      }

      size_type
      _M_check(size_type __pos, const char* __s) const
      {
	if (__pos > this->size())
	  __throw_out_of_range_fmt(__N("%s: __pos (which is %zu) > "
				       "this->size() (which is %zu)"),
				   __s, __pos, this->size());
	return __pos;
      }

      void
      _M_check_length(size_type __n1, size_type __n2, const char* __s) const
      {
	if (this->max_size() - (this->size() - __n1) < __n2)
	  __throw_length_error(__N(__s));
      }

      // NB: _M_limit doesn't check for a bad __pos value.
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
      static void
      _M_copy(_CharT* __d, const _CharT* __s, size_type __n) _GLIBCXX_NOEXCEPT
      {
	if (__n == 1)
	  traits_type::assign(*__d, *__s);
	else
	  traits_type::copy(__d, __s, __n);
      }

      static void
      _M_move(_CharT* __d, const _CharT* __s, size_type __n) _GLIBCXX_NOEXCEPT
      {
	if (__n == 1)
	  traits_type::assign(*__d, *__s);
	else
	  traits_type::move(__d, __s, __n);
      }

      static void
      _M_assign(_CharT* __d, size_type __n, _CharT __c) _GLIBCXX_NOEXCEPT
      {
	if (__n == 1)
	  traits_type::assign(*__d, __c);
	else
	  traits_type::assign(__d, __n, __c);
      }

      // _S_copy_chars is a separate template to permit specialization
      // to optimize for the common case of pointers as iterators.
      template<class _Iterator>
	static void
	_S_copy_chars(_CharT* __p, _Iterator __k1, _Iterator __k2)
	{
	  for (; __k1 != __k2; ++__k1, (void)++__p)
	    traits_type::assign(*__p, *__k1); // These types are off.
	}

      static void
      _S_copy_chars(_CharT* __p, iterator __k1, iterator __k2) _GLIBCXX_NOEXCEPT
      { _S_copy_chars(__p, __k1.base(), __k2.base()); }

      static void
      _S_copy_chars(_CharT* __p, const_iterator __k1, const_iterator __k2)
      _GLIBCXX_NOEXCEPT
      { _S_copy_chars(__p, __k1.base(), __k2.base()); }

      static void
      _S_copy_chars(_CharT* __p, _CharT* __k1, _CharT* __k2) _GLIBCXX_NOEXCEPT
      { _M_copy(__p, __k1, __k2 - __k1); }

      static void
      _S_copy_chars(_CharT* __p, const _CharT* __k1, const _CharT* __k2)
      _GLIBCXX_NOEXCEPT
      { _M_copy(__p, __k1, __k2 - __k1); }

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

      void
      _M_mutate(size_type __pos, size_type __len1, size_type __len2);

      void
      _M_leak_hard();

      static _Rep&
      _S_empty_rep() _GLIBCXX_NOEXCEPT
      { return _Rep::_S_empty_rep(); }

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
      static __sv_type
      _S_to_string_view(__sv_type __svt) noexcept
      { return __svt; }

      // Wraps a string_view by explicit conversion and thus
      // allows to add an internal constructor that does not
      // participate in overload resolution when a string_view
      // is provided.
      struct __sv_wrapper
      {
	explicit __sv_wrapper(__sv_type __sv) noexcept : _M_sv(__sv) { }
	__sv_type _M_sv;
      };

      /**
       *  @brief  Only internally used: Construct string from a string view
       *          wrapper.
       *  @param  __svw  string view wrapper.
       *  @param  __a  Allocator to use.
       */
      explicit
      basic_string(__sv_wrapper __svw, const _Alloc& __a)
      : basic_string(__svw._M_sv.data(), __svw._M_sv.size(), __a) { }
#endif

    public:
      // Construct/copy/destroy:
      // NB: We overload ctors in some cases instead of using default
      // arguments, per 17.4.4.4 para. 2 item 2.

      /**
       *  @brief  Default constructor creates an empty string.
       */
      basic_string()
#if _GLIBCXX_FULLY_DYNAMIC_STRING == 0
      _GLIBCXX_NOEXCEPT
      : _M_dataplus(_S_empty_rep()._M_refdata(), _Alloc())
#else
      : _M_dataplus(_S_construct(size_type(), _CharT(), _Alloc()), _Alloc())
#endif
      { }

      /**
       *  @brief  Construct an empty string using allocator @a a.
       */
      explicit
      basic_string(const _Alloc& __a)
      : _M_dataplus(_S_construct(size_type(), _CharT(), __a), __a)
      { }

      // NB: per LWG issue 42, semantics different from IS:
      /**
       *  @brief  Construct string with copy of value of @a str.
       *  @param  __str  Source string.
       */
      basic_string(const basic_string& __str)
      : _M_dataplus(__str._M_rep()->_M_grab(_Alloc(__str.get_allocator()),
					    __str.get_allocator()),
		    __str.get_allocator())
      { }

      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 2583. no way to supply an allocator for basic_string(str, pos)
      /**
       *  @brief  Construct string as copy of a substring.
       *  @param  __str  Source string.
       *  @param  __pos  Index of first character to copy from.
       *  @param  __a  Allocator to use.
       */
      basic_string(const basic_string& __str, size_type __pos,
		   const _Alloc& __a = _Alloc());

      /**
       *  @brief  Construct string as copy of a substring.
       *  @param  __str  Source string.
       *  @param  __pos  Index of first character to copy from.
       *  @param  __n  Number of characters to copy.
       */
      basic_string(const basic_string& __str, size_type __pos,
		   size_type __n);
      /**
       *  @brief  Construct string as copy of a substring.
       *  @param  __str  Source string.
       *  @param  __pos  Index of first character to copy from.
       *  @param  __n  Number of characters to copy.
       *  @param  __a  Allocator to use.
       */
      basic_string(const basic_string& __str, size_type __pos,
		   size_type __n, const _Alloc& __a);

      /**
       *  @brief  Construct string initialized by a character %array.
       *  @param  __s  Source character %array.
       *  @param  __n  Number of characters to copy.
       *  @param  __a  Allocator to use (default is default allocator).
       *
       *  NB: @a __s must have at least @a __n characters, &apos;\\0&apos;
       *  has no special meaning.
       */
      basic_string(const _CharT* __s, size_type __n,
		   const _Alloc& __a = _Alloc())
      : _M_dataplus(_S_construct(__s, __s + __n, __a), __a)
      { }

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
      basic_string(const _CharT* __s, const _Alloc& __a = _Alloc())
      : _M_dataplus(_S_construct(__s, __s ? __s + traits_type::length(__s) :
				 __s + npos, __a), __a)
      { }

      /**
       *  @brief  Construct string as multiple characters.
       *  @param  __n  Number of characters.
       *  @param  __c  Character to use.
       *  @param  __a  Allocator to use (default is default allocator).
       */
      basic_string(size_type __n, _CharT __c, const _Alloc& __a = _Alloc())
      : _M_dataplus(_S_construct(__n, __c, __a), __a)
      { }

#if __cplusplus >= 201103L
      /**
       *  @brief  Move construct string.
       *  @param  __str  Source string.
       *
       *  The newly-created string contains the exact contents of @a __str.
       *  @a __str is a valid, but unspecified string.
       */
      basic_string(basic_string&& __str) noexcept
      : _M_dataplus(std::move(__str._M_dataplus))
      {
#if _GLIBCXX_FULLY_DYNAMIC_STRING == 0
	// Make __str use the shared empty string rep.
	__str._M_data(_S_empty_rep()._M_refdata());
#else
	// Rather than allocate an empty string for the rvalue string,
	// just share ownership with it by incrementing the reference count.
	// If the rvalue string was the unique owner then there are exactly
	// two owners now.
	if (_M_rep()->_M_is_shared())
	  __gnu_cxx::__atomic_add_dispatch(&_M_rep()->_M_refcount, 1);
	else
	  _M_rep()->_M_refcount = 1;
#endif
      }

      /**
       *  @brief  Construct string from an initializer %list.
       *  @param  __l  std::initializer_list of characters.
       *  @param  __a  Allocator to use (default is default allocator).
       */
      basic_string(initializer_list<_CharT> __l, const _Alloc& __a = _Alloc())
      : _M_dataplus(_S_construct(__l.begin(), __l.end(), __a), __a)
      { }

      basic_string(const basic_string& __str, const _Alloc& __a)
      : _M_dataplus(__str._M_rep()->_M_grab(__a, __str.get_allocator()), __a)
      { }

      basic_string(basic_string&& __str, const _Alloc& __a)
      : _M_dataplus(__str._M_data(), __a)
      {
	if (__a == __str.get_allocator())
	  {
#if _GLIBCXX_FULLY_DYNAMIC_STRING == 0
	    __str._M_data(_S_empty_rep()._M_refdata());
#else
	    __str._M_data(_S_construct(size_type(), _CharT(), __a));
#endif
	  }
	else
	  _M_dataplus._M_p = _S_construct(__str.begin(), __str.end(), __a);
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
      template<class _InputIterator>
	basic_string(_InputIterator __beg, _InputIterator __end,
		     const _Alloc& __a = _Alloc())
	: _M_dataplus(_S_construct(__beg, __end, __a), __a)
	{ }

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
	basic_string(const _Tp& __t, size_type __pos, size_type __n,
		     const _Alloc& __a = _Alloc())
	: basic_string(_S_to_string_view(__t).substr(__pos, __n), __a) { }

      /**
       *  @brief  Construct string from a string_view.
       *  @param  __t  Source object convertible to string view.
       *  @param  __a  Allocator to use (default is default allocator).
       */
      template<typename _Tp, typename = _If_sv<_Tp, void>>
	explicit
	basic_string(const _Tp& __t, const _Alloc& __a = _Alloc())
	: basic_string(__sv_wrapper(_S_to_string_view(__t)), __a) { }
#endif // C++17

      /**
       *  @brief  Destroy the string instance.
       */
      ~basic_string() _GLIBCXX_NOEXCEPT
      { _M_rep()->_M_dispose(this->get_allocator()); }

      /**
       *  @brief  Assign the value of @a str to this string.
       *  @param  __str  Source string.
       */
      basic_string&
      operator=(const basic_string& __str)
      { return this->assign(__str); }

      /**
       *  @brief  Copy contents of @a s into this string.
       *  @param  __s  Source null-terminated string.
       */
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
      basic_string&
      operator=(basic_string&& __str)
      _GLIBCXX_NOEXCEPT_IF(allocator_traits<_Alloc>::is_always_equal::value)
      {
	// NB: DR 1204.
	this->swap(__str);
	return *this;
      }

      /**
       *  @brief  Set value to string constructed from initializer %list.
       *  @param  __l  std::initializer_list.
       */
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
       *  @param  __svt An object convertible to  string_view.
       */
      template<typename _Tp>
	_If_sv<_Tp, basic_string&>
	operator=(const _Tp& __svt)
	{ return this->assign(__svt); }

      /**
       *  @brief  Convert to a string_view.
       *  @return A string_view.
       */
      operator __sv_type() const noexcept
      { return __sv_type(data(), size()); }
#endif // C++17

      // Iterators:
      /**
       *  Returns a read/write iterator that points to the first character in
       *  the %string.  Unshares the string.
       */
      iterator
      begin() // FIXME C++11: should be noexcept.
      {
	_M_leak();
	return iterator(_M_data());
      }

      /**
       *  Returns a read-only (constant) iterator that points to the first
       *  character in the %string.
       */
      const_iterator
      begin() const _GLIBCXX_NOEXCEPT
      { return const_iterator(_M_data()); }

      /**
       *  Returns a read/write iterator that points one past the last
       *  character in the %string.  Unshares the string.
       */
      iterator
      end() // FIXME C++11: should be noexcept.
      {
	_M_leak();
	return iterator(_M_data() + this->size());
      }

      /**
       *  Returns a read-only (constant) iterator that points one past the
       *  last character in the %string.
       */
      const_iterator
      end() const _GLIBCXX_NOEXCEPT
      { return const_iterator(_M_data() + this->size()); }

      /**
       *  Returns a read/write reverse iterator that points to the last
       *  character in the %string.  Iteration is done in reverse element
       *  order.  Unshares the string.
       */
      reverse_iterator
      rbegin() // FIXME C++11: should be noexcept.
      { return reverse_iterator(this->end()); }

      /**
       *  Returns a read-only (constant) reverse iterator that points
       *  to the last character in the %string.  Iteration is done in
       *  reverse element order.
       */
      const_reverse_iterator
      rbegin() const _GLIBCXX_NOEXCEPT
      { return const_reverse_iterator(this->end()); }

      /**
       *  Returns a read/write reverse iterator that points to one before the
       *  first character in the %string.  Iteration is done in reverse
       *  element order.  Unshares the string.
       */
      reverse_iterator
      rend() // FIXME C++11: should be noexcept.
      { return reverse_iterator(this->begin()); }

      /**
       *  Returns a read-only (constant) reverse iterator that points
       *  to one before the first character in the %string.  Iteration
       *  is done in reverse element order.
       */
      const_reverse_iterator
      rend() const _GLIBCXX_NOEXCEPT
      { return const_reverse_iterator(this->begin()); }

#if __cplusplus >= 201103L
      /**
       *  Returns a read-only (constant) iterator that points to the first
       *  character in the %string.
       */
      const_iterator
      cbegin() const noexcept
      { return const_iterator(this->_M_data()); }

      /**
       *  Returns a read-only (constant) iterator that points one past the
       *  last character in the %string.
       */
      const_iterator
      cend() const noexcept
      { return const_iterator(this->_M_data() + this->size()); }

      /**
       *  Returns a read-only (constant) reverse iterator that points
       *  to the last character in the %string.  Iteration is done in
       *  reverse element order.
       */
      const_reverse_iterator
      crbegin() const noexcept
      { return const_reverse_iterator(this->end()); }

      /**
       *  Returns a read-only (constant) reverse iterator that points
       *  to one before the first character in the %string.  Iteration
       *  is done in reverse element order.
       */
      const_reverse_iterator
      crend() const noexcept
      { return const_reverse_iterator(this->begin()); }
#endif

    public:
      // Capacity:
      ///  Returns the number of characters in the string, not including any
      ///  null-termination.
      size_type
      size() const _GLIBCXX_NOEXCEPT
      { return _M_rep()->_M_length; }

      ///  Returns the number of characters in the string, not including any
      ///  null-termination.
      size_type
      length() const _GLIBCXX_NOEXCEPT
      { return _M_rep()->_M_length; }

      ///  Returns the size() of the largest possible %string.
      size_type
      max_size() const _GLIBCXX_NOEXCEPT
      { return _Rep::_S_max_size; }

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
      void
      resize(size_type __n)
      { this->resize(__n, _CharT()); }

#if __cplusplus >= 201103L
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"
      ///  A non-binding request to reduce capacity() to size().
      void
      shrink_to_fit() noexcept
      { reserve(); }
#pragma GCC diagnostic pop
#endif

      /**
       *  Returns the total number of characters that the %string can hold
       *  before needing to allocate more memory.
       */
      size_type
      capacity() const _GLIBCXX_NOEXCEPT
      { return _M_rep()->_M_capacity; }

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
      void
      reserve(size_type __res_arg);

      /// Equivalent to shrink_to_fit().
#if __cplusplus > 201703L
      [[deprecated("use shrink_to_fit() instead")]]
#endif
      void
      reserve();

      /**
       *  Erases the string, making it empty.
       */
#if _GLIBCXX_FULLY_DYNAMIC_STRING == 0
      void
      clear() _GLIBCXX_NOEXCEPT
      {
	if (_M_rep()->_M_is_shared())
	  {
	    _M_rep()->_M_dispose(this->get_allocator());
	    _M_data(_S_empty_rep()._M_refdata());
	  }
	else
	  _M_rep()->_M_set_length_and_sharable(0);
      }
#else
      // PR 56166: this should not throw.
      void
      clear()
      { _M_mutate(0, this->size(), 0); }
#endif

      /**
       *  Returns true if the %string is empty.  Equivalent to
       *  <code>*this == ""</code>.
       */
      _GLIBCXX_NODISCARD bool
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
       *  see at().)  Unshares the string.
       */
      reference
      operator[](size_type __pos)
      {
	// Allow pos == size() both in C++98 mode, as v3 extension,
	// and in C++11 mode.
	__glibcxx_assert(__pos <= size());
	// In pedantic mode be strict in C++98 mode.
	_GLIBCXX_DEBUG_PEDASSERT(__cplusplus >= 201103L || __pos < size());
	_M_leak();
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
       *  throws out_of_range if the check fails.  Success results in
       *  unsharing the string.
       */
      reference
      at(size_type __n)
      {
	if (__n >= size())
	  __throw_out_of_range_fmt(__N("basic_string::at: __n "
				       "(which is %zu) >= this->size() "
				       "(which is %zu)"),
				   __n, this->size());
	_M_leak();
	return _M_data()[__n];
      }

#if __cplusplus >= 201103L
      /**
       *  Returns a read/write reference to the data at the first
       *  element of the %string.
       */
      reference
      front()
      {
	__glibcxx_assert(!empty());
	return operator[](0);
      }

      /**
       *  Returns a read-only (constant) reference to the data at the first
       *  element of the %string.
       */
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
      reference
      back()
      {
	__glibcxx_assert(!empty());
	return operator[](this->size() - 1);
      }

      /**
       *  Returns a read-only (constant) reference to the data at the
       *  last element of the %string.
       */
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
      basic_string&
      operator+=(const basic_string& __str)
      { return this->append(__str); }

      /**
       *  @brief  Append a C string.
       *  @param __s  The C string to append.
       *  @return  Reference to this string.
       */
      basic_string&
      operator+=(const _CharT* __s)
      { return this->append(__s); }

      /**
       *  @brief  Append a character.
       *  @param __c  The character to append.
       *  @return  Reference to this string.
       */
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
      basic_string&
      operator+=(initializer_list<_CharT> __l)
      { return this->append(__l.begin(), __l.size()); }
#endif // C++11

#if __cplusplus >= 201703L
      /**
       *  @brief  Append a string_view.
       *  @param __svt The object convertible to string_view to be appended.
       *  @return  Reference to this string.
       */
      template<typename _Tp>
	_If_sv<_Tp, basic_string&>
	operator+=(const _Tp& __svt)
	{ return this->append(__svt); }
#endif // C++17

      /**
       *  @brief  Append a string to this string.
       *  @param __str  The string to append.
       *  @return  Reference to this string.
       */
      basic_string&
      append(const basic_string& __str);

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
      basic_string&
      append(const basic_string& __str, size_type __pos, size_type __n = npos);

      /**
       *  @brief  Append a C substring.
       *  @param __s  The C string to append.
       *  @param __n  The number of characters to append.
       *  @return  Reference to this string.
       */
      basic_string&
      append(const _CharT* __s, size_type __n);

      /**
       *  @brief  Append a C string.
       *  @param __s  The C string to append.
       *  @return  Reference to this string.
       */
      basic_string&
      append(const _CharT* __s)
      {
	__glibcxx_requires_string(__s);
	return this->append(__s, traits_type::length(__s));
      }

      /**
       *  @brief  Append multiple characters.
       *  @param __n  The number of characters to append.
       *  @param __c  The character to use.
       *  @return  Reference to this string.
       *
       *  Appends __n copies of __c to this string.
       */
      basic_string&
      append(size_type __n, _CharT __c);

#if __cplusplus >= 201103L
      /**
       *  @brief  Append an initializer_list of characters.
       *  @param __l  The initializer_list of characters to append.
       *  @return  Reference to this string.
       */
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
      template<class _InputIterator>
	basic_string&
	append(_InputIterator __first, _InputIterator __last)
	{ return this->replace(_M_iend(), _M_iend(), __first, __last); }

#if __cplusplus >= 201703L
      /**
       *  @brief  Append a string_view.
       *  @param __svt The object convertible to string_view to be appended.
       *  @return  Reference to this string.
       */
      template<typename _Tp>
	_If_sv<_Tp, basic_string&>
	append(const _Tp& __svt)
	{
	  __sv_type __sv = __svt;
	  return this->append(__sv.data(), __sv.size());
	}

      /**
       *  @brief  Append a range of characters from a string_view.
       *  @param __svt The object convertible to string_view to be appended
       *               from.
       *  @param __pos The position in the string_view to append from.
       *  @param __n   The number of characters to append from the string_view.
       *  @return  Reference to this string.
       */
      template<typename _Tp>
	_If_sv<_Tp, basic_string&>
	append(const _Tp& __svt, size_type __pos, size_type __n = npos)
	{
	  __sv_type __sv = __svt;
	  return append(__sv.data()
	      + std::__sv_check(__sv.size(), __pos, "basic_string::append"),
	      std::__sv_limit(__sv.size(), __pos, __n));
	}
#endif // C++17

      /**
       *  @brief  Append a single character.
       *  @param __c  Character to append.
       */
      void
      push_back(_CharT __c)
      {
	const size_type __len = 1 + this->size();
	if (__len > this->capacity() || _M_rep()->_M_is_shared())
	  this->reserve(__len);
	traits_type::assign(_M_data()[this->size()], __c);
	_M_rep()->_M_set_length_and_sharable(__len);
      }

      /**
       *  @brief  Set value to contents of another string.
       *  @param  __str  Source string to use.
       *  @return  Reference to this string.
       */
      basic_string&
      assign(const basic_string& __str);

#if __cplusplus >= 201103L
      /**
       *  @brief  Set value to contents of another string.
       *  @param  __str  Source string to use.
       *  @return  Reference to this string.
       *
       *  This function sets this string to the exact contents of @a __str.
       *  @a __str is a valid, but unspecified string.
       */
      basic_string&
      assign(basic_string&& __str)
      noexcept(allocator_traits<_Alloc>::is_always_equal::value)
      {
	this->swap(__str);
	return *this;
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
      basic_string&
      assign(const basic_string& __str, size_type __pos, size_type __n = npos)
      { return this->assign(__str._M_data()
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
      basic_string&
      assign(const _CharT* __s, size_type __n);

      /**
       *  @brief  Set value to contents of a C string.
       *  @param __s  The C string to use.
       *  @return  Reference to this string.
       *
       *  This function sets the value of this string to the value of @a __s.
       *  The data is copied, so there is no dependence on @a __s once the
       *  function returns.
       */
      basic_string&
      assign(const _CharT* __s)
      {
	__glibcxx_requires_string(__s);
	return this->assign(__s, traits_type::length(__s));
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
      template<class _InputIterator>
	basic_string&
	assign(_InputIterator __first, _InputIterator __last)
	{ return this->replace(_M_ibegin(), _M_iend(), __first, __last); }

#if __cplusplus >= 201103L
      /**
       *  @brief  Set value to an initializer_list of characters.
       *  @param __l  The initializer_list of characters to assign.
       *  @return  Reference to this string.
       */
      basic_string&
      assign(initializer_list<_CharT> __l)
      { return this->assign(__l.begin(), __l.size()); }
#endif // C++11

#if __cplusplus >= 201703L
      /**
       *  @brief  Set value from a string_view.
       *  @param __svt The source object convertible to string_view.
       *  @return  Reference to this string.
       */
      template<typename _Tp>
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
	_If_sv<_Tp, basic_string&>
	assign(const _Tp& __svt, size_type __pos, size_type __n = npos)
	{
	  __sv_type __sv = __svt;
	  return assign(__sv.data()
	      + std::__sv_check(__sv.size(), __pos, "basic_string::assign"),
	      std::__sv_limit(__sv.size(), __pos, __n));
	}
#endif // C++17

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

#if __cplusplus >= 201103L
      /**
       *  @brief  Insert an initializer_list of characters.
       *  @param __p  Iterator referencing location in string to insert at.
       *  @param __l  The initializer_list of characters to insert.
       *  @throw  std::length_error  If new length exceeds @c max_size().
       */
      void
      insert(iterator __p, initializer_list<_CharT> __l)
      {
	_GLIBCXX_DEBUG_PEDASSERT(__p >= _M_ibegin() && __p <= _M_iend());
	this->insert(__p - _M_ibegin(), __l.begin(), __l.size());
      }
#endif // C++11

      /**
       *  @brief  Insert value of a string.
       *  @param __pos1  Position in string to insert at.
       *  @param __str  The string to insert.
       *  @return  Reference to this string.
       *  @throw  std::length_error  If new length exceeds @c max_size().
       *
       *  Inserts value of @a __str starting at @a __pos1.  If adding
       *  characters causes the length to exceed max_size(),
       *  length_error is thrown.  The value of the string doesn't
       *  change if an error is thrown.
      */
      basic_string&
      insert(size_type __pos1, const basic_string& __str)
      { return this->insert(__pos1, __str, size_type(0), __str.size()); }

      /**
       *  @brief  Insert a substring.
       *  @param __pos1  Position in string to insert at.
       *  @param __str  The string to insert.
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
      basic_string&
      insert(size_type __pos1, const basic_string& __str,
	     size_type __pos2, size_type __n = npos)
      { return this->insert(__pos1, __str._M_data()
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
      basic_string&
      insert(size_type __pos, const _CharT* __s, size_type __n);

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
      basic_string&
      insert(size_type __pos, const _CharT* __s)
      {
	__glibcxx_requires_string(__s);
	return this->insert(__pos, __s, traits_type::length(__s));
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
      iterator
      insert(iterator __p, _CharT __c)
      {
	_GLIBCXX_DEBUG_PEDASSERT(__p >= _M_ibegin() && __p <= _M_iend());
	const size_type __pos = __p - _M_ibegin();
	_M_replace_aux(__pos, size_type(0), size_type(1), __c);
	_M_rep()->_M_set_leaked();
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
       *  @param __pos2  Position in string_view to insert from.
       *  @param __n    The number of characters to insert.
       *  @return  Reference to this string.
      */
      template<typename _Tp>
	_If_sv<_Tp, basic_string&>
	insert(size_type __pos1, const _Tp& __svt,
	       size_type __pos2, size_type __n = npos)
	{
	  __sv_type __sv = __svt;
	  return this->replace(__pos1, size_type(0), __sv.data()
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
      basic_string&
      erase(size_type __pos = 0, size_type __n = npos)
      {
	_M_mutate(_M_check(__pos, "basic_string::erase"),
		  _M_limit(__pos, __n), size_type(0));
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
      iterator
      erase(iterator __position)
      {
	_GLIBCXX_DEBUG_PEDASSERT(__position >= _M_ibegin()
				 && __position < _M_iend());
	const size_type __pos = __position - _M_ibegin();
	_M_mutate(__pos, size_type(1), size_type(0));
	_M_rep()->_M_set_leaked();
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
      iterator
      erase(iterator __first, iterator __last);

#if __cplusplus >= 201103L
      /**
       *  @brief  Remove the last character.
       *
       *  The string must be non-empty.
       */
      void
      pop_back() // FIXME C++11: should be noexcept.
      {
	__glibcxx_assert(!empty());
	erase(size() - 1, 1);
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
      basic_string&
      replace(size_type __pos, size_type __n1, const _CharT* __s,
	      size_type __n2);

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
      basic_string&
      replace(iterator __i1, iterator __i2, const basic_string& __str)
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
      basic_string&
      replace(iterator __i1, iterator __i2, const _CharT* __s, size_type __n)
      {
	_GLIBCXX_DEBUG_PEDASSERT(_M_ibegin() <= __i1 && __i1 <= __i2
				 && __i2 <= _M_iend());
	return this->replace(__i1 - _M_ibegin(), __i2 - __i1, __s, __n);
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
      basic_string&
      replace(iterator __i1, iterator __i2, const _CharT* __s)
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
      basic_string&
      replace(iterator __i1, iterator __i2, size_type __n, _CharT __c)
      {
	_GLIBCXX_DEBUG_PEDASSERT(_M_ibegin() <= __i1 && __i1 <= __i2
				 && __i2 <= _M_iend());
	return _M_replace_aux(__i1 - _M_ibegin(), __i2 - __i1, __n, __c);
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
      template<class _InputIterator>
	basic_string&
	replace(iterator __i1, iterator __i2,
		_InputIterator __k1, _InputIterator __k2)
	{
	  _GLIBCXX_DEBUG_PEDASSERT(_M_ibegin() <= __i1 && __i1 <= __i2
				   && __i2 <= _M_iend());
	  __glibcxx_requires_valid_range(__k1, __k2);
	  typedef typename std::__is_integer<_InputIterator>::__type _Integral;
	  return _M_replace_dispatch(__i1, __i2, __k1, __k2, _Integral());
	}

      // Specializations for the common case of pointer and iterator:
      // useful to avoid the overhead of temporary buffering in _M_replace.
      basic_string&
      replace(iterator __i1, iterator __i2, _CharT* __k1, _CharT* __k2)
      {
	_GLIBCXX_DEBUG_PEDASSERT(_M_ibegin() <= __i1 && __i1 <= __i2
				 && __i2 <= _M_iend());
	__glibcxx_requires_valid_range(__k1, __k2);
	return this->replace(__i1 - _M_ibegin(), __i2 - __i1,
			     __k1, __k2 - __k1);
      }

      basic_string&
      replace(iterator __i1, iterator __i2,
	      const _CharT* __k1, const _CharT* __k2)
      {
	_GLIBCXX_DEBUG_PEDASSERT(_M_ibegin() <= __i1 && __i1 <= __i2
				 && __i2 <= _M_iend());
	__glibcxx_requires_valid_range(__k1, __k2);
	return this->replace(__i1 - _M_ibegin(), __i2 - __i1,
			     __k1, __k2 - __k1);
      }

      basic_string&
      replace(iterator __i1, iterator __i2, iterator __k1, iterator __k2)
      {
	_GLIBCXX_DEBUG_PEDASSERT(_M_ibegin() <= __i1 && __i1 <= __i2
				 && __i2 <= _M_iend());
	__glibcxx_requires_valid_range(__k1, __k2);
	return this->replace(__i1 - _M_ibegin(), __i2 - __i1,
			     __k1.base(), __k2 - __k1);
      }

      basic_string&
      replace(iterator __i1, iterator __i2,
	      const_iterator __k1, const_iterator __k2)
      {
	_GLIBCXX_DEBUG_PEDASSERT(_M_ibegin() <= __i1 && __i1 <= __i2
				 && __i2 <= _M_iend());
	__glibcxx_requires_valid_range(__k1, __k2);
	return this->replace(__i1 - _M_ibegin(), __i2 - __i1,
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
      basic_string& replace(iterator __i1, iterator __i2,
			    initializer_list<_CharT> __l)
      { return this->replace(__i1, __i2, __l.begin(), __l.end()); }
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
       *  to replace at.
       *  @param __i2    An iterator referencing the end position
       *  for the replace.
       *  @param __svt   The object convertible to string_view to insert from.
       *  @return  Reference to this string.
      */
      template<typename _Tp>
	_If_sv<_Tp, basic_string&>
	replace(const_iterator __i1, const_iterator __i2, const _Tp& __svt)
	{
	  __sv_type __sv = __svt;
	  return this->replace(__i1 - begin(), __i2 - __i1, __sv);
	}
#endif // C++17

    private:
      template<class _Integer>
	basic_string&
	_M_replace_dispatch(iterator __i1, iterator __i2, _Integer __n,
			    _Integer __val, __true_type)
	{ return _M_replace_aux(__i1 - _M_ibegin(), __i2 - __i1, __n, __val); }

      template<class _InputIterator>
	basic_string&
	_M_replace_dispatch(iterator __i1, iterator __i2, _InputIterator __k1,
			    _InputIterator __k2, __false_type);

      basic_string&
      _M_replace_aux(size_type __pos1, size_type __n1, size_type __n2,
		     _CharT __c);

      basic_string&
      _M_replace_safe(size_type __pos1, size_type __n1, const _CharT* __s,
		      size_type __n2);

      // _S_construct_aux is used to implement the 21.3.1 para 15 which
      // requires special behaviour if _InIter is an integral type
      template<class _InIterator>
	static _CharT*
	_S_construct_aux(_InIterator __beg, _InIterator __end,
			 const _Alloc& __a, __false_type)
	{
	  typedef typename iterator_traits<_InIterator>::iterator_category _Tag;
	  return _S_construct(__beg, __end, __a, _Tag());
	}

      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 438. Ambiguity in the "do the right thing" clause
      template<class _Integer>
	static _CharT*
	_S_construct_aux(_Integer __beg, _Integer __end,
			 const _Alloc& __a, __true_type)
	{ return _S_construct_aux_2(static_cast<size_type>(__beg),
				    __end, __a); }

      static _CharT*
      _S_construct_aux_2(size_type __req, _CharT __c, const _Alloc& __a)
      { return _S_construct(__req, __c, __a); }

      template<class _InIterator>
	static _CharT*
	_S_construct(_InIterator __beg, _InIterator __end, const _Alloc& __a)
	{
	  typedef typename std::__is_integer<_InIterator>::__type _Integral;
	  return _S_construct_aux(__beg, __end, __a, _Integral());
	}

      // For Input Iterators, used in istreambuf_iterators, etc.
      template<class _InIterator>
	static _CharT*
	 _S_construct(_InIterator __beg, _InIterator __end, const _Alloc& __a,
		      input_iterator_tag);

      // For forward_iterators up to random_access_iterators, used for
      // string::iterator, _CharT*, etc.
      template<class _FwdIterator>
	static _CharT*
	_S_construct(_FwdIterator __beg, _FwdIterator __end, const _Alloc& __a,
		     forward_iterator_tag);

      static _CharT*
      _S_construct(size_type __req, _CharT __c, const _Alloc& __a);

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
      size_type
      copy(_CharT* __s, size_type __n, size_type __pos = 0) const;

      /**
       *  @brief  Swap contents with another string.
       *  @param __s  String to swap with.
       *
       *  Exchanges the contents of this string with that of @a __s in constant
       *  time.
      */
      void
      swap(basic_string& __s)
      _GLIBCXX_NOEXCEPT_IF(allocator_traits<_Alloc>::is_always_equal::value);

      // String operations:
      /**
       *  @brief  Return const pointer to null-terminated contents.
       *
       *  This is a handle to internal data.  Do not modify or dire things may
       *  happen.
      */
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
      _CharT*
      data() noexcept
      {
	_M_leak();
	return _M_data();
      }
#endif

      /**
       *  @brief  Return copy of allocator used to construct this string.
      */
      allocator_type
      get_allocator() const _GLIBCXX_NOEXCEPT
      { return _M_dataplus; }

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
      size_type
      find(const basic_string& __str, size_type __pos = 0) const
      _GLIBCXX_NOEXCEPT
      { return this->find(__str.data(), __pos, __str.size()); }

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
      size_type
      find(_CharT __c, size_type __pos = 0) const _GLIBCXX_NOEXCEPT;

#if __cplusplus >= 201703L
      /**
       *  @brief  Find position of a string_view.
       *  @param __svt  The object convertible to string_view to locate.
       *  @param __pos  Index of character to search from (default 0).
       *  @return  Index of start of first occurrence.
      */
      template<typename _Tp>
	_If_sv<_Tp, size_type>
	find(const _Tp& __svt, size_type __pos = 0) const
	noexcept(is_same<_Tp, __sv_type>::value)
	{
	  __sv_type __sv = __svt;
	  return this->find(__sv.data(), __pos, __sv.size());
	}
#endif // C++17

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
      size_type
      rfind(const basic_string& __str, size_type __pos = npos) const
      _GLIBCXX_NOEXCEPT
      { return this->rfind(__str.data(), __pos, __str.size()); }

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
      size_type
      rfind(const _CharT* __s, size_type __pos = npos) const _GLIBCXX_NOEXCEPT
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
      size_type
      rfind(_CharT __c, size_type __pos = npos) const _GLIBCXX_NOEXCEPT;

#if __cplusplus >= 201703L
      /**
       *  @brief  Find last position of a string_view.
       *  @param __svt  The object convertible to string_view to locate.
       *  @param __pos  Index of character to search back from (default end).
       *  @return  Index of start of last occurrence.
      */
      template<typename _Tp>
	_If_sv<_Tp, size_type>
	rfind(const _Tp& __svt, size_type __pos = npos) const
	noexcept(is_same<_Tp, __sv_type>::value)
	{
	  __sv_type __sv = __svt;
	  return this->rfind(__sv.data(), __pos, __sv.size());
	}
#endif // C++17

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
      size_type
      find_first_of(const basic_string& __str, size_type __pos = 0) const
      _GLIBCXX_NOEXCEPT
      { return this->find_first_of(__str.data(), __pos, __str.size()); }

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
      size_type
      find_first_of(_CharT __c, size_type __pos = 0) const _GLIBCXX_NOEXCEPT
      { return this->find(__c, __pos); }

#if __cplusplus >= 201703L
      /**
       *  @brief  Find position of a character of a string_view.
       *  @param __svt  An object convertible to string_view containing
       *                characters to locate.
       *  @param __pos  Index of character to search from (default 0).
       *  @return  Index of first occurrence.
      */
      template<typename _Tp>
	_If_sv<_Tp, size_type>
	find_first_of(const _Tp& __svt, size_type __pos = 0) const
	noexcept(is_same<_Tp, __sv_type>::value)
	{
	  __sv_type __sv = __svt;
	  return this->find_first_of(__sv.data(), __pos, __sv.size());
	}
#endif // C++17

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
      size_type
      find_last_of(const basic_string& __str, size_type __pos = npos) const
      _GLIBCXX_NOEXCEPT
      { return this->find_last_of(__str.data(), __pos, __str.size()); }

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
      size_type
      find_last_of(_CharT __c, size_type __pos = npos) const _GLIBCXX_NOEXCEPT
      { return this->rfind(__c, __pos); }

#if __cplusplus >= 201703L
      /**
       *  @brief  Find last position of a character of string.
       *  @param __svt  An object convertible to string_view containing
       *                characters to locate.
       *  @param __pos  Index of character to search back from (default end).
       *  @return  Index of last occurrence.
      */
      template<typename _Tp>
	_If_sv<_Tp, size_type>
	find_last_of(const _Tp& __svt, size_type __pos = npos) const
	noexcept(is_same<_Tp, __sv_type>::value)
	{
	  __sv_type __sv = __svt;
	  return this->find_last_of(__sv.data(), __pos, __sv.size());
	}
#endif // C++17

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
      size_type
      find_first_not_of(const basic_string& __str, size_type __pos = 0) const
      _GLIBCXX_NOEXCEPT
      { return this->find_first_not_of(__str.data(), __pos, __str.size()); }

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
      size_type
      find_first_not_of(_CharT __c, size_type __pos = 0) const
      _GLIBCXX_NOEXCEPT;

#if __cplusplus >= 201703L
      /**
       *  @brief  Find position of a character not in a string_view.
       *  @param __svt  An object convertible to string_view containing
       *                characters to avoid.
       *  @param __pos  Index of character to search from (default 0).
       *  @return  Index of first occurrence.
       */
      template<typename _Tp>
	_If_sv<_Tp, size_type>
	find_first_not_of(const _Tp& __svt, size_type __pos = 0) const
	noexcept(is_same<_Tp, __sv_type>::value)
	{
	  __sv_type __sv = __svt;
	  return this->find_first_not_of(__sv.data(), __pos, __sv.size());
	}
#endif // C++17

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
      size_type
      find_last_not_of(const basic_string& __str, size_type __pos = npos) const
      _GLIBCXX_NOEXCEPT
      { return this->find_last_not_of(__str.data(), __pos, __str.size()); }

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
      size_type
      find_last_not_of(_CharT __c, size_type __pos = npos) const
      _GLIBCXX_NOEXCEPT;

#if __cplusplus >= 201703L
      /**
       *  @brief  Find last position of a character not in a string_view.
       *  @param __svt  An object convertible to string_view containing
       *                characters to avoid.
       *  @param __pos  Index of character to search back from (default end).
       *  @return  Index of last occurrence.
       */
      template<typename _Tp>
	_If_sv<_Tp, size_type>
	find_last_not_of(const _Tp& __svt, size_type __pos = npos) const
	noexcept(is_same<_Tp, __sv_type>::value)
	{
	  __sv_type __sv = __svt;
	  return this->find_last_not_of(__sv.data(), __pos, __sv.size());
	}
#endif // C++17

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
       *  @param __svt   An object convertible to string_view to compare
       *                 against.
       *  @param __pos2  A position in the string_view to start comparing from.
       *  @param __n2  The number of characters to compare.
       *  @return  Integer < 0, 0, or > 0.
       */
      template<typename _Tp>
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
      int
      compare(size_type __pos, size_type __n1, const _CharT* __s,
	      size_type __n2) const;

#if __cplusplus > 201703L
      bool
      starts_with(basic_string_view<_CharT, _Traits> __x) const noexcept
      { return __sv_type(this->data(), this->size()).starts_with(__x); }

      bool
      starts_with(_CharT __x) const noexcept
      { return __sv_type(this->data(), this->size()).starts_with(__x); }

      bool
      starts_with(const _CharT* __x) const noexcept
      { return __sv_type(this->data(), this->size()).starts_with(__x); }

      bool
      ends_with(basic_string_view<_CharT, _Traits> __x) const noexcept
      { return __sv_type(this->data(), this->size()).ends_with(__x); }

      bool
      ends_with(_CharT __x) const noexcept
      { return __sv_type(this->data(), this->size()).ends_with(__x); }

      bool
      ends_with(const _CharT* __x) const noexcept
      { return __sv_type(this->data(), this->size()).ends_with(__x); }
#endif // C++20

#if __cplusplus > 202011L
      bool
      contains(basic_string_view<_CharT, _Traits> __x) const noexcept
      { return __sv_type(this->data(), this->size()).contains(__x); }

      bool
      contains(_CharT __x) const noexcept
      { return __sv_type(this->data(), this->size()).contains(__x); }

      bool
      contains(const _CharT* __x) const noexcept
      { return __sv_type(this->data(), this->size()).contains(__x); }
#endif // C++23

# ifdef _GLIBCXX_TM_TS_INTERNAL
      friend void
      ::_txnal_cow_string_C1_for_exceptions(void* that, const char* s,
					    void* exc);
      friend const char*
      ::_txnal_cow_string_c_str(const void *that);
      friend void
      ::_txnal_cow_string_D1(void *that);
      friend void
      ::_txnal_cow_string_D1_commit(void *that);
# endif
  };

  template<typename _CharT, typename _Traits, typename _Alloc>
    const typename basic_string<_CharT, _Traits, _Alloc>::size_type
    basic_string<_CharT, _Traits, _Alloc>::
    _Rep::_S_max_size = (((npos - sizeof(_Rep_base))/sizeof(_CharT)) - 1) / 4;

  template<typename _CharT, typename _Traits, typename _Alloc>
    const _CharT
    basic_string<_CharT, _Traits, _Alloc>::
    _Rep::_S_terminal = _CharT();

  template<typename _CharT, typename _Traits, typename _Alloc>
    const typename basic_string<_CharT, _Traits, _Alloc>::size_type
    basic_string<_CharT, _Traits, _Alloc>::npos;

  // Linker sets _S_empty_rep_storage to all 0s (one reference, empty string)
  // at static init time (before static ctors are run).
  template<typename _CharT, typename _Traits, typename _Alloc>
    typename basic_string<_CharT, _Traits, _Alloc>::size_type
    basic_string<_CharT, _Traits, _Alloc>::_Rep::_S_empty_rep_storage[
    (sizeof(_Rep_base) + sizeof(_CharT) + sizeof(size_type) - 1) /
      sizeof(size_type)];

  // NB: This is the special case for Input Iterators, used in
  // istreambuf_iterators, etc.
  // Input Iterators have a cost structure very different from
  // pointers, calling for a different coding style.
  template<typename _CharT, typename _Traits, typename _Alloc>
    template<typename _InIterator>
      _CharT*
      basic_string<_CharT, _Traits, _Alloc>::
      _S_construct(_InIterator __beg, _InIterator __end, const _Alloc& __a,
		   input_iterator_tag)
      {
#if _GLIBCXX_FULLY_DYNAMIC_STRING == 0
	if (__beg == __end && __a == _Alloc())
	  return _S_empty_rep()._M_refdata();
#endif
	// Avoid reallocation for common case.
	_CharT __buf[128];
	size_type __len = 0;
	while (__beg != __end && __len < sizeof(__buf) / sizeof(_CharT))
	  {
	    __buf[__len++] = *__beg;
	    ++__beg;
	  }
	_Rep* __r = _Rep::_S_create(__len, size_type(0), __a);
	_M_copy(__r->_M_refdata(), __buf, __len);
	__try
	  {
	    while (__beg != __end)
	      {
		if (__len == __r->_M_capacity)
		  {
		    // Allocate more space.
		    _Rep* __another = _Rep::_S_create(__len + 1, __len, __a);
		    _M_copy(__another->_M_refdata(), __r->_M_refdata(), __len);
		    __r->_M_destroy(__a);
		    __r = __another;
		  }
		__r->_M_refdata()[__len++] = *__beg;
		++__beg;
	      }
	  }
	__catch(...)
	  {
	    __r->_M_destroy(__a);
	    __throw_exception_again;
	  }
	__r->_M_set_length_and_sharable(__len);
	return __r->_M_refdata();
      }

  template<typename _CharT, typename _Traits, typename _Alloc>
    template <typename _InIterator>
      _CharT*
      basic_string<_CharT, _Traits, _Alloc>::
      _S_construct(_InIterator __beg, _InIterator __end, const _Alloc& __a,
		   forward_iterator_tag)
      {
#if _GLIBCXX_FULLY_DYNAMIC_STRING == 0
	if (__beg == __end && __a == _Alloc())
	  return _S_empty_rep()._M_refdata();
#endif
	// NB: Not required, but considered best practice.
	if (__gnu_cxx::__is_null_pointer(__beg) && __beg != __end)
	  __throw_logic_error(__N("basic_string::_S_construct null not valid"));

	const size_type __dnew = static_cast<size_type>(std::distance(__beg,
								      __end));
	// Check for out_of_range and length_error exceptions.
	_Rep* __r = _Rep::_S_create(__dnew, size_type(0), __a);
	__try
	  { _S_copy_chars(__r->_M_refdata(), __beg, __end); }
	__catch(...)
	  {
	    __r->_M_destroy(__a);
	    __throw_exception_again;
	  }
	__r->_M_set_length_and_sharable(__dnew);
	return __r->_M_refdata();
      }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _CharT*
    basic_string<_CharT, _Traits, _Alloc>::
    _S_construct(size_type __n, _CharT __c, const _Alloc& __a)
    {
#if _GLIBCXX_FULLY_DYNAMIC_STRING == 0
      if (__n == 0 && __a == _Alloc())
	return _S_empty_rep()._M_refdata();
#endif
      // Check for out_of_range and length_error exceptions.
      _Rep* __r = _Rep::_S_create(__n, size_type(0), __a);
      if (__n)
	_M_assign(__r->_M_refdata(), __n, __c);

      __r->_M_set_length_and_sharable(__n);
      return __r->_M_refdata();
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    basic_string<_CharT, _Traits, _Alloc>::
    basic_string(const basic_string& __str, size_type __pos, const _Alloc& __a)
    : _M_dataplus(_S_construct(__str._M_data()
			       + __str._M_check(__pos,
						"basic_string::basic_string"),
			       __str._M_data() + __str._M_limit(__pos, npos)
			       + __pos, __a), __a)
    { }

  template<typename _CharT, typename _Traits, typename _Alloc>
    basic_string<_CharT, _Traits, _Alloc>::
    basic_string(const basic_string& __str, size_type __pos, size_type __n)
    : _M_dataplus(_S_construct(__str._M_data()
			       + __str._M_check(__pos,
						"basic_string::basic_string"),
			       __str._M_data() + __str._M_limit(__pos, __n)
			       + __pos, _Alloc()), _Alloc())
    { }

  template<typename _CharT, typename _Traits, typename _Alloc>
    basic_string<_CharT, _Traits, _Alloc>::
    basic_string(const basic_string& __str, size_type __pos,
		 size_type __n, const _Alloc& __a)
    : _M_dataplus(_S_construct(__str._M_data()
			       + __str._M_check(__pos,
						"basic_string::basic_string"),
			       __str._M_data() + __str._M_limit(__pos, __n)
			       + __pos, __a), __a)
    { }

  template<typename _CharT, typename _Traits, typename _Alloc>
    basic_string<_CharT, _Traits, _Alloc>&
    basic_string<_CharT, _Traits, _Alloc>::
    assign(const basic_string& __str)
    {
      if (_M_rep() != __str._M_rep())
	{
	  // XXX MT
	  const allocator_type __a = this->get_allocator();
	  _CharT* __tmp = __str._M_rep()->_M_grab(__a, __str.get_allocator());
	  _M_rep()->_M_dispose(__a);
	  _M_data(__tmp);
	}
      return *this;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    basic_string<_CharT, _Traits, _Alloc>&
    basic_string<_CharT, _Traits, _Alloc>::
    assign(const _CharT* __s, size_type __n)
    {
      __glibcxx_requires_string_len(__s, __n);
      _M_check_length(this->size(), __n, "basic_string::assign");
      if (_M_disjunct(__s) || _M_rep()->_M_is_shared())
	return _M_replace_safe(size_type(0), this->size(), __s, __n);
      else
	{
	  // Work in-place.
	  const size_type __pos = __s - _M_data();
	  if (__pos >= __n)
	    _M_copy(_M_data(), __s, __n);
	  else if (__pos)
	    _M_move(_M_data(), __s, __n);
	  _M_rep()->_M_set_length_and_sharable(__n);
	  return *this;
	}
     }

  template<typename _CharT, typename _Traits, typename _Alloc>
    basic_string<_CharT, _Traits, _Alloc>&
    basic_string<_CharT, _Traits, _Alloc>::
    append(size_type __n, _CharT __c)
    {
      if (__n)
	{
	  _M_check_length(size_type(0), __n, "basic_string::append");
	  const size_type __len = __n + this->size();
	  if (__len > this->capacity() || _M_rep()->_M_is_shared())
	    this->reserve(__len);
	  _M_assign(_M_data() + this->size(), __n, __c);
	  _M_rep()->_M_set_length_and_sharable(__len);
	}
      return *this;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    basic_string<_CharT, _Traits, _Alloc>&
    basic_string<_CharT, _Traits, _Alloc>::
    append(const _CharT* __s, size_type __n)
    {
      __glibcxx_requires_string_len(__s, __n);
      if (__n)
	{
	  _M_check_length(size_type(0), __n, "basic_string::append");
	  const size_type __len = __n + this->size();
	  if (__len > this->capacity() || _M_rep()->_M_is_shared())
	    {
	      if (_M_disjunct(__s))
		this->reserve(__len);
	      else
		{
		  const size_type __off = __s - _M_data();
		  this->reserve(__len);
		  __s = _M_data() + __off;
		}
	    }
	  _M_copy(_M_data() + this->size(), __s, __n);
	  _M_rep()->_M_set_length_and_sharable(__len);
	}
      return *this;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    basic_string<_CharT, _Traits, _Alloc>&
    basic_string<_CharT, _Traits, _Alloc>::
    append(const basic_string& __str)
    {
      const size_type __size = __str.size();
      if (__size)
	{
	  const size_type __len = __size + this->size();
	  if (__len > this->capacity() || _M_rep()->_M_is_shared())
	    this->reserve(__len);
	  _M_copy(_M_data() + this->size(), __str._M_data(), __size);
	  _M_rep()->_M_set_length_and_sharable(__len);
	}
      return *this;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    basic_string<_CharT, _Traits, _Alloc>&
    basic_string<_CharT, _Traits, _Alloc>::
    append(const basic_string& __str, size_type __pos, size_type __n)
    {
      __str._M_check(__pos, "basic_string::append");
      __n = __str._M_limit(__pos, __n);
      if (__n)
	{
	  const size_type __len = __n + this->size();
	  if (__len > this->capacity() || _M_rep()->_M_is_shared())
	    this->reserve(__len);
	  _M_copy(_M_data() + this->size(), __str._M_data() + __pos, __n);
	  _M_rep()->_M_set_length_and_sharable(__len);
	}
      return *this;
    }

   template<typename _CharT, typename _Traits, typename _Alloc>
     basic_string<_CharT, _Traits, _Alloc>&
     basic_string<_CharT, _Traits, _Alloc>::
     insert(size_type __pos, const _CharT* __s, size_type __n)
     {
       __glibcxx_requires_string_len(__s, __n);
       _M_check(__pos, "basic_string::insert");
       _M_check_length(size_type(0), __n, "basic_string::insert");
       if (_M_disjunct(__s) || _M_rep()->_M_is_shared())
	 return _M_replace_safe(__pos, size_type(0), __s, __n);
       else
	 {
	   // Work in-place.
	   const size_type __off = __s - _M_data();
	   _M_mutate(__pos, 0, __n);
	   __s = _M_data() + __off;
	   _CharT* __p = _M_data() + __pos;
	   if (__s  + __n <= __p)
	     _M_copy(__p, __s, __n);
	   else if (__s >= __p)
	     _M_copy(__p, __s + __n, __n);
	   else
	     {
	       const size_type __nleft = __p - __s;
	       _M_copy(__p, __s, __nleft);
	       _M_copy(__p + __nleft, __p + __n, __n - __nleft);
	     }
	   return *this;
	 }
     }

   template<typename _CharT, typename _Traits, typename _Alloc>
     typename basic_string<_CharT, _Traits, _Alloc>::iterator
     basic_string<_CharT, _Traits, _Alloc>::
     erase(iterator __first, iterator __last)
     {
       _GLIBCXX_DEBUG_PEDASSERT(__first >= _M_ibegin() && __first <= __last
				&& __last <= _M_iend());

       // NB: This isn't just an optimization (bail out early when
       // there is nothing to do, really), it's also a correctness
       // issue vs MT, see libstdc++/40518.
       const size_type __size = __last - __first;
       if (__size)
	 {
	   const size_type __pos = __first - _M_ibegin();
	   _M_mutate(__pos, __size, size_type(0));
	   _M_rep()->_M_set_leaked();
	   return iterator(_M_data() + __pos);
	 }
       else
	 return __first;
     }

   template<typename _CharT, typename _Traits, typename _Alloc>
     basic_string<_CharT, _Traits, _Alloc>&
     basic_string<_CharT, _Traits, _Alloc>::
     replace(size_type __pos, size_type __n1, const _CharT* __s,
	     size_type __n2)
     {
       __glibcxx_requires_string_len(__s, __n2);
       _M_check(__pos, "basic_string::replace");
       __n1 = _M_limit(__pos, __n1);
       _M_check_length(__n1, __n2, "basic_string::replace");
       bool __left;
       if (_M_disjunct(__s) || _M_rep()->_M_is_shared())
	 return _M_replace_safe(__pos, __n1, __s, __n2);
       else if ((__left = __s + __n2 <= _M_data() + __pos)
		|| _M_data() + __pos + __n1 <= __s)
	 {
	   // Work in-place: non-overlapping case.
	   size_type __off = __s - _M_data();
	   __left ? __off : (__off += __n2 - __n1);
	   _M_mutate(__pos, __n1, __n2);
	   _M_copy(_M_data() + __pos, _M_data() + __off, __n2);
	   return *this;
	 }
       else
	 {
	   // Todo: overlapping case.
	   const basic_string __tmp(__s, __n2);
	   return _M_replace_safe(__pos, __n1, __tmp._M_data(), __n2);
	 }
     }

  template<typename _CharT, typename _Traits, typename _Alloc>
    void
    basic_string<_CharT, _Traits, _Alloc>::_Rep::
    _M_destroy(const _Alloc& __a) throw ()
    {
      const size_type __size = sizeof(_Rep_base)
			       + (this->_M_capacity + 1) * sizeof(_CharT);
      _Raw_bytes_alloc(__a).deallocate(reinterpret_cast<char*>(this), __size);
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    void
    basic_string<_CharT, _Traits, _Alloc>::
    _M_leak_hard()
    {
      // No need to create a new copy of an empty string when a non-const
      // reference/pointer/iterator into it is obtained. Modifying the
      // trailing null character is undefined, so the ref/pointer/iterator
      // is effectively const anyway.
      if (this->empty())
	return;

      if (_M_rep()->_M_is_shared())
	_M_mutate(0, 0, 0);
      _M_rep()->_M_set_leaked();
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    void
    basic_string<_CharT, _Traits, _Alloc>::
    _M_mutate(size_type __pos, size_type __len1, size_type __len2)
    {
      const size_type __old_size = this->size();
      const size_type __new_size = __old_size + __len2 - __len1;
      const size_type __how_much = __old_size - __pos - __len1;

      if (__new_size > this->capacity() || _M_rep()->_M_is_shared())
	{
	  // Must reallocate.
	  const allocator_type __a = get_allocator();
	  _Rep* __r = _Rep::_S_create(__new_size, this->capacity(), __a);

	  if (__pos)
	    _M_copy(__r->_M_refdata(), _M_data(), __pos);
	  if (__how_much)
	    _M_copy(__r->_M_refdata() + __pos + __len2,
		    _M_data() + __pos + __len1, __how_much);

	  _M_rep()->_M_dispose(__a);
	  _M_data(__r->_M_refdata());
	}
      else if (__how_much && __len1 != __len2)
	{
	  // Work in-place.
	  _M_move(_M_data() + __pos + __len2,
		  _M_data() + __pos + __len1, __how_much);
	}
      _M_rep()->_M_set_length_and_sharable(__new_size);
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    void
    basic_string<_CharT, _Traits, _Alloc>::
    reserve(size_type __res)
    {
      const size_type __capacity = capacity();

      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 2968. Inconsistencies between basic_string reserve and
      // vector/unordered_map/unordered_set reserve functions
      // P0966 reserve should not shrink
      if (__res <= __capacity)
	{
	  if (!_M_rep()->_M_is_shared())
	    return;

	  // unshare, but keep same capacity
	  __res = __capacity;
	}

      const allocator_type __a = get_allocator();
      _CharT* __tmp = _M_rep()->_M_clone(__a, __res - this->size());
      _M_rep()->_M_dispose(__a);
      _M_data(__tmp);
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    void
    basic_string<_CharT, _Traits, _Alloc>::
    swap(basic_string& __s)
    _GLIBCXX_NOEXCEPT_IF(allocator_traits<_Alloc>::is_always_equal::value)
    {
      if (_M_rep()->_M_is_leaked())
	_M_rep()->_M_set_sharable();
      if (__s._M_rep()->_M_is_leaked())
	__s._M_rep()->_M_set_sharable();
      if (this->get_allocator() == __s.get_allocator())
	{
	  _CharT* __tmp = _M_data();
	  _M_data(__s._M_data());
	  __s._M_data(__tmp);
	}
      // The code below can usually be optimized away.
      else
	{
	  const basic_string __tmp1(_M_ibegin(), _M_iend(),
				    __s.get_allocator());
	  const basic_string __tmp2(__s._M_ibegin(), __s._M_iend(),
				    this->get_allocator());
	  *this = __tmp2;
	  __s = __tmp1;
	}
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    typename basic_string<_CharT, _Traits, _Alloc>::_Rep*
    basic_string<_CharT, _Traits, _Alloc>::_Rep::
    _S_create(size_type __capacity, size_type __old_capacity,
	      const _Alloc& __alloc)
    {
      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 83.  String::npos vs. string::max_size()
      if (__capacity > _S_max_size)
	__throw_length_error(__N("basic_string::_S_create"));

      // The standard places no restriction on allocating more memory
      // than is strictly needed within this layer at the moment or as
      // requested by an explicit application call to reserve(n).

      // Many malloc implementations perform quite poorly when an
      // application attempts to allocate memory in a stepwise fashion
      // growing each allocation size by only 1 char.  Additionally,
      // it makes little sense to allocate less linear memory than the
      // natural blocking size of the malloc implementation.
      // Unfortunately, we would need a somewhat low-level calculation
      // with tuned parameters to get this perfect for any particular
      // malloc implementation.  Fortunately, generalizations about
      // common features seen among implementations seems to suffice.

      // __pagesize need not match the actual VM page size for good
      // results in practice, thus we pick a common value on the low
      // side.  __malloc_header_size is an estimate of the amount of
      // overhead per memory allocation (in practice seen N * sizeof
      // (void*) where N is 0, 2 or 4).  According to folklore,
      // picking this value on the high side is better than
      // low-balling it (especially when this algorithm is used with
      // malloc implementations that allocate memory blocks rounded up
      // to a size which is a power of 2).
      const size_type __pagesize = 4096;
      const size_type __malloc_header_size = 4 * sizeof(void*);

      // The below implements an exponential growth policy, necessary to
      // meet amortized linear time requirements of the library: see
      // http://gcc.gnu.org/ml/libstdc++/2001-07/msg00085.html.
      // It's active for allocations requiring an amount of memory above
      // system pagesize. This is consistent with the requirements of the
      // standard: http://gcc.gnu.org/ml/libstdc++/2001-07/msg00130.html
      if (__capacity > __old_capacity && __capacity < 2 * __old_capacity)
	__capacity = 2 * __old_capacity;

      // NB: Need an array of char_type[__capacity], plus a terminating
      // null char_type() element, plus enough for the _Rep data structure.
      // Whew. Seemingly so needy, yet so elemental.
      size_type __size = (__capacity + 1) * sizeof(_CharT) + sizeof(_Rep);

      const size_type __adj_size = __size + __malloc_header_size;
      if (__adj_size > __pagesize && __capacity > __old_capacity)
	{
	  const size_type __extra = __pagesize - __adj_size % __pagesize;
	  __capacity += __extra / sizeof(_CharT);
	  // Never allocate a string bigger than _S_max_size.
	  if (__capacity > _S_max_size)
	    __capacity = _S_max_size;
	  __size = (__capacity + 1) * sizeof(_CharT) + sizeof(_Rep);
	}

      // NB: Might throw, but no worries about a leak, mate: _Rep()
      // does not throw.
      void* __place = _Raw_bytes_alloc(__alloc).allocate(__size);
      _Rep *__p = new (__place) _Rep;
      __p->_M_capacity = __capacity;
      // ABI compatibility - 3.4.x set in _S_create both
      // _M_refcount and _M_length.  All callers of _S_create
      // in basic_string.tcc then set just _M_length.
      // In 4.0.x and later both _M_refcount and _M_length
      // are initialized in the callers, unfortunately we can
      // have 3.4.x compiled code with _S_create callers inlined
      // calling 4.0.x+ _S_create.
      __p->_M_set_sharable();
      return __p;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    _CharT*
    basic_string<_CharT, _Traits, _Alloc>::_Rep::
    _M_clone(const _Alloc& __alloc, size_type __res)
    {
      // Requested capacity of the clone.
      const size_type __requested_cap = this->_M_length + __res;
      _Rep* __r = _Rep::_S_create(__requested_cap, this->_M_capacity,
				  __alloc);
      if (this->_M_length)
	_M_copy(__r->_M_refdata(), _M_refdata(), this->_M_length);

      __r->_M_set_length_and_sharable(this->_M_length);
      return __r->_M_refdata();
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    void
    basic_string<_CharT, _Traits, _Alloc>::
    resize(size_type __n, _CharT __c)
    {
      const size_type __size = this->size();
      _M_check_length(__size, __n, "basic_string::resize");
      if (__size < __n)
	this->append(__n - __size, __c);
      else if (__n < __size)
	this->erase(__n);
      // else nothing (in particular, avoid calling _M_mutate() unnecessarily.)
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    template<typename _InputIterator>
      basic_string<_CharT, _Traits, _Alloc>&
      basic_string<_CharT, _Traits, _Alloc>::
      _M_replace_dispatch(iterator __i1, iterator __i2, _InputIterator __k1,
			  _InputIterator __k2, __false_type)
      {
	const basic_string __s(__k1, __k2);
	const size_type __n1 = __i2 - __i1;
	_M_check_length(__n1, __s.size(), "basic_string::_M_replace_dispatch");
	return _M_replace_safe(__i1 - _M_ibegin(), __n1, __s._M_data(),
			       __s.size());
      }

  template<typename _CharT, typename _Traits, typename _Alloc>
    basic_string<_CharT, _Traits, _Alloc>&
    basic_string<_CharT, _Traits, _Alloc>::
    _M_replace_aux(size_type __pos1, size_type __n1, size_type __n2,
		   _CharT __c)
    {
      _M_check_length(__n1, __n2, "basic_string::_M_replace_aux");
      _M_mutate(__pos1, __n1, __n2);
      if (__n2)
	_M_assign(_M_data() + __pos1, __n2, __c);
      return *this;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    basic_string<_CharT, _Traits, _Alloc>&
    basic_string<_CharT, _Traits, _Alloc>::
    _M_replace_safe(size_type __pos1, size_type __n1, const _CharT* __s,
		    size_type __n2)
    {
      _M_mutate(__pos1, __n1, __n2);
      if (__n2)
	_M_copy(_M_data() + __pos1, __s, __n2);
      return *this;
    }

  template<typename _CharT, typename _Traits, typename _Alloc>
    void
    basic_string<_CharT, _Traits, _Alloc>::
    reserve()
    {
#if __cpp_exceptions
      if (length() < capacity() || _M_rep()->_M_is_shared())
	try
	  {
	    const allocator_type __a = get_allocator();
	    _CharT* __tmp = _M_rep()->_M_clone(__a);
	    _M_rep()->_M_dispose(__a);
	    _M_data(__tmp);
	  }
	catch (const __cxxabiv1::__forced_unwind&)
	  { throw; }
	catch (...)
	  { /* swallow the exception */ }
#endif
    }

    template<typename _CharT, typename _Traits, typename _Alloc>
    typename basic_string<_CharT, _Traits, _Alloc>::size_type
    basic_string<_CharT, _Traits, _Alloc>::
    copy(_CharT* __s, size_type __n, size_type __pos) const
    {
      _M_check(__pos, "basic_string::copy");
      __n = _M_limit(__pos, __n);
      __glibcxx_requires_string_len(__s, __n);
      if (__n)
	_M_copy(__s, _M_data() + __pos, __n);
      // 21.3.5.7 par 3: do not append null.  (good.)
      return __n;
    }
_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std
#endif  // ! _GLIBCXX_USE_CXX11_ABI
#endif  // _COW_STRING_H
