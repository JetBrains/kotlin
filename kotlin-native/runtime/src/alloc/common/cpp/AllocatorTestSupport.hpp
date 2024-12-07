/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "Allocator.hpp"

#include <vector>

#include "ThreadData.hpp"

namespace kotlin::alloc::test_support {

void assertClear(Allocator& allocator) noexcept;
std::vector<ObjHeader*> allocatedObjects(mm::ThreadData& threadData) noexcept;
void detachAndDestroyExtraObjectData(mm::ExtraObjectData& extraObject) noexcept;

} // namespace kotlin::alloc::test_support
