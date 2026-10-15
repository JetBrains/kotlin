/*
 * Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "DeltaMainStackMap.hpp"
#include "DeltaMainStackMapTestSupport.hpp"
#include "DeltaMath.hpp"
#include "VarIntCodec.hpp"

#include <algorithm>
#include <cstdint>
#include <cstring>
#include <optional>
#include <vector>

using namespace kotlin::stackMap;

namespace {

std::vector<uint8_t> uleb128Bytes(uint64_t value) {
    std::vector<uint8_t> bytes;
    do {
        uint8_t byte = value & 0x7f;
        value >>= 7;
        if (value != 0) byte |= 0x80;
        bytes.push_back(byte);
    } while (value != 0);
    return bytes;
}

std::vector<uint8_t> sleb128Bytes(int64_t value) {
    std::vector<uint8_t> bytes;
    bool more = true;
    while (more) {
        uint8_t byte = value & 0x7f;
        value >>= 7;
        if ((value == 0 && !(byte & 0x40)) || (value == -1 && (byte & 0x40))) {
            more = false;
        } else {
            byte |= 0x80;
        }
        bytes.push_back(byte);
    }
    return bytes;
}

} // namespace

TEST(DeltaMainVarIntCodecTest, ULEB128Roundtrip) {
    std::vector<uint64_t> values = {0, 1, 127, 128, 300, UINT64_MAX / 2, UINT64_MAX};
    for (uint64_t value : values) {
        std::vector<uint8_t> bytes = uleb128Bytes(value);
        ByteReader reader(bytes.data());
        EXPECT_EQ(reader.nextULEB128(), value) << "value=" << value;
    }
}

TEST(DeltaMainVarIntCodecTest, SLEB128Roundtrip) {
    std::vector<int64_t> values = {0, 1, -1, 63, -64, 64, -65, INT64_MIN, INT64_MAX};
    for (int64_t value : values) {
        std::vector<uint8_t> bytes = sleb128Bytes(value);
        ByteReader reader(bytes.data());
        EXPECT_EQ(reader.nextSLEB128(), value) << "value=" << value;
    }
}

TEST(DeltaMainVarIntCodecTest, NextIntSignExtends) {
    // A single byte 0xFF, read as a 1-byte signed integer, should sign-extend to -1.
    std::vector<uint8_t> bytes = {0xFF};
    ByteReader reader(bytes.data());
    EXPECT_EQ(reader.nextInt(1), -1);
}

TEST(DeltaMainDeltaTest, XorIsSelfInverse) {
    // D ^ D should decode to no live locations at all: this is the
    // property DeltaMainStackMap::tryGetRootsInfo relies on
    // when a callsite's Delta equals its function's base Delta.
    Delta d;
    d.slots = {0b0110};

    Delta selfXor = d ^ d;
    EXPECT_TRUE(selfXor.slots.empty() || selfXor.slots[0] == 0);
}

TEST(DeltaMainDeltaTest, XorRecoversOriginalViaSecondXor) {
    // base ^ (base ^ other) should recover `other`'s live-location set --
    // this is exactly how a callsite's Delta is reconstructed from its
    // function's base Delta plus the stored per-callsite Delta.
    Delta base;
    base.slots = {0b1100};

    Delta other;
    other.slots = {0b1100};

    Delta diff = base ^ other;
    Delta recovered = base ^ diff;

    EXPECT_EQ(recovered.slots, other.slots);
}

TEST(DeltaMainDeltaTest, ToRootInfoDecodesStackSlotBits) {
    Delta d;
    d.slots = {0b101}; // bits 0 and 2 set: two stack slots.

    RootsInfo info = d.toRootInfo(/*baseOffset=*/64);

    // Two live base locations (bit 0 and bit 2 of the slot bit vector).
    EXPECT_EQ(info.totalCount(), 2u);
}

// --- The section as the compiler writes it ------------------------------------------------------
//
// The byte sequences below are what the delta-main emitter of the LLVM fork produces; the same
// layout is checked on the compiler side by CodeGen/AArch64/kn-delta-main-stackmap-format.ll.
// A record starts at the address that a function stores in its frame at fp - 16:
//
//   start            4 bytes, little endian, function address - record address
//   size             4 bytes, little endian, size of the function
//   base             SLEB128, offset of slot 0 from the frame pointer
//   frame size       ULEB128
//   live set         ULEB128 bit vector, bit i is the slot at (base - 8 * i)
//   call count       ULEB128
//   calls            (return address - function address, delta index) as two ULEB128
//   deltas           one ULEB128 bit vector each; the live set of a call is (live set ^ delta)

namespace {

using test::FakeStackMapSection;
using test::bytes;

// The frame of a function: the word at fp - 16 holds the address of the record.
class FakeFrame {
public:
    explicit FakeFrame(uintptr_t record) : words_{record, 0} {}

    const uint64_t* fp() const { return words_ + 2; }

private:
    uint64_t words_[2];
};

std::optional<std::vector<int32_t>> lookup(const FakeStackMapSection& section, uintptr_t record, uintptr_t pc) {
    FakeFrame frame(record);
    RootsInfo info;
    if (!section.map().tryGetRootsInfo(frame.fp(), pc, info)) return std::nullopt;
    std::vector<int32_t> result;
    for (const auto& root : info.bases()) {
        EXPECT_EQ(root.Type, RootLocation::Indirect);
        result.push_back(root.Offset);
    }
    return result;
}

} // namespace

TEST(DeltaMainDecoderTest, DecodesTwoCallsWithShrinkingLiveSet) {
    // Output of llc for a function that has two slots live at the first call and one at the second.
    FakeStackMapSection section;
    auto record = section.addRecord(0x100, 0x40, bytes({
            {0x78},  // base: SLEB128 0x78 = -8
            {32},    // frame size
            {0b11},  // live set: slots 0 and 1
            {2},     // two calls
            {12, 0}, // pc +12, delta 0
            {20, 1}, // pc +20, delta 1
            {0},     // delta 0: empty
            {0b01},  // delta 1: slot 0 is dead
    }));

    EXPECT_THAT(lookup(section, record, section.code(0x100 + 12)), testing::Optional(testing::ElementsAre(-8, -16)));
    EXPECT_THAT(lookup(section, record, section.code(0x100 + 20)), testing::Optional(testing::ElementsAre(-16)));
}

TEST(DeltaMainDecoderTest, CallWithoutRootsHasEmptyRoots) {
    FakeStackMapSection section;
    auto record = section.addRecord(0x80, 0x40, bytes({{0}, {16}, {0}, {1}, {10, 0}, {0}}));

    EXPECT_THAT(lookup(section, record, section.code(0x80 + 10)), testing::Optional(testing::IsEmpty()));
}

TEST(DeltaMainDecoderTest, LiveSetLongerThanOneByte) {
    // Eight slots: the bit vector is 7 bits per byte, 0xFF 0x01 = bits 0..7.
    FakeStackMapSection section;
    auto record = section.addRecord(0x100, 0x40, bytes({{0x78}, {0x90, 0x01}, {0xFF, 0x01}, {1}, {8, 0}, {0}}));

    EXPECT_THAT(lookup(section, record, section.code(0x100 + 8)),
            testing::Optional(testing::ElementsAre(-8, -16, -24, -32, -40, -48, -56, -64)));
}

TEST(DeltaMainDecoderTest, MoreThanSixtyFourSlots) {
    // Slots 0 and 70: the bit vector is more than one 64-bit word.
    // 70 = 7 * 10 + 0: bit 70 is bit 0 of the 11th ULEB128 byte.
    std::vector<uint8_t> liveSet(11, 0x80);
    liveSet[0] = 0x81;  // slot 0, continues
    liveSet[10] = 0x01; // slot 70, last byte (no continuation flag)
    FakeStackMapSection section;
    auto record = section.addRecord(0x100, 0x40, bytes({{0x78}, {0xC0, 0x04}, liveSet, {1}, {8, 0}, {0}}));

    EXPECT_THAT(lookup(section, record, section.code(0x100 + 8)),
            testing::Optional(testing::ElementsAre(-8, -8 - 8 * 70)));
}

TEST(DeltaMainDecoderTest, BaseOffsetOfSeveralBytes) {
    // -200 as SLEB128 is 0xB8 0x7E.
    FakeStackMapSection section;
    auto record = section.addRecord(0x100, 0x40, bytes({{0xB8, 0x7E}, {0xF0, 0x01}, {0b101}, {1}, {8, 0}, {0}}));

    EXPECT_THAT(lookup(section, record, section.code(0x100 + 8)), testing::Optional(testing::ElementsAre(-200, -216)));
}

TEST(DeltaMainDecoderTest, SeveralFunctions) {
    FakeStackMapSection section;
    auto first = section.addRecord(0x100, 0x40, bytes({{0x78}, {32}, {0b1}, {1}, {8, 0}, {0}}));
    auto second = section.addRecord(0x200, 0x40, bytes({{0x70}, {48}, {0b11}, {2}, {4, 0}, {12, 1}, {0}, {0b10}}));

    EXPECT_THAT(lookup(section, first, section.code(0x100 + 8)), testing::Optional(testing::ElementsAre(-8)));
    EXPECT_THAT(lookup(section, second, section.code(0x200 + 4)), testing::Optional(testing::ElementsAre(-16, -24)));
    // Function 2: base 0x70 as SLEB128 is -16, so slot i is at -16 - 8 * i. The live set of the
    // second call is 0b11 ^ 0b10 = 0b01: slot 1 (-24) is dead, slot 0 (-16) stays.
    EXPECT_THAT(lookup(section, second, section.code(0x200 + 12)), testing::Optional(testing::ElementsAre(-16)));
}

class DeltaMainFrameValidationTest : public testing::Test {
protected:
    DeltaMainFrameValidationTest() {
        record_ = section_.addRecord(0x100, 0x40, bytes({{0x78}, {32}, {0b1}, {1}, {8, 0}, {0}}));
    }

    FakeStackMapSection section_;
    uintptr_t record_;
};

TEST_F(DeltaMainFrameValidationTest, AcceptsPcInsideTheFunction) {
    EXPECT_TRUE(lookup(section_, record_, section_.code(0x100 + 8)).has_value());
}

TEST_F(DeltaMainFrameValidationTest, RejectsNullRecord) {
    EXPECT_FALSE(lookup(section_, 0, section_.code(0x100 + 8)).has_value());
}

TEST_F(DeltaMainFrameValidationTest, RejectsRecordBeforeTheSection) {
    EXPECT_FALSE(lookup(section_, section_.begin() - 8, section_.code(0x100 + 8)).has_value());
}

TEST_F(DeltaMainFrameValidationTest, RejectsRecordAfterTheSection) {
    EXPECT_FALSE(lookup(section_, section_.end(), section_.code(0x100 + 8)).has_value());
}

TEST_F(DeltaMainFrameValidationTest, RejectsRecordWithoutRoomForTheHeader) {
    EXPECT_FALSE(lookup(section_, section_.end() - 4, section_.code(0x100 + 8)).has_value());
}

TEST_F(DeltaMainFrameValidationTest, RejectsPcBeforeTheFunction) {
    EXPECT_FALSE(lookup(section_, record_, section_.code(0x100 - 4)).has_value());
}

TEST_F(DeltaMainFrameValidationTest, RejectsPcAtTheEndOfTheFunction) {
    EXPECT_FALSE(lookup(section_, record_, section_.code(0x100 + 0x40)).has_value());
}

TEST_F(DeltaMainFrameValidationTest, RejectsPcOfAnotherFunction) {
    EXPECT_FALSE(lookup(section_, record_, section_.code(0x300 + 8)).has_value());
}

TEST_F(DeltaMainFrameValidationTest, PcInsideTheFunctionWithoutCallIsFatal) {
    EXPECT_DEATH(lookup(section_, record_, section_.code(0x100 + 12)), "No stack map");
}

TEST(DeltaMainEmptySectionTest, RejectsEveryFrame) {
    FakeFrame frame(0x1000);
    RootsInfo info;

    EXPECT_FALSE(DeltaMainStackMap(nullptr, nullptr, true).tryGetRootsInfo(frame.fp(), 0x1000, info));
}

// --- The mode that decodes the whole section at the start ---------------------------------------

namespace {

std::optional<std::vector<int32_t>> lookupCollected(const DeltaMainStackMap& map, uintptr_t pc) {
    uint64_t unused[2] = {0, 0};
    RootsInfo info;
    if (!map.tryGetRootsInfo(unused + 2, pc, info)) return std::nullopt;
    std::vector<int32_t> result;
    for (const auto& root : info.bases()) result.push_back(root.Offset);
    return result;
}

} // namespace

TEST(DeltaMainCollectTest, FindsEveryCallOfEveryFunction) {
    FakeStackMapSection section;
    section.addRecord(0x100, 0x40, bytes({{0x78}, {32}, {0b11}, {2}, {12, 0}, {20, 1}, {0}, {0b01}}));
    section.addRecord(0x200, 0x40, bytes({{0x70}, {48}, {0b1}, {1}, {4, 0}, {0}}));
    auto map = section.map(false);
    map.collect();

    EXPECT_THAT(lookupCollected(map, section.code(0x100 + 12)), testing::Optional(testing::ElementsAre(-8, -16)));
    EXPECT_THAT(lookupCollected(map, section.code(0x100 + 20)), testing::Optional(testing::ElementsAre(-16)));
    EXPECT_THAT(lookupCollected(map, section.code(0x200 + 4)), testing::Optional(testing::ElementsAre(-16)));
}

TEST(DeltaMainCollectTest, UnknownPcHasNoMap) {
    FakeStackMapSection section;
    section.addRecord(0x100, 0x40, bytes({{0x78}, {32}, {0b1}, {1}, {8, 0}, {0}}));
    auto map = section.map(false);
    map.collect();

    EXPECT_FALSE(lookupCollected(map, section.code(0x100 + 12)).has_value());
    EXPECT_FALSE(lookupCollected(map, section.code(0x300 + 8)).has_value());
}

TEST(DeltaMainCollectTest, EmptySectionHasNoMaps) {
    FakeStackMapSection section;
    auto map = section.map(false);
    map.collect();

    EXPECT_FALSE(lookupCollected(map, section.code(0x100)).has_value());
}

TEST(DeltaMainCollectTest, BytesAfterTheLastRecordAreFatal) {
    FakeStackMapSection section;
    section.addRecord(0x100, 0x40, bytes({{0x78}, {32}, {0b1}, {1}, {8, 0}, {0}}));
    section.addBytes({0x7f, 0x7f, 0x7f});
    auto map = section.map(false);

    EXPECT_DEATH(map.collect(), "do not fill the section");
}
