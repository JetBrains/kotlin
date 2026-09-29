#pragma once

#include "KAssert.h"

#include <cstdint>
#include <vector>

namespace kotlin::stackMap {

/// Reads the variable-length integer encodings fixed-width little-endian
/// integers, ULEB128, SLEB128, and ULEB128-packed bit vectors.
class ByteReader {
public:
    explicit ByteReader(uint8_t* p) noexcept : p_(p) {}

    uint8_t* pos() const noexcept { return p_; }
    void setPos(uint8_t* p) noexcept { p_ = p; }

    /// Reads `size` (<= 8) little-endian bytes and sign-extends the result
    /// as if it were a `size`-byte signed integer.
    int64_t nextInt(uint64_t size) noexcept {
        uint64_t buffer = 0;
        unsigned shift = 0;
        uint64_t originalSize = size;

        while (size) {
            buffer |= (static_cast<uint64_t>(nextByte()) << shift);
            shift += 8;
            size -= 1;
        }

        int64_t result = static_cast<int64_t>(buffer);
        unsigned shiftAmount = (8 - originalSize) * 8;
        result <<= shiftAmount;
        result >>= shiftAmount;
        return result;
    }

    uint64_t nextULEB128() noexcept {
        uint64_t result = 0;
        uint64_t shift = 0;
        uint8_t byte;

        do {
            byte = nextByte();

            if (shift >= 64 || (shift == 63 && (byte & kLoadMask) > 1)) {
                RuntimeFail("Reading StackMaps error: ULEB128 value is too big for uint64_t");
            }

            result |= (static_cast<uint64_t>(byte & kLoadMask)) << shift;
            shift += 7;
        } while (byte & kNextFlag);

        return result;
    }

    int64_t nextSLEB128() noexcept {
        uint64_t result = 0;
        uint64_t shift = 0;
        uint8_t byte;

        while (true) {
            byte = nextByte();

            if (shift >= 64) {
                RuntimeFail("Reading StackMaps error: SLEB128 value is too big for int64_t");
            }

            result |= (static_cast<uint64_t>(byte & kLoadMask)) << shift;

            if (!(byte & kNextFlag)) {
                break;
            }
            shift += 7;
        }

        if ((byte & kSignBit) && (shift < 63)) {
            result |= (~0ULL << (shift + 7));
        }

        return static_cast<int64_t>(result);
    }

    /// Reads a bit vector packed as a ULEB128-style byte sequence: bit 7 of
    /// each byte is the continuation flag, and the remaining 7 bits hold
    /// the next 7 bits of the vector (see
    /// llvm::encodeULEB128(const BitVector&, raw_ostream&) on the emitting
    /// side, in llvm/include/llvm/Support/LEB128.h). Returns the vector as
    /// 64-bit words, bit 0 of word 0 first.
    std::vector<uint64_t> nextULEB128BitVector() {
        std::vector<uint64_t> result;
        uint64_t buffer = 0;
        int bitPos = 0;
        uint8_t byte;

        do {
            byte = nextByte();
            uint64_t payload = byte & kLoadMask;

            if (bitPos + 7 <= 64) {
                buffer |= (payload << bitPos);
                bitPos += 7;
            } else {
                int bitsForCurrentWord = 64 - bitPos;
                buffer |= (payload << bitPos);
                result.push_back(buffer);
                buffer = payload >> bitsForCurrentWord;
                bitPos = 7 - bitsForCurrentWord;
            }

            if (bitPos == 64) {
                result.push_back(buffer);
                buffer = 0;
                bitPos = 0;
            }
        } while (byte & kNextFlag);

        if (bitPos > 0 || result.empty()) {
            result.push_back(buffer);
        }
        return result;
    }

private:
    static constexpr uint8_t kNextFlag = 0b10000000;
    static constexpr uint8_t kSignBit = 0b01000000;
    static constexpr uint8_t kLoadMask = 0b01111111;

    uint8_t nextByte() noexcept { return *p_++; }

    uint8_t* p_;
};

} // namespace kotlin::stackMap
