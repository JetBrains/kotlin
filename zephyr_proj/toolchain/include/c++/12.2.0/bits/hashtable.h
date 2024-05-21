// hashtable.h header -*- C++ -*-

// Copyright (C) 2007-2022 Free Software Foundation, Inc.
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

/** @file bits/hashtable.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{unordered_map, unordered_set}
 */

#ifndef _HASHTABLE_H
#define _HASHTABLE_H 1

#pragma GCC system_header

#include <bits/hashtable_policy.h>
#include <bits/enable_special_members.h>
#if __cplusplus > 201402L
# include <bits/node_handle.h>
#endif
#include <bits/functional_hash.h>
#include <bits/stl_function.h> // equal_to, _Identity, _Select1st

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION
/// @cond undocumented

  template<typename _Tp, typename _Hash>
    using __cache_default
      =  __not_<__and_<// Do not cache for fast hasher.
		       __is_fast_hash<_Hash>,
		       // Mandatory to have erase not throwing.
		       __is_nothrow_invocable<const _Hash&, const _Tp&>>>;

  // Helper to conditionally delete the default constructor.
  // The _Hash_node_base type is used to distinguish this specialization
  // from any other potentially-overlapping subobjects of the hashtable.
  template<typename _Equal, typename _Hash, typename _Allocator>
    using _Hashtable_enable_default_ctor
      = _Enable_default_constructor<__and_<is_default_constructible<_Equal>,
				       is_default_constructible<_Hash>,
				       is_default_constructible<_Allocator>>{},
				    __detail::_Hash_node_base>;

  /**
   *  Primary class template _Hashtable.
   *
   *  @ingroup hashtable-detail
   *
   *  @tparam _Value  CopyConstructible type.
   *
   *  @tparam _Key    CopyConstructible type.
   *
   *  @tparam _Alloc  An allocator type
   *  ([lib.allocator.requirements]) whose _Alloc::value_type is
   *  _Value.  As a conforming extension, we allow for
   *  _Alloc::value_type != _Value.
   *
   *  @tparam _ExtractKey  Function object that takes an object of type
   *  _Value and returns a value of type _Key.
   *
   *  @tparam _Equal  Function object that takes two objects of type k
   *  and returns a bool-like value that is true if the two objects
   *  are considered equal.
   *
   *  @tparam _Hash  The hash function. A unary function object with
   *  argument type _Key and result type size_t. Return values should
   *  be distributed over the entire range [0, numeric_limits<size_t>:::max()].
   *
   *  @tparam _RangeHash  The range-hashing function (in the terminology of
   *  Tavori and Dreizin).  A binary function object whose argument
   *  types and result type are all size_t.  Given arguments r and N,
   *  the return value is in the range [0, N).
   *
   *  @tparam _Unused  Not used.
   *
   *  @tparam _RehashPolicy  Policy class with three members, all of
   *  which govern the bucket count. _M_next_bkt(n) returns a bucket
   *  count no smaller than n.  _M_bkt_for_elements(n) returns a
   *  bucket count appropriate for an element count of n.
   *  _M_need_rehash(n_bkt, n_elt, n_ins) determines whether, if the
   *  current bucket count is n_bkt and the current element count is
   *  n_elt, we need to increase the bucket count for n_ins insertions.
   *  If so, returns make_pair(true, n), where n is the new bucket count. If
   *  not, returns make_pair(false, <anything>)
   *
   *  @tparam _Traits  Compile-time class with three boolean
   *  std::integral_constant members:  __cache_hash_code, __constant_iterators,
   *   __unique_keys.
   *
   *  Each _Hashtable data structure has:
   *
   *  - _Bucket[]       _M_buckets
   *  - _Hash_node_base _M_before_begin
   *  - size_type       _M_bucket_count
   *  - size_type       _M_element_count
   *
   *  with _Bucket being _Hash_node_base* and _Hash_node containing:
   *
   *  - _Hash_node*   _M_next
   *  - Tp            _M_value
   *  - size_t        _M_hash_code if cache_hash_code is true
   *
   *  In terms of Standard containers the hashtable is like the aggregation of:
   *
   *  - std::forward_list<_Node> containing the elements
   *  - std::vector<std::forward_list<_Node>::iterator> representing the buckets
   *
   *  The non-empty buckets contain the node before the first node in the
   *  bucket. This design makes it possible to implement something like a
   *  std::forward_list::insert_after on container insertion and
   *  std::forward_list::erase_after on container erase
   *  calls. _M_before_begin is equivalent to
   *  std::forward_list::before_begin. Empty buckets contain
   *  nullptr.  Note that one of the non-empty buckets contains
   *  &_M_before_begin which is not a dereferenceable node so the
   *  node pointer in a bucket shall never be dereferenced, only its
   *  next node can be.
   *
   *  Walking through a bucket's nodes requires a check on the hash code to
   *  see if each node is still in the bucket. Such a design assumes a
   *  quite efficient hash functor and is one of the reasons it is
   *  highly advisable to set __cache_hash_code to true.
   *
   *  The container iterators are simply built from nodes. This way
   *  incrementing the iterator is perfectly efficient independent of
   *  how many empty buckets there are in the container.
   *
   *  On insert we compute the element's hash code and use it to find the
   *  bucket index. If the element must be inserted in an empty bucket
   *  we add it at the beginning of the singly linked list and make the
   *  bucket point to _M_before_begin. The bucket that used to point to
   *  _M_before_begin, if any, is updated to point to its new before
   *  begin node.
   *
   *  On erase, the simple iterator design requires using the hash
   *  functor to get the index of the bucket to update. For this
   *  reason, when __cache_hash_code is set to false the hash functor must
   *  not throw and this is enforced by a static assertion.
   *
   *  Functionality is implemented by decomposition into base classes,
   *  where the derived _Hashtable class is used in _Map_base,
   *  _Insert, _Rehash_base, and _Equality base classes to access the
   *  "this" pointer. _Hashtable_base is used in the base classes as a
   *  non-recursive, fully-completed-type so that detailed nested type
   *  information, such as iterator type and node type, can be
   *  used. This is similar to the "Curiously Recurring Template
   *  Pattern" (CRTP) technique, but uses a reconstructed, not
   *  explicitly passed, template pattern.
   *
   *  Base class templates are: 
   *    - __detail::_Hashtable_base
   *    - __detail::_Map_base
   *    - __detail::_Insert
   *    - __detail::_Rehash_base
   *    - __detail::_Equality
   */
  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    class _Hashtable
    : public __detail::_Hashtable_base<_Key, _Value, _ExtractKey, _Equal,
				       _Hash, _RangeHash, _Unused, _Traits>,
      public __detail::_Map_base<_Key, _Value, _Alloc, _ExtractKey, _Equal,
				 _Hash, _RangeHash, _Unused,
				 _RehashPolicy, _Traits>,
      public __detail::_Insert<_Key, _Value, _Alloc, _ExtractKey, _Equal,
			       _Hash, _RangeHash, _Unused,
			       _RehashPolicy, _Traits>,
      public __detail::_Rehash_base<_Key, _Value, _Alloc, _ExtractKey, _Equal,
				    _Hash, _RangeHash, _Unused,
				    _RehashPolicy, _Traits>,
      public __detail::_Equality<_Key, _Value, _Alloc, _ExtractKey, _Equal,
				 _Hash, _RangeHash, _Unused,
				 _RehashPolicy, _Traits>,
      private __detail::_Hashtable_alloc<
	__alloc_rebind<_Alloc,
		       __detail::_Hash_node<_Value,
					    _Traits::__hash_cached::value>>>,
      private _Hashtable_enable_default_ctor<_Equal, _Hash, _Alloc>
    {
      static_assert(is_same<typename remove_cv<_Value>::type, _Value>::value,
	  "unordered container must have a non-const, non-volatile value_type");
#if __cplusplus > 201703L || defined __STRICT_ANSI__
      static_assert(is_same<typename _Alloc::value_type, _Value>{},
	  "unordered container must have the same value_type as its allocator");
#endif

      using __traits_type = _Traits;
      using __hash_cached = typename __traits_type::__hash_cached;
      using __constant_iterators = typename __traits_type::__constant_iterators;
      using __node_type = __detail::_Hash_node<_Value, __hash_cached::value>;
      using __node_alloc_type = __alloc_rebind<_Alloc, __node_type>;

      using __hashtable_alloc = __detail::_Hashtable_alloc<__node_alloc_type>;

      using __node_value_type =
	__detail::_Hash_node_value<_Value, __hash_cached::value>;
      using __node_ptr = typename __hashtable_alloc::__node_ptr;
      using __value_alloc_traits =
	typename __hashtable_alloc::__value_alloc_traits;
      using __node_alloc_traits =
	typename __hashtable_alloc::__node_alloc_traits;
      using __node_base = typename __hashtable_alloc::__node_base;
      using __node_base_ptr = typename __hashtable_alloc::__node_base_ptr;
      using __buckets_ptr = typename __hashtable_alloc::__buckets_ptr;

      using __insert_base = __detail::_Insert<_Key, _Value, _Alloc, _ExtractKey,
					      _Equal, _Hash,
					      _RangeHash, _Unused,
					      _RehashPolicy, _Traits>;
      using __enable_default_ctor
	= _Hashtable_enable_default_ctor<_Equal, _Hash, _Alloc>;

    public:
      typedef _Key						key_type;
      typedef _Value						value_type;
      typedef _Alloc						allocator_type;
      typedef _Equal						key_equal;

      // mapped_type, if present, comes from _Map_base.
      // hasher, if present, comes from _Hash_code_base/_Hashtable_base.
      typedef typename __value_alloc_traits::pointer		pointer;
      typedef typename __value_alloc_traits::const_pointer	const_pointer;
      typedef value_type&					reference;
      typedef const value_type&					const_reference;

      using iterator = typename __insert_base::iterator;

      using const_iterator = typename __insert_base::const_iterator;

      using local_iterator = __detail::_Local_iterator<key_type, _Value,
			_ExtractKey, _Hash, _RangeHash, _Unused,
					     __constant_iterators::value,
					     __hash_cached::value>;

      using const_local_iterator = __detail::_Local_const_iterator<
			key_type, _Value,
			_ExtractKey, _Hash, _RangeHash, _Unused,
			__constant_iterators::value, __hash_cached::value>;

    private:
      using __rehash_type = _RehashPolicy;
      using __rehash_state = typename __rehash_type::_State;

      using __unique_keys = typename __traits_type::__unique_keys;

      using __hashtable_base = __detail::
	_Hashtable_base<_Key, _Value, _ExtractKey,
			_Equal, _Hash, _RangeHash, _Unused, _Traits>;

      using __hash_code_base =  typename __hashtable_base::__hash_code_base;
      using __hash_code =  typename __hashtable_base::__hash_code;
      using __ireturn_type = typename __insert_base::__ireturn_type;

      using __map_base = __detail::_Map_base<_Key, _Value, _Alloc, _ExtractKey,
					     _Equal, _Hash, _RangeHash, _Unused,
					     _RehashPolicy, _Traits>;

      using __rehash_base = __detail::_Rehash_base<_Key, _Value, _Alloc,
						   _ExtractKey, _Equal,
						   _Hash, _RangeHash, _Unused,
						   _RehashPolicy, _Traits>;

      using __eq_base = __detail::_Equality<_Key, _Value, _Alloc, _ExtractKey,
					    _Equal, _Hash, _RangeHash, _Unused,
					    _RehashPolicy, _Traits>;

      using __reuse_or_alloc_node_gen_t =
	__detail::_ReuseOrAllocNode<__node_alloc_type>;
      using __alloc_node_gen_t =
	__detail::_AllocNode<__node_alloc_type>;
      using __node_builder_t =
	__detail::_NodeBuilder<_ExtractKey>;

      // Simple RAII type for managing a node containing an element
      struct _Scoped_node
      {
	// Take ownership of a node with a constructed element.
	_Scoped_node(__node_ptr __n, __hashtable_alloc* __h)
	: _M_h(__h), _M_node(__n) { }

	// Allocate a node and construct an element within it.
	template<typename... _Args>
	  _Scoped_node(__hashtable_alloc* __h, _Args&&... __args)
	  : _M_h(__h),
	    _M_node(__h->_M_allocate_node(std::forward<_Args>(__args)...))
	  { }

	// Destroy element and deallocate node.
	~_Scoped_node() { if (_M_node) _M_h->_M_deallocate_node(_M_node); };

	_Scoped_node(const _Scoped_node&) = delete;
	_Scoped_node& operator=(const _Scoped_node&) = delete;

	__hashtable_alloc* _M_h;
	__node_ptr _M_node;
      };

      template<typename _Ht>
	static constexpr
	__conditional_t<std::is_lvalue_reference<_Ht>::value,
			const value_type&, value_type&&>
	__fwd_value_for(value_type& __val) noexcept
	{ return std::move(__val); }

      // Compile-time diagnostics.

      // _Hash_code_base has everything protected, so use this derived type to
      // access it.
      struct __hash_code_base_access : __hash_code_base
      { using __hash_code_base::_M_bucket_index; };

      // To get bucket index we need _RangeHash not to throw.
      static_assert(is_nothrow_default_constructible<_RangeHash>::value,
		    "Functor used to map hash code to bucket index"
		    " must be nothrow default constructible");
      static_assert(noexcept(
	std::declval<const _RangeHash&>()((std::size_t)0, (std::size_t)0)),
		    "Functor used to map hash code to bucket index must be"
		    " noexcept");

      // To compute bucket index we also need _ExtratKey not to throw.
      static_assert(is_nothrow_default_constructible<_ExtractKey>::value,
		    "_ExtractKey must be nothrow default constructible");
      static_assert(noexcept(
	std::declval<const _ExtractKey&>()(std::declval<_Value>())),
		    "_ExtractKey functor must be noexcept invocable");

      template<typename _Keya, typename _Valuea, typename _Alloca,
	       typename _ExtractKeya, typename _Equala,
	       typename _Hasha, typename _RangeHasha, typename _Unuseda,
	       typename _RehashPolicya, typename _Traitsa,
	       bool _Unique_keysa>
	friend struct __detail::_Map_base;

      template<typename _Keya, typename _Valuea, typename _Alloca,
	       typename _ExtractKeya, typename _Equala,
	       typename _Hasha, typename _RangeHasha, typename _Unuseda,
	       typename _RehashPolicya, typename _Traitsa>
	friend struct __detail::_Insert_base;

      template<typename _Keya, typename _Valuea, typename _Alloca,
	       typename _ExtractKeya, typename _Equala,
	       typename _Hasha, typename _RangeHasha, typename _Unuseda,
	       typename _RehashPolicya, typename _Traitsa,
	       bool _Constant_iteratorsa>
	friend struct __detail::_Insert;

      template<typename _Keya, typename _Valuea, typename _Alloca,
	       typename _ExtractKeya, typename _Equala,
	       typename _Hasha, typename _RangeHasha, typename _Unuseda,
	       typename _RehashPolicya, typename _Traitsa,
	       bool _Unique_keysa>
	friend struct __detail::_Equality;

    public:
      using size_type = typename __hashtable_base::size_type;
      using difference_type = typename __hashtable_base::difference_type;

#if __cplusplus > 201402L
      using node_type = _Node_handle<_Key, _Value, __node_alloc_type>;
      using insert_return_type = _Node_insert_return<iterator, node_type>;
#endif

    private:
      __buckets_ptr		_M_buckets		= &_M_single_bucket;
      size_type			_M_bucket_count		= 1;
      __node_base		_M_before_begin;
      size_type			_M_element_count	= 0;
      _RehashPolicy		_M_rehash_policy;

      // A single bucket used when only need for 1 bucket. Especially
      // interesting in move semantic to leave hashtable with only 1 bucket
      // which is not allocated so that we can have those operations noexcept
      // qualified.
      // Note that we can't leave hashtable with 0 bucket without adding
      // numerous checks in the code to avoid 0 modulus.
      __node_base_ptr		_M_single_bucket	= nullptr;

      void
      _M_update_bbegin()
      {
	if (_M_begin())
	  _M_buckets[_M_bucket_index(*_M_begin())] = &_M_before_begin;
      }

      void
      _M_update_bbegin(__node_ptr __n)
      {
	_M_before_begin._M_nxt = __n;
	_M_update_bbegin();
      }

      bool
      _M_uses_single_bucket(__buckets_ptr __bkts) const
      { return __builtin_expect(__bkts == &_M_single_bucket, false); }

      bool
      _M_uses_single_bucket() const
      { return _M_uses_single_bucket(_M_buckets); }

      static constexpr size_t
      __small_size_threshold() noexcept
      {
	return
	  __detail::_Hashtable_hash_traits<_Hash>::__small_size_threshold();
      }

      __hashtable_alloc&
      _M_base_alloc() { return *this; }

      __buckets_ptr
      _M_allocate_buckets(size_type __bkt_count)
      {
	if (__builtin_expect(__bkt_count == 1, false))
	  {
	    _M_single_bucket = nullptr;
	    return &_M_single_bucket;
	  }

	return __hashtable_alloc::_M_allocate_buckets(__bkt_count);
      }

      void
      _M_deallocate_buckets(__buckets_ptr __bkts, size_type __bkt_count)
      {
	if (_M_uses_single_bucket(__bkts))
	  return;

	__hashtable_alloc::_M_deallocate_buckets(__bkts, __bkt_count);
      }

      void
      _M_deallocate_buckets()
      { _M_deallocate_buckets(_M_buckets, _M_bucket_count); }

      // Gets bucket begin, deals with the fact that non-empty buckets contain
      // their before begin node.
      __node_ptr
      _M_bucket_begin(size_type __bkt) const;

      __node_ptr
      _M_begin() const
      { return static_cast<__node_ptr>(_M_before_begin._M_nxt); }

      // Assign *this using another _Hashtable instance. Whether elements
      // are copied or moved depends on the _Ht reference.
      template<typename _Ht>
	void
	_M_assign_elements(_Ht&&);

      template<typename _Ht, typename _NodeGenerator>
	void
	_M_assign(_Ht&&, const _NodeGenerator&);

      void
      _M_move_assign(_Hashtable&&, true_type);

      void
      _M_move_assign(_Hashtable&&, false_type);

      void
      _M_reset() noexcept;

      _Hashtable(const _Hash& __h, const _Equal& __eq,
		 const allocator_type& __a)
      : __hashtable_base(__h, __eq),
	__hashtable_alloc(__node_alloc_type(__a)),
	__enable_default_ctor(_Enable_default_constructor_tag{})
      { }

      template<bool _No_realloc = true>
	static constexpr bool
	_S_nothrow_move()
	{
#if __cplusplus <= 201402L
	  return __and_<__bool_constant<_No_realloc>,
			is_nothrow_copy_constructible<_Hash>,
			is_nothrow_copy_constructible<_Equal>>::value;
#else
	  if constexpr (_No_realloc)
	    if constexpr (is_nothrow_copy_constructible<_Hash>())
	      return is_nothrow_copy_constructible<_Equal>();
	  return false;
#endif
	}

      _Hashtable(_Hashtable&& __ht, __node_alloc_type&& __a,
		 true_type /* alloc always equal */)
	noexcept(_S_nothrow_move());

      _Hashtable(_Hashtable&&, __node_alloc_type&&,
		 false_type /* alloc always equal */);

      template<typename _InputIterator>
	_Hashtable(_InputIterator __first, _InputIterator __last,
		   size_type __bkt_count_hint,
		   const _Hash&, const _Equal&, const allocator_type&,
		   true_type __uks);

      template<typename _InputIterator>
	_Hashtable(_InputIterator __first, _InputIterator __last,
		   size_type __bkt_count_hint,
		   const _Hash&, const _Equal&, const allocator_type&,
		   false_type __uks);

    public:
      // Constructor, destructor, assignment, swap
      _Hashtable() = default;

      _Hashtable(const _Hashtable&);

      _Hashtable(const _Hashtable&, const allocator_type&);

      explicit
      _Hashtable(size_type __bkt_count_hint,
		 const _Hash& __hf = _Hash(),
		 const key_equal& __eql = key_equal(),
		 const allocator_type& __a = allocator_type());

      // Use delegating constructors.
      _Hashtable(_Hashtable&& __ht)
	noexcept(_S_nothrow_move())
      : _Hashtable(std::move(__ht), std::move(__ht._M_node_allocator()),
		   true_type{})
      { }

      _Hashtable(_Hashtable&& __ht, const allocator_type& __a)
	noexcept(_S_nothrow_move<__node_alloc_traits::_S_always_equal()>())
      : _Hashtable(std::move(__ht), __node_alloc_type(__a),
		   typename __node_alloc_traits::is_always_equal{})
      { }

      explicit
      _Hashtable(const allocator_type& __a)
      : __hashtable_alloc(__node_alloc_type(__a)),
	__enable_default_ctor(_Enable_default_constructor_tag{})
      { }

      template<typename _InputIterator>
	_Hashtable(_InputIterator __f, _InputIterator __l,
		   size_type __bkt_count_hint = 0,
		   const _Hash& __hf = _Hash(),
		   const key_equal& __eql = key_equal(),
		   const allocator_type& __a = allocator_type())
	: _Hashtable(__f, __l, __bkt_count_hint, __hf, __eql, __a,
		     __unique_keys{})
	{ }

      _Hashtable(initializer_list<value_type> __l,
		 size_type __bkt_count_hint = 0,
		 const _Hash& __hf = _Hash(),
		 const key_equal& __eql = key_equal(),
		 const allocator_type& __a = allocator_type())
      : _Hashtable(__l.begin(), __l.end(), __bkt_count_hint,
		   __hf, __eql, __a, __unique_keys{})
      { }

      _Hashtable&
      operator=(const _Hashtable& __ht);

      _Hashtable&
      operator=(_Hashtable&& __ht)
      noexcept(__node_alloc_traits::_S_nothrow_move()
	       && is_nothrow_move_assignable<_Hash>::value
	       && is_nothrow_move_assignable<_Equal>::value)
      {
	constexpr bool __move_storage =
	  __node_alloc_traits::_S_propagate_on_move_assign()
	  || __node_alloc_traits::_S_always_equal();
	_M_move_assign(std::move(__ht), __bool_constant<__move_storage>());
	return *this;
      }

      _Hashtable&
      operator=(initializer_list<value_type> __l)
      {
	__reuse_or_alloc_node_gen_t __roan(_M_begin(), *this);
	_M_before_begin._M_nxt = nullptr;
	clear();

	// We consider that all elements of __l are going to be inserted.
	auto __l_bkt_count = _M_rehash_policy._M_bkt_for_elements(__l.size());

	// Do not shrink to keep potential user reservation.
	if (_M_bucket_count < __l_bkt_count)
	  rehash(__l_bkt_count);

	this->_M_insert_range(__l.begin(), __l.end(), __roan, __unique_keys{});
	return *this;
      }

      ~_Hashtable() noexcept;

      void
      swap(_Hashtable&)
      noexcept(__and_<__is_nothrow_swappable<_Hash>,
		      __is_nothrow_swappable<_Equal>>::value);

      // Basic container operations
      iterator
      begin() noexcept
      { return iterator(_M_begin()); }

      const_iterator
      begin() const noexcept
      { return const_iterator(_M_begin()); }

      iterator
      end() noexcept
      { return iterator(nullptr); }

      const_iterator
      end() const noexcept
      { return const_iterator(nullptr); }

      const_iterator
      cbegin() const noexcept
      { return const_iterator(_M_begin()); }

      const_iterator
      cend() const noexcept
      { return const_iterator(nullptr); }

      size_type
      size() const noexcept
      { return _M_element_count; }

      _GLIBCXX_NODISCARD bool
      empty() const noexcept
      { return size() == 0; }

      allocator_type
      get_allocator() const noexcept
      { return allocator_type(this->_M_node_allocator()); }

      size_type
      max_size() const noexcept
      { return __node_alloc_traits::max_size(this->_M_node_allocator()); }

      // Observers
      key_equal
      key_eq() const
      { return this->_M_eq(); }

      // hash_function, if present, comes from _Hash_code_base.

      // Bucket operations
      size_type
      bucket_count() const noexcept
      { return _M_bucket_count; }

      size_type
      max_bucket_count() const noexcept
      { return max_size(); }

      size_type
      bucket_size(size_type __bkt) const
      { return std::distance(begin(__bkt), end(__bkt)); }

      size_type
      bucket(const key_type& __k) const
      { return _M_bucket_index(this->_M_hash_code(__k)); }

      local_iterator
      begin(size_type __bkt)
      {
	return local_iterator(*this, _M_bucket_begin(__bkt),
			      __bkt, _M_bucket_count);
      }

      local_iterator
      end(size_type __bkt)
      { return local_iterator(*this, nullptr, __bkt, _M_bucket_count); }

      const_local_iterator
      begin(size_type __bkt) const
      {
	return const_local_iterator(*this, _M_bucket_begin(__bkt),
				    __bkt, _M_bucket_count);
      }

      const_local_iterator
      end(size_type __bkt) const
      { return const_local_iterator(*this, nullptr, __bkt, _M_bucket_count); }

      // DR 691.
      const_local_iterator
      cbegin(size_type __bkt) const
      {
	return const_local_iterator(*this, _M_bucket_begin(__bkt),
				    __bkt, _M_bucket_count);
      }

      const_local_iterator
      cend(size_type __bkt) const
      { return const_local_iterator(*this, nullptr, __bkt, _M_bucket_count); }

      float
      load_factor() const noexcept
      {
	return static_cast<float>(size()) / static_cast<float>(bucket_count());
      }

      // max_load_factor, if present, comes from _Rehash_base.

      // Generalization of max_load_factor.  Extension, not found in
      // TR1.  Only useful if _RehashPolicy is something other than
      // the default.
      const _RehashPolicy&
      __rehash_policy() const
      { return _M_rehash_policy; }

      void
      __rehash_policy(const _RehashPolicy& __pol)
      { _M_rehash_policy = __pol; }

      // Lookup.
      iterator
      find(const key_type& __k);

      const_iterator
      find(const key_type& __k) const;

      size_type
      count(const key_type& __k) const;

      std::pair<iterator, iterator>
      equal_range(const key_type& __k);

      std::pair<const_iterator, const_iterator>
      equal_range(const key_type& __k) const;

#if __cplusplus >= 202002L
#define __cpp_lib_generic_unordered_lookup 201811L

      template<typename _Kt,
	       typename = __has_is_transparent_t<_Hash, _Kt>,
	       typename = __has_is_transparent_t<_Equal, _Kt>>
	iterator
	_M_find_tr(const _Kt& __k);

      template<typename _Kt,
	       typename = __has_is_transparent_t<_Hash, _Kt>,
	       typename = __has_is_transparent_t<_Equal, _Kt>>
	const_iterator
	_M_find_tr(const _Kt& __k) const;

      template<typename _Kt,
	       typename = __has_is_transparent_t<_Hash, _Kt>,
	       typename = __has_is_transparent_t<_Equal, _Kt>>
	size_type
	_M_count_tr(const _Kt& __k) const;

      template<typename _Kt,
	       typename = __has_is_transparent_t<_Hash, _Kt>,
	       typename = __has_is_transparent_t<_Equal, _Kt>>
	pair<iterator, iterator>
	_M_equal_range_tr(const _Kt& __k);

      template<typename _Kt,
	       typename = __has_is_transparent_t<_Hash, _Kt>,
	       typename = __has_is_transparent_t<_Equal, _Kt>>
	pair<const_iterator, const_iterator>
	_M_equal_range_tr(const _Kt& __k) const;
#endif // C++20

    private:
      // Bucket index computation helpers.
      size_type
      _M_bucket_index(const __node_value_type& __n) const noexcept
      { return __hash_code_base::_M_bucket_index(__n, _M_bucket_count); }

      size_type
      _M_bucket_index(__hash_code __c) const
      { return __hash_code_base::_M_bucket_index(__c, _M_bucket_count); }

      __node_base_ptr
      _M_find_before_node(const key_type&);

      // Find and insert helper functions and types
      // Find the node before the one matching the criteria.
      __node_base_ptr
      _M_find_before_node(size_type, const key_type&, __hash_code) const;

      template<typename _Kt>
	__node_base_ptr
	_M_find_before_node_tr(size_type, const _Kt&, __hash_code) const;

      __node_ptr
      _M_find_node(size_type __bkt, const key_type& __key,
		   __hash_code __c) const
      {
	__node_base_ptr __before_n = _M_find_before_node(__bkt, __key, __c);
	if (__before_n)
	  return static_cast<__node_ptr>(__before_n->_M_nxt);
	return nullptr;
      }

      template<typename _Kt>
	__node_ptr
	_M_find_node_tr(size_type __bkt, const _Kt& __key,
			__hash_code __c) const
	{
	  auto __before_n = _M_find_before_node_tr(__bkt, __key, __c);
	  if (__before_n)
	    return static_cast<__node_ptr>(__before_n->_M_nxt);
	  return nullptr;
	}

      // Insert a node at the beginning of a bucket.
      void
      _M_insert_bucket_begin(size_type, __node_ptr);

      // Remove the bucket first node
      void
      _M_remove_bucket_begin(size_type __bkt, __node_ptr __next_n,
			     size_type __next_bkt);

      // Get the node before __n in the bucket __bkt
      __node_base_ptr
      _M_get_previous_node(size_type __bkt, __node_ptr __n);

      pair<const_iterator, __hash_code>
      _M_compute_hash_code(const_iterator __hint, const key_type& __k) const;

      // Insert node __n with hash code __code, in bucket __bkt if no
      // rehash (assumes no element with same key already present).
      // Takes ownership of __n if insertion succeeds, throws otherwise.
      iterator
      _M_insert_unique_node(size_type __bkt, __hash_code,
			    __node_ptr __n, size_type __n_elt = 1);

      // Insert node __n with key __k and hash code __code.
      // Takes ownership of __n if insertion succeeds, throws otherwise.
      iterator
      _M_insert_multi_node(__node_ptr __hint,
			   __hash_code __code, __node_ptr __n);

      template<typename... _Args>
	std::pair<iterator, bool>
	_M_emplace(true_type __uks, _Args&&... __args);

      template<typename... _Args>
	iterator
	_M_emplace(false_type __uks, _Args&&... __args)
	{ return _M_emplace(cend(), __uks, std::forward<_Args>(__args)...); }

      // Emplace with hint, useless when keys are unique.
      template<typename... _Args>
	iterator
	_M_emplace(const_iterator, true_type __uks, _Args&&... __args)
	{ return _M_emplace(__uks, std::forward<_Args>(__args)...).first; }

      template<typename... _Args>
	iterator
	_M_emplace(const_iterator, false_type __uks, _Args&&... __args);

      template<typename _Kt, typename _Arg, typename _NodeGenerator>
	std::pair<iterator, bool>
	_M_insert_unique(_Kt&&, _Arg&&, const _NodeGenerator&);

      template<typename _Kt>
	static __conditional_t<
	  __and_<__is_nothrow_invocable<_Hash&, const key_type&>,
		 __not_<__is_nothrow_invocable<_Hash&, _Kt>>>::value,
	  key_type, _Kt&&>
	_S_forward_key(_Kt&& __k)
	{ return std::forward<_Kt>(__k); }

      static const key_type&
      _S_forward_key(const key_type& __k)
      { return __k; }

      static key_type&&
      _S_forward_key(key_type&& __k)
      { return std::move(__k); }

      template<typename _Arg, typename _NodeGenerator>
	std::pair<iterator, bool>
	_M_insert(_Arg&& __arg, const _NodeGenerator& __node_gen,
		  true_type /* __uks */)
	{
	  return _M_insert_unique(
	    _S_forward_key(_ExtractKey{}(std::forward<_Arg>(__arg))),
	    std::forward<_Arg>(__arg), __node_gen);
	}

      template<typename _Arg, typename _NodeGenerator>
	iterator
	_M_insert(_Arg&& __arg, const _NodeGenerator& __node_gen,
		  false_type __uks)
	{
	  return _M_insert(cend(), std::forward<_Arg>(__arg), __node_gen,
			   __uks);
	}

      // Insert with hint, not used when keys are unique.
      template<typename _Arg, typename _NodeGenerator>
	iterator
	_M_insert(const_iterator, _Arg&& __arg,
		  const _NodeGenerator& __node_gen, true_type __uks)
	{
	  return
	    _M_insert(std::forward<_Arg>(__arg), __node_gen, __uks).first;
	}

      // Insert with hint when keys are not unique.
      template<typename _Arg, typename _NodeGenerator>
	iterator
	_M_insert(const_iterator, _Arg&&,
		  const _NodeGenerator&, false_type __uks);

      size_type
      _M_erase(true_type __uks, const key_type&);

      size_type
      _M_erase(false_type __uks, const key_type&);

      iterator
      _M_erase(size_type __bkt, __node_base_ptr __prev_n, __node_ptr __n);

    public:
      // Emplace
      template<typename... _Args>
	__ireturn_type
	emplace(_Args&&... __args)
	{ return _M_emplace(__unique_keys{}, std::forward<_Args>(__args)...); }

      template<typename... _Args>
	iterator
	emplace_hint(const_iterator __hint, _Args&&... __args)
	{
	  return _M_emplace(__hint, __unique_keys{},
			    std::forward<_Args>(__args)...);
	}

      // Insert member functions via inheritance.

      // Erase
      iterator
      erase(const_iterator);

      // LWG 2059.
      iterator
      erase(iterator __it)
      { return erase(const_iterator(__it)); }

      size_type
      erase(const key_type& __k)
      { return _M_erase(__unique_keys{}, __k); }

      iterator
      erase(const_iterator, const_iterator);

      void
      clear() noexcept;

      // Set number of buckets keeping it appropriate for container's number
      // of elements.
      void rehash(size_type __bkt_count);

      // DR 1189.
      // reserve, if present, comes from _Rehash_base.

#if __cplusplus > 201402L
      /// Re-insert an extracted node into a container with unique keys.
      insert_return_type
      _M_reinsert_node(node_type&& __nh)
      {
	insert_return_type __ret;
	if (__nh.empty())
	  __ret.position = end();
	else
	  {
	    __glibcxx_assert(get_allocator() == __nh.get_allocator());

	    const key_type& __k = __nh._M_key();
	    __hash_code __code = this->_M_hash_code(__k);
	    size_type __bkt = _M_bucket_index(__code);
	    if (__node_ptr __n = _M_find_node(__bkt, __k, __code))
	      {
		__ret.node = std::move(__nh);
		__ret.position = iterator(__n);
		__ret.inserted = false;
	      }
	    else
	      {
		__ret.position
		  = _M_insert_unique_node(__bkt, __code, __nh._M_ptr);
		__nh._M_ptr = nullptr;
		__ret.inserted = true;
	      }
	  }
	return __ret;
      }

      /// Re-insert an extracted node into a container with equivalent keys.
      iterator
      _M_reinsert_node_multi(const_iterator __hint, node_type&& __nh)
      {
	if (__nh.empty())
	  return end();

	__glibcxx_assert(get_allocator() == __nh.get_allocator());

	const key_type& __k = __nh._M_key();
	auto __code = this->_M_hash_code(__k);
	auto __ret
	  = _M_insert_multi_node(__hint._M_cur, __code, __nh._M_ptr);
	__nh._M_ptr = nullptr;
	return __ret;
      }

    private:
      node_type
      _M_extract_node(size_t __bkt, __node_base_ptr __prev_n)
      {
	__node_ptr __n = static_cast<__node_ptr>(__prev_n->_M_nxt);
	if (__prev_n == _M_buckets[__bkt])
	  _M_remove_bucket_begin(__bkt, __n->_M_next(),
	     __n->_M_nxt ? _M_bucket_index(*__n->_M_next()) : 0);
	else if (__n->_M_nxt)
	  {
	    size_type __next_bkt = _M_bucket_index(*__n->_M_next());
	    if (__next_bkt != __bkt)
	      _M_buckets[__next_bkt] = __prev_n;
	  }

	__prev_n->_M_nxt = __n->_M_nxt;
	__n->_M_nxt = nullptr;
	--_M_element_count;
	return { __n, this->_M_node_allocator() };
      }

    public:
      // Extract a node.
      node_type
      extract(const_iterator __pos)
      {
	size_t __bkt = _M_bucket_index(*__pos._M_cur);
	return _M_extract_node(__bkt,
			       _M_get_previous_node(__bkt, __pos._M_cur));
      }

      /// Extract a node.
      node_type
      extract(const _Key& __k)
      {
	node_type __nh;
	__hash_code __code = this->_M_hash_code(__k);
	std::size_t __bkt = _M_bucket_index(__code);
	if (__node_base_ptr __prev_node = _M_find_before_node(__bkt, __k, __code))
	  __nh = _M_extract_node(__bkt, __prev_node);
	return __nh;
      }

      /// Merge from a compatible container into one with unique keys.
      template<typename _Compatible_Hashtable>
	void
	_M_merge_unique(_Compatible_Hashtable& __src)
	{
	  static_assert(is_same_v<typename _Compatible_Hashtable::node_type,
	      node_type>, "Node types are compatible");
	  __glibcxx_assert(get_allocator() == __src.get_allocator());

	  auto __n_elt = __src.size();
	  for (auto __i = __src.cbegin(), __end = __src.cend(); __i != __end;)
	    {
	      auto __pos = __i++;
	      const key_type& __k = _ExtractKey{}(*__pos);
	      __hash_code __code
		= this->_M_hash_code(__src.hash_function(), *__pos._M_cur);
	      size_type __bkt = _M_bucket_index(__code);
	      if (_M_find_node(__bkt, __k, __code) == nullptr)
		{
		  auto __nh = __src.extract(__pos);
		  _M_insert_unique_node(__bkt, __code, __nh._M_ptr, __n_elt);
		  __nh._M_ptr = nullptr;
		  __n_elt = 1;
		}
	      else if (__n_elt != 1)
		--__n_elt;
	    }
	}

      /// Merge from a compatible container into one with equivalent keys.
      template<typename _Compatible_Hashtable>
	void
	_M_merge_multi(_Compatible_Hashtable& __src)
	{
	  static_assert(is_same_v<typename _Compatible_Hashtable::node_type,
	      node_type>, "Node types are compatible");
	  __glibcxx_assert(get_allocator() == __src.get_allocator());

	  __node_ptr __hint = nullptr;
	  this->reserve(size() + __src.size());
	  for (auto __i = __src.cbegin(), __end = __src.cend(); __i != __end;)
	    {
	      auto __pos = __i++;
	      __hash_code __code
		= this->_M_hash_code(__src.hash_function(), *__pos._M_cur);
	      auto __nh = __src.extract(__pos);
	      __hint = _M_insert_multi_node(__hint, __code, __nh._M_ptr)._M_cur;
	      __nh._M_ptr = nullptr;
	    }
	}
#endif // C++17

    private:
      // Helper rehash method used when keys are unique.
      void _M_rehash_aux(size_type __bkt_count, true_type __uks);

      // Helper rehash method used when keys can be non-unique.
      void _M_rehash_aux(size_type __bkt_count, false_type __uks);

      // Unconditionally change size of bucket array to n, restore
      // hash policy state to __state on exception.
      void _M_rehash(size_type __bkt_count, const __rehash_state& __state);
    };

  // Definitions of class template _Hashtable's out-of-line member functions.
  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    auto
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    _M_bucket_begin(size_type __bkt) const
    -> __node_ptr
    {
      __node_base_ptr __n = _M_buckets[__bkt];
      return __n ? static_cast<__node_ptr>(__n->_M_nxt) : nullptr;
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    _Hashtable(size_type __bkt_count_hint,
	       const _Hash& __h, const _Equal& __eq, const allocator_type& __a)
    : _Hashtable(__h, __eq, __a)
    {
      auto __bkt_count = _M_rehash_policy._M_next_bkt(__bkt_count_hint);
      if (__bkt_count > _M_bucket_count)
	{
	  _M_buckets = _M_allocate_buckets(__bkt_count);
	  _M_bucket_count = __bkt_count;
	}
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    template<typename _InputIterator>
      _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
		 _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
      _Hashtable(_InputIterator __f, _InputIterator __l,
		 size_type __bkt_count_hint,
		 const _Hash& __h, const _Equal& __eq,
		 const allocator_type& __a, true_type /* __uks */)
      : _Hashtable(__bkt_count_hint, __h, __eq, __a)
      {
	for (; __f != __l; ++__f)
	  this->insert(*__f);
      }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    template<typename _InputIterator>
      _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
		 _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
      _Hashtable(_InputIterator __f, _InputIterator __l,
		 size_type __bkt_count_hint,
		 const _Hash& __h, const _Equal& __eq,
		 const allocator_type& __a, false_type /* __uks */)
      : _Hashtable(__h, __eq, __a)
      {
	auto __nb_elems = __detail::__distance_fw(__f, __l);
	auto __bkt_count =
	  _M_rehash_policy._M_next_bkt(
	    std::max(_M_rehash_policy._M_bkt_for_elements(__nb_elems),
		     __bkt_count_hint));

	if (__bkt_count > _M_bucket_count)
	  {
	    _M_buckets = _M_allocate_buckets(__bkt_count);
	    _M_bucket_count = __bkt_count;
	  }

	for (; __f != __l; ++__f)
	  this->insert(*__f);
      }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    auto
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    operator=(const _Hashtable& __ht)
    -> _Hashtable&
    {
      if (&__ht == this)
	return *this;

      if (__node_alloc_traits::_S_propagate_on_copy_assign())
	{
	  auto& __this_alloc = this->_M_node_allocator();
	  auto& __that_alloc = __ht._M_node_allocator();
	  if (!__node_alloc_traits::_S_always_equal()
	      && __this_alloc != __that_alloc)
	    {
	      // Replacement allocator cannot free existing storage.
	      this->_M_deallocate_nodes(_M_begin());
	      _M_before_begin._M_nxt = nullptr;
	      _M_deallocate_buckets();
	      _M_buckets = nullptr;
	      std::__alloc_on_copy(__this_alloc, __that_alloc);
	      __hashtable_base::operator=(__ht);
	      _M_bucket_count = __ht._M_bucket_count;
	      _M_element_count = __ht._M_element_count;
	      _M_rehash_policy = __ht._M_rehash_policy;
	      __alloc_node_gen_t __alloc_node_gen(*this);
	      __try
		{
		  _M_assign(__ht, __alloc_node_gen);
		}
	      __catch(...)
		{
		  // _M_assign took care of deallocating all memory. Now we
		  // must make sure this instance remains in a usable state.
		  _M_reset();
		  __throw_exception_again;
		}
	      return *this;
	    }
	  std::__alloc_on_copy(__this_alloc, __that_alloc);
	}

      // Reuse allocated buckets and nodes.
      _M_assign_elements(__ht);
      return *this;
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    template<typename _Ht>
      void
      _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
		 _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
      _M_assign_elements(_Ht&& __ht)
      {
	__buckets_ptr __former_buckets = nullptr;
	std::size_t __former_bucket_count = _M_bucket_count;
	const __rehash_state& __former_state = _M_rehash_policy._M_state();

	if (_M_bucket_count != __ht._M_bucket_count)
	  {
	    __former_buckets = _M_buckets;
	    _M_buckets = _M_allocate_buckets(__ht._M_bucket_count);
	    _M_bucket_count = __ht._M_bucket_count;
	  }
	else
	  __builtin_memset(_M_buckets, 0,
			   _M_bucket_count * sizeof(__node_base_ptr));

	__try
	  {
	    __hashtable_base::operator=(std::forward<_Ht>(__ht));
	    _M_element_count = __ht._M_element_count;
	    _M_rehash_policy = __ht._M_rehash_policy;
	    __reuse_or_alloc_node_gen_t __roan(_M_begin(), *this);
	    _M_before_begin._M_nxt = nullptr;
	    _M_assign(std::forward<_Ht>(__ht), __roan);
	    if (__former_buckets)
	      _M_deallocate_buckets(__former_buckets, __former_bucket_count);
	  }
	__catch(...)
	  {
	    if (__former_buckets)
	      {
		// Restore previous buckets.
		_M_deallocate_buckets();
		_M_rehash_policy._M_reset(__former_state);
		_M_buckets = __former_buckets;
		_M_bucket_count = __former_bucket_count;
	      }
	    __builtin_memset(_M_buckets, 0,
			     _M_bucket_count * sizeof(__node_base_ptr));
	    __throw_exception_again;
	  }
      }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    template<typename _Ht, typename _NodeGenerator>
      void
      _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
		 _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
      _M_assign(_Ht&& __ht, const _NodeGenerator& __node_gen)
      {
	__buckets_ptr __buckets = nullptr;
	if (!_M_buckets)
	  _M_buckets = __buckets = _M_allocate_buckets(_M_bucket_count);

	__try
	  {
	    if (!__ht._M_before_begin._M_nxt)
	      return;

	    // First deal with the special first node pointed to by
	    // _M_before_begin.
	    __node_ptr __ht_n = __ht._M_begin();
	    __node_ptr __this_n
	      = __node_gen(__fwd_value_for<_Ht>(__ht_n->_M_v()));
	    this->_M_copy_code(*__this_n, *__ht_n);
	    _M_update_bbegin(__this_n);

	    // Then deal with other nodes.
	    __node_ptr __prev_n = __this_n;
	    for (__ht_n = __ht_n->_M_next(); __ht_n; __ht_n = __ht_n->_M_next())
	      {
		__this_n = __node_gen(__fwd_value_for<_Ht>(__ht_n->_M_v()));
		__prev_n->_M_nxt = __this_n;
		this->_M_copy_code(*__this_n, *__ht_n);
		size_type __bkt = _M_bucket_index(*__this_n);
		if (!_M_buckets[__bkt])
		  _M_buckets[__bkt] = __prev_n;
		__prev_n = __this_n;
	      }
	  }
	__catch(...)
	  {
	    clear();
	    if (__buckets)
	      _M_deallocate_buckets();
	    __throw_exception_again;
	  }
      }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    void
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    _M_reset() noexcept
    {
      _M_rehash_policy._M_reset();
      _M_bucket_count = 1;
      _M_single_bucket = nullptr;
      _M_buckets = &_M_single_bucket;
      _M_before_begin._M_nxt = nullptr;
      _M_element_count = 0;
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    void
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    _M_move_assign(_Hashtable&& __ht, true_type)
    {
      if (__builtin_expect(std::__addressof(__ht) == this, false))
	return;

      this->_M_deallocate_nodes(_M_begin());
      _M_deallocate_buckets();
      __hashtable_base::operator=(std::move(__ht));
      _M_rehash_policy = __ht._M_rehash_policy;
      if (!__ht._M_uses_single_bucket())
	_M_buckets = __ht._M_buckets;
      else
	{
	  _M_buckets = &_M_single_bucket;
	  _M_single_bucket = __ht._M_single_bucket;
	}

      _M_bucket_count = __ht._M_bucket_count;
      _M_before_begin._M_nxt = __ht._M_before_begin._M_nxt;
      _M_element_count = __ht._M_element_count;
      std::__alloc_on_move(this->_M_node_allocator(), __ht._M_node_allocator());

      // Fix bucket containing the _M_before_begin pointer that can't be moved.
      _M_update_bbegin();
      __ht._M_reset();
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    void
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    _M_move_assign(_Hashtable&& __ht, false_type)
    {
      if (__ht._M_node_allocator() == this->_M_node_allocator())
	_M_move_assign(std::move(__ht), true_type{});
      else
	{
	  // Can't move memory, move elements then.
	  _M_assign_elements(std::move(__ht));
	  __ht.clear();
	}
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    _Hashtable(const _Hashtable& __ht)
    : __hashtable_base(__ht),
      __map_base(__ht),
      __rehash_base(__ht),
      __hashtable_alloc(
	__node_alloc_traits::_S_select_on_copy(__ht._M_node_allocator())),
      __enable_default_ctor(__ht),
      _M_buckets(nullptr),
      _M_bucket_count(__ht._M_bucket_count),
      _M_element_count(__ht._M_element_count),
      _M_rehash_policy(__ht._M_rehash_policy)
    {
      __alloc_node_gen_t __alloc_node_gen(*this);
      _M_assign(__ht, __alloc_node_gen);
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    _Hashtable(_Hashtable&& __ht, __node_alloc_type&& __a,
	       true_type /* alloc always equal */)
    noexcept(_S_nothrow_move())
    : __hashtable_base(__ht),
      __map_base(__ht),
      __rehash_base(__ht),
      __hashtable_alloc(std::move(__a)),
      __enable_default_ctor(__ht),
      _M_buckets(__ht._M_buckets),
      _M_bucket_count(__ht._M_bucket_count),
      _M_before_begin(__ht._M_before_begin._M_nxt),
      _M_element_count(__ht._M_element_count),
      _M_rehash_policy(__ht._M_rehash_policy)
    {
      // Update buckets if __ht is using its single bucket.
      if (__ht._M_uses_single_bucket())
	{
	  _M_buckets = &_M_single_bucket;
	  _M_single_bucket = __ht._M_single_bucket;
	}

      // Fix bucket containing the _M_before_begin pointer that can't be moved.
      _M_update_bbegin();

      __ht._M_reset();
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    _Hashtable(const _Hashtable& __ht, const allocator_type& __a)
    : __hashtable_base(__ht),
      __map_base(__ht),
      __rehash_base(__ht),
      __hashtable_alloc(__node_alloc_type(__a)),
      __enable_default_ctor(__ht),
      _M_buckets(),
      _M_bucket_count(__ht._M_bucket_count),
      _M_element_count(__ht._M_element_count),
      _M_rehash_policy(__ht._M_rehash_policy)
    {
      __alloc_node_gen_t __alloc_node_gen(*this);
      _M_assign(__ht, __alloc_node_gen);
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    _Hashtable(_Hashtable&& __ht, __node_alloc_type&& __a,
	       false_type /* alloc always equal */)
    : __hashtable_base(__ht),
      __map_base(__ht),
      __rehash_base(__ht),
      __hashtable_alloc(std::move(__a)),
      __enable_default_ctor(__ht),
      _M_buckets(nullptr),
      _M_bucket_count(__ht._M_bucket_count),
      _M_element_count(__ht._M_element_count),
      _M_rehash_policy(__ht._M_rehash_policy)
    {
      if (__ht._M_node_allocator() == this->_M_node_allocator())
	{
	  if (__ht._M_uses_single_bucket())
	    {
	      _M_buckets = &_M_single_bucket;
	      _M_single_bucket = __ht._M_single_bucket;
	    }
	  else
	    _M_buckets = __ht._M_buckets;

	  // Fix bucket containing the _M_before_begin pointer that can't be
	  // moved.
	  _M_update_bbegin(__ht._M_begin());

	  __ht._M_reset();
	}
      else
	{
	  __alloc_node_gen_t __alloc_gen(*this);

	  using _Fwd_Ht = __conditional_t<
	    __move_if_noexcept_cond<value_type>::value,
	    const _Hashtable&, _Hashtable&&>;
	  _M_assign(std::forward<_Fwd_Ht>(__ht), __alloc_gen);
	  __ht.clear();
	}
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    ~_Hashtable() noexcept
    {
      // Getting a bucket index from a node shall not throw because it is used
      // in methods (erase, swap...) that shall not throw. Need a complete
      // type to check this, so do it in the destructor not at class scope.
      static_assert(noexcept(declval<const __hash_code_base_access&>()
			._M_bucket_index(declval<const __node_value_type&>(),
					 (std::size_t)0)),
		    "Cache the hash code or qualify your functors involved"
		    " in hash code and bucket index computation with noexcept");

      clear();
      _M_deallocate_buckets();
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    void
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    swap(_Hashtable& __x)
    noexcept(__and_<__is_nothrow_swappable<_Hash>,
			__is_nothrow_swappable<_Equal>>::value)
    {
      // The only base class with member variables is hash_code_base.
      // We define _Hash_code_base::_M_swap because different
      // specializations have different members.
      this->_M_swap(__x);

      std::__alloc_on_swap(this->_M_node_allocator(), __x._M_node_allocator());
      std::swap(_M_rehash_policy, __x._M_rehash_policy);

      // Deal properly with potentially moved instances.
      if (this->_M_uses_single_bucket())
	{
	  if (!__x._M_uses_single_bucket())
	    {
	      _M_buckets = __x._M_buckets;
	      __x._M_buckets = &__x._M_single_bucket;
	    }
	}
      else if (__x._M_uses_single_bucket())
	{
	  __x._M_buckets = _M_buckets;
	  _M_buckets = &_M_single_bucket;
	}	
      else
	std::swap(_M_buckets, __x._M_buckets);

      std::swap(_M_bucket_count, __x._M_bucket_count);
      std::swap(_M_before_begin._M_nxt, __x._M_before_begin._M_nxt);
      std::swap(_M_element_count, __x._M_element_count);
      std::swap(_M_single_bucket, __x._M_single_bucket);

      // Fix buckets containing the _M_before_begin pointers that can't be
      // swapped.
      _M_update_bbegin();
      __x._M_update_bbegin();
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    auto
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    find(const key_type& __k)
    -> iterator
    {
      if (size() <= __small_size_threshold())
	{
	  for (auto __it = begin(); __it != end(); ++__it)
	    if (this->_M_key_equals(__k, *__it._M_cur))
	      return __it;
	  return end();
	}

      __hash_code __code = this->_M_hash_code(__k);
      std::size_t __bkt = _M_bucket_index(__code);
      return iterator(_M_find_node(__bkt, __k, __code));
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    auto
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    find(const key_type& __k) const
    -> const_iterator
    {
      if (size() <= __small_size_threshold())
	{
	  for (auto __it = begin(); __it != end(); ++__it)
	    if (this->_M_key_equals(__k, *__it._M_cur))
	      return __it;
	  return end();
	}

      __hash_code __code = this->_M_hash_code(__k);
      std::size_t __bkt = _M_bucket_index(__code);
      return const_iterator(_M_find_node(__bkt, __k, __code));
    }

#if __cplusplus > 201703L
  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    template<typename _Kt, typename, typename>
      auto
      _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
		 _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
      _M_find_tr(const _Kt& __k)
      -> iterator
      {
	__hash_code __code = this->_M_hash_code_tr(__k);
	std::size_t __bkt = _M_bucket_index(__code);
	return iterator(_M_find_node_tr(__bkt, __k, __code));
      }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    template<typename _Kt, typename, typename>
      auto
      _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
		 _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
      _M_find_tr(const _Kt& __k) const
      -> const_iterator
      {
	__hash_code __code = this->_M_hash_code_tr(__k);
	std::size_t __bkt = _M_bucket_index(__code);
	return const_iterator(_M_find_node_tr(__bkt, __k, __code));
      }
#endif

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    auto
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    count(const key_type& __k) const
    -> size_type
    {
      auto __it = find(__k);
      if (!__it._M_cur)
	return 0;

      if (__unique_keys::value)
	return 1;

      // All equivalent values are next to each other, if we find a
      // non-equivalent value after an equivalent one it means that we won't
      // find any new equivalent value.
      size_type __result = 1;
      for (auto __ref = __it++;
	   __it._M_cur && this->_M_node_equals(*__ref._M_cur, *__it._M_cur);
	   ++__it)
	++__result;

      return __result;
    }

#if __cplusplus > 201703L
  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    template<typename _Kt, typename, typename>
      auto
      _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
		 _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
      _M_count_tr(const _Kt& __k) const
      -> size_type
      {
	__hash_code __code = this->_M_hash_code_tr(__k);
	std::size_t __bkt = _M_bucket_index(__code);
	auto __n = _M_find_node_tr(__bkt, __k, __code);
	if (!__n)
	  return 0;

	// All equivalent values are next to each other, if we find a
	// non-equivalent value after an equivalent one it means that we won't
	// find any new equivalent value.
	iterator __it(__n);
	size_type __result = 1;
	for (++__it;
	     __it._M_cur && this->_M_equals_tr(__k, __code, *__it._M_cur);
	     ++__it)
	  ++__result;

	return __result;
      }
#endif

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    auto
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    equal_range(const key_type& __k)
    -> pair<iterator, iterator>
    {
      auto __ite = find(__k);
      if (!__ite._M_cur)
	return { __ite, __ite };

      auto __beg = __ite++;
      if (__unique_keys::value)
	return { __beg, __ite };

      // All equivalent values are next to each other, if we find a
      // non-equivalent value after an equivalent one it means that we won't
      // find any new equivalent value.
      while (__ite._M_cur && this->_M_node_equals(*__beg._M_cur, *__ite._M_cur))
	++__ite;

      return { __beg, __ite };
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    auto
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    equal_range(const key_type& __k) const
    -> pair<const_iterator, const_iterator>
    {
      auto __ite = find(__k);
      if (!__ite._M_cur)
	return { __ite, __ite };

      auto __beg = __ite++;
      if (__unique_keys::value)
	return { __beg, __ite };

      // All equivalent values are next to each other, if we find a
      // non-equivalent value after an equivalent one it means that we won't
      // find any new equivalent value.
      while (__ite._M_cur && this->_M_node_equals(*__beg._M_cur, *__ite._M_cur))
	++__ite;

      return { __beg, __ite };
    }

#if __cplusplus > 201703L
  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    template<typename _Kt, typename, typename>
      auto
      _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
		 _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
      _M_equal_range_tr(const _Kt& __k)
      -> pair<iterator, iterator>
      {
	__hash_code __code = this->_M_hash_code_tr(__k);
	std::size_t __bkt = _M_bucket_index(__code);
	auto __n = _M_find_node_tr(__bkt, __k, __code);
	iterator __ite(__n);
	if (!__n)
	  return { __ite, __ite };

	// All equivalent values are next to each other, if we find a
	// non-equivalent value after an equivalent one it means that we won't
	// find any new equivalent value.
	auto __beg = __ite++;
	while (__ite._M_cur && this->_M_equals_tr(__k, __code, *__ite._M_cur))
	  ++__ite;

	return { __beg, __ite };
      }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    template<typename _Kt, typename, typename>
      auto
      _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
		 _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
      _M_equal_range_tr(const _Kt& __k) const
      -> pair<const_iterator, const_iterator>
      {
	__hash_code __code = this->_M_hash_code_tr(__k);
	std::size_t __bkt = _M_bucket_index(__code);
	auto __n = _M_find_node_tr(__bkt, __k, __code);
	const_iterator __ite(__n);
	if (!__n)
	  return { __ite, __ite };

	// All equivalent values are next to each other, if we find a
	// non-equivalent value after an equivalent one it means that we won't
	// find any new equivalent value.
	auto __beg = __ite++;
	while (__ite._M_cur && this->_M_equals_tr(__k, __code, *__ite._M_cur))
	  ++__ite;

	return { __beg, __ite };
      }
#endif

  // Find the node before the one whose key compares equal to k.
  // Return nullptr if no node is found.
  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    auto
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    _M_find_before_node(const key_type& __k)
    -> __node_base_ptr
    {
      __node_base_ptr __prev_p = &_M_before_begin;
      if (!__prev_p->_M_nxt)
	return nullptr;

      for (__node_ptr __p = static_cast<__node_ptr>(__prev_p->_M_nxt);
	   __p != nullptr;
	   __p = __p->_M_next())
	{
	  if (this->_M_key_equals(__k, *__p))
	    return __prev_p;

	  __prev_p = __p;
	}

      return nullptr;
    }

  // Find the node before the one whose key compares equal to k in the bucket
  // bkt. Return nullptr if no node is found.
  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    auto
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    _M_find_before_node(size_type __bkt, const key_type& __k,
			__hash_code __code) const
    -> __node_base_ptr
    {
      __node_base_ptr __prev_p = _M_buckets[__bkt];
      if (!__prev_p)
	return nullptr;

      for (__node_ptr __p = static_cast<__node_ptr>(__prev_p->_M_nxt);;
	   __p = __p->_M_next())
	{
	  if (this->_M_equals(__k, __code, *__p))
	    return __prev_p;

	  if (!__p->_M_nxt || _M_bucket_index(*__p->_M_next()) != __bkt)
	    break;
	  __prev_p = __p;
	}

      return nullptr;
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    template<typename _Kt>
      auto
      _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
		 _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
      _M_find_before_node_tr(size_type __bkt, const _Kt& __k,
			     __hash_code __code) const
      -> __node_base_ptr
      {
	__node_base_ptr __prev_p = _M_buckets[__bkt];
	if (!__prev_p)
	  return nullptr;

	for (__node_ptr __p = static_cast<__node_ptr>(__prev_p->_M_nxt);;
	     __p = __p->_M_next())
	  {
	    if (this->_M_equals_tr(__k, __code, *__p))
	      return __prev_p;

	    if (!__p->_M_nxt || _M_bucket_index(*__p->_M_next()) != __bkt)
	      break;
	    __prev_p = __p;
	  }

	return nullptr;
      }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    void
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    _M_insert_bucket_begin(size_type __bkt, __node_ptr __node)
    {
      if (_M_buckets[__bkt])
	{
	  // Bucket is not empty, we just need to insert the new node
	  // after the bucket before begin.
	  __node->_M_nxt = _M_buckets[__bkt]->_M_nxt;
	  _M_buckets[__bkt]->_M_nxt = __node;
	}
      else
	{
	  // The bucket is empty, the new node is inserted at the
	  // beginning of the singly-linked list and the bucket will
	  // contain _M_before_begin pointer.
	  __node->_M_nxt = _M_before_begin._M_nxt;
	  _M_before_begin._M_nxt = __node;

	  if (__node->_M_nxt)
	    // We must update former begin bucket that is pointing to
	    // _M_before_begin.
	    _M_buckets[_M_bucket_index(*__node->_M_next())] = __node;

	  _M_buckets[__bkt] = &_M_before_begin;
	}
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    void
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    _M_remove_bucket_begin(size_type __bkt, __node_ptr __next,
			   size_type __next_bkt)
    {
      if (!__next || __next_bkt != __bkt)
	{
	  // Bucket is now empty
	  // First update next bucket if any
	  if (__next)
	    _M_buckets[__next_bkt] = _M_buckets[__bkt];

	  // Second update before begin node if necessary
	  if (&_M_before_begin == _M_buckets[__bkt])
	    _M_before_begin._M_nxt = __next;
	  _M_buckets[__bkt] = nullptr;
	}
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    auto
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    _M_get_previous_node(size_type __bkt, __node_ptr __n)
    -> __node_base_ptr
    {
      __node_base_ptr __prev_n = _M_buckets[__bkt];
      while (__prev_n->_M_nxt != __n)
	__prev_n = __prev_n->_M_nxt;
      return __prev_n;
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    template<typename... _Args>
      auto
      _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
		 _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
      _M_emplace(true_type /* __uks */, _Args&&... __args)
      -> pair<iterator, bool>
      {
	// First build the node to get access to the hash code
	_Scoped_node __node { this, std::forward<_Args>(__args)...  };
	const key_type& __k = _ExtractKey{}(__node._M_node->_M_v());
	if (size() <= __small_size_threshold())
	  {
	    for (auto __it = begin(); __it != end(); ++__it)
	      if (this->_M_key_equals(__k, *__it._M_cur))
		// There is already an equivalent node, no insertion
		return { __it, false };
	  }

	__hash_code __code = this->_M_hash_code(__k);
	size_type __bkt = _M_bucket_index(__code);
	if (size() > __small_size_threshold())
	  if (__node_ptr __p = _M_find_node(__bkt, __k, __code))
	    // There is already an equivalent node, no insertion
	    return { iterator(__p), false };

	// Insert the node
	auto __pos = _M_insert_unique_node(__bkt, __code, __node._M_node);
	__node._M_node = nullptr;
	return { __pos, true };
      }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    template<typename... _Args>
      auto
      _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
		 _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
      _M_emplace(const_iterator __hint, false_type /* __uks */,
		 _Args&&... __args)
      -> iterator
      {
	// First build the node to get its hash code.
	_Scoped_node __node { this, std::forward<_Args>(__args)...  };
	const key_type& __k = _ExtractKey{}(__node._M_node->_M_v());

	auto __res = this->_M_compute_hash_code(__hint, __k);
	auto __pos
	  = _M_insert_multi_node(__res.first._M_cur, __res.second,
				 __node._M_node);
	__node._M_node = nullptr;
	return __pos;
      }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    auto
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    _M_compute_hash_code(const_iterator __hint, const key_type& __k) const
    -> pair<const_iterator, __hash_code>
    {
      if (size() <= __small_size_threshold())
	{
	  if (__hint != cend())
	    {
	      for (auto __it = __hint; __it != cend(); ++__it)
		if (this->_M_key_equals(__k, *__it._M_cur))
		  return { __it, this->_M_hash_code(*__it._M_cur) };
	    }

	  for (auto __it = cbegin(); __it != __hint; ++__it)
	    if (this->_M_key_equals(__k, *__it._M_cur))
	      return { __it, this->_M_hash_code(*__it._M_cur) };
	}

      return { __hint, this->_M_hash_code(__k) };
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    auto
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    _M_insert_unique_node(size_type __bkt, __hash_code __code,
			  __node_ptr __node, size_type __n_elt)
    -> iterator
    {
      const __rehash_state& __saved_state = _M_rehash_policy._M_state();
      std::pair<bool, std::size_t> __do_rehash
	= _M_rehash_policy._M_need_rehash(_M_bucket_count, _M_element_count,
					  __n_elt);

      if (__do_rehash.first)
	{
	  _M_rehash(__do_rehash.second, __saved_state);
	  __bkt = _M_bucket_index(__code);
	}

      this->_M_store_code(*__node, __code);

      // Always insert at the beginning of the bucket.
      _M_insert_bucket_begin(__bkt, __node);
      ++_M_element_count;
      return iterator(__node);
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    auto
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    _M_insert_multi_node(__node_ptr __hint,
			 __hash_code __code, __node_ptr __node)
    -> iterator
    {
      const __rehash_state& __saved_state = _M_rehash_policy._M_state();
      std::pair<bool, std::size_t> __do_rehash
	= _M_rehash_policy._M_need_rehash(_M_bucket_count, _M_element_count, 1);

      if (__do_rehash.first)
	_M_rehash(__do_rehash.second, __saved_state);

      this->_M_store_code(*__node, __code);
      const key_type& __k = _ExtractKey{}(__node->_M_v());
      size_type __bkt = _M_bucket_index(__code);

      // Find the node before an equivalent one or use hint if it exists and
      // if it is equivalent.
      __node_base_ptr __prev
	= __builtin_expect(__hint != nullptr, false)
	  && this->_M_equals(__k, __code, *__hint)
	    ? __hint
	    : _M_find_before_node(__bkt, __k, __code);

      if (__prev)
	{
	  // Insert after the node before the equivalent one.
	  __node->_M_nxt = __prev->_M_nxt;
	  __prev->_M_nxt = __node;
	  if (__builtin_expect(__prev == __hint, false))
	    // hint might be the last bucket node, in this case we need to
	    // update next bucket.
	    if (__node->_M_nxt
		&& !this->_M_equals(__k, __code, *__node->_M_next()))
	      {
		size_type __next_bkt = _M_bucket_index(*__node->_M_next());
		if (__next_bkt != __bkt)
		  _M_buckets[__next_bkt] = __node;
	      }
	}
      else
	// The inserted node has no equivalent in the hashtable. We must
	// insert the new node at the beginning of the bucket to preserve
	// equivalent elements' relative positions.
	_M_insert_bucket_begin(__bkt, __node);
      ++_M_element_count;
      return iterator(__node);
    }

  // Insert v if no element with its key is already present.
  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    template<typename _Kt, typename _Arg, typename _NodeGenerator>
      auto
      _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
		 _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
      _M_insert_unique(_Kt&& __k, _Arg&& __v,
		       const _NodeGenerator& __node_gen)
      -> pair<iterator, bool>
      {
	if (size() <= __small_size_threshold())
	  for (auto __it = begin(); __it != end(); ++__it)
	    if (this->_M_key_equals_tr(__k, *__it._M_cur))
	      return { __it, false };

	__hash_code __code = this->_M_hash_code_tr(__k);
	size_type __bkt = _M_bucket_index(__code);

	if (size() > __small_size_threshold())
	  if (__node_ptr __node = _M_find_node_tr(__bkt, __k, __code))
	    return { iterator(__node), false };

	_Scoped_node __node {
	  __node_builder_t::_S_build(std::forward<_Kt>(__k),
				     std::forward<_Arg>(__v),
				     __node_gen),
	  this
	};
	auto __pos
	  = _M_insert_unique_node(__bkt, __code, __node._M_node);
	__node._M_node = nullptr;
	return { __pos, true };
      }

  // Insert v unconditionally.
  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    template<typename _Arg, typename _NodeGenerator>
      auto
      _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
		 _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
      _M_insert(const_iterator __hint, _Arg&& __v,
		const _NodeGenerator& __node_gen,
		false_type /* __uks */)
      -> iterator
      {
	// First allocate new node so that we don't do anything if it throws.
	_Scoped_node __node{ __node_gen(std::forward<_Arg>(__v)), this };

	// Second compute the hash code so that we don't rehash if it throws.
	auto __res = this->_M_compute_hash_code(
	  __hint, _ExtractKey{}(__node._M_node->_M_v()));

	auto __pos
	  = _M_insert_multi_node(__res.first._M_cur, __res.second,
				 __node._M_node);
	__node._M_node = nullptr;
	return __pos;
      }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    auto
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    erase(const_iterator __it)
    -> iterator
    {
      __node_ptr __n = __it._M_cur;
      std::size_t __bkt = _M_bucket_index(*__n);

      // Look for previous node to unlink it from the erased one, this
      // is why we need buckets to contain the before begin to make
      // this search fast.
      __node_base_ptr __prev_n = _M_get_previous_node(__bkt, __n);
      return _M_erase(__bkt, __prev_n, __n);
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    auto
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    _M_erase(size_type __bkt, __node_base_ptr __prev_n, __node_ptr __n)
    -> iterator
    {
      if (__prev_n == _M_buckets[__bkt])
	_M_remove_bucket_begin(__bkt, __n->_M_next(),
	  __n->_M_nxt ? _M_bucket_index(*__n->_M_next()) : 0);
      else if (__n->_M_nxt)
	{
	  size_type __next_bkt = _M_bucket_index(*__n->_M_next());
	  if (__next_bkt != __bkt)
	    _M_buckets[__next_bkt] = __prev_n;
	}

      __prev_n->_M_nxt = __n->_M_nxt;
      iterator __result(__n->_M_next());
      this->_M_deallocate_node(__n);
      --_M_element_count;

      return __result;
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    auto
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    _M_erase(true_type /* __uks */, const key_type& __k)
    -> size_type
    {
      __node_base_ptr __prev_n;
      __node_ptr __n;
      std::size_t __bkt;
      if (size() <= __small_size_threshold())
	{
	  __prev_n = _M_find_before_node(__k);
	  if (!__prev_n)
	    return 0;

	  // We found a matching node, erase it.
	  __n = static_cast<__node_ptr>(__prev_n->_M_nxt);
	  __bkt = _M_bucket_index(*__n);
	}
      else
	{
	  __hash_code __code = this->_M_hash_code(__k);
	  __bkt = _M_bucket_index(__code);

	  // Look for the node before the first matching node.
	  __prev_n = _M_find_before_node(__bkt, __k, __code);
	  if (!__prev_n)
	    return 0;

	  // We found a matching node, erase it.
	  __n = static_cast<__node_ptr>(__prev_n->_M_nxt);
	}

      _M_erase(__bkt, __prev_n, __n);
      return 1;
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    auto
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    _M_erase(false_type /* __uks */, const key_type& __k)
    -> size_type
    {
      std::size_t __bkt;
      __node_base_ptr __prev_n;
      __node_ptr __n;
      if (size() <= __small_size_threshold())
	{
	  __prev_n = _M_find_before_node(__k);
	  if (!__prev_n)
	    return 0;

	  // We found a matching node, erase it.
	  __n = static_cast<__node_ptr>(__prev_n->_M_nxt);
	  __bkt = _M_bucket_index(*__n);
	}
      else
	{
	  __hash_code __code = this->_M_hash_code(__k);
	  __bkt = _M_bucket_index(__code);

	  // Look for the node before the first matching node.
	  __prev_n = _M_find_before_node(__bkt, __k, __code);
	  if (!__prev_n)
	    return 0;

	  __n = static_cast<__node_ptr>(__prev_n->_M_nxt);
	}

      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 526. Is it undefined if a function in the standard changes
      // in parameters?
      // We use one loop to find all matching nodes and another to deallocate
      // them so that the key stays valid during the first loop. It might be
      // invalidated indirectly when destroying nodes.
      __node_ptr __n_last = __n->_M_next();
      while (__n_last && this->_M_node_equals(*__n, *__n_last))
	__n_last = __n_last->_M_next();

      std::size_t __n_last_bkt = __n_last ? _M_bucket_index(*__n_last) : __bkt;

      // Deallocate nodes.
      size_type __result = 0;
      do
	{
	  __node_ptr __p = __n->_M_next();
	  this->_M_deallocate_node(__n);
	  __n = __p;
	  ++__result;
	}
      while (__n != __n_last);

      _M_element_count -= __result;
      if (__prev_n == _M_buckets[__bkt])
	_M_remove_bucket_begin(__bkt, __n_last, __n_last_bkt);
      else if (__n_last_bkt != __bkt)
	_M_buckets[__n_last_bkt] = __prev_n;
      __prev_n->_M_nxt = __n_last;
      return __result;
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    auto
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    erase(const_iterator __first, const_iterator __last)
    -> iterator
    {
      __node_ptr __n = __first._M_cur;
      __node_ptr __last_n = __last._M_cur;
      if (__n == __last_n)
	return iterator(__n);

      std::size_t __bkt = _M_bucket_index(*__n);

      __node_base_ptr __prev_n = _M_get_previous_node(__bkt, __n);
      bool __is_bucket_begin = __n == _M_bucket_begin(__bkt);
      std::size_t __n_bkt = __bkt;
      for (;;)
	{
	  do
	    {
	      __node_ptr __tmp = __n;
	      __n = __n->_M_next();
	      this->_M_deallocate_node(__tmp);
	      --_M_element_count;
	      if (!__n)
		break;
	      __n_bkt = _M_bucket_index(*__n);
	    }
	  while (__n != __last_n && __n_bkt == __bkt);
	  if (__is_bucket_begin)
	    _M_remove_bucket_begin(__bkt, __n, __n_bkt);
	  if (__n == __last_n)
	    break;
	  __is_bucket_begin = true;
	  __bkt = __n_bkt;
	}

      if (__n && (__n_bkt != __bkt || __is_bucket_begin))
	_M_buckets[__n_bkt] = __prev_n;
      __prev_n->_M_nxt = __n;
      return iterator(__n);
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    void
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    clear() noexcept
    {
      this->_M_deallocate_nodes(_M_begin());
      __builtin_memset(_M_buckets, 0,
		       _M_bucket_count * sizeof(__node_base_ptr));
      _M_element_count = 0;
      _M_before_begin._M_nxt = nullptr;
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    void
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    rehash(size_type __bkt_count)
    {
      const __rehash_state& __saved_state = _M_rehash_policy._M_state();
      __bkt_count
	= std::max(_M_rehash_policy._M_bkt_for_elements(_M_element_count + 1),
		   __bkt_count);
      __bkt_count = _M_rehash_policy._M_next_bkt(__bkt_count);

      if (__bkt_count != _M_bucket_count)
	_M_rehash(__bkt_count, __saved_state);
      else
	// No rehash, restore previous state to keep it consistent with
	// container state.
	_M_rehash_policy._M_reset(__saved_state);
    }

  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    void
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    _M_rehash(size_type __bkt_count, const __rehash_state& __state)
    {
      __try
	{
	  _M_rehash_aux(__bkt_count, __unique_keys{});
	}
      __catch(...)
	{
	  // A failure here means that buckets allocation failed.  We only
	  // have to restore hash policy previous state.
	  _M_rehash_policy._M_reset(__state);
	  __throw_exception_again;
	}
    }

  // Rehash when there is no equivalent elements.
  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    void
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    _M_rehash_aux(size_type __bkt_count, true_type /* __uks */)
    {
      __buckets_ptr __new_buckets = _M_allocate_buckets(__bkt_count);
      __node_ptr __p = _M_begin();
      _M_before_begin._M_nxt = nullptr;
      std::size_t __bbegin_bkt = 0;
      while (__p)
	{
	  __node_ptr __next = __p->_M_next();
	  std::size_t __bkt
	    = __hash_code_base::_M_bucket_index(*__p, __bkt_count);
	  if (!__new_buckets[__bkt])
	    {
	      __p->_M_nxt = _M_before_begin._M_nxt;
	      _M_before_begin._M_nxt = __p;
	      __new_buckets[__bkt] = &_M_before_begin;
	      if (__p->_M_nxt)
		__new_buckets[__bbegin_bkt] = __p;
	      __bbegin_bkt = __bkt;
	    }
	  else
	    {
	      __p->_M_nxt = __new_buckets[__bkt]->_M_nxt;
	      __new_buckets[__bkt]->_M_nxt = __p;
	    }

	  __p = __next;
	}

      _M_deallocate_buckets();
      _M_bucket_count = __bkt_count;
      _M_buckets = __new_buckets;
    }

  // Rehash when there can be equivalent elements, preserve their relative
  // order.
  template<typename _Key, typename _Value, typename _Alloc,
	   typename _ExtractKey, typename _Equal,
	   typename _Hash, typename _RangeHash, typename _Unused,
	   typename _RehashPolicy, typename _Traits>
    void
    _Hashtable<_Key, _Value, _Alloc, _ExtractKey, _Equal,
	       _Hash, _RangeHash, _Unused, _RehashPolicy, _Traits>::
    _M_rehash_aux(size_type __bkt_count, false_type /* __uks */)
    {
      __buckets_ptr __new_buckets = _M_allocate_buckets(__bkt_count);
      __node_ptr __p = _M_begin();
      _M_before_begin._M_nxt = nullptr;
      std::size_t __bbegin_bkt = 0;
      std::size_t __prev_bkt = 0;
      __node_ptr __prev_p = nullptr;
      bool __check_bucket = false;

      while (__p)
	{
	  __node_ptr __next = __p->_M_next();
	  std::size_t __bkt
	    = __hash_code_base::_M_bucket_index(*__p, __bkt_count);

	  if (__prev_p && __prev_bkt == __bkt)
	    {
	      // Previous insert was already in this bucket, we insert after
	      // the previously inserted one to preserve equivalent elements
	      // relative order.
	      __p->_M_nxt = __prev_p->_M_nxt;
	      __prev_p->_M_nxt = __p;

	      // Inserting after a node in a bucket require to check that we
	      // haven't change the bucket last node, in this case next
	      // bucket containing its before begin node must be updated. We
	      // schedule a check as soon as we move out of the sequence of
	      // equivalent nodes to limit the number of checks.
	      __check_bucket = true;
	    }
	  else
	    {
	      if (__check_bucket)
		{
		  // Check if we shall update the next bucket because of
		  // insertions into __prev_bkt bucket.
		  if (__prev_p->_M_nxt)
		    {
		      std::size_t __next_bkt
			= __hash_code_base::_M_bucket_index(
			  *__prev_p->_M_next(), __bkt_count);
		      if (__next_bkt != __prev_bkt)
			__new_buckets[__next_bkt] = __prev_p;
		    }
		  __check_bucket = false;
		}

	      if (!__new_buckets[__bkt])
		{
		  __p->_M_nxt = _M_before_begin._M_nxt;
		  _M_before_begin._M_nxt = __p;
		  __new_buckets[__bkt] = &_M_before_begin;
		  if (__p->_M_nxt)
		    __new_buckets[__bbegin_bkt] = __p;
		  __bbegin_bkt = __bkt;
		}
	      else
		{
		  __p->_M_nxt = __new_buckets[__bkt]->_M_nxt;
		  __new_buckets[__bkt]->_M_nxt = __p;
		}
	    }
	  __prev_p = __p;
	  __prev_bkt = __bkt;
	  __p = __next;
	}

      if (__check_bucket && __prev_p->_M_nxt)
	{
	  std::size_t __next_bkt
	    = __hash_code_base::_M_bucket_index(*__prev_p->_M_next(),
						__bkt_count);
	  if (__next_bkt != __prev_bkt)
	    __new_buckets[__next_bkt] = __prev_p;
	}

      _M_deallocate_buckets();
      _M_bucket_count = __bkt_count;
      _M_buckets = __new_buckets;
    }

#if __cplusplus > 201402L
  template<typename, typename, typename> class _Hash_merge_helper { };
#endif // C++17

#if __cpp_deduction_guides >= 201606
  // Used to constrain deduction guides
  template<typename _Hash>
    using _RequireNotAllocatorOrIntegral
      = __enable_if_t<!__or_<is_integral<_Hash>, __is_allocator<_Hash>>::value>;
#endif

/// @endcond
_GLIBCXX_END_NAMESPACE_VERSION
} // namespace std

#endif // _HASHTABLE_H
