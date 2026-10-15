/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ReferenceOps.hpp"
#include "mm/ThreadData.hpp"
#include "mm/ThreadRegistry.hpp"

using namespace kotlin;

// on stack
template <>
ALWAYS_INLINE void mm::StackRefAccessor::beforeStore(ObjHeader*) noexcept {}
template <>
ALWAYS_INLINE void mm::StackRefAccessor::afterStore(ObjHeader*) noexcept {}
template <>
ALWAYS_INLINE void mm::StackRefAccessor::beforeLoad() noexcept {}
template <>
ALWAYS_INLINE void mm::StackRefAccessor::afterLoad() noexcept {}

// on heap
template <>
ALWAYS_INLINE void mm::RefFieldAccessor::beforeStore(ObjHeader* value) noexcept {
    gc::beforeHeapRefUpdate(direct(), value, false);
}
template <>
ALWAYS_INLINE void mm::RefFieldAccessor::afterStore(ObjHeader*) noexcept {}
template <>
ALWAYS_INLINE void mm::RefFieldAccessor::beforeLoad() noexcept {}
template <>
ALWAYS_INLINE void mm::RefFieldAccessor::afterLoad() noexcept {}

// in globals
template <>
ALWAYS_INLINE void mm::GlobalRefAccessor::beforeStore(ObjHeader* value) noexcept {
    gc::beforeHeapRefUpdate(direct(), value, false);
}
template <>
ALWAYS_INLINE void mm::GlobalRefAccessor::afterStore(ObjHeader*) noexcept {}
template <>
ALWAYS_INLINE void mm::GlobalRefAccessor::beforeLoad() noexcept {}
template <>
ALWAYS_INLINE void mm::GlobalRefAccessor::afterLoad() noexcept {}

ALWAYS_INLINE OBJ_GETTER(mm::weakRefReadBarrier, std_support::atomic_ref<ObjHeader*> weakReferee) noexcept {
    RETURN_RESULT_OF(gc::weakRefReadBarrier, weakReferee);
}
