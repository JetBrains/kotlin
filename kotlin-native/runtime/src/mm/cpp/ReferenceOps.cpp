/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ReferenceOps.hpp"
#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"

using namespace kotlin;

// on stack
template<> void mm::RefAccessor<true>::beforeStore(ObjHeader*) noexcept {}
template<> void mm::RefAccessor<true>::afterStore(ObjHeader*) noexcept {}
template<> void mm::RefAccessor<true>::beforeLoad() noexcept {}
template<> void mm::RefAccessor<true>::afterLoad() noexcept {}

// on heap
template<> void mm::RefAccessor<false>::beforeStore(ObjHeader*) noexcept {}
template<> void mm::RefAccessor<false>::afterStore(ObjHeader*) noexcept {}
template<> void mm::RefAccessor<false>::beforeLoad() noexcept {}
template<> void mm::RefAccessor<false>::afterLoad() noexcept {}

ALWAYS_INLINE OBJ_GETTER(mm::weakRefReadBarrier, std::atomic<ObjHeader*>& referee) noexcept {
    RETURN_RESULT_OF(kotlin::gc::tryRef, referee);
}
