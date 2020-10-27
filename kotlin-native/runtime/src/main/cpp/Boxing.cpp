/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

#include "Memory.h"
#include "Types.h"

// C++ part of box caching.

template<class T>
struct KBox {
  ObjHeader header;
  const T value;
};

// Keep naming of these in sync with codegen part.
extern const KBoolean BOOLEAN_RANGE_FROM;
extern const KBoolean BOOLEAN_RANGE_TO;

extern const KByte BYTE_RANGE_FROM;
extern const KByte BYTE_RANGE_TO;

extern const KChar CHAR_RANGE_FROM;
extern const KChar CHAR_RANGE_TO;

extern const KShort SHORT_RANGE_FROM;
extern const KShort SHORT_RANGE_TO;

extern const KInt INT_RANGE_FROM;
extern const KInt INT_RANGE_TO;

extern const KLong LONG_RANGE_FROM;
extern const KLong LONG_RANGE_TO;

extern KBox<KBoolean> BOOLEAN_CACHE[];
extern KBox<KByte>    BYTE_CACHE[];
extern KBox<KChar>    CHAR_CACHE[];
extern KBox<KShort>   SHORT_CACHE[];
extern KBox<KInt>     INT_CACHE[];
extern KBox<KLong>    LONG_CACHE[];

namespace {

template<class T>
inline bool isInRange(T value, T from, T to) {
  return value >= from && value <= to;
}

template<class T>
OBJ_GETTER(getCachedBox, T value, KBox<T> cache[], T from) {
  uint64_t index = value - from;
  RETURN_OBJ(&cache[index].header);
}

} // namespace

extern "C" {

bool inBooleanBoxCache(KBoolean value) {
  return isInRange(value, BOOLEAN_RANGE_FROM, BOOLEAN_RANGE_TO);
}

bool inByteBoxCache(KByte value) {
  return isInRange(value, BYTE_RANGE_FROM, BYTE_RANGE_TO);
}

bool inCharBoxCache(KChar value) {
  return isInRange(value, CHAR_RANGE_FROM, CHAR_RANGE_TO);
}

bool inShortBoxCache(KShort value) {
  return isInRange(value, SHORT_RANGE_FROM, SHORT_RANGE_TO);
}

bool inIntBoxCache(KInt value) {
  return isInRange(value, INT_RANGE_FROM, INT_RANGE_TO);
}

bool inLongBoxCache(KLong value) {
  return isInRange(value, LONG_RANGE_FROM, LONG_RANGE_TO);
}

OBJ_GETTER(getCachedBooleanBox, KBoolean value) {
  RETURN_RESULT_OF(getCachedBox, value, BOOLEAN_CACHE, BOOLEAN_RANGE_FROM);
}

OBJ_GETTER(getCachedByteBox, KByte value) {
  // Remember that KByte can't handle values >= 127
  // so it can't be used as indexing type.
  RETURN_RESULT_OF(getCachedBox, value, BYTE_CACHE, BYTE_RANGE_FROM);
}

OBJ_GETTER(getCachedCharBox, KChar value) {
  RETURN_RESULT_OF(getCachedBox, value, CHAR_CACHE, CHAR_RANGE_FROM);
}

OBJ_GETTER(getCachedShortBox, KShort value) {
  RETURN_RESULT_OF(getCachedBox, value, SHORT_CACHE, SHORT_RANGE_FROM);
}

OBJ_GETTER(getCachedIntBox, KInt value) {
  RETURN_RESULT_OF(getCachedBox, value, INT_CACHE, INT_RANGE_FROM);
}

OBJ_GETTER(getCachedLongBox, KLong value) {
  RETURN_RESULT_OF(getCachedBox, value, LONG_CACHE, LONG_RANGE_FROM);
}

}