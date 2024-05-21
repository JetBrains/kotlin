// shared_ptr atomic access -*- C++ -*-

// Copyright (C) 2014-2022 Free Software Foundation, Inc.
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

/** @file bits/shared_ptr_atomic.h
 *  This is an internal header file, included by other library headers.
 *  Do not attempt to use it directly. @headername{memory}
 */

#ifndef _SHARED_PTR_ATOMIC_H
#define _SHARED_PTR_ATOMIC_H 1

#include <bits/atomic_base.h>

namespace std _GLIBCXX_VISIBILITY(default)
{
_GLIBCXX_BEGIN_NAMESPACE_VERSION

  /**
   * @addtogroup pointer_abstractions
   * @{
   */
  /// @relates shared_ptr @{

  /// @cond undocumented

  struct _Sp_locker
  {
    _Sp_locker(const _Sp_locker&) = delete;
    _Sp_locker& operator=(const _Sp_locker&) = delete;

#ifdef __GTHREADS
    explicit
    _Sp_locker(const void*) noexcept;
    _Sp_locker(const void*, const void*) noexcept;
    ~_Sp_locker();

  private:
    unsigned char _M_key1;
    unsigned char _M_key2;
#else
    explicit _Sp_locker(const void*, const void* = nullptr) { }
#endif
  };

  /// @endcond

  /**
   *  @brief  Report whether shared_ptr atomic operations are lock-free.
   *  @param  __p A non-null pointer to a shared_ptr object.
   *  @return True if atomic access to @c *__p is lock-free, false otherwise.
   *  @{
  */
  template<typename _Tp, _Lock_policy _Lp>
    inline bool
    atomic_is_lock_free(const __shared_ptr<_Tp, _Lp>* __p)
    {
#ifdef __GTHREADS
      return __gthread_active_p() == 0;
#else
      return true;
#endif
    }

  template<typename _Tp>
    inline bool
    atomic_is_lock_free(const shared_ptr<_Tp>* __p)
    { return std::atomic_is_lock_free<_Tp, __default_lock_policy>(__p); }

  /// @}

  /**
   *  @brief  Atomic load for shared_ptr objects.
   *  @param  __p A non-null pointer to a shared_ptr object.
   *  @return @c *__p
   *
   *  The memory order shall not be @c memory_order_release or
   *  @c memory_order_acq_rel.
   *  @{
  */
  template<typename _Tp>
    inline shared_ptr<_Tp>
    atomic_load_explicit(const shared_ptr<_Tp>* __p, memory_order)
    {
      _Sp_locker __lock{__p};
      return *__p;
    }

  template<typename _Tp>
    inline shared_ptr<_Tp>
    atomic_load(const shared_ptr<_Tp>* __p)
    { return std::atomic_load_explicit(__p, memory_order_seq_cst); }

  template<typename _Tp, _Lock_policy _Lp>
    inline __shared_ptr<_Tp, _Lp>
    atomic_load_explicit(const __shared_ptr<_Tp, _Lp>* __p, memory_order)
    {
      _Sp_locker __lock{__p};
      return *__p;
    }

  template<typename _Tp, _Lock_policy _Lp>
    inline __shared_ptr<_Tp, _Lp>
    atomic_load(const __shared_ptr<_Tp, _Lp>* __p)
    { return std::atomic_load_explicit(__p, memory_order_seq_cst); }
  /// @}

  /**
   *  @brief  Atomic store for shared_ptr objects.
   *  @param  __p A non-null pointer to a shared_ptr object.
   *  @param  __r The value to store.
   *
   *  The memory order shall not be @c memory_order_acquire or
   *  @c memory_order_acq_rel.
   *  @{
  */
  template<typename _Tp>
    inline void
    atomic_store_explicit(shared_ptr<_Tp>* __p, shared_ptr<_Tp> __r,
			  memory_order)
    {
      _Sp_locker __lock{__p};
      __p->swap(__r); // use swap so that **__p not destroyed while lock held
    }

  template<typename _Tp>
    inline void
    atomic_store(shared_ptr<_Tp>* __p, shared_ptr<_Tp> __r)
    { std::atomic_store_explicit(__p, std::move(__r), memory_order_seq_cst); }

  template<typename _Tp, _Lock_policy _Lp>
    inline void
    atomic_store_explicit(__shared_ptr<_Tp, _Lp>* __p,
			  __shared_ptr<_Tp, _Lp> __r,
			  memory_order)
    {
      _Sp_locker __lock{__p};
      __p->swap(__r); // use swap so that **__p not destroyed while lock held
    }

  template<typename _Tp, _Lock_policy _Lp>
    inline void
    atomic_store(__shared_ptr<_Tp, _Lp>* __p, __shared_ptr<_Tp, _Lp> __r)
    { std::atomic_store_explicit(__p, std::move(__r), memory_order_seq_cst); }
  /// @}

  /**
   *  @brief  Atomic exchange for shared_ptr objects.
   *  @param  __p A non-null pointer to a shared_ptr object.
   *  @param  __r New value to store in @c *__p.
   *  @return The original value of @c *__p
   *  @{
  */
  template<typename _Tp>
    inline shared_ptr<_Tp>
    atomic_exchange_explicit(shared_ptr<_Tp>* __p, shared_ptr<_Tp> __r,
			     memory_order)
    {
      _Sp_locker __lock{__p};
      __p->swap(__r);
      return __r;
    }

  template<typename _Tp>
    inline shared_ptr<_Tp>
    atomic_exchange(shared_ptr<_Tp>* __p, shared_ptr<_Tp> __r)
    {
      return std::atomic_exchange_explicit(__p, std::move(__r),
					   memory_order_seq_cst);
    }

  template<typename _Tp, _Lock_policy _Lp>
    inline __shared_ptr<_Tp, _Lp>
    atomic_exchange_explicit(__shared_ptr<_Tp, _Lp>* __p,
			     __shared_ptr<_Tp, _Lp> __r,
			     memory_order)
    {
      _Sp_locker __lock{__p};
      __p->swap(__r);
      return __r;
    }

  template<typename _Tp, _Lock_policy _Lp>
    inline __shared_ptr<_Tp, _Lp>
    atomic_exchange(__shared_ptr<_Tp, _Lp>* __p, __shared_ptr<_Tp, _Lp> __r)
    {
      return std::atomic_exchange_explicit(__p, std::move(__r),
					   memory_order_seq_cst);
    }
  /// @}

  /**
   *  @brief  Atomic compare-and-swap for shared_ptr objects.
   *  @param  __p A non-null pointer to a shared_ptr object.
   *  @param  __v A non-null pointer to a shared_ptr object.
   *  @param  __w A non-null pointer to a shared_ptr object.
   *  @return True if @c *__p was equivalent to @c *__v, false otherwise.
   *
   *  The memory order for failure shall not be @c memory_order_release or
   *  @c memory_order_acq_rel, or stronger than the memory order for success.
   *  @{
  */
  template<typename _Tp>
    bool
    atomic_compare_exchange_strong_explicit(shared_ptr<_Tp>* __p,
					    shared_ptr<_Tp>* __v,
					    shared_ptr<_Tp> __w,
					    memory_order,
					    memory_order)
    {
      shared_ptr<_Tp> __x; // goes out of scope after __lock
      _Sp_locker __lock{__p, __v};
      owner_less<shared_ptr<_Tp>> __less;
      if (*__p == *__v && !__less(*__p, *__v) && !__less(*__v, *__p))
	{
	  __x = std::move(*__p);
	  *__p = std::move(__w);
	  return true;
	}
      __x = std::move(*__v);
      *__v = *__p;
      return false;
    }

  template<typename _Tp>
    inline bool
    atomic_compare_exchange_strong(shared_ptr<_Tp>* __p, shared_ptr<_Tp>* __v,
				 shared_ptr<_Tp> __w)
    {
      return std::atomic_compare_exchange_strong_explicit(__p, __v,
	  std::move(__w), memory_order_seq_cst, memory_order_seq_cst);
    }

  template<typename _Tp>
    inline bool
    atomic_compare_exchange_weak_explicit(shared_ptr<_Tp>* __p,
					  shared_ptr<_Tp>* __v,
					  shared_ptr<_Tp> __w,
					  memory_order __success,
					  memory_order __failure)
    {
      return std::atomic_compare_exchange_strong_explicit(__p, __v,
	  std::move(__w), __success, __failure);
    }

  template<typename _Tp>
    inline bool
    atomic_compare_exchange_weak(shared_ptr<_Tp>* __p, shared_ptr<_Tp>* __v,
				 shared_ptr<_Tp> __w)
    {
      return std::atomic_compare_exchange_weak_explicit(__p, __v,
	  std::move(__w), memory_order_seq_cst, memory_order_seq_cst);
    }

  template<typename _Tp, _Lock_policy _Lp>
    bool
    atomic_compare_exchange_strong_explicit(__shared_ptr<_Tp, _Lp>* __p,
					    __shared_ptr<_Tp, _Lp>* __v,
					    __shared_ptr<_Tp, _Lp> __w,
					    memory_order,
					    memory_order)
    {
      __shared_ptr<_Tp, _Lp> __x; // goes out of scope after __lock
      _Sp_locker __lock{__p, __v};
      owner_less<__shared_ptr<_Tp, _Lp>> __less;
      if (*__p == *__v && !__less(*__p, *__v) && !__less(*__v, *__p))
	{
	  __x = std::move(*__p);
	  *__p = std::move(__w);
	  return true;
	}
      __x = std::move(*__v);
      *__v = *__p;
      return false;
    }

  template<typename _Tp, _Lock_policy _Lp>
    inline bool
    atomic_compare_exchange_strong(__shared_ptr<_Tp, _Lp>* __p,
				   __shared_ptr<_Tp, _Lp>* __v,
				   __shared_ptr<_Tp, _Lp> __w)
    {
      return std::atomic_compare_exchange_strong_explicit(__p, __v,
	  std::move(__w), memory_order_seq_cst, memory_order_seq_cst);
    }

  template<typename _Tp, _Lock_policy _Lp>
    inline bool
    atomic_compare_exchange_weak_explicit(__shared_ptr<_Tp, _Lp>* __p,
					  __shared_ptr<_Tp, _Lp>* __v,
					  __shared_ptr<_Tp, _Lp> __w,
					  memory_order __success,
					  memory_order __failure)
    {
      return std::atomic_compare_exchange_strong_explicit(__p, __v,
	  std::move(__w), __success, __failure);
    }

  template<typename _Tp, _Lock_policy _Lp>
    inline bool
    atomic_compare_exchange_weak(__shared_ptr<_Tp, _Lp>* __p,
				 __shared_ptr<_Tp, _Lp>* __v,
				 __shared_ptr<_Tp, _Lp> __w)
    {
      return std::atomic_compare_exchange_weak_explicit(__p, __v,
	  std::move(__w), memory_order_seq_cst, memory_order_seq_cst);
    }
  /// @}

#if __cplusplus >= 202002L
# define __cpp_lib_atomic_shared_ptr 201711L
  template<typename _Tp>
    class atomic;

  template<typename _Up>
    static constexpr bool __is_shared_ptr = false;
  template<typename _Up>
    static constexpr bool __is_shared_ptr<shared_ptr<_Up>> = true;

  template<typename _Tp>
    class _Sp_atomic
    {
      using value_type = _Tp;

      friend class atomic<_Tp>;

      // An atomic version of __shared_count<> and __weak_count<>.
      // Stores a _Sp_counted_base<>* but uses the LSB as a lock.
      struct _Atomic_count
      {
	// Either __shared_count<> or __weak_count<>
	using __count_type = decltype(_Tp::_M_refcount);

	// _Sp_counted_base<>*
	using pointer = decltype(__count_type::_M_pi);

	// Ensure we can use the LSB as the lock bit.
	static_assert(alignof(remove_pointer_t<pointer>) > 1);

	constexpr _Atomic_count() noexcept = default;

	explicit
	_Atomic_count(__count_type&& __c) noexcept
	: _M_val(reinterpret_cast<uintptr_t>(__c._M_pi))
	{
	  __c._M_pi = nullptr;
	}

	~_Atomic_count()
	{
	  auto __val = _M_val.load(memory_order_relaxed);
	  __glibcxx_assert(!(__val & _S_lock_bit));
	  if (auto __pi = reinterpret_cast<pointer>(__val))
	    {
	      if constexpr (__is_shared_ptr<_Tp>)
		__pi->_M_release();
	      else
		__pi->_M_weak_release();
	    }
	}

	_Atomic_count(const _Atomic_count&) = delete;
	_Atomic_count& operator=(const _Atomic_count&) = delete;

	// Precondition: Caller does not hold lock!
	// Returns the raw pointer value without the lock bit set.
	pointer
	lock(memory_order __o) const noexcept
	{
	  // To acquire the lock we flip the LSB from 0 to 1.

	  auto __current = _M_val.load(memory_order_relaxed);
	  while (__current & _S_lock_bit)
	    {
#if __cpp_lib_atomic_wait
	      __detail::__thread_relax();
#endif
	      __current = _M_val.load(memory_order_relaxed);
	    }

	  while (!_M_val.compare_exchange_strong(__current,
						 __current | _S_lock_bit,
						 __o,
						 memory_order_relaxed))
	    {
#if __cpp_lib_atomic_wait
	      __detail::__thread_relax();
#endif
	      __current = __current & ~_S_lock_bit;
	    }
	  return reinterpret_cast<pointer>(__current);
	}

	// Precondition: caller holds lock!
	void
	unlock(memory_order __o) const noexcept
	{
	  _M_val.fetch_sub(1, __o);
	}

	// Swaps the values of *this and __c, and unlocks *this.
	// Precondition: caller holds lock!
	void
	_M_swap_unlock(__count_type& __c, memory_order __o) noexcept
	{
	  if (__o != memory_order_seq_cst)
	    __o = memory_order_release;
	  auto __x = reinterpret_cast<uintptr_t>(__c._M_pi);
	  __x = _M_val.exchange(__x, __o);
	  __c._M_pi = reinterpret_cast<pointer>(__x & ~_S_lock_bit);
	}

#if __cpp_lib_atomic_wait
	// Precondition: caller holds lock!
	void
	_M_wait_unlock(memory_order __o) const noexcept
	{
	  auto __v = _M_val.fetch_sub(1, memory_order_relaxed);
	  _M_val.wait(__v & ~_S_lock_bit, __o);
	}

	void
	notify_one() noexcept
	{
	  _M_val.notify_one();
	}

	void
	notify_all() noexcept
	{
	  _M_val.notify_all();
	}
#endif

      private:
	mutable __atomic_base<uintptr_t> _M_val{0};
	static constexpr uintptr_t _S_lock_bit{1};
      };

      typename _Tp::element_type* _M_ptr = nullptr;
      _Atomic_count _M_refcount;

      static typename _Atomic_count::pointer
      _S_add_ref(typename _Atomic_count::pointer __p)
      {
	if (__p)
	  {
	    if constexpr (__is_shared_ptr<_Tp>)
	      __p->_M_add_ref_copy();
	    else
	      __p->_M_weak_add_ref();
	  }
	return __p;
      }

      constexpr _Sp_atomic() noexcept = default;

      explicit
      _Sp_atomic(value_type __r) noexcept
      : _M_ptr(__r._M_ptr), _M_refcount(std::move(__r._M_refcount))
      { }

      ~_Sp_atomic() = default;

      _Sp_atomic(const _Sp_atomic&) = delete;
      void operator=(const _Sp_atomic&) = delete;

      value_type
      load(memory_order __o) const noexcept
      {
	__glibcxx_assert(__o != memory_order_release
			   && __o != memory_order_acq_rel);
	// Ensure that the correct value of _M_ptr is visible after locking.,
	// by upgrading relaxed or consume to acquire.
	if (__o != memory_order_seq_cst)
	  __o = memory_order_acquire;

	value_type __ret;
	auto __pi = _M_refcount.lock(__o);
	__ret._M_ptr = _M_ptr;
	__ret._M_refcount._M_pi = _S_add_ref(__pi);
	_M_refcount.unlock(memory_order_relaxed);
	return __ret;
      }

      void
      swap(value_type& __r, memory_order __o) noexcept
      {
	_M_refcount.lock(memory_order_acquire);
	std::swap(_M_ptr, __r._M_ptr);
	_M_refcount._M_swap_unlock(__r._M_refcount, __o);
      }

      bool
      compare_exchange_strong(value_type& __expected, value_type __desired,
			      memory_order __o, memory_order __o2) noexcept
      {
	bool __result = true;
	auto __pi = _M_refcount.lock(memory_order_acquire);
	if (_M_ptr == __expected._M_ptr
	      && __pi == __expected._M_refcount._M_pi)
	  {
	    _M_ptr = __desired._M_ptr;
	    _M_refcount._M_swap_unlock(__desired._M_refcount, __o);
	  }
	else
	  {
	    _Tp __sink = std::move(__expected);
	    __expected._M_ptr = _M_ptr;
	    __expected._M_refcount._M_pi = _S_add_ref(__pi);
	    _M_refcount.unlock(__o2);
	    __result = false;
	  }
	return __result;
      }

#if __cpp_lib_atomic_wait
      void
      wait(value_type __old, memory_order __o) const noexcept
      {
	auto __pi = _M_refcount.lock(memory_order_acquire);
	if (_M_ptr == __old._M_ptr && __pi == __old._M_refcount._M_pi)
	  _M_refcount._M_wait_unlock(__o);
	else
	  _M_refcount.unlock(memory_order_relaxed);
      }

      void
      notify_one() noexcept
      {
	_M_refcount.notify_one();
      }

      void
      notify_all() noexcept
      {
	_M_refcount.notify_all();
      }
#endif
    };

  template<typename _Tp>
    class atomic<shared_ptr<_Tp>>
    {
    public:
      using value_type = shared_ptr<_Tp>;

      static constexpr bool is_always_lock_free = false;

      bool
      is_lock_free() const noexcept
      { return false; }

      constexpr atomic() noexcept = default;

      // _GLIBCXX_RESOLVE_LIB_DEFECTS
      // 3661. constinit atomic<shared_ptr<T>> a(nullptr); should work
      constexpr atomic(nullptr_t) noexcept : atomic() { }

      atomic(shared_ptr<_Tp> __r) noexcept
      : _M_impl(std::move(__r))
      { }

      atomic(const atomic&) = delete;
      void operator=(const atomic&) = delete;

      shared_ptr<_Tp>
      load(memory_order __o = memory_order_seq_cst) const noexcept
      { return _M_impl.load(__o); }

      operator shared_ptr<_Tp>() const noexcept
      { return _M_impl.load(memory_order_seq_cst); }

      void
      store(shared_ptr<_Tp> __desired,
	    memory_order __o = memory_order_seq_cst) noexcept
      { _M_impl.swap(__desired, __o); }

      void
      operator=(shared_ptr<_Tp> __desired) noexcept
      { _M_impl.swap(__desired, memory_order_seq_cst); }

      shared_ptr<_Tp>
      exchange(shared_ptr<_Tp> __desired,
	       memory_order __o = memory_order_seq_cst) noexcept
      {
	_M_impl.swap(__desired, __o);
	return __desired;
      }

      bool
      compare_exchange_strong(shared_ptr<_Tp>& __expected,
			      shared_ptr<_Tp> __desired,
			      memory_order __o, memory_order __o2) noexcept
      {
	return _M_impl.compare_exchange_strong(__expected, __desired, __o, __o2);
      }

      bool
      compare_exchange_strong(value_type& __expected, value_type __desired,
			      memory_order __o = memory_order_seq_cst) noexcept
      {
	memory_order __o2;
	switch (__o)
	{
	case memory_order_acq_rel:
	  __o2 = memory_order_acquire;
	  break;
	case memory_order_release:
	  __o2 = memory_order_relaxed;
	  break;
	default:
	  __o2 = __o;
	}
	return compare_exchange_strong(__expected, std::move(__desired),
				       __o, __o2);
      }

      bool
      compare_exchange_weak(value_type& __expected, value_type __desired,
			    memory_order __o, memory_order __o2) noexcept
      {
	return compare_exchange_strong(__expected, std::move(__desired),
				       __o, __o2);
      }

      bool
      compare_exchange_weak(value_type& __expected, value_type __desired,
			    memory_order __o = memory_order_seq_cst) noexcept
      {
	return compare_exchange_strong(__expected, std::move(__desired), __o);
      }

#if __cpp_lib_atomic_wait
      void
      wait(value_type __old,
	   memory_order __o = memory_order_seq_cst) const noexcept
      {
	_M_impl.wait(std::move(__old), __o);
      }

      void
      notify_one() noexcept
      {
	_M_impl.notify_one();
      }

      void
      notify_all() noexcept
      {
	_M_impl.notify_all();
      }
#endif

    private:
      _Sp_atomic<shared_ptr<_Tp>> _M_impl;
    };

  template<typename _Tp>
    class atomic<weak_ptr<_Tp>>
    {
    public:
      using value_type = weak_ptr<_Tp>;

      static constexpr bool is_always_lock_free = false;

      bool
      is_lock_free() const noexcept
      { return false; }

      constexpr atomic() noexcept = default;

      atomic(weak_ptr<_Tp> __r) noexcept
     : _M_impl(move(__r))
      { }

      atomic(const atomic&) = delete;
      void operator=(const atomic&) = delete;

      weak_ptr<_Tp>
      load(memory_order __o = memory_order_seq_cst) const noexcept
      { return _M_impl.load(__o); }

      operator weak_ptr<_Tp>() const noexcept
      { return _M_impl.load(memory_order_seq_cst); }

      void
      store(weak_ptr<_Tp> __desired,
	    memory_order __o = memory_order_seq_cst) noexcept
      { _M_impl.swap(__desired, __o); }

      void
      operator=(weak_ptr<_Tp> __desired) noexcept
      { _M_impl.swap(__desired, memory_order_seq_cst); }

      weak_ptr<_Tp>
      exchange(weak_ptr<_Tp> __desired,
	       memory_order __o = memory_order_seq_cst) noexcept
      {
	_M_impl.swap(__desired, __o);
	return __desired;
      }

      bool
      compare_exchange_strong(weak_ptr<_Tp>& __expected,
			      weak_ptr<_Tp> __desired,
			      memory_order __o, memory_order __o2) noexcept
      {
	return _M_impl.compare_exchange_strong(__expected, __desired, __o, __o2);
      }

      bool
      compare_exchange_strong(value_type& __expected, value_type __desired,
			      memory_order __o = memory_order_seq_cst) noexcept
      {
	memory_order __o2;
	switch (__o)
	{
	case memory_order_acq_rel:
	  __o2 = memory_order_acquire;
	  break;
	case memory_order_release:
	  __o2 = memory_order_relaxed;
	  break;
	default:
	  __o2 = __o;
	}
	return compare_exchange_strong(__expected, std::move(__desired),
				       __o, __o2);
      }

      bool
      compare_exchange_weak(value_type& __expected, value_type __desired,
			    memory_order __o, memory_order __o2) noexcept
      {
	return compare_exchange_strong(__expected, std::move(__desired),
				       __o, __o2);
      }

      bool
      compare_exchange_weak(value_type& __expected, value_type __desired,
			    memory_order __o = memory_order_seq_cst) noexcept
      {
	return compare_exchange_strong(__expected, std::move(__desired), __o);
      }

#if __cpp_lib_atomic_wait
      void
      wait(value_type __old,
	   memory_order __o = memory_order_seq_cst) const noexcept
      {
	_M_impl.wait(std::move(__old), __o);
      }

      void
      notify_one() noexcept
      {
	_M_impl.notify_one();
      }

      void
      notify_all() noexcept
      {
	_M_impl.notify_all();
      }
#endif

    private:
      _Sp_atomic<weak_ptr<_Tp>> _M_impl;
    };
#endif // C++20

  /// @} relates shared_ptr
  /// @} group pointer_abstractions

_GLIBCXX_END_NAMESPACE_VERSION
} // namespace

#endif // _SHARED_PTR_ATOMIC_H
