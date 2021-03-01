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

#include <limits>
#include <string.h>

#include "KAssert.h"
#include "City.h"
#include "Exceptions.h"
#include "Memory.h"
#include "Natives.h"
#include "KString.h"
#include "Porting.h"
#include "Types.h"

#include "utf8.h"

#include "polyhash/PolyHash.h"

namespace {

typedef std::back_insert_iterator<KStdString> KStdStringInserter;
typedef KChar* utf8to16(const char*, const char*, KChar*);
typedef KStdStringInserter utf16to8(const KChar*,const KChar*, KStdStringInserter);

KStdStringInserter utf16toUtf8OrThrow(const KChar* start, const KChar* end, KStdStringInserter result) {
  TRY_CATCH(result = utf8::utf16to8(start, end, result),
            result = utf8::unchecked::utf16to8(start, end, result),
            ThrowCharacterCodingException());
  return result;
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
  KStdString utf8;
  utf8.reserve(size);
  conversion(utf16, utf16 + size, back_inserter(utf8));
  ArrayHeader* result = AllocArrayInstance(theByteArrayTypeInfo, utf8.size(), OBJ_RESULT)->array();
  ::memcpy(ByteArrayAddressOfElementAt(result, 0), utf8.c_str(), utf8.size());
  RETURN_OBJ(result->obj());
}

OBJ_GETTER(utf8ToUtf16OrThrow, const char* rawString, size_t rawStringLength) {
  const char* end = rawString + rawStringLength;
  uint32_t charCount;
  TRY_CATCH(charCount = utf8::utf16_length(rawString, end),
            charCount = utf8::unchecked::utf16_length(rawString, end),
            ThrowCharacterCodingException());
  RETURN_RESULT_OF(utf8ToUtf16Impl<utf8::unchecked::utf8to16>, rawString, end, charCount);
}

OBJ_GETTER(utf8ToUtf16, const char* rawString, size_t rawStringLength) {
  const char* end = rawString + rawStringLength;
  uint32_t charCount = utf8::with_replacement::utf16_length(rawString, end);
  RETURN_RESULT_OF(utf8ToUtf16Impl<utf8::with_replacement::utf8to16>, rawString, end, charCount);
}

constexpr KChar digitKeys[] = {
  0x30, 0x41, 0x61, 0x660, 0x6f0, 0x966, 0x9e6, 0xa66, 0xae6, 0xb66, 0xbe7, 0xc66, 0xce6, 0xd66, 0xe50, 0xed0, 0xf20, 0x1040, 0x1369, 0x17e0,
  0x1810, 0xff10, 0xff21, 0xff41
};

constexpr KChar digitValues[] = {
  0x39, 0x30, 0x5a, 0x37, 0x7a, 0x57, 0x669, 0x660, 0x6f9, 0x6f0, 0x96f, 0x966, 0x9ef, 0x9e6, 0xa6f, 0xa66, 0xaef, 0xae6, 0xb6f, 0xb66,
  0xbef, 0xbe6, 0xc6f, 0xc66, 0xcef, 0xce6, 0xd6f, 0xd66, 0xe59, 0xe50, 0xed9, 0xed0, 0xf29, 0xf20, 0x1049, 0x1040, 0x1371, 0x1368, 0x17e9, 0x17e0,
  0x1819, 0x1810, 0xff19, 0xff10, 0xff3a, 0xff17, 0xff5a, 0xff37
};

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
  KStdString utf8;
  utf8.reserve(kstring->count_);
  utf8::unchecked::utf16to8(utf16, utf16 + kstring->count_, back_inserter(utf8));
  char* result = reinterpret_cast<char*>(konan::calloc(1, utf8.size() + 1));
  ::memcpy(result, utf8.c_str(), utf8.size());
  return result;
}

void DisposeCString(char* cstring) {
  if (cstring) konan::free(cstring);
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
    ThrowArrayIndexOutOfBoundsException();
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

OBJ_GETTER(Kotlin_String_toCharArray, KString string, KInt start, KInt size) {
  ArrayHeader* result = AllocArrayInstance(theCharArrayTypeInfo, size, OBJ_RESULT)->array();
  memcpy(CharArrayAddressOfElementAt(result, 0),
         CharArrayAddressOfElementAt(string, start),
         size * sizeof(KChar));
  RETURN_OBJ(result->obj());
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
  int result = memcmp(
    CharArrayAddressOfElementAt(thiz, 0),
    CharArrayAddressOfElementAt(other, 0),
    (thiz->count_ < other->count_ ? thiz->count_ : other->count_) * sizeof(KChar));
  if (result != 0) return result;
  int diff = thiz->count_ - other->count_;
  if (diff == 0) return 0;
  return diff < 0 ? -1 : 1;
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
  auto length = konan::snprintf(cstring, sizeof(cstring), "%d", value);
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

KBoolean Kotlin_Char_isIdentifierIgnorable(KChar ch) {
  RuntimeAssert(false, "Kotlin_Char_isIdentifierIgnorable() is not implemented");
  return false;
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

constexpr KInt digits[] = {
  0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
  -1, -1, -1, -1, -1, -1, -1,
  10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
  20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
  30, 31, 32, 33, 34, 35,
  -1, -1, -1, -1, -1, -1,
  10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
  20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
  30, 31, 32, 33, 34, 35
};

// Based on Apache Harmony implementation.
// Radix check is performed on the Kotlin side.
KInt Kotlin_Char_digitOfChecked(KChar ch, KInt radix) {

  KInt result = -1;
  if (ch >= 0x30 /* 0 */ && ch <= 0x7a /* z */) {
    result = digits[ch - 0x30];
  } else {
    int index = -1;
    index = binarySearchRange(digitKeys, ARRAY_SIZE(digitKeys), ch);
    if (index >= 0 && ch <= digitValues[index * 2]) {
      result = ch - digitValues[index * 2 + 1];
    }
  }
  if (result >= radix) return -1;
  return result;
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

// TODO: or code up Knuth-Moris-Pratt.
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
  const KChar* thizRaw = CharArrayAddressOfElementAt(thiz, fromIndex);
  const KChar* otherRaw = CharArrayAddressOfElementAt(other, 0);
  void* result = konan::memmem(thizRaw, (thiz->count_ - fromIndex) * sizeof(KChar),
                               otherRaw, other->count_ * sizeof(KChar));
  if (result == nullptr) return -1;

  return (reinterpret_cast<intptr_t>(result) - reinterpret_cast<intptr_t>(
      CharArrayAddressOfElementAt(thiz, 0))) / sizeof(KChar);
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
