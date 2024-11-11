/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <cstdio>
#include <cstdlib>
#include <limits>
#include <string.h>
#include <string>
#include <optional>

#include "KAssert.h"
#include "Exceptions.h"
#include "Memory.h"
#include "Natives.h"
#include "KString.h"
#include "Porting.h"
#include "Types.h"

#include "utf8.h"

#include "polyhash/PolyHash.h"

using namespace kotlin;

namespace {

static constexpr const uint32_t MAX_STRING_SIZE =
    static_cast<uint32_t>(std::numeric_limits<int32_t>::max());

char* StringRawData(KRef kstring) {
    return StringHeader::of(kstring)->data();
}

const char* StringRawData(KConstRef kstring) {
    return StringHeader::of(kstring)->data();
}

size_t StringRawSize(KConstRef kstring) {
    return StringHeader::of(kstring)->size();
}

KChar* StringUtf16Data(KRef kstring) {
    return reinterpret_cast<KChar*>(StringRawData(kstring));
}

const KChar* StringUtf16Data(KConstRef kstring) {
    return reinterpret_cast<const KChar*>(StringRawData(kstring));
}

size_t StringUtf16Length(KConstRef kstring) {
    return StringRawSize(kstring) / sizeof(KChar);
}

template <typename CharCountF /*= uint32_t(const char*, const char*) */, typename ConvertF /*= void(const char*, const char*, KChar*) */>
OBJ_GETTER(convertToUTF16, const char* rawString, size_t rawStringLength, CharCountF&& countChars, ConvertF&& convert) {
    if (rawString == nullptr) RETURN_OBJ(nullptr);
    if (rawStringLength == 0) RETURN_RESULT_OF0(TheEmptyString);

    auto rawStringEnd = rawString + rawStringLength;
    auto result = CreateUninitializedUtf16String(countChars(rawString, rawStringEnd), OBJ_RESULT);
    convert(rawString, rawStringEnd, StringUtf16Data(result));
    RETURN_OBJ(result);
}

template <KStringConversionMode mode>
OBJ_GETTER(unsafeConvertToUTF8, KConstRef thiz, KInt start, KInt size) {
    std::string utf8;
    try {
        utf8 = kotlin::to_string<mode>(thiz, static_cast<size_t>(start), static_cast<size_t>(size));
    } catch (...) {
        ThrowCharacterCodingException();
    }

    ArrayHeader* result = AllocArrayInstance(theByteArrayTypeInfo, utf8.size(), OBJ_RESULT)->array();
    ::memcpy(ByteArrayAddressOfElementAt(result, 0), utf8.data(), utf8.size());
    RETURN_OBJ(result->obj());
}

uint32_t mismatch(const uint16_t* first, const uint16_t* second, uint32_t size) {
    const long* firstLong = reinterpret_cast<const long*>(first);
    const long* secondLong = reinterpret_cast<const long*>(second);
    constexpr int step = sizeof(long) / sizeof(uint16_t);
    uint32_t sizeLong = size / step;
    uint32_t iLong;
    for (iLong = 0; iLong < sizeLong; iLong++) {
        if (firstLong[iLong] != secondLong[iLong]) {
            break;
        }
    }
    for (uint32_t i = iLong * step; i < size; i++) {
        if (first[i] != second[i]) {
            return i;
        }
    }
    return size;
}

const char* unsafeGetByteArrayData(KConstRef thiz, KInt start) {
    RuntimeAssert(thiz->type_info() == theByteArrayTypeInfo, "Must use a byte array");
    return reinterpret_cast<const char*>(ByteArrayAddressOfElementAt(thiz->array(), start));
}

template <typename T>
int threeWayCompare(T a, T b) {
    return (a == b) ? 0 : (a < b ? -1 : 1);
}

PERFORMANCE_INLINE inline const KChar* boundsCheckedIteratorAt(KConstRef string, KInt index) {
    // We couldn't have created a string bigger than max KInt value.
    // So if index is < 0, conversion to an unsigned value would make it bigger
    // than the array size.
    if (static_cast<uint32_t>(index) >= StringUtf16Length(string)) {
        ThrowArrayIndexOutOfBoundsException();
    }
    return StringUtf16Data(string) + index;
}

#if KONAN_WINDOWS
void* memmem(const void *big, size_t bigLen, const void *little, size_t littleLen) {
    for (size_t i = 0; i + littleLen <= bigLen; ++i) {
        void* pos = ((char*)big) + i;
        if (memcmp(little, pos, littleLen) == 0) return pos;
    }
    return nullptr;
}
#endif

} // namespace

extern "C" OBJ_GETTER(CreateStringFromCString, const char* cstring) {
    RETURN_RESULT_OF(CreateStringFromUtf8, cstring, cstring ? strlen(cstring) : 0);
}

extern "C" OBJ_GETTER(CreateStringFromUtf8, const char* utf8, uint32_t lengthBytes) {
    RETURN_RESULT_OF(convertToUTF16, utf8, lengthBytes,
        [](auto data, auto end) { return utf8::with_replacement::utf16_length(data, end); },
        [](auto data, auto end, auto out) { utf8::with_replacement::utf8to16(data, end, out); });
}

extern "C" OBJ_GETTER(CreateStringFromUtf8OrThrow, const char* utf8, uint32_t lengthBytes) {
    RETURN_RESULT_OF(convertToUTF16, utf8, lengthBytes,
        [](const char* data, const char* end) {
            try {
                return utf8::utf16_length(data, end);
            } catch (...) {
                ThrowCharacterCodingException();
            }
        },
        [](auto data, auto end, auto out) { utf8::unchecked::utf8to16(data, end, out); });
}

extern "C" OBJ_GETTER(CreateStringFromUtf16, const KChar* utf16, uint32_t lengthChars) {
    if (utf16 == nullptr) RETURN_OBJ(nullptr);
    if (lengthChars == 0) RETURN_RESULT_OF0(TheEmptyString);

    auto result = CreateUninitializedUtf16String(lengthChars, OBJ_RESULT);
    memcpy(StringRawData(result), utf16, StringRawSize(result));
    RETURN_OBJ(result);
}

extern "C" OBJ_GETTER(CreateUninitializedUtf16String, uint32_t lengthChars) {
    RETURN_RESULT_OF(AllocArrayInstance, theStringTypeInfo, lengthChars + StringHeader::extraLength(0) / sizeof(KChar));
}

extern "C" char* CreateCStringFromString(KConstRef kref) {
    if (kref == nullptr) return nullptr;
    std::string utf8 = kotlin::to_string<KStringConversionMode::UNCHECKED>(kref);
    char* result = reinterpret_cast<char*>(std::calloc(1, utf8.size() + 1));
    ::memcpy(result, utf8.data(), utf8.size());
    return result;
}

extern "C" void DisposeCString(char* cstring) {
    if (cstring) std::free(cstring);
}

extern "C" KRef CreatePermanentStringFromCString(const char* nullTerminatedUTF8) {
    // Note: this function can be called in "Native" thread state. But this is fine:
    //   while it indeed manipulates Kotlin objects, it doesn't in fact access _Kotlin heap_,
    //   because the accessed object is off-heap, imitating permanent static objects.
    const char* end = nullTerminatedUTF8 + strlen(nullTerminatedUTF8);
    size_t count = utf8::with_replacement::utf16_length(nullTerminatedUTF8, end) + StringHeader::extraLength(0) / sizeof(KChar);
    size_t headerSize = alignUp(sizeof(ArrayHeader), alignof(char16_t));
    size_t arraySize = headerSize + count * sizeof(char16_t);

    auto header = (ObjHeader*)std::calloc(arraySize, 1);
    header->typeInfoOrMeta_ = setPointerBits((TypeInfo *)theStringTypeInfo, OBJECT_TAG_PERMANENT_CONTAINER);
    header->array()->count_ = count;
    utf8::with_replacement::utf8to16(nullTerminatedUTF8, end, StringUtf16Data(header));
    return header;
}

extern "C" void FreePermanentStringForTests(KConstRef header) {
    std::free(const_cast<KRef>(header));
}

// String.kt
extern "C" KInt Kotlin_String_getStringLength(KConstRef thiz) {
    return StringUtf16Length(thiz);
}

extern "C" OBJ_GETTER(Kotlin_String_replace, KConstRef thiz, KChar oldChar, KChar newChar) {
    auto count = StringUtf16Length(thiz);
    auto result = CreateUninitializedUtf16String(count, OBJ_RESULT);
    auto resultRaw = StringUtf16Data(result);
    for (auto it = StringUtf16Data(thiz), end = it + count; it != end; it++) {
        KChar thizChar = *it;
        *resultRaw++ = thizChar == oldChar ? newChar : thizChar;
    }
    RETURN_OBJ(result);
}

extern "C" OBJ_GETTER(Kotlin_String_plusImpl, KConstRef thiz, KConstRef other) {
    auto thizLength = StringUtf16Length(thiz);
    auto otherLength = StringUtf16Length(other);
    RuntimeAssert(thizLength <= MAX_STRING_SIZE, "this cannot be this large");
    RuntimeAssert(otherLength <= MAX_STRING_SIZE, "other cannot be this large");
    auto resultLength = thizLength + otherLength; // can't overflow since MAX_STRING_SIZE is (max value)/2
    if (resultLength > MAX_STRING_SIZE) {
        ThrowOutOfMemoryError();
    }

    auto result = CreateUninitializedUtf16String(resultLength, OBJ_RESULT);
    auto resultRaw = StringUtf16Data(result);
    memcpy(resultRaw, StringUtf16Data(thiz), StringRawSize(thiz));
    memcpy(resultRaw + thizLength, StringUtf16Data(other), StringRawSize(other));
    RETURN_OBJ(result);
}

extern "C" OBJ_GETTER(Kotlin_String_unsafeStringFromCharArray, KConstRef thiz, KInt start, KInt size) {
    RuntimeAssert(thiz->type_info() == theCharArrayTypeInfo, "Must use a char array");

    if (size == 0) {
        RETURN_RESULT_OF0(TheEmptyString);
    }

    auto result = CreateUninitializedUtf16String(size, OBJ_RESULT);
    memcpy(StringRawData(result), CharArrayAddressOfElementAt(thiz->array(), start), size * sizeof(KChar));
    RETURN_OBJ(result);
}

extern "C" OBJ_GETTER(Kotlin_String_toCharArray, KConstRef string, KRef destination, KInt destinationOffset, KInt start, KInt size) {
    memcpy(CharArrayAddressOfElementAt(destination->array(), destinationOffset),
        StringUtf16Data(string) + start, size * sizeof(KChar));
    RETURN_OBJ(destination);
}

extern "C" OBJ_GETTER(Kotlin_String_subSequence, KConstRef thiz, KInt startIndex, KInt endIndex) {
    if (startIndex < 0 || static_cast<uint32_t>(endIndex) > StringUtf16Length(thiz) || startIndex > endIndex) {
        // TODO: is it correct exception?
        ThrowArrayIndexOutOfBoundsException();
    }

    if (startIndex == endIndex) {
        RETURN_RESULT_OF0(TheEmptyString);
    }

    KInt length = endIndex - startIndex;
    auto result = CreateUninitializedUtf16String(length, OBJ_RESULT);
    memcpy(StringUtf16Data(result), StringUtf16Data(thiz) + startIndex, length * sizeof(KChar));
    RETURN_OBJ(result);
}

extern "C" KInt Kotlin_String_compareTo(KConstRef thiz, KConstRef other) {
    auto first = StringUtf16Data(thiz);
    auto firstSize = StringUtf16Length(thiz);
    auto second = StringUtf16Data(other);
    auto secondSize = StringUtf16Length(other);
    auto minSize = std::min(firstSize, secondSize);
    auto mismatch_position = mismatch(first, second, minSize);
    if (mismatch_position != minSize) {
        return threeWayCompare(first[mismatch_position], second[mismatch_position]);
    }
    return threeWayCompare(firstSize, secondSize);
}

extern "C" KChar Kotlin_String_get(KConstRef thiz, KInt index) {
    return *boundsCheckedIteratorAt(thiz, index);
}

extern "C" OBJ_GETTER(Kotlin_ByteArray_unsafeStringFromUtf8OrThrow, KConstRef thiz, KInt start, KInt size) {
    RETURN_RESULT_OF(CreateStringFromUtf8OrThrow, unsafeGetByteArrayData(thiz, start), size);
}

extern "C" OBJ_GETTER(Kotlin_ByteArray_unsafeStringFromUtf8, KConstRef thiz, KInt start, KInt size) {
    RETURN_RESULT_OF(CreateStringFromUtf8, unsafeGetByteArrayData(thiz, start), size);
}

extern "C" OBJ_GETTER(Kotlin_String_unsafeStringToUtf8, KConstRef thiz, KInt start, KInt size) {
    RETURN_RESULT_OF(unsafeConvertToUTF8<KStringConversionMode::REPLACE_INVALID>, thiz, start, size);
}

extern "C" OBJ_GETTER(Kotlin_String_unsafeStringToUtf8OrThrow, KConstRef thiz, KInt start, KInt size) {
    RETURN_RESULT_OF(unsafeConvertToUTF8<KStringConversionMode::CHECKED>, thiz, start, size);
}

extern "C" KInt Kotlin_StringBuilder_insertString(KRef builder, KInt distIndex, KConstRef fromString, KInt sourceIndex, KInt count) {
    auto toArray = builder->array();
    RuntimeAssert(sourceIndex >= 0 && static_cast<uint32_t>(sourceIndex + count) <= StringUtf16Length(fromString), "must be true");
    RuntimeAssert(distIndex >= 0 && static_cast<uint32_t>(distIndex + count) <= toArray->count_, "must be true");
    memcpy(CharArrayAddressOfElementAt(toArray, distIndex), StringUtf16Data(fromString) + sourceIndex, count * sizeof(KChar));
    return count;
}

extern "C" KInt Kotlin_StringBuilder_insertInt(KRef builder, KInt position, KInt value) {
    auto toArray = builder->array();
    RuntimeAssert(toArray->count_ >= static_cast<uint32_t>(11 + position), "must be true");
    char cstring[12];
    auto length = std::snprintf(cstring, sizeof(cstring), "%d", value);
    RuntimeAssert(length >= 0, "This should never happen"); // may be overkill
    RuntimeAssert(static_cast<size_t>(length) < sizeof(cstring), "Unexpectedly large value"); // Can't be, but this is what sNprintf for
    auto* from = &cstring[0];
    auto* to = CharArrayAddressOfElementAt(toArray, position);
    while (*from) {
        *to++ = *from++;
    }
    return from - cstring;
}

static std::optional<KInt> Kotlin_String_cachedHashCode(KConstRef thiz) {
    auto header = StringHeader::of(thiz);
    if (header->size() == 0) return 0;
    auto hash = kotlin::std_support::atomic_ref{header->hashCode_}.load(std::memory_order_relaxed);
    if (hash || kotlin::std_support::atomic_ref{header->flags_}.load(std::memory_order_relaxed) & StringHeader::HASHCODE_IS_ZERO) {
        return hash;
    }
    return {};
}

extern "C" KBoolean Kotlin_String_equals(KConstRef thiz, KConstRef other) {
    if (other == nullptr || other->type_info() != theStringTypeInfo) return false;
    if (thiz == other) return true;
    // TODO: this assumes identical encodings
    return StringRawSize(thiz) == StringRawSize(other) &&
        memcmp(StringRawData(thiz), StringRawData(other), StringRawSize(thiz)) == 0;
}

// Bounds checks is are performed on Kotlin side
extern "C" KBoolean Kotlin_String_unsafeRangeEquals(KConstRef thiz, KInt thizOffset, KConstRef other, KInt otherOffset, KInt length) {
    return memcmp(StringUtf16Data(thiz) + thizOffset, StringUtf16Data(other) + otherOffset, length * sizeof(KChar)) == 0;
}

extern "C" KBoolean Kotlin_Char_isISOControl(KChar ch) {
    return (ch <= 0x1F) || (ch >= 0x7F && ch <= 0x9F);
}

extern "C" KBoolean Kotlin_Char_isHighSurrogate(KChar ch) {
    return ((ch & 0xfc00) == 0xd800);
}

extern "C" KBoolean Kotlin_Char_isLowSurrogate(KChar ch) {
    return ((ch & 0xfc00) == 0xdc00);
}

extern "C" KInt Kotlin_String_indexOfChar(KConstRef thiz, KChar ch, KInt fromIndex) {
    if (fromIndex < 0) {
        fromIndex = 0;
    }
    KInt count = Kotlin_String_getStringLength(thiz);
    if (static_cast<uint32_t>(fromIndex) > static_cast<uint32_t>(count)) {
        return -1;
    }
    auto thizRaw = StringUtf16Data(thiz) + fromIndex;
    while (fromIndex < count) {
        if (*thizRaw++ == ch) return fromIndex;
        fromIndex++;
    }
    return -1;
}

extern "C" KInt Kotlin_String_lastIndexOfChar(KConstRef thiz, KChar ch, KInt fromIndex) {
    auto length = static_cast<uint32_t>(Kotlin_String_getStringLength(thiz));
    if (fromIndex < 0 || length == 0) {
        return -1;
    }
    if (static_cast<uint32_t>(fromIndex) >= length) {
        fromIndex = length - 1;
    }
    KInt index = fromIndex;
    const KChar* thizRaw = StringUtf16Data(thiz) + index;
    while (index >= 0) {
        if (*thizRaw-- == ch) return index;
        index--;
    }
    return -1;
}

// TODO: or code up Knuth-Moris-Pratt,
//       or use std::search with std::boyer_moore_searcher:
//       https://en.cppreference.com/w/cpp/algorithm/search
extern "C" KInt Kotlin_String_indexOfString(KConstRef thiz, KConstRef other, KInt fromIndex) {
    if (fromIndex < 0) {
        fromIndex = 0;
    }
    KInt thizLength = Kotlin_String_getStringLength(thiz);
    KInt otherLength = Kotlin_String_getStringLength(other);
    if (static_cast<uint32_t>(fromIndex) >= static_cast<uint32_t>(thizLength)) {
        return otherLength == 0 ? thizLength : -1;
    }
    if (otherLength > thizLength - fromIndex) {
        return -1;
    }
    // An empty string can be always found.
    if (otherLength == 0) {
        return fromIndex;
    }

    auto thizRaw = StringUtf16Data(thiz);
    auto otherRaw = StringUtf16Data(other);
    auto otherRawSize = StringRawSize(other);
    while (true) {
        void* result = memmem(thizRaw + fromIndex, (thizLength - fromIndex) * sizeof(KChar),
                              otherRaw, otherRawSize);
        if (result == nullptr) return -1;
        auto byteIndex = reinterpret_cast<intptr_t>(result) - reinterpret_cast<intptr_t>(thizRaw);
        if (byteIndex % sizeof(KChar) == 0) {
            return byteIndex / sizeof(KChar);
        } else {
            fromIndex = byteIndex / sizeof(KChar) + 1;
        }
    }
}

extern "C" KInt Kotlin_String_lastIndexOfString(KConstRef thiz, KConstRef other, KInt fromIndex) {
    KInt count = Kotlin_String_getStringLength(thiz);
    KInt otherCount = Kotlin_String_getStringLength(other);

    if (fromIndex < 0 || otherCount > count) {
        return -1;
    }
    if (otherCount == 0) {
        return fromIndex < count ? fromIndex : count;
    }

    KInt start = std::min(fromIndex, count - otherCount);
    KChar firstChar = Kotlin_String_get(other, 0);
    while (true) {
        KInt candidate = Kotlin_String_lastIndexOfChar(thiz, firstChar, start);
        if (candidate == -1) return -1;
        if (memcmp(StringUtf16Data(thiz) + candidate, StringUtf16Data(other), otherCount * sizeof(KChar)) == 0) {
            return candidate;
        }
        start = candidate - 1;
    }
}

extern "C" KInt Kotlin_String_hashCode(KRef thiz) {
    if (auto cached = Kotlin_String_cachedHashCode(thiz)) {
        return *cached;
    }
    KInt result = polyHash(StringUtf16Length(thiz), StringUtf16Data(thiz));

    auto header = StringHeader::of(thiz);
    // Having exactly one write per computation allows them to be relaxed, since there's no need to order them with any other write.
    // Since most relevant platforms have atomic word-sized writes by default, this is theoretically much faster.
    if (result != 0) {
        kotlin::std_support::atomic_ref{header->hashCode_}.store(result, std::memory_order_relaxed);
    } else {
        kotlin::std_support::atomic_ref{header->flags_}.fetch_or(StringHeader::HASHCODE_IS_ZERO, std::memory_order_relaxed);
    }
    return result;
}

extern "C" const KChar* Kotlin_String_utf16pointer(KConstRef message) {
    return StringUtf16Data(message);
}

extern "C" KInt Kotlin_String_utf16length(KConstRef message) {
    return StringRawSize(message);
}

extern "C" KConstNativePtr Kotlin_Arrays_getStringAddressOfElement(KConstRef thiz, KInt index) {
    return reinterpret_cast<KConstNativePtr>(boundsCheckedIteratorAt(thiz, index));
}

template <KStringConversionMode mode>
std::string kotlin::to_string(KConstRef kstring, size_t start, size_t size) noexcept(mode != KStringConversionMode::CHECKED) {
    auto length = StringUtf16Length(kstring);
    RuntimeAssert(start <= length, "start index out of bounds");
    auto utf16 = StringUtf16Data(kstring) + start;
    if (size == std::string::npos) {
        size = length - start;
    } else {
        RuntimeAssert(size <= length - start, "size out of bounds");
    }
    std::string utf8;
    utf8.reserve(size);
    switch (mode) {
    case KStringConversionMode::UNCHECKED:
        utf8::unchecked::utf16to8(utf16, utf16 + size, back_inserter(utf8));
        break;
    case KStringConversionMode::CHECKED:
        utf8::utf16to8(utf16, utf16 + size, back_inserter(utf8));
        break;
    case KStringConversionMode::REPLACE_INVALID:
        utf8::with_replacement::utf16to8(utf16, utf16 + size, back_inserter(utf8));
        break;
    }
    return utf8;
}

template std::string kotlin::to_string<KStringConversionMode::CHECKED>(KConstRef, size_t, size_t);
template std::string kotlin::to_string<KStringConversionMode::UNCHECKED>(KConstRef, size_t, size_t) noexcept;
template std::string kotlin::to_string<KStringConversionMode::REPLACE_INVALID>(KConstRef, size_t, size_t) noexcept;
