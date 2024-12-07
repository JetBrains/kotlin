/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "std_support/CStdlib.hpp"

#include <cstdint>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "Alignment.hpp"

using namespace kotlin;

namespace {

inline constexpr auto mallocAlignment = alignof(std::max_align_t);

struct Struct {
    int32_t x;
};

using Vector4f = float __attribute__((__vector_size__(16)));

struct alignas(32) OverAlignedStruct {
    int32_t x;
};
static_assert(
        alignof(OverAlignedStruct) > mallocAlignment,
        "OverAlignedStruct should require alignment of more than supported by malloc");

} // namespace

TEST(StdSupportCStdlibTest, AlignedMalloc) {
    {
        Struct* ptr = (Struct*)std_support::aligned_malloc(alignof(Struct), sizeof(Struct));
        EXPECT_TRUE(IsAligned(ptr, alignof(Struct)));
        ptr->x = 123;
        EXPECT_THAT(ptr->x, 123);
        std_support::aligned_free(ptr);
    }
    {
        void* ptr = std_support::aligned_malloc(alignof(int), 0);
        EXPECT_TRUE(IsAligned(ptr, alignof(int)));
        std_support::aligned_free(ptr);
    }
    {
        Vector4f* ptr = (Vector4f*)std_support::aligned_malloc(alignof(Vector4f), sizeof(Vector4f));
        EXPECT_TRUE(IsAligned(ptr, alignof(Vector4f)));
        *ptr = {1.1f, 2.2f, 3.3f, 4.4f};
        EXPECT_THAT((*ptr)[0], 1.1f);
        EXPECT_THAT((*ptr)[1], 2.2f);
        EXPECT_THAT((*ptr)[2], 3.3f);
        EXPECT_THAT((*ptr)[3], 4.4f);
        std_support::aligned_free(ptr);
    }
    {
        OverAlignedStruct* ptr = (OverAlignedStruct*)std_support::aligned_malloc(alignof(OverAlignedStruct), sizeof(OverAlignedStruct));
        EXPECT_TRUE(IsAligned(ptr, alignof(OverAlignedStruct)));
        ptr->x = 123;
        EXPECT_THAT(ptr->x, 123);
        std_support::aligned_free(ptr);
    }
}

TEST(StdSupportCStdlibTest, AlignedFree) {
    std_support::aligned_free(nullptr);
}
