/*
 * Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "CompressedRefs.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

using namespace kotlin;

namespace {

class CompressedRefsTest : public testing::Test {
public:
    static constexpr uintptr_t kBase = 0x100000000;

    CompressedRefsTest() noexcept { Kotlin_mm_compressedRefsBase = kBase; }
    ~CompressedRefsTest() override { Kotlin_mm_compressedRefsBase = 0; }
};

} // namespace

TEST_F(CompressedRefsTest, Null) {
    EXPECT_THAT(mm::compressRef(nullptr), 0u);
    EXPECT_THAT(mm::decompressRef(0), nullptr);
}

TEST_F(CompressedRefsTest, LowestObject) {
    auto* object = reinterpret_cast<ObjHeader*>(kBase + 8);
    EXPECT_THAT(mm::compressRef(object), 1u);
    EXPECT_THAT(mm::decompressRef(1), object);
}

TEST_F(CompressedRefsTest, HighestObject) {
    auto* object = reinterpret_cast<ObjHeader*>(kBase + mm::kCompressedRefsWindowSize - 8);
    EXPECT_THAT(mm::compressRef(object), 0xFFFFFFFFu);
    EXPECT_THAT(mm::decompressRef(0xFFFFFFFF), object);
}
