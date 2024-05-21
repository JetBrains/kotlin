// -*- C++ -*-
//===-- parallel_backend_tbb.h --------------------------------------------===//
//
// Part of the LLVM Project, under the Apache License v2.0 with LLVM Exceptions.
// See https://llvm.org/LICENSE.txt for license information.
// SPDX-License-Identifier: Apache-2.0 WITH LLVM-exception
//
//===----------------------------------------------------------------------===//

#ifndef _PSTL_PARALLEL_BACKEND_TBB_H
#define _PSTL_PARALLEL_BACKEND_TBB_H

#include <algorithm>
#include <type_traits>

#include "parallel_backend_utils.h"

// Bring in minimal required subset of Intel TBB
#include <tbb/blocked_range.h>
#include <tbb/parallel_for.h>
#include <tbb/parallel_reduce.h>
#include <tbb/parallel_scan.h>
#include <tbb/parallel_invoke.h>
#include <tbb/task_arena.h>
#include <tbb/tbb_allocator.h>
#include <tbb/task.h>

#if TBB_INTERFACE_VERSION < 10000
#    error Intel(R) Threading Building Blocks 2018 is required; older versions are not supported.
#endif

namespace __pstl
{
namespace __tbb_backend
{

//! Raw memory buffer with automatic freeing and no exceptions.
/** Some of our algorithms need to start with raw memory buffer,
not an initialize array, because initialization/destruction
would make the span be at least O(N). */
// tbb::allocator can improve performance in some cases.
template <typename _Tp>
class __buffer
{
    tbb::tbb_allocator<_Tp> _M_allocator;
    _Tp* _M_ptr;
    const std::size_t _M_buf_size;
    __buffer(const __buffer&) = delete;
    void
    operator=(const __buffer&) = delete;

  public:
    //! Try to obtain buffer of given size to store objects of _Tp type
    __buffer(std::size_t n) : _M_allocator(), _M_ptr(_M_allocator.allocate(n)), _M_buf_size(n) {}
    //! True if buffer was successfully obtained, zero otherwise.
    operator bool() const { return _M_ptr != NULL; }
    //! Return pointer to buffer, or  NULL if buffer could not be obtained.
    _Tp*
    get() const
    {
        return _M_ptr;
    }
    //! Destroy buffer
    ~__buffer() { _M_allocator.deallocate(_M_ptr, _M_buf_size); }
};

// Wrapper for tbb::task
inline void
__cancel_execution()
{
#if TBB_INTERFACE_VERSION <= 12000
    tbb::task::self().group()->cancel_group_execution();
#else
    tbb::task::current_context()->cancel_group_execution();
#endif
}

//------------------------------------------------------------------------
// parallel_for
//------------------------------------------------------------------------

template <class _Index, class _RealBody>
class __parallel_for_body
{
  public:
    __parallel_for_body(const _RealBody& __body) : _M_body(__body) {}
    __parallel_for_body(const __parallel_for_body& __body) : _M_body(__body._M_body) {}
    void
    operator()(const tbb::blocked_range<_Index>& __range) const
    {
        _M_body(__range.begin(), __range.end());
    }

  private:
    _RealBody _M_body;
};

//! Evaluation of brick f[i,j) for each subrange [i,j) of [first,last)
// wrapper over tbb::parallel_for
template <class _ExecutionPolicy, class _Index, class _Fp>
void
__parallel_for(_ExecutionPolicy&&, _Index __first, _Index __last, _Fp __f)
{
    tbb::this_task_arena::isolate([=]() {
        tbb::parallel_for(tbb::blocked_range<_Index>(__first, __last), __parallel_for_body<_Index, _Fp>(__f));
    });
}

//! Evaluation of brick f[i,j) for each subrange [i,j) of [first,last)
// wrapper over tbb::parallel_reduce
template <class _ExecutionPolicy, class _Value, class _Index, typename _RealBody, typename _Reduction>
_Value
__parallel_reduce(_ExecutionPolicy&&, _Index __first, _Index __last, const _Value& __identity,
                  const _RealBody& __real_body, const _Reduction& __reduction)
{
    return tbb::this_task_arena::isolate([__first, __last, &__identity, &__real_body, &__reduction]() -> _Value {
        return tbb::parallel_reduce(
            tbb::blocked_range<_Index>(__first, __last), __identity,
            [__real_body](const tbb::blocked_range<_Index>& __r, const _Value& __value) -> _Value {
                return __real_body(__r.begin(), __r.end(), __value);
            },
            __reduction);
    });
}

//------------------------------------------------------------------------
// parallel_transform_reduce
//
// Notation:
//      r(i,j,init) returns reduction of init with reduction over [i,j)
//      u(i) returns f(i,i+1,identity) for a hypothetical left identity element of r
//      c(x,y) combines values x and y that were the result of r or u
//------------------------------------------------------------------------

template <class _Index, class _Up, class _Tp, class _Cp, class _Rp>
struct __par_trans_red_body
{
    alignas(_Tp) char _M_sum_storage[sizeof(_Tp)]; // Holds generalized non-commutative sum when has_sum==true
    _Rp _M_brick_reduce;                           // Most likely to have non-empty layout
    _Up _M_u;
    _Cp _M_combine;
    bool _M_has_sum; // Put last to minimize size of class
    _Tp&
    sum()
    {
        _PSTL_ASSERT_MSG(_M_has_sum, "sum expected");
        return *(_Tp*)_M_sum_storage;
    }
    __par_trans_red_body(_Up __u, _Tp __init, _Cp __c, _Rp __r)
        : _M_brick_reduce(__r), _M_u(__u), _M_combine(__c), _M_has_sum(true)
    {
        new (_M_sum_storage) _Tp(__init);
    }

    __par_trans_red_body(__par_trans_red_body& __left, tbb::split)
        : _M_brick_reduce(__left._M_brick_reduce), _M_u(__left._M_u), _M_combine(__left._M_combine), _M_has_sum(false)
    {
    }

    ~__par_trans_red_body()
    {
        // 17.6.5.12 tells us to not worry about catching exceptions from destructors.
        if (_M_has_sum)
            sum().~_Tp();
    }

    void
    join(__par_trans_red_body& __rhs)
    {
        sum() = _M_combine(sum(), __rhs.sum());
    }

    void
    operator()(const tbb::blocked_range<_Index>& __range)
    {
        _Index __i = __range.begin();
        _Index __j = __range.end();
        if (!_M_has_sum)
        {
            _PSTL_ASSERT_MSG(__range.size() > 1, "there should be at least 2 elements");
            new (&_M_sum_storage)
                _Tp(_M_combine(_M_u(__i), _M_u(__i + 1))); // The condition i+1 < j is provided by the grain size of 3
            _M_has_sum = true;
            std::advance(__i, 2);
            if (__i == __j)
                return;
        }
        sum() = _M_brick_reduce(__i, __j, sum());
    }
};

template <class _ExecutionPolicy, class _Index, class _Up, class _Tp, class _Cp, class _Rp>
_Tp
__parallel_transform_reduce(_ExecutionPolicy&&, _Index __first, _Index __last, _Up __u, _Tp __init, _Cp __combine,
                            _Rp __brick_reduce)
{
    __tbb_backend::__par_trans_red_body<_Index, _Up, _Tp, _Cp, _Rp> __body(__u, __init, __combine, __brick_reduce);
    // The grain size of 3 is used in order to provide mininum 2 elements for each body
    tbb::this_task_arena::isolate(
        [__first, __last, &__body]() { tbb::parallel_reduce(tbb::blocked_range<_Index>(__first, __last, 3), __body); });
    return __body.sum();
}

//------------------------------------------------------------------------
// parallel_scan
//------------------------------------------------------------------------

template <class _Index, class _Up, class _Tp, class _Cp, class _Rp, class _Sp>
class __trans_scan_body
{
    alignas(_Tp) char _M_sum_storage[sizeof(_Tp)]; // Holds generalized non-commutative sum when has_sum==true
    _Rp _M_brick_reduce;                           // Most likely to have non-empty layout
    _Up _M_u;
    _Cp _M_combine;
    _Sp _M_scan;
    bool _M_has_sum; // Put last to minimize size of class
  public:
    __trans_scan_body(_Up __u, _Tp __init, _Cp __combine, _Rp __reduce, _Sp __scan)
        : _M_brick_reduce(__reduce), _M_u(__u), _M_combine(__combine), _M_scan(__scan), _M_has_sum(true)
    {
        new (_M_sum_storage) _Tp(__init);
    }

    __trans_scan_body(__trans_scan_body& __b, tbb::split)
        : _M_brick_reduce(__b._M_brick_reduce), _M_u(__b._M_u), _M_combine(__b._M_combine), _M_scan(__b._M_scan),
          _M_has_sum(false)
    {
    }

    ~__trans_scan_body()
    {
        // 17.6.5.12 tells us to not worry about catching exceptions from destructors.
        if (_M_has_sum)
            sum().~_Tp();
    }

    _Tp&
    sum() const
    {
        _PSTL_ASSERT_MSG(_M_has_sum, "sum expected");
        return *const_cast<_Tp*>(reinterpret_cast<_Tp const*>(_M_sum_storage));
    }

    void
    operator()(const tbb::blocked_range<_Index>& __range, tbb::pre_scan_tag)
    {
        _Index __i = __range.begin();
        _Index __j = __range.end();
        if (!_M_has_sum)
        {
            new (&_M_sum_storage) _Tp(_M_u(__i));
            _M_has_sum = true;
            ++__i;
            if (__i == __j)
                return;
        }
        sum() = _M_brick_reduce(__i, __j, sum());
    }

    void
    operator()(const tbb::blocked_range<_Index>& __range, tbb::final_scan_tag)
    {
        sum() = _M_scan(__range.begin(), __range.end(), sum());
    }

    void
    reverse_join(__trans_scan_body& __a)
    {
        if (_M_has_sum)
        {
            sum() = _M_combine(__a.sum(), sum());
        }
        else
        {
            new (&_M_sum_storage) _Tp(__a.sum());
            _M_has_sum = true;
        }
    }

    void
    assign(__trans_scan_body& __b)
    {
        sum() = __b.sum();
    }
};

template <typename _Index>
_Index
__split(_Index __m)
{
    _Index __k = 1;
    while (2 * __k < __m)
        __k *= 2;
    return __k;
}

//------------------------------------------------------------------------
// __parallel_strict_scan
//------------------------------------------------------------------------

template <typename _Index, typename _Tp, typename _Rp, typename _Cp>
void
__upsweep(_Index __i, _Index __m, _Index __tilesize, _Tp* __r, _Index __lastsize, _Rp __reduce, _Cp __combine)
{
    if (__m == 1)
        __r[0] = __reduce(__i * __tilesize, __lastsize);
    else
    {
        _Index __k = __split(__m);
        tbb::parallel_invoke(
            [=] { __tbb_backend::__upsweep(__i, __k, __tilesize, __r, __tilesize, __reduce, __combine); },
            [=] {
                __tbb_backend::__upsweep(__i + __k, __m - __k, __tilesize, __r + __k, __lastsize, __reduce, __combine);
            });
        if (__m == 2 * __k)
            __r[__m - 1] = __combine(__r[__k - 1], __r[__m - 1]);
    }
}

template <typename _Index, typename _Tp, typename _Cp, typename _Sp>
void
__downsweep(_Index __i, _Index __m, _Index __tilesize, _Tp* __r, _Index __lastsize, _Tp __initial, _Cp __combine,
            _Sp __scan)
{
    if (__m == 1)
        __scan(__i * __tilesize, __lastsize, __initial);
    else
    {
        const _Index __k = __split(__m);
        tbb::parallel_invoke(
            [=] { __tbb_backend::__downsweep(__i, __k, __tilesize, __r, __tilesize, __initial, __combine, __scan); },
            // Assumes that __combine never throws.
            //TODO: Consider adding a requirement for user functors to be constant.
            [=, &__combine] {
                __tbb_backend::__downsweep(__i + __k, __m - __k, __tilesize, __r + __k, __lastsize,
                                           __combine(__initial, __r[__k - 1]), __combine, __scan);
            });
    }
}

// Adapted from Intel(R) Cilk(TM) version from cilkpub.
// Let i:len denote a counted interval of length n starting at i.  s denotes a generalized-sum value.
// Expected actions of the functors are:
//     reduce(i,len) -> s  -- return reduction value of i:len.
//     combine(s1,s2) -> s -- return merged sum
//     apex(s) -- do any processing necessary between reduce and scan.
//     scan(i,len,initial) -- perform scan over i:len starting with initial.
// The initial range 0:n is partitioned into consecutive subranges.
// reduce and scan are each called exactly once per subrange.
// Thus callers can rely upon side effects in reduce.
// combine must not throw an exception.
// apex is called exactly once, after all calls to reduce and before all calls to scan.
// For example, it's useful for allocating a __buffer used by scan but whose size is the sum of all reduction values.
// T must have a trivial constructor and destructor.
template <class _ExecutionPolicy, typename _Index, typename _Tp, typename _Rp, typename _Cp, typename _Sp, typename _Ap>
void
__parallel_strict_scan(_ExecutionPolicy&&, _Index __n, _Tp __initial, _Rp __reduce, _Cp __combine, _Sp __scan,
                       _Ap __apex)
{
    tbb::this_task_arena::isolate([=, &__combine]() {
        if (__n > 1)
        {
            _Index __p = tbb::this_task_arena::max_concurrency();
            const _Index __slack = 4;
            _Index __tilesize = (__n - 1) / (__slack * __p) + 1;
            _Index __m = (__n - 1) / __tilesize;
            __buffer<_Tp> __buf(__m + 1);
            _Tp* __r = __buf.get();
            __tbb_backend::__upsweep(_Index(0), _Index(__m + 1), __tilesize, __r, __n - __m * __tilesize, __reduce,
                                     __combine);

            // When __apex is a no-op and __combine has no side effects, a good optimizer
            // should be able to eliminate all code between here and __apex.
            // Alternatively, provide a default value for __apex that can be
            // recognized by metaprogramming that conditionlly executes the following.
            size_t __k = __m + 1;
            _Tp __t = __r[__k - 1];
            while ((__k &= __k - 1))
                __t = __combine(__r[__k - 1], __t);
            __apex(__combine(__initial, __t));
            __tbb_backend::__downsweep(_Index(0), _Index(__m + 1), __tilesize, __r, __n - __m * __tilesize, __initial,
                                       __combine, __scan);
            return;
        }
        // Fewer than 2 elements in sequence, or out of memory.  Handle has single block.
        _Tp __sum = __initial;
        if (__n)
            __sum = __combine(__sum, __reduce(_Index(0), __n));
        __apex(__sum);
        if (__n)
            __scan(_Index(0), __n, __initial);
    });
}

template <class _ExecutionPolicy, class _Index, class _Up, class _Tp, class _Cp, class _Rp, class _Sp>
_Tp
__parallel_transform_scan(_ExecutionPolicy&&, _Index __n, _Up __u, _Tp __init, _Cp __combine, _Rp __brick_reduce,
                          _Sp __scan)
{
    __trans_scan_body<_Index, _Up, _Tp, _Cp, _Rp, _Sp> __body(__u, __init, __combine, __brick_reduce, __scan);
    auto __range = tbb::blocked_range<_Index>(0, __n);
    tbb::this_task_arena::isolate([__range, &__body]() { tbb::parallel_scan(__range, __body); });
    return __body.sum();
}

//------------------------------------------------------------------------
// parallel_stable_sort
//------------------------------------------------------------------------

//------------------------------------------------------------------------
// stable_sort utilities
//
// These are used by parallel implementations but do not depend on them.
//------------------------------------------------------------------------
#define _PSTL_MERGE_CUT_OFF 2000

template <typename _Func>
class __func_task;
template <typename _Func>
class __root_task;

#if TBB_INTERFACE_VERSION <= 12000
class __task : public tbb::task
{
  public:
    template <typename _Fn>
    __task*
    make_continuation(_Fn&& __f)
    {
        return new (allocate_continuation()) __func_task<typename std::decay<_Fn>::type>(std::forward<_Fn>(__f));
    }

    template <typename _Fn>
    __task*
    make_child_of(__task* parent, _Fn&& __f)
    {
        return new (parent->allocate_child()) __func_task<typename std::decay<_Fn>::type>(std::forward<_Fn>(__f));
    }

    template <typename _Fn>
    __task*
    make_additional_child_of(tbb::task* parent, _Fn&& __f)
    {
        return new (tbb::task::allocate_additional_child_of(*parent))
            __func_task<typename std::decay<_Fn>::type>(std::forward<_Fn>(__f));
    }

    inline void
    recycle_as_continuation()
    {
        tbb::task::recycle_as_continuation();
    }

    inline void
    recycle_as_child_of(__task* parent)
    {
        tbb::task::recycle_as_child_of(*parent);
    }

    inline void
    spawn(__task* __t)
    {
        tbb::task::spawn(*__t);
    }

    template <typename _Fn>
    static inline void
    spawn_root_and_wait(__root_task<_Fn>& __root)
    {
        tbb::task::spawn_root_and_wait(*__root._M_task);
    }
};

template <typename _Func>
class __func_task : public __task
{
    _Func _M_func;

    tbb::task*
    execute()
    {
        return _M_func(this);
    };

  public:
    template <typename _Fn>
    __func_task(_Fn&& __f) : _M_func{std::forward<_Fn>(__f)}
    {
    }

    _Func&
    body()
    {
        return _M_func;
    }
};

template <typename _Func>
class __root_task
{
    tbb::task* _M_task;

  public:
    template <typename... Args>
    __root_task(Args&&... args)
        : _M_task{new (tbb::task::allocate_root()) __func_task<_Func>{_Func(std::forward<Args>(args)...)}}
    {
    }

    friend class __task;
    friend class __func_task<_Func>;
};

#else  // TBB_INTERFACE_VERSION <= 12000
class __task : public tbb::detail::d1::task
{
  protected:
    tbb::detail::d1::small_object_allocator _M_allocator{};
    tbb::detail::d1::execution_data* _M_execute_data{};
    __task* _M_parent{};
    std::atomic<int> _M_refcount{};
    bool _M_recycle{};

    template <typename _Fn>
    __task*
    allocate_func_task(_Fn&& __f)
    {
        _PSTL_ASSERT(_M_execute_data != nullptr);
        tbb::detail::d1::small_object_allocator __alloc{};
        auto __t =
            __alloc.new_object<__func_task<typename std::decay<_Fn>::type>>(*_M_execute_data, std::forward<_Fn>(__f));
        __t->_M_allocator = __alloc;
        return __t;
    }

  public:
    __task*
    parent()
    {
        return _M_parent;
    }

    void
    set_ref_count(int __n)
    {
        _M_refcount.store(__n, std::memory_order_release);
    }

    template <typename _Fn>
    __task*
    make_continuation(_Fn&& __f)
    {
        auto __t = allocate_func_task(std::forward<_Fn&&>(__f));
        __t->_M_parent = _M_parent;
        _M_parent = nullptr;
        return __t;
    }

    template <typename _Fn>
    __task*
    make_child_of(__task* __parent, _Fn&& __f)
    {
        auto __t = allocate_func_task(std::forward<_Fn&&>(__f));
        __t->_M_parent = __parent;
        return __t;
    }

    template <typename _Fn>
    __task*
    make_additional_child_of(__task* __parent, _Fn&& __f)
    {
        auto __t = make_child_of(__parent, std::forward<_Fn>(__f));
        _PSTL_ASSERT(__parent->_M_refcount.load(std::memory_order_relaxed) > 0);
        ++__parent->_M_refcount;
        return __t;
    }

    inline void
    recycle_as_continuation()
    {
        _M_recycle = true;
    }

    inline void
    recycle_as_child_of(__task* parent)
    {
        _M_recycle = true;
        _M_parent = parent;
    }

    inline void
    spawn(__task* __t)
    {
        _PSTL_ASSERT(_M_execute_data != nullptr);
        tbb::detail::d1::spawn(*__t, *_M_execute_data->context);
    }

    template <typename _Fn>
    static inline void
    spawn_root_and_wait(__root_task<_Fn>& __root)
    {
        tbb::detail::d1::execute_and_wait(*__root._M_func_task, __root._M_context, __root._M_wait_object,
                                          __root._M_context);
    }

    template <typename _Func>
    friend class __func_task;
};

template <typename _Func>
class __func_task : public __task
{
    _Func _M_func;

    __task*
    execute(tbb::detail::d1::execution_data& __ed) override
    {
        _M_execute_data = &__ed;
        _M_recycle = false;
        __task* __next = _M_func(this);
        return finalize(__next);
    };

    __task*
    cancel(tbb::detail::d1::execution_data& __ed) override
    {
        return finalize(nullptr);
    }

    __task*
    finalize(__task* __next)
    {
        bool __recycle = _M_recycle;
        _M_recycle = false;

        if (__recycle)
        {
            return __next;
        }

        auto __parent = _M_parent;
        auto __alloc = _M_allocator;
        auto __ed = _M_execute_data;

        this->~__func_task();

        _PSTL_ASSERT(__parent != nullptr);
        _PSTL_ASSERT(__parent->_M_refcount.load(std::memory_order_relaxed) > 0);
        if (--__parent->_M_refcount == 0)
        {
            _PSTL_ASSERT(__next == nullptr);
            __alloc.deallocate(this, *__ed);
            return __parent;
        }

        return __next;
    }

    friend class __root_task<_Func>;

  public:
    template <typename _Fn>
    __func_task(_Fn&& __f) : _M_func(std::forward<_Fn>(__f))
    {
    }

    _Func&
    body()
    {
        return _M_func;
    }
};

template <typename _Func>
class __root_task : public __task
{
    __task*
    execute(tbb::detail::d1::execution_data& __ed) override
    {
        _M_wait_object.release();
        return nullptr;
    };

    __task*
    cancel(tbb::detail::d1::execution_data& __ed) override
    {
        _M_wait_object.release();
        return nullptr;
    }

    __func_task<_Func>* _M_func_task{};
    tbb::detail::d1::wait_context _M_wait_object{0};
    tbb::task_group_context _M_context{};

  public:
    template <typename... Args>
    __root_task(Args&&... args) : _M_wait_object{1}
    {
        tbb::detail::d1::small_object_allocator __alloc{};
        _M_func_task = __alloc.new_object<__func_task<_Func>>(_Func(std::forward<Args>(args)...));
        _M_func_task->_M_allocator = __alloc;
        _M_func_task->_M_parent = this;
        _M_refcount.store(1, std::memory_order_relaxed);
    }

    friend class __task;
};
#endif // TBB_INTERFACE_VERSION <= 12000

template <typename _RandomAccessIterator1, typename _RandomAccessIterator2, typename _Compare, typename _Cleanup,
          typename _LeafMerge>
class __merge_func
{
    typedef typename std::iterator_traits<_RandomAccessIterator1>::difference_type _DifferenceType1;
    typedef typename std::iterator_traits<_RandomAccessIterator2>::difference_type _DifferenceType2;
    typedef typename std::common_type<_DifferenceType1, _DifferenceType2>::type _SizeType;
    typedef typename std::iterator_traits<_RandomAccessIterator1>::value_type _ValueType;

    _RandomAccessIterator1 _M_x_beg;
    _RandomAccessIterator2 _M_z_beg;

    _SizeType _M_xs, _M_xe;
    _SizeType _M_ys, _M_ye;
    _SizeType _M_zs;
    _Compare _M_comp;
    _LeafMerge _M_leaf_merge;
    _SizeType _M_nsort; //number of elements to be sorted for partial_sort alforithm

    static const _SizeType __merge_cut_off = _PSTL_MERGE_CUT_OFF;

    bool _root;   //means a task is merging root task
    bool _x_orig; //"true" means X(or left ) subrange is in the original container; false - in the buffer
    bool _y_orig; //"true" means Y(or right) subrange is in the original container; false - in the buffer
    bool _split; //"true" means a merge task is a split task for parallel merging, the execution logic differs

    bool
    is_partial() const
    {
        return _M_nsort > 0;
    }

    struct __move_value
    {
        template <typename Iterator1, typename Iterator2>
        void
        operator()(Iterator1 __x, Iterator2 __z)
        {
            *__z = std::move(*__x);
        }
    };

    struct __move_value_construct
    {
        template <typename Iterator1, typename Iterator2>
        void
        operator()(Iterator1 __x, Iterator2 __z)
        {
            ::new (std::addressof(*__z)) _ValueType(std::move(*__x));
        }
    };

    struct __move_range
    {
        template <typename Iterator1, typename Iterator2>
        Iterator2
        operator()(Iterator1 __first1, Iterator1 __last1, Iterator2 __first2)
        {
            if (__last1 - __first1 < __merge_cut_off)
                return std::move(__first1, __last1, __first2);

            auto __n = __last1 - __first1;
            tbb::parallel_for(tbb::blocked_range<_SizeType>(0, __n, __merge_cut_off),
                              [__first1, __first2](const tbb::blocked_range<_SizeType>& __range) {
                                  std::move(__first1 + __range.begin(), __first1 + __range.end(),
                                            __first2 + __range.begin());
                              });
            return __first2 + __n;
        }
    };

    struct __move_range_construct
    {
        template <typename Iterator1, typename Iterator2>
        Iterator2
        operator()(Iterator1 __first1, Iterator1 __last1, Iterator2 __first2)
        {
            if (__last1 - __first1 < __merge_cut_off)
            {
                for (; __first1 != __last1; ++__first1, ++__first2)
                    __move_value_construct()(__first1, __first2);
                return __first2;
            }

            auto __n = __last1 - __first1;
            tbb::parallel_for(tbb::blocked_range<_SizeType>(0, __n, __merge_cut_off),
                              [__first1, __first2](const tbb::blocked_range<_SizeType>& __range) {
                                  for (auto i = __range.begin(); i != __range.end(); ++i)
                                      __move_value_construct()(__first1 + i, __first2 + i);
                              });
            return __first2 + __n;
        }
    };

    struct __cleanup_range
    {
        template <typename Iterator>
        void
        operator()(Iterator __first, Iterator __last)
        {
            if (__last - __first < __merge_cut_off)
                _Cleanup()(__first, __last);
            else
            {
                auto __n = __last - __first;
                tbb::parallel_for(tbb::blocked_range<_SizeType>(0, __n, __merge_cut_off),
                                  [__first](const tbb::blocked_range<_SizeType>& __range) {
                                      _Cleanup()(__first + __range.begin(), __first + __range.end());
                                  });
            }
        }
    };

  public:
    __merge_func(_SizeType __xs, _SizeType __xe, _SizeType __ys, _SizeType __ye, _SizeType __zs, _Compare __comp,
                 _Cleanup, _LeafMerge __leaf_merge, _SizeType __nsort, _RandomAccessIterator1 __x_beg,
                 _RandomAccessIterator2 __z_beg, bool __x_orig, bool __y_orig, bool __root)
        : _M_xs(__xs), _M_xe(__xe), _M_ys(__ys), _M_ye(__ye), _M_zs(__zs), _M_x_beg(__x_beg), _M_z_beg(__z_beg),
          _M_comp(__comp), _M_leaf_merge(__leaf_merge), _M_nsort(__nsort), _root(__root),
          _x_orig(__x_orig), _y_orig(__y_orig), _split(false)
    {
    }

    bool
    is_left(_SizeType __idx) const
    {
        return _M_xs == __idx;
    }

    template <typename IndexType>
    void
    set_odd(IndexType __idx, bool __on_off)
    {
        if (is_left(__idx))
            _x_orig = __on_off;
        else
            _y_orig = __on_off;
    }

    __task*
    operator()(__task* __self);

  private:
    __merge_func*
    parent_merge(__task* __self) const
    {
        return _root ? nullptr : &static_cast<__func_task<__merge_func>*>(__self->parent())->body();
    }
    bool
    x_less_y()
    {
        const auto __nx = (_M_xe - _M_xs);
        const auto __ny = (_M_ye - _M_ys);
        _PSTL_ASSERT(__nx > 0 && __ny > 0);

        _PSTL_ASSERT(_x_orig == _y_orig);
        _PSTL_ASSERT(!is_partial());

        if (_x_orig)
        {
            _PSTL_ASSERT(std::is_sorted(_M_x_beg + _M_xs, _M_x_beg + _M_xe, _M_comp));
            _PSTL_ASSERT(std::is_sorted(_M_x_beg + _M_ys, _M_x_beg + _M_ye, _M_comp));
            return !_M_comp(*(_M_x_beg + _M_ys), *(_M_x_beg + _M_xe - 1));
        }

        _PSTL_ASSERT(std::is_sorted(_M_z_beg + _M_xs, _M_z_beg + _M_xe, _M_comp));
        _PSTL_ASSERT(std::is_sorted(_M_z_beg + _M_ys, _M_z_beg + _M_ye, _M_comp));
        return !_M_comp(*(_M_z_beg + _M_zs + __nx), *(_M_z_beg + _M_zs + __nx - 1));
    }
    void
    move_x_range()
    {
        const auto __nx = (_M_xe - _M_xs);
        const auto __ny = (_M_ye - _M_ys);
        _PSTL_ASSERT(__nx > 0 && __ny > 0);

        if (_x_orig)
            __move_range_construct()(_M_x_beg + _M_xs, _M_x_beg + _M_xe, _M_z_beg + _M_zs);
        else
        {
            __move_range()(_M_z_beg + _M_zs, _M_z_beg + _M_zs + __nx, _M_x_beg + _M_xs);
            __cleanup_range()(_M_z_beg + _M_zs, _M_z_beg + _M_zs + __nx);
        }

        _x_orig = !_x_orig;
    }
    void
    move_y_range()
    {
        const auto __nx = (_M_xe - _M_xs);
        const auto __ny = (_M_ye - _M_ys);

        if (_y_orig)
            __move_range_construct()(_M_x_beg + _M_ys, _M_x_beg + _M_ye, _M_z_beg + _M_zs + __nx);
        else
        {
            __move_range()(_M_z_beg + _M_zs + __nx, _M_z_beg + _M_zs + __nx + __ny, _M_x_beg + _M_ys);
            __cleanup_range()(_M_z_beg + _M_zs + __nx, _M_z_beg + _M_zs + __nx + __ny);
        }

        _y_orig = !_y_orig;
    }
    __task*
    merge_ranges(__task* __self)
    {
        _PSTL_ASSERT(_x_orig == _y_orig); //two merged subrange must be lie into the same buffer

        const auto __nx = (_M_xe - _M_xs);
        const auto __ny = (_M_ye - _M_ys);
        const auto __n = __nx + __ny;

        // need to merge {x} and {y}
        if (__n > __merge_cut_off)
            return split_merging(__self);

        //merge to buffer
        if (_x_orig)
        {
            _M_leaf_merge(_M_x_beg + _M_xs, _M_x_beg + _M_xe, _M_x_beg + _M_ys, _M_x_beg + _M_ye, _M_z_beg + _M_zs,
                          _M_comp, __move_value_construct(), __move_value_construct(), __move_range_construct(),
                          __move_range_construct());
            _PSTL_ASSERT(parent_merge(__self)); //not root merging task
        }
        //merge to "origin"
        else
        {
            _PSTL_ASSERT(_x_orig == _y_orig);

            _PSTL_ASSERT(is_partial() || std::is_sorted(_M_z_beg + _M_xs, _M_z_beg + _M_xe, _M_comp));
            _PSTL_ASSERT(is_partial() || std::is_sorted(_M_z_beg + _M_ys, _M_z_beg + _M_ye, _M_comp));

            const auto __nx = (_M_xe - _M_xs);
            const auto __ny = (_M_ye - _M_ys);

            _M_leaf_merge(_M_z_beg + _M_xs, _M_z_beg + _M_xe, _M_z_beg + _M_ys, _M_z_beg + _M_ye, _M_x_beg + _M_zs,
                          _M_comp, __move_value(), __move_value(), __move_range(), __move_range());

            __cleanup_range()(_M_z_beg + _M_xs, _M_z_beg + _M_xe);
            __cleanup_range()(_M_z_beg + _M_ys, _M_z_beg + _M_ye);
        }
        return nullptr;
    }

    __task*
    process_ranges(__task* __self)
    {
        _PSTL_ASSERT(_x_orig == _y_orig);
        _PSTL_ASSERT(!_split);

        auto p = parent_merge(__self);

        if (!p)
        { //root merging task

            //optimization, just for sort algorithm, //{x} <= {y}
            if (!is_partial() && x_less_y()) //we have a solution
            {
                if (!_x_orig)
                {                   //we have to move the solution to the origin
                    move_x_range(); //parallel moving
                    move_y_range(); //parallel moving
                }
                return nullptr;
            }
            //else: if we have data in the origin,
            //we have to move data to the buffer for final merging into the origin.
            if (_x_orig)
            {
                move_x_range(); //parallel moving
                move_y_range(); //parallel moving
            }
            // need to merge {x} and {y}.
            return merge_ranges(__self);
        }
        //else: not root merging task (parent_merge() == NULL)
        //optimization, just for sort algorithm, //{x} <= {y}
        if (!is_partial() && x_less_y())
        {
            const auto id_range = _M_zs;
            p->set_odd(id_range, _x_orig);
            return nullptr;
        }
        //else: we have to revert "_x(y)_orig" flag of the parent merging task
        const auto id_range = _M_zs;
        p->set_odd(id_range, !_x_orig);

        return merge_ranges(__self);
    }

    //splitting as merge task into 2 of the same level
    __task*
    split_merging(__task* __self)
    {
        _PSTL_ASSERT(_x_orig == _y_orig);
        const auto __nx = (_M_xe - _M_xs);
        const auto __ny = (_M_ye - _M_ys);

        _SizeType __xm{};
        _SizeType __ym{};
        if (__nx < __ny)
        {
            __ym = _M_ys + __ny / 2;

            if (_x_orig)
                __xm = std::upper_bound(_M_x_beg + _M_xs, _M_x_beg + _M_xe, *(_M_x_beg + __ym), _M_comp) - _M_x_beg;
            else
                __xm = std::upper_bound(_M_z_beg + _M_xs, _M_z_beg + _M_xe, *(_M_z_beg + __ym), _M_comp) - _M_z_beg;
        }
        else
        {
            __xm = _M_xs + __nx / 2;

            if (_y_orig)
                __ym = std::lower_bound(_M_x_beg + _M_ys, _M_x_beg + _M_ye, *(_M_x_beg + __xm), _M_comp) - _M_x_beg;
            else
                __ym = std::lower_bound(_M_z_beg + _M_ys, _M_z_beg + _M_ye, *(_M_z_beg + __xm), _M_comp) - _M_z_beg;
        }

        auto __zm = _M_zs + ((__xm - _M_xs) + (__ym - _M_ys));
        __merge_func __right_func(__xm, _M_xe, __ym, _M_ye, __zm, _M_comp, _Cleanup(), _M_leaf_merge, _M_nsort,
                                  _M_x_beg, _M_z_beg, _x_orig, _y_orig, _root);
        __right_func._split = true;
        auto __merge_task = __self->make_additional_child_of(__self->parent(), std::move(__right_func));
        __self->spawn(__merge_task);
        __self->recycle_as_continuation();

        _M_xe = __xm;
        _M_ye = __ym;
        _split = true;

        return __self;
    }
};

template <typename _RandomAccessIterator1, typename _RandomAccessIterator2, typename __M_Compare, typename _Cleanup,
          typename _LeafMerge>
__task*
__merge_func<_RandomAccessIterator1, _RandomAccessIterator2, __M_Compare, _Cleanup, _LeafMerge>::
operator()(__task* __self)
{
    //a. split merge task into 2 of the same level; the special logic,
    //without processing(process_ranges) adjacent sub-ranges x and y
    if (_split)
        return merge_ranges(__self);

    //b. General merging of adjacent sub-ranges x and y (with optimization in case of {x} <= {y} )

    //1. x and y are in the even buffer
    //2. x and y are in the odd buffer
    if (_x_orig == _y_orig)
        return process_ranges(__self);

    //3. x is in even buffer, y is in the odd buffer
    //4. x is in odd buffer, y is in the even buffer
    if (!parent_merge(__self))
    { //root merge task
        if (_x_orig)
            move_x_range();
        else
            move_y_range();
    }
    else
    {
        const _SizeType __nx = (_M_xe - _M_xs);
        const _SizeType __ny = (_M_ye - _M_ys);
        _PSTL_ASSERT(__nx > 0);
        _PSTL_ASSERT(__nx > 0);

        if (__nx < __ny)
            move_x_range();
        else
            move_y_range();
    }

    return process_ranges(__self);
}

template <typename _RandomAccessIterator1, typename _RandomAccessIterator2, typename _Compare, typename _LeafSort>
class __stable_sort_func
{
  public:
    typedef typename std::iterator_traits<_RandomAccessIterator1>::difference_type _DifferenceType1;
    typedef typename std::iterator_traits<_RandomAccessIterator2>::difference_type _DifferenceType2;
    typedef typename std::common_type<_DifferenceType1, _DifferenceType2>::type _SizeType;

  private:
    _RandomAccessIterator1 _M_xs, _M_xe, _M_x_beg;
    _RandomAccessIterator2 _M_zs, _M_z_beg;
    _Compare _M_comp;
    _LeafSort _M_leaf_sort;
    bool _M_root;
    _SizeType _M_nsort; //zero or number of elements to be sorted for partial_sort alforithm

  public:
    __stable_sort_func(_RandomAccessIterator1 __xs, _RandomAccessIterator1 __xe, _RandomAccessIterator2 __zs,
                       bool __root, _Compare __comp, _LeafSort __leaf_sort, _SizeType __nsort,
                       _RandomAccessIterator1 __x_beg, _RandomAccessIterator2 __z_beg)
        : _M_xs(__xs), _M_xe(__xe), _M_x_beg(__x_beg), _M_zs(__zs), _M_z_beg(__z_beg), _M_comp(__comp),
          _M_leaf_sort(__leaf_sort), _M_root(__root), _M_nsort(__nsort)
    {
    }

    __task*
    operator()(__task* __self);
};

#define _PSTL_STABLE_SORT_CUT_OFF 500

template <typename _RandomAccessIterator1, typename _RandomAccessIterator2, typename _Compare, typename _LeafSort>
__task*
__stable_sort_func<_RandomAccessIterator1, _RandomAccessIterator2, _Compare, _LeafSort>::operator()(__task* __self)
{
    typedef __merge_func<_RandomAccessIterator1, _RandomAccessIterator2, _Compare, __utils::__serial_destroy,
                         __utils::__serial_move_merge>
        _MergeTaskType;

    const _SizeType __n = _M_xe - _M_xs;
    const _SizeType __nmerge = _M_nsort > 0 ? _M_nsort : __n;
    const _SizeType __sort_cut_off = _PSTL_STABLE_SORT_CUT_OFF;
    if (__n <= __sort_cut_off)
    {
        _M_leaf_sort(_M_xs, _M_xe, _M_comp);
        _PSTL_ASSERT(!_M_root);
        return nullptr;
    }

    const _RandomAccessIterator1 __xm = _M_xs + __n / 2;
    const _RandomAccessIterator2 __zm = _M_zs + (__xm - _M_xs);
    const _RandomAccessIterator2 __ze = _M_zs + __n;
    _MergeTaskType __m(_MergeTaskType(_M_xs - _M_x_beg, __xm - _M_x_beg, __xm - _M_x_beg, _M_xe - _M_x_beg,
                                      _M_zs - _M_z_beg, _M_comp, __utils::__serial_destroy(),
                                      __utils::__serial_move_merge(__nmerge), _M_nsort, _M_x_beg, _M_z_beg,
                                      /*x_orig*/ true, /*y_orig*/ true, /*root*/ _M_root));
    auto __parent = __self->make_continuation(std::move(__m));
    __parent->set_ref_count(2);
    auto __right = __self->make_child_of(
        __parent, __stable_sort_func(__xm, _M_xe, __zm, false, _M_comp, _M_leaf_sort, _M_nsort, _M_x_beg, _M_z_beg));
    __self->spawn(__right);
    __self->recycle_as_child_of(__parent);
    _M_root = false;
    _M_xe = __xm;

    return __self;
}

template <class _ExecutionPolicy, typename _RandomAccessIterator, typename _Compare, typename _LeafSort>
void
__parallel_stable_sort(_ExecutionPolicy&&, _RandomAccessIterator __xs, _RandomAccessIterator __xe, _Compare __comp,
                       _LeafSort __leaf_sort, std::size_t __nsort = 0)
{
    tbb::this_task_arena::isolate([=, &__nsort]() {
        //sorting based on task tree and parallel merge
        typedef typename std::iterator_traits<_RandomAccessIterator>::value_type _ValueType;
        typedef typename std::iterator_traits<_RandomAccessIterator>::difference_type _DifferenceType;
        const _DifferenceType __n = __xe - __xs;
        if (__nsort == __n)
            __nsort = 0; // 'partial_sort' becames 'sort'

        const _DifferenceType __sort_cut_off = _PSTL_STABLE_SORT_CUT_OFF;
        if (__n > __sort_cut_off)
        {
            __buffer<_ValueType> __buf(__n);
            __root_task<__stable_sort_func<_RandomAccessIterator, _ValueType*, _Compare, _LeafSort>> __root{
                __xs, __xe, __buf.get(), true, __comp, __leaf_sort, __nsort, __xs, __buf.get()};
            __task::spawn_root_and_wait(__root);
            return;
        }
        //serial sort
        __leaf_sort(__xs, __xe, __comp);
    });
}

//------------------------------------------------------------------------
// parallel_merge
//------------------------------------------------------------------------
template <typename _RandomAccessIterator1, typename _RandomAccessIterator2, typename _RandomAccessIterator3,
          typename _Compare, typename _LeafMerge>
class __merge_func_static
{
    _RandomAccessIterator1 _M_xs, _M_xe;
    _RandomAccessIterator2 _M_ys, _M_ye;
    _RandomAccessIterator3 _M_zs;
    _Compare _M_comp;
    _LeafMerge _M_leaf_merge;

  public:
    __merge_func_static(_RandomAccessIterator1 __xs, _RandomAccessIterator1 __xe, _RandomAccessIterator2 __ys,
                        _RandomAccessIterator2 __ye, _RandomAccessIterator3 __zs, _Compare __comp,
                        _LeafMerge __leaf_merge)
        : _M_xs(__xs), _M_xe(__xe), _M_ys(__ys), _M_ye(__ye), _M_zs(__zs), _M_comp(__comp), _M_leaf_merge(__leaf_merge)
    {
    }

    __task*
    operator()(__task* __self);
};

//TODO: consider usage of parallel_for with a custom blocked_range
template <typename _RandomAccessIterator1, typename _RandomAccessIterator2, typename _RandomAccessIterator3,
          typename __M_Compare, typename _LeafMerge>
__task*
__merge_func_static<_RandomAccessIterator1, _RandomAccessIterator2, _RandomAccessIterator3, __M_Compare, _LeafMerge>::
operator()(__task* __self)
{
    typedef typename std::iterator_traits<_RandomAccessIterator1>::difference_type _DifferenceType1;
    typedef typename std::iterator_traits<_RandomAccessIterator2>::difference_type _DifferenceType2;
    typedef typename std::common_type<_DifferenceType1, _DifferenceType2>::type _SizeType;
    const _SizeType __n = (_M_xe - _M_xs) + (_M_ye - _M_ys);
    const _SizeType __merge_cut_off = _PSTL_MERGE_CUT_OFF;
    if (__n <= __merge_cut_off)
    {
        _M_leaf_merge(_M_xs, _M_xe, _M_ys, _M_ye, _M_zs, _M_comp);
        return nullptr;
    }

    _RandomAccessIterator1 __xm;
    _RandomAccessIterator2 __ym;
    if (_M_xe - _M_xs < _M_ye - _M_ys)
    {
        __ym = _M_ys + (_M_ye - _M_ys) / 2;
        __xm = std::upper_bound(_M_xs, _M_xe, *__ym, _M_comp);
    }
    else
    {
        __xm = _M_xs + (_M_xe - _M_xs) / 2;
        __ym = std::lower_bound(_M_ys, _M_ye, *__xm, _M_comp);
    }
    const _RandomAccessIterator3 __zm = _M_zs + ((__xm - _M_xs) + (__ym - _M_ys));
    auto __right = __self->make_additional_child_of(
        __self->parent(), __merge_func_static(__xm, _M_xe, __ym, _M_ye, __zm, _M_comp, _M_leaf_merge));
    __self->spawn(__right);
    __self->recycle_as_continuation();
    _M_xe = __xm;
    _M_ye = __ym;

    return __self;
}

template <class _ExecutionPolicy, typename _RandomAccessIterator1, typename _RandomAccessIterator2,
          typename _RandomAccessIterator3, typename _Compare, typename _LeafMerge>
void
__parallel_merge(_ExecutionPolicy&&, _RandomAccessIterator1 __xs, _RandomAccessIterator1 __xe,
                 _RandomAccessIterator2 __ys, _RandomAccessIterator2 __ye, _RandomAccessIterator3 __zs, _Compare __comp,
                 _LeafMerge __leaf_merge)
{
    typedef typename std::iterator_traits<_RandomAccessIterator1>::difference_type _DifferenceType1;
    typedef typename std::iterator_traits<_RandomAccessIterator2>::difference_type _DifferenceType2;
    typedef typename std::common_type<_DifferenceType1, _DifferenceType2>::type _SizeType;
    const _SizeType __n = (__xe - __xs) + (__ye - __ys);
    const _SizeType __merge_cut_off = _PSTL_MERGE_CUT_OFF;
    if (__n <= __merge_cut_off)
    {
        // Fall back on serial merge
        __leaf_merge(__xs, __xe, __ys, __ye, __zs, __comp);
    }
    else
    {
        tbb::this_task_arena::isolate([=]() {
            typedef __merge_func_static<_RandomAccessIterator1, _RandomAccessIterator2, _RandomAccessIterator3,
                                        _Compare, _LeafMerge>
                _TaskType;
            __root_task<_TaskType> __root{__xs, __xe, __ys, __ye, __zs, __comp, __leaf_merge};
            __task::spawn_root_and_wait(__root);
        });
    }
}

//------------------------------------------------------------------------
// parallel_invoke
//------------------------------------------------------------------------
template <class _ExecutionPolicy, typename _F1, typename _F2>
void
__parallel_invoke(_ExecutionPolicy&&, _F1&& __f1, _F2&& __f2)
{
    //TODO: a version of tbb::this_task_arena::isolate with variadic arguments pack should be added in the future
    tbb::this_task_arena::isolate([&]() { tbb::parallel_invoke(std::forward<_F1>(__f1), std::forward<_F2>(__f2)); });
}

} // namespace __tbb_backend
} // namespace __pstl

#endif /* _PSTL_PARALLEL_BACKEND_TBB_H */
