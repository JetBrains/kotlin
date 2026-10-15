#pragma once

#include "DeltaMainStackMap.hpp"

#include <cstddef>
#include <cstdint>
#include <cstring>
#include <initializer_list>
#include <vector>

namespace kotlin::stackMap::test {

inline std::vector<uint8_t> bytes(std::initializer_list<std::vector<uint8_t>> parts) {
    std::vector<uint8_t> result;
    for (const auto& part : parts) result.insert(result.end(), part.begin(), part.end());
    return result;
}

// Both arrays are static: a record stores the distance to its function as a 32-bit number.
class FakeStackMapSection {
public:
    FakeStackMapSection() {
        std::memset(section_, 0, sizeof(section_));
        used_ = 0;
    }

    static uintptr_t code(size_t offset) { return reinterpret_cast<uintptr_t>(code_) + offset; }

    // `body` is everything in a record after the two 32-bit numbers.
    uintptr_t addRecord(size_t functionOffset, uint32_t functionSize, const std::vector<uint8_t>& body) {
        uint8_t* record = section_ + used_;
        auto distance = static_cast<int32_t>(code(functionOffset) - reinterpret_cast<uintptr_t>(record));
        std::memcpy(record, &distance, sizeof(distance));
        std::memcpy(record + 4, &functionSize, sizeof(functionSize));
        std::memcpy(record + 8, body.data(), body.size());
        used_ += 8 + body.size();
        return reinterpret_cast<uintptr_t>(record);
    }

    static uintptr_t begin() { return reinterpret_cast<uintptr_t>(section_); }
    static uintptr_t end() { return reinterpret_cast<uintptr_t>(section_ + used_); }

    void addBytes(const std::vector<uint8_t>& data) {
        std::memcpy(section_ + used_, data.data(), data.size());
        used_ += data.size();
    }

    DeltaMainStackMap map(bool lazy = true) const { return DeltaMainStackMap(section_, section_ + used_, lazy); }

private:
    static inline uint8_t section_[4096];
    static inline uint8_t code_[4096];
    static inline size_t used_ = 0;
};

} // namespace kotlin::stackMap::test
