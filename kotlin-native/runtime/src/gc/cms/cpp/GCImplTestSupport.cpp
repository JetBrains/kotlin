/*
 * Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "gc/GCTestSupport.hpp"

#include "alloc/Allocator.hpp"
#include "ObjectData.hpp"

using namespace kotlin;

bool gc::test_support::tryMark(ObjHeader* object) noexcept {
    return alloc::objectDataForObject(object).tryMark();
}