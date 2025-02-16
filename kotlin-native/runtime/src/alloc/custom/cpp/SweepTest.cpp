/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <cstddef>
#include <cstdint>

#include "CustomAllocatorTestSupport.hpp"
#include "gtest/gtest.h"

#include "AtomicStack.hpp"
#include "ExtraObjectData.hpp"
#include "FixedBlockPage.hpp"
#include "SingleObjectPage.hpp"
#include "AllocationSize.hpp"

using namespace kotlin::alloc::test_support;

namespace {

using FixedBlockPage = typename kotlin::alloc::FixedBlockPage;
using SingleObjectPage = typename kotlin::alloc::SingleObjectPage;
using AllocationSize = typename kotlin::alloc::AllocationSize;
using ExtraObjectCell = typename kotlin::alloc::ExtraObjectCell;

} // namespace

TEST_F(CustomAllocatorTest, ExtraDataSweepFullFinalizedPage) {
    auto* page = FixedBlockPage::Create(kExtraObjCellSize.inCells());
    while (ExtraObjectCell* cell = allocExtraObjectCell(page)) {
        cell->Data()->setFlag(kotlin::mm::ExtraObjectData::FLAGS_SWEEPABLE);
    }
    auto sweepScope = kotlin::alloc::ExtraDataSweepTraits::currentGCSweepScope(gcHandle());
    EXPECT_FALSE(page->Sweep<kotlin::alloc::ExtraDataSweepTraits>(sweepScope, finalizerQueue()));
    EXPECT_EQ(finalizerQueue().size(), size_t(0));
    page->Destroy();
}
