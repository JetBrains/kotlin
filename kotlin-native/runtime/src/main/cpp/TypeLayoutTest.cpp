/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "TypeLayout.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "std_support/CStdlib.hpp"

using namespace kotlin;

namespace {

#define CHECK_OVERALL_DESCRIPTOR(DESCRIPTOR) \
    static_assert(DESCRIPTOR().alignment() == alignof(DESCRIPTOR::value_type)); \
    static_assert(DESCRIPTOR().size() == sizeof(DESCRIPTOR::value_type))

#define CHECK_FIELD_DESCRIPTOR(DESCRIPTOR, FIELD) \
    static_assert(DESCRIPTOR().fieldOffset<FIELD>() == offsetof(DESCRIPTOR::value_type, f##FIELD))

#define CHECK_DESCRIPTOR(DESCRIPTOR, F0, F1, F2) \
    CHECK_OVERALL_DESCRIPTOR(DESCRIPTOR); \
    CHECK_FIELD_DESCRIPTOR(DESCRIPTOR, F0); \
    CHECK_FIELD_DESCRIPTOR(DESCRIPTOR, F1); \
    CHECK_FIELD_DESCRIPTOR(DESCRIPTOR, F2)

struct TEmpty {
    using descriptor = type_layout::Composite<TEmpty>;
};

static_assert(type_layout::descriptor_t<TEmpty>().alignment() == alignof(TEmpty));
static_assert(type_layout::descriptor_t<TEmpty>().size() == 0);

struct T323232 {
    using descriptor = type_layout::Composite<T323232, int32_t, int32_t, int32_t>;

    int32_t f0;
    int32_t f1;
    int32_t f2;
};
CHECK_DESCRIPTOR(type_layout::descriptor_t<T323232>, 0, 1, 2);

struct T643232 {
    using descriptor = type_layout::Composite<T643232, int64_t, int32_t, int32_t>;

    int64_t f0;
    int32_t f1;
    int32_t f2;
};
CHECK_DESCRIPTOR(type_layout::descriptor_t<T643232>, 0, 1, 2);

struct T326432 {
    using descriptor = type_layout::Composite<T326432, int32_t, int64_t, int32_t>;

    int32_t f0;
    int64_t f1;
    int32_t f2;
};
CHECK_DESCRIPTOR(type_layout::descriptor_t<T326432>, 0, 1, 2);

struct T323264 {
    using descriptor = type_layout::Composite<T323264, int32_t, int32_t, int64_t>;

    int32_t f0;
    int32_t f1;
    int64_t f2;
};
CHECK_DESCRIPTOR(type_layout::descriptor_t<T323264>, 0, 1, 2);

struct TEmpty326432 {
    using descriptor = type_layout::Composite<TEmpty326432, TEmpty, int32_t, int64_t, int32_t>;

    [[no_unique_address]] TEmpty f0;
    int32_t f1;
    int64_t f2;
    int32_t f3;
};
// Offset of an empty field serves no purpose, skipping it.
CHECK_DESCRIPTOR(type_layout::descriptor_t<TEmpty326432>, 1, 2, 3);

struct T32Empty6432 {
    using descriptor = type_layout::Composite<T32Empty6432, int32_t, TEmpty, int64_t, int32_t>;

    int32_t f0;
    [[no_unique_address]] TEmpty f1;
    int64_t f2;
    int32_t f3;
};
// Offset of an empty field serves no purpose, skipping it.
CHECK_DESCRIPTOR(type_layout::descriptor_t<T32Empty6432>, 0, 2, 3);

struct T3264Empty32 {
    using descriptor = type_layout::Composite<T3264Empty32, int32_t, int64_t, TEmpty, int32_t>;

    int32_t f0;
    int64_t f1;
    [[no_unique_address]] TEmpty f2;
    int32_t f3;
};
// Offset of an empty field serves no purpose, skipping it.
CHECK_DESCRIPTOR(type_layout::descriptor_t<T3264Empty32>, 0, 1, 3);

struct T326432Empty {
    using descriptor = type_layout::Composite<T326432Empty, int32_t, int64_t, int32_t, TEmpty>;

    int32_t f0;
    int64_t f1;
    int32_t f2;
    [[no_unique_address]] TEmpty f3;
};
// Offset of an empty field serves no purpose, skipping it.
CHECK_DESCRIPTOR(type_layout::descriptor_t<T326432Empty>, 0, 1, 2);

struct TUndefined {
    static testing::MockFunction<void(uint8_t*)>* ctorMock;

    struct descriptor {
        using value_type = TUndefined;

        static constexpr size_t alignment() noexcept { return 2 * alignof(uint64_t); }
        static constexpr size_t size() noexcept { return 4 * sizeof(uint64_t); }

        static value_type* construct(uint8_t* ptr) noexcept {
            ctorMock->Call(ptr);
            return reinterpret_cast<value_type*>(ptr);
        }
    };

private:
    TUndefined() = delete;
    ~TUndefined() = delete;
};

// static
testing::MockFunction<void(uint8_t*)>* TUndefined::ctorMock = nullptr;

struct TDynamic {
    static testing::MockFunction<void(uint8_t*)>* ctorMock;

    struct descriptor {
        using value_type = TDynamic;

        descriptor(uint64_t size, size_t alignment) noexcept : size_(size), alignment_(alignment) {}

        size_t alignment() noexcept { return alignment_; }
        uint64_t size() noexcept { return size_; }

        static value_type* construct(uint8_t* ptr) noexcept {
            ctorMock->Call(ptr);
            return reinterpret_cast<value_type*>(ptr);
        }

    private:
        uint64_t size_;
        size_t alignment_;
    };

    static descriptor make_descriptor(uint64_t size, size_t alignment) noexcept { return descriptor(size, alignment); }

private:
    TDynamic() = delete;
    ~TDynamic() = delete;
};

// static
testing::MockFunction<void(uint8_t*)>* TDynamic::ctorMock = nullptr;

struct THeader {
    using descriptor = type_layout::Composite<THeader, T323232, TUndefined>;

    T323232* flags() noexcept { return descriptor().field<0>(this).second; }
    TUndefined* header() noexcept { return descriptor().field<1>(this).second; }

    static THeader* fromFlags(T323232* flags) noexcept { return descriptor().fromField<0>(flags); }
    static THeader* fromHeader(TUndefined* header) noexcept { return descriptor().fromField<1>(header); }

private:
    THeader() = delete;
    ~THeader() = delete;
};

struct TVLA {
    using descriptor = type_layout::Composite<TVLA, THeader, TDynamic, T326432>;

    static descriptor make_descriptor(uint64_t size, size_t alignment) noexcept {
        return descriptor({}, TDynamic::descriptor(size, alignment), {});
    }

    THeader* dataHeader(descriptor descriptor) noexcept { return descriptor.field<0>(this).second; }
    TDynamic* data(descriptor descriptor) noexcept { return descriptor.field<1>(this).second; }
    T326432* footer(descriptor descriptor) noexcept { return descriptor.field<2>(this).second; }

    static TVLA* fromDataHeader(descriptor descriptor, THeader* header) noexcept { return descriptor.fromField<0>(header); }
    static TVLA* fromData(descriptor descriptor, TDynamic* data) noexcept { return descriptor.fromField<1>(data); }
    static TVLA* fromFooter(descriptor descriptor, T326432* footer) noexcept { return descriptor.fromField<2>(footer); }

private:
    TVLA() = delete;
    ~TVLA() = delete;
};

} // namespace

class TypeLayoutTest : public ::testing::Test {
public:
    TypeLayoutTest() noexcept {
        TUndefined::ctorMock = &ctorMock_;
        TDynamic::ctorMock = &ctorMock_;
    }

    ~TypeLayoutTest() {
        TUndefined::ctorMock = nullptr;
        TDynamic::ctorMock = nullptr;
    }

    testing::MockFunction<void(uint8_t*)>& ctorMock() noexcept { return ctorMock_; }

private:
    testing::StrictMock<testing::MockFunction<void(uint8_t*)>> ctorMock_;
};

TEST_F(TypeLayoutTest, VLA) {
    constexpr size_t vlaAlignment = 1;
    constexpr size_t vlaSize = 100;
    constexpr size_t expectedDataHeaderOffset = 0;
    constexpr size_t expectedFlagsOffset = expectedDataHeaderOffset;
    constexpr size_t expectedHeaderOffset = expectedFlagsOffset + 2 * sizeof(uint64_t);
    constexpr size_t expectedDataOffset = expectedHeaderOffset + 4 * sizeof(uint64_t);
    constexpr size_t expectedFooterOffset = AlignUp(expectedDataOffset + vlaSize, alignof(T326432));
    constexpr size_t expectedAlignment = 16;
    constexpr size_t expectedSize = AlignUp(expectedFooterOffset + sizeof(T326432), expectedAlignment);
    auto vlaDescriptor = TVLA::make_descriptor(vlaSize, vlaAlignment);
    EXPECT_THAT(vlaDescriptor.size(), expectedSize);
    EXPECT_THAT(vlaDescriptor.alignment(), expectedAlignment);

    auto* ptr = reinterpret_cast<uint8_t*>(std_support::aligned_malloc(vlaDescriptor.alignment(), vlaDescriptor.size()));

    uint8_t* expectedHeader = ptr + 2 * sizeof(uint64_t);
    uint8_t* expectedData = ptr + 6 * sizeof(uint64_t);
    {
        testing::InSequence seq;
        EXPECT_CALL(ctorMock(), Call(expectedHeader));
        EXPECT_CALL(ctorMock(), Call(expectedData));
    }
    TVLA* instance = vlaDescriptor.construct(ptr);
    testing::Mock::VerifyAndClear(&ctorMock());

    auto* dataHeader = instance->dataHeader(vlaDescriptor);
    EXPECT_THAT(TVLA::fromDataHeader(vlaDescriptor, dataHeader), instance);
    EXPECT_THAT(reinterpret_cast<uint8_t*>(dataHeader), ptr + expectedDataHeaderOffset);

    auto* flags = dataHeader->flags();
    EXPECT_THAT(THeader::fromFlags(flags), dataHeader);
    EXPECT_THAT(reinterpret_cast<uint8_t*>(flags), ptr + expectedFlagsOffset);

    auto* header = dataHeader->header();
    EXPECT_THAT(THeader::fromHeader(header), dataHeader);
    EXPECT_THAT(reinterpret_cast<uint8_t*>(header), ptr + expectedHeaderOffset);

    auto* data = instance->data(vlaDescriptor);
    EXPECT_THAT(TVLA::fromData(vlaDescriptor, data), instance);
    EXPECT_THAT(reinterpret_cast<uint8_t*>(data), ptr + expectedDataOffset);

    auto* footer = instance->footer(vlaDescriptor);
    EXPECT_THAT(TVLA::fromFooter(vlaDescriptor, footer), instance);
    EXPECT_THAT(reinterpret_cast<uint8_t*>(footer), ptr + expectedFooterOffset);

    std_support::aligned_free(ptr);
}

TEST_F(TypeLayoutTest, VLAVeryLarge) {
    constexpr size_t vlaAlignment = 1;
    constexpr uint64_t vlaSize = std::numeric_limits<uint64_t>::max() / 2;

    auto vlaDescriptor = TVLA::make_descriptor(vlaSize, vlaAlignment);
    // Checking that no unsigned integer overflow happened.
    EXPECT_THAT(vlaDescriptor.size(), testing::Gt(std::numeric_limits<uint32_t>::max()));
}
