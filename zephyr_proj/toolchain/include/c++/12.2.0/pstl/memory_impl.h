// -*- C++ -*-
//===-- memory_impl.h -----------------------------------------------------===//
//
// Part of the LLVM Project, under the Apache License v2.0 with LLVM Exceptions.
// See https://llvm.org/LICENSE.txt for license information.
// SPDX-License-Identifier: Apache-2.0 WITH LLVM-exception
//
//===----------------------------------------------------------------------===//

#ifndef _PSTL_MEMORY_IMPL_H
#define _PSTL_MEMORY_IMPL_H

#include <iterator>

#include "unseq_backend_simd.h"

namespace __pstl
{
namespace __internal
{

//------------------------------------------------------------------------
// uninitialized_move
//------------------------------------------------------------------------

template <typename _ForwardIterator, typename _OutputIterator>
_OutputIterator
__brick_uninitialized_move(_ForwardIterator __first, _ForwardIterator __last, _OutputIterator __result,
                           /*vector=*/std::false_type) noexcept
{
    using _ValueType = typename std::iterator_traits<_OutputIterator>::value_type;
    for (; __first != __last; ++__first, ++__result)
    {
        ::new (std::addressof(*__result)) _ValueType(std::move(*__first));
    }
    return __result;
}

template <typename _ForwardIterator, typename _OutputIterator>
_OutputIterator
__brick_uninitialized_move(_ForwardIterator __first, _ForwardIterator __last, _OutputIterator __result,
                           /*vector=*/std::true_type) noexcept
{
    using __ValueType = typename std::iterator_traits<_OutputIterator>::value_type;
    using _ReferenceType1 = typename std::iterator_traits<_ForwardIterator>::reference;
    using _ReferenceType2 = typename std::iterator_traits<_OutputIterator>::reference;

    return __unseq_backend::__simd_walk_2(
        __first, __last - __first, __result,
        [](_ReferenceType1 __x, _ReferenceType2 __y) { ::new (std::addressof(__y)) __ValueType(std::move(__x)); });
}

template <typename _Iterator>
void
__brick_destroy(_Iterator __first, _Iterator __last, /*vector*/ std::false_type) noexcept
{
    using _ValueType = typename std::iterator_traits<_Iterator>::value_type;

    for (; __first != __last; ++__first)
        __first->~_ValueType();
}

template <typename _Iterator>
void
__brick_destroy(_Iterator __first, _Iterator __last, /*vector*/ std::true_type) noexcept
{
    using _ValueType = typename std::iterator_traits<_Iterator>::value_type;
    using _ReferenceType = typename std::iterator_traits<_Iterator>::reference;

    __unseq_backend::__simd_walk_1(__first, __last - __first, [](_ReferenceType __x) { __x.~_ValueType(); });
}

//------------------------------------------------------------------------
// uninitialized copy
//------------------------------------------------------------------------

template <typename _ForwardIterator, typename _OutputIterator>
_OutputIterator
__brick_uninitialized_copy(_ForwardIterator __first, _ForwardIterator __last, _OutputIterator __result,
                           /*vector=*/std::false_type) noexcept
{
    using _ValueType = typename std::iterator_traits<_OutputIterator>::value_type;
    for (; __first != __last; ++__first, ++__result)
    {
        ::new (std::addressof(*__result)) _ValueType(*__first);
    }
    return __result;
}

template <typename _ForwardIterator, typename _OutputIterator>
_OutputIterator
__brick_uninitialized_copy(_ForwardIterator __first, _ForwardIterator __last, _OutputIterator __result,
                           /*vector=*/std::true_type) noexcept
{
    using __ValueType = typename std::iterator_traits<_OutputIterator>::value_type;
    using _ReferenceType1 = typename std::iterator_traits<_ForwardIterator>::reference;
    using _ReferenceType2 = typename std::iterator_traits<_OutputIterator>::reference;

    return __unseq_backend::__simd_walk_2(
        __first, __last - __first, __result,
        [](_ReferenceType1 __x, _ReferenceType2 __y) { ::new (std::addressof(__y)) __ValueType(__x); });
}

} // namespace __internal
} // namespace __pstl

#endif /* _PSTL_MEMORY_IMPL_H */
