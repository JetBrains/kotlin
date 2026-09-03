/*
 * Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "gtest/gtest.h"

#include "DeltaMath.hpp"
#include "VarIntCodec.hpp"

#include <cstdint>
#include <set>
#include <utility>
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
    // property DeltaMainStackMapBuilder::Reader::getRootsInfo relies on
    // when a callsite's Delta equals its function's base Delta.
    Delta d;
    d.regs = {0b1010};
    d.slots = {0b0110};
    d.derives = {{-1, -3}};

    Delta selfXor = d ^ d;
    EXPECT_TRUE(selfXor.regs.empty() || selfXor.regs[0] == 0);
    EXPECT_TRUE(selfXor.slots.empty() || selfXor.slots[0] == 0);
    EXPECT_TRUE(selfXor.derives.empty());
}

TEST(DeltaMainDeltaTest, XorRecoversOriginalViaSecondXor) {
    // base ^ (base ^ other) should recover `other`'s live-location set --
    // this is exactly how a callsite's Delta is reconstructed from its
    // function's base Delta plus the stored per-callsite Delta.
    Delta base;
    base.regs = {0b0001};
    base.slots = {0b1100};
    base.derives = {{-1, -3}};

    Delta other;
    other.regs = {0b0101};
    other.slots = {0b1100};
    other.derives = {{-1, -3}, {-2, -4}};

    Delta diff = base ^ other;
    Delta recovered = base ^ diff;

    EXPECT_EQ(recovered.regs, other.regs);
    EXPECT_EQ(recovered.slots, other.slots);
    EXPECT_EQ(std::set<std::pair<int64_t, int64_t>>(recovered.derives.begin(), recovered.derives.end()),
             std::set<std::pair<int64_t, int64_t>>(other.derives.begin(), other.derives.end()));
}

TEST(DeltaMainDeltaTest, ToRootInfoDecodesRegisterAndStackSlotBits) {
    Delta d;
    d.slots = {0b101}; // bits 0 and 2 set: two stack slots.
    d.derives = {};

    RootsInfo info = d.toRootInfo(/*baseOffset=*/64);

    // Two live base locations (bit 0 and bit 2 of the slot bit vector),
    // each with no derived pointers.
    EXPECT_EQ(info.totalCount(), 2u);
}

TEST(DeltaMainDeltaTest, ToRootInfoResolvesDerivedPointerAgainstBase) {
    Delta d;
    // A stack-slot base at enumerated index 0 (signedEnumerate(-1) form: -1)
    // with one derived pointer, also a stack slot, at enumerated index 1
    // (signed form: -2).
    d.derives = {{/*location=*/-2, /*base=*/-1}};

    RootsInfo info = d.toRootInfo(/*baseOffset=*/64);

    // One base plus one derived pointer.
    EXPECT_EQ(info.totalCount(), 2u);
}
