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

typedef std::back_insert_iterator<std::string> KStdStringInserter;
typedef KChar* utf8to16(const char*, const char*, KChar*);
typedef KStdStringInserter utf16to8(const KChar*,const KChar*, KStdStringInserter);

KStdStringInserter utf16toUtf8OrThrow(const KChar* start, const KChar* end, KStdStringInserter result) {
    try {
        result = utf8::utf16to8(start, end, result);
        return result;
    } catch (...) {
        ThrowCharacterCodingException();
    }
}

template<utf8to16 conversion>
OBJ_GETTER(utf8ToUtf16Impl, const char* rawString, const char* end, uint32_t charCount) {
  if (rawString == nullptr) RETURN_OBJ(nullptr);
  ArrayHeader* result = AllocArrayInstance(theStringTypeInfo, charCount, OBJ_RESULT)->array();
  KChar* rawResult = CharArrayAddressOfElementAt(result, 0);
  conversion(rawString, end, rawResult);
  RETURN_OBJ(result->obj());
}

template<utf16to8 conversion>
OBJ_GETTER(unsafeUtf16ToUtf8Impl, KString thiz, KInt start, KInt size) {
  RuntimeAssert(thiz->type_info() == theStringTypeInfo, "Must use String");
  const KChar* utf16 = CharArrayAddressOfElementAt(thiz, start);
  std::string utf8;
  utf8.reserve(size);
  conversion(utf16, utf16 + size, back_inserter(utf8));
  ArrayHeader* result = AllocArrayInstance(theByteArrayTypeInfo, utf8.size(), OBJ_RESULT)->array();
  ::memcpy(ByteArrayAddressOfElementAt(result, 0), utf8.c_str(), utf8.size());
  RETURN_OBJ(result->obj());
}

OBJ_GETTER(utf8ToUtf16OrThrow, const char* rawString, size_t rawStringLength) {
  const char* end = rawString + rawStringLength;
  uint32_t charCount;
  try {
      charCount = utf8::utf16_length(rawString, end);
  } catch (...) {
      ThrowCharacterCodingException();
  }
  RETURN_RESULT_OF(utf8ToUtf16Impl<utf8::unchecked::utf8to16>, rawString, end, charCount);
}

OBJ_GETTER(utf8ToUtf16, const char* rawString, size_t rawStringLength) {
  const char* end = rawString + rawStringLength;
  uint32_t charCount = utf8::with_replacement::utf16_length(rawString, end);
  RETURN_RESULT_OF(utf8ToUtf16Impl<utf8::with_replacement::utf8to16>, rawString, end, charCount);
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

template <typename T>
int threeWayCompare(T a, T b) {
    return (a == b) ? 0 : (a < b ? -1 : 1);
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

extern "C" {

OBJ_GETTER(CreateStringFromCString, const char* cstring) {
  RETURN_RESULT_OF(utf8ToUtf16, cstring, cstring ? strlen(cstring) : 0);
}

OBJ_GETTER(CreateStringFromUtf8, const char* utf8, uint32_t lengthBytes) {
  RETURN_RESULT_OF(utf8ToUtf16, utf8, lengthBytes);
}

char* CreateCStringFromString(KConstRef kref) {
  if (kref == nullptr) return nullptr;
  KString kstring = kref->array();
  const KChar* utf16 = CharArrayAddressOfElementAt(kstring, 0);
  std::string utf8;
  utf8.reserve(kstring->count_);
  utf8::unchecked::utf16to8(utf16, utf16 + kstring->count_, back_inserter(utf8));
  char* result = reinterpret_cast<char*>(std::calloc(1, utf8.size() + 1));
  ::memcpy(result, utf8.c_str(), utf8.size());
  return result;
}

void DisposeCString(char* cstring) {
    if (cstring) std::free(cstring);
}

ObjHeader* CreatePermanentStringFromCString(const char* nullTerminatedUTF8) {
    // Note: this function can be called in "Native" thread state. But this is fine:
    //   while it indeed manipulates Kotlin objects, it doesn't in fact access _Kotlin heap_,
    //   because the accessed object is off-heap, imitating permanent static objects.
    const char* end = nullTerminatedUTF8 + strlen(nullTerminatedUTF8);
    size_t count = utf8::with_replacement::utf16_length(nullTerminatedUTF8, end);
    size_t headerSize = alignUp(sizeof(ArrayHeader), alignof(char16_t));
    size_t arraySize = headerSize + count * sizeof(char16_t);

    ArrayHeader* header = (ArrayHeader*)std::calloc(arraySize, 1);
    header->obj()->typeInfoOrMeta_ = setPointerBits((TypeInfo *)theStringTypeInfo, OBJECT_TAG_PERMANENT_CONTAINER);
    header->count_ = count;
    utf8::with_replacement::utf8to16(nullTerminatedUTF8, end, CharArrayAddressOfElementAt(header, 0));

    return header->obj();
}

void FreePermanentStringForTests(ArrayHeader* header) {
    std::free(header);
}

// String.kt
OBJ_GETTER(Kotlin_String_replace, KString thiz, KChar oldChar, KChar newChar) {
  auto count = thiz->count_;
  ArrayHeader* result = AllocArrayInstance(theStringTypeInfo, count, OBJ_RESULT)->array();
  const KChar* thizRaw = CharArrayAddressOfElementAt(thiz, 0);
  KChar* resultRaw = CharArrayAddressOfElementAt(result, 0);
  for (uint32_t index = 0; index < count; ++index) {
    KChar thizChar = *thizRaw++;
    *resultRaw++ = thizChar == oldChar ? newChar : thizChar;
  }
  RETURN_OBJ(result->obj());
}

OBJ_GETTER(Kotlin_String_plusImpl, KString thiz, KString other) {
  RuntimeAssert(thiz != nullptr, "this cannot be null");
  RuntimeAssert(other != nullptr, "other cannot be null");
  RuntimeAssert(thiz->type_info() == theStringTypeInfo, "Must be a string");
  RuntimeAssert(other->type_info() == theStringTypeInfo, "Must be a string");
  RuntimeAssert(thiz->count_ <= static_cast<uint32_t>(std::numeric_limits<int32_t>::max()), "this cannot be this large");
  RuntimeAssert(other->count_ <= static_cast<uint32_t>(std::numeric_limits<int32_t>::max()), "other cannot be this large");
  // Since thiz and other sizes are bounded by int32_t max value, their sum cannot exceed uint32_t max value - 1.
  uint32_t result_length = thiz->count_ + other->count_;
  if (result_length > static_cast<uint32_t>(std::numeric_limits<int32_t>::max())) {
    ThrowOutOfMemoryError();
  }
  ArrayHeader* result = AllocArrayInstance(theStringTypeInfo, result_length, OBJ_RESULT)->array();
  memcpy(
      CharArrayAddressOfElementAt(result, 0),
      CharArrayAddressOfElementAt(thiz, 0),
      thiz->count_ * sizeof(KChar));
  memcpy(
      CharArrayAddressOfElementAt(result, thiz->count_),
      CharArrayAddressOfElementAt(other, 0),
      other->count_ * sizeof(KChar));
  RETURN_OBJ(result->obj());
}

OBJ_GETTER(Kotlin_String_unsafeStringFromCharArray, KConstRef thiz, KInt start, KInt size) {
  const ArrayHeader* array = thiz->array();
  RuntimeAssert(array->type_info() == theCharArrayTypeInfo, "Must use a char array");

  if (size == 0) {
    RETURN_RESULT_OF0(TheEmptyString);
  }

  ArrayHeader* result = AllocArrayInstance(theStringTypeInfo, size, OBJ_RESULT)->array();
  memcpy(CharArrayAddressOfElementAt(result, 0),
         CharArrayAddressOfElementAt(array, start),
         size * sizeof(KChar));
  RETURN_OBJ(result->obj());
}

OBJ_GETTER(Kotlin_String_toCharArray, KString string, KRef destination, KInt destinationOffset, KInt start, KInt size) {
  ArrayHeader* destinationArray = destination->array();
  memcpy(CharArrayAddressOfElementAt(destinationArray, destinationOffset),
         CharArrayAddressOfElementAt(string, start),
         size * sizeof(KChar));
  RETURN_OBJ(destinationArray->obj());
}

OBJ_GETTER(Kotlin_String_subSequence, KString thiz, KInt startIndex, KInt endIndex) {
  if (startIndex < 0 || static_cast<uint32_t>(endIndex) > thiz->count_ || startIndex > endIndex) {
    // TODO: is it correct exception?
    ThrowArrayIndexOutOfBoundsException();
  }
  if (startIndex == endIndex) {
    RETURN_RESULT_OF0(TheEmptyString);
  }
  KInt length = endIndex - startIndex;
  ArrayHeader* result = AllocArrayInstance(theStringTypeInfo, length, OBJ_RESULT)->array();
  memcpy(CharArrayAddressOfElementAt(result, 0),
         CharArrayAddressOfElementAt(thiz, startIndex),
         length * sizeof(KChar));
  RETURN_OBJ(result->obj());
}

KInt Kotlin_String_compareTo(KString thiz, KString other) {
    const uint16_t *first = CharArrayAddressOfElementAt(thiz, 0);
    const uint16_t *second = CharArrayAddressOfElementAt(other, 0);
    uint32_t minSize = std::min(thiz->count_, other->count_);
    uint32_t mismatch_position = mismatch(first, second, minSize);
    if (mismatch_position != minSize) {
        return threeWayCompare(first[mismatch_position], second[mismatch_position]);
    }
    return threeWayCompare(thiz->count_, other->count_);
}

KChar Kotlin_String_get(KString thiz, KInt index) {
  // We couldn't have created a string bigger than max KInt value.
  // So if index is < 0, conversion to an unsigned value would make it bigger
  // than the array size.
  if (static_cast<uint32_t>(index) >= thiz->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *CharArrayAddressOfElementAt(thiz, index);
}

KInt Kotlin_String_getStringLength(KString thiz) {
  return thiz->count_;
}

const char* unsafeByteArrayAsCString(KConstRef thiz, KInt start, KInt size) {
  const ArrayHeader* array = thiz->array();
  RuntimeAssert(array->type_info() == theByteArrayTypeInfo, "Must use a byte array");
  return reinterpret_cast<const char*>(ByteArrayAddressOfElementAt(array, start));
}

OBJ_GETTER(Kotlin_ByteArray_unsafeStringFromUtf8OrThrow, KConstRef thiz, KInt start, KInt size) {
  if (size == 0) {
    RETURN_RESULT_OF0(TheEmptyString);
  }
  const char* rawString = unsafeByteArrayAsCString(thiz, start, size);
  RETURN_RESULT_OF(utf8ToUtf16OrThrow, rawString, size);
}

OBJ_GETTER(Kotlin_ByteArray_unsafeStringFromUtf8, KConstRef thiz, KInt start, KInt size) {
  if (size == 0) {
    RETURN_RESULT_OF0(TheEmptyString);
  }
  const char* rawString = unsafeByteArrayAsCString(thiz, start, size);
  RETURN_RESULT_OF(utf8ToUtf16, rawString, size);
}

OBJ_GETTER(StringFromUtf8Buffer, const char* start, size_t size) {
  if (size == 0) {
    RETURN_RESULT_OF0(TheEmptyString);
  }
  RETURN_RESULT_OF(utf8ToUtf16, start, size);
}

OBJ_GETTER(Kotlin_String_unsafeStringToUtf8, KString thiz, KInt start, KInt size) {
  RETURN_RESULT_OF(unsafeUtf16ToUtf8Impl<utf8::with_replacement::utf16to8>, thiz, start, size);
}

OBJ_GETTER(Kotlin_String_unsafeStringToUtf8OrThrow, KString thiz, KInt start, KInt size) {
  RETURN_RESULT_OF(unsafeUtf16ToUtf8Impl<utf16toUtf8OrThrow>, thiz, start, size);
}

KInt Kotlin_StringBuilder_insertString(KRef builder, KInt distIndex, KString fromString, KInt sourceIndex, KInt count) {
  auto toArray = builder->array();
  RuntimeAssert(sourceIndex >= 0 && static_cast<uint32_t>(sourceIndex + count) <= fromString->count_, "must be true");
  RuntimeAssert(distIndex >= 0 && static_cast<uint32_t>(distIndex + count) <= toArray->count_, "must be true");
  memcpy(CharArrayAddressOfElementAt(toArray, distIndex),
         CharArrayAddressOfElementAt(fromString, sourceIndex),
         count * sizeof(KChar));
  return count;
}

KInt Kotlin_StringBuilder_insertInt(KRef builder, KInt position, KInt value) {
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


KBoolean Kotlin_String_equals(KString thiz, KConstRef other) {
  if (other == nullptr || other->type_info() != theStringTypeInfo) return false;
  // Important, due to literal internalization.
  KString otherString = other->array();
  if (thiz == otherString) return true;
  return thiz->count_ == otherString->count_ &&
      memcmp(CharArrayAddressOfElementAt(thiz, 0),
             CharArrayAddressOfElementAt(otherString, 0),
             thiz->count_ * sizeof(KChar)) == 0;
}

// Bounds checks is are performed on Kotlin side
KBoolean Kotlin_String_unsafeRangeEquals(KString thiz, KInt thizOffset, KString other, KInt otherOffset, KInt length) {
  return memcmp(
    CharArrayAddressOfElementAt(thiz, thizOffset),
    CharArrayAddressOfElementAt(other, otherOffset),
    length * sizeof(KChar)
  ) == 0;
}

KBoolean Kotlin_Char_isISOControl(KChar ch) {
  return (ch <= 0x1F) || (ch >= 0x7F && ch <= 0x9F);
}

KBoolean Kotlin_Char_isHighSurrogate(KChar ch) {
  return ((ch & 0xfc00) == 0xd800);
}

KBoolean Kotlin_Char_isLowSurrogate(KChar ch) {
  return ((ch & 0xfc00) == 0xdc00);
}

KInt Kotlin_String_indexOfChar(KString thiz, KChar ch, KInt fromIndex) {
  if (fromIndex < 0) {
    fromIndex = 0;
  }
  if (static_cast<uint32_t>(fromIndex) > thiz->count_) {
    return -1;
  }
  KInt count = thiz->count_;
  const KChar* thizRaw = CharArrayAddressOfElementAt(thiz, fromIndex);
  while (fromIndex < count) {
    if (*thizRaw++ == ch) return fromIndex;
    fromIndex++;
  }
  return -1;
}

KInt Kotlin_String_lastIndexOfChar(KString thiz, KChar ch, KInt fromIndex) {
  if (fromIndex < 0 || thiz->count_ == 0) {
    return -1;
  }
  if (static_cast<uint32_t>(fromIndex) >= thiz->count_) {
    fromIndex = thiz->count_ - 1;
  }
  KInt index = fromIndex;
  const KChar* thizRaw = CharArrayAddressOfElementAt(thiz, index);
  while (index >= 0) {
    if (*thizRaw-- == ch) return index;
    index--;
  }
  return -1;
}

// TODO: or code up Knuth-Moris-Pratt,
//       or use std::search with std::boyer_moore_searcher:
//       https://en.cppreference.com/w/cpp/algorithm/search
KInt Kotlin_String_indexOfString(KString thiz, KString other, KInt fromIndex) {
  if (fromIndex < 0) {
    fromIndex = 0;
  }
  if (static_cast<uint32_t>(fromIndex) >= thiz->count_) {
    return (other->count_ == 0) ? thiz->count_ : -1;
  }
  if (static_cast<KInt>(other->count_) > static_cast<KInt>(thiz->count_) - fromIndex) {
    return -1;
  }
  // An empty string can be always found.
  if (other->count_ == 0) {
    return fromIndex;
  }
  const KChar* thizRaw = CharArrayAddressOfElementAt(thiz, 0);
  const KChar* otherRaw = CharArrayAddressOfElementAt(other, 0);
  const auto otherSize = other->count_ * sizeof(KChar);
  while (true) {
    void* result = memmem(thizRaw + fromIndex, (thiz->count_ - fromIndex) * sizeof(KChar),
                                 otherRaw, otherSize);
    if (result == nullptr) return -1;
    auto byteIndex = reinterpret_cast<intptr_t>(result) - reinterpret_cast<intptr_t>(thizRaw);
    if (byteIndex % sizeof(KChar) == 0) {
      return byteIndex / sizeof(KChar);
    } else {
      fromIndex = byteIndex / sizeof(KChar) + 1;
    }
  }
}

KInt Kotlin_String_lastIndexOfString(KString thiz, KString other, KInt fromIndex) {
  KInt count = thiz->count_;
  KInt otherCount = other->count_;

  if (fromIndex < 0 || otherCount > count) {
    return -1;
  }
  if (otherCount == 0) {
    return fromIndex < count ? fromIndex : count;
  }

  KInt start = fromIndex;
  if (fromIndex > count - otherCount)
    start = count - otherCount;
  KChar firstChar = *CharArrayAddressOfElementAt(other, 0);
  while (true) {
    KInt candidate = Kotlin_String_lastIndexOfChar(thiz, firstChar, start);
    if (candidate == -1) return -1;
    KInt offsetThiz = candidate;
    KInt offsetOther = 0;
    while (++offsetOther < otherCount &&
           *CharArrayAddressOfElementAt(thiz, ++offsetThiz) ==
           *CharArrayAddressOfElementAt(other, offsetOther)) {}
    if (offsetOther == otherCount) {
      return candidate;
    }
    start = candidate - 1;
  }
}

KInt Kotlin_String_hashCode(KString thiz) {
  // TODO: consider caching strings hashes.
  return polyHash(thiz->count_, CharArrayAddressOfElementAt(thiz, 0));
}

const KChar* Kotlin_String_utf16pointer(KString message) {
  RuntimeAssert(message->type_info() == theStringTypeInfo, "Must use a string");
  const KChar* utf16 = CharArrayAddressOfElementAt(message, 0);
  return utf16;
}

KInt Kotlin_String_utf16length(KString message) {
  RuntimeAssert(message->type_info() == theStringTypeInfo, "Must use a string");
  return message->count_ * sizeof(KChar);
}


} // extern "C"
