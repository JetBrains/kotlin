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

#ifndef RUNTIME_TYPES_H
#define RUNTIME_TYPES_H

#include <stdlib.h>

#if (KONAN_WASM || KONAN_ZEPHYR) && !defined(assert)
// assert() is needed by STLport.
#define assert(cond) if (!(cond)) abort()
#endif

#include <deque>
#include <list>
#include <map>
#include <string>
#include <set>
#include <unordered_map>
#include <unordered_set>
#include <vector>

#include "Alloc.h"
#include "Common.h"
#include "Memory.h"
#include "TypeInfo.h"

// Note that almost all types are signed.
typedef bool KBoolean;
typedef int8_t  KByte;
typedef uint16_t KChar;
typedef int16_t KShort;
typedef int32_t KInt;
typedef int64_t KLong;
typedef uint8_t  KUByte;
typedef uint16_t KUShort;
typedef uint32_t KUInt;
typedef uint64_t KULong;
typedef float   KFloat;
typedef double  KDouble;
typedef void*   KNativePtr;
typedef KFloat __attribute__ ((__vector_size__ (16)))   KVector4f;

typedef const void* KConstNativePtr;

typedef ObjHeader* KRef;
typedef const ObjHeader* KConstRef;
typedef const ArrayHeader* KString;

// Definitions of STL classes used inside Konan runtime.
typedef std::basic_string<char, std::char_traits<char>,
                          KonanAllocator<char>> KStdString;
template<class Value>
using KStdDeque = std::deque<Value, KonanAllocator<Value>>;
template<class Key, class Value>
using KStdUnorderedMap = std::unordered_map<Key, Value,
  std::hash<Key>, std::equal_to<Key>,
  KonanAllocator<std::pair<const Key, Value>>>;
template<class Value>
using KStdUnorderedSet = std::unordered_set<Value,
  std::hash<Value>, std::equal_to<Value>,
  KonanAllocator<Value>>;
template<class Value, class Compare = std::less<Value>>
using KStdOrderedSet = std::set<Value, Compare, KonanAllocator<Value>>;
template<class Key, class Value, class Compare = std::less<Key>>
using KStdOrderedMap = std::map<Key, Value, Compare, KonanAllocator<std::pair<const Key, Value>>>;
template<class Value>
using KStdVector = std::vector<Value, KonanAllocator<Value>>;
template<class Value>
using KStdList = std::list<Value, KonanAllocator<Value>>;

#ifdef __cplusplus
extern "C" {
#endif

extern const TypeInfo* theAnyTypeInfo;
extern const TypeInfo* theArrayTypeInfo;
extern const TypeInfo* theBooleanArrayTypeInfo;
extern const TypeInfo* theByteArrayTypeInfo;
extern const TypeInfo* theCharArrayTypeInfo;
extern const TypeInfo* theDoubleArrayTypeInfo;
extern const TypeInfo* theForeignObjCObjectTypeInfo;
extern const TypeInfo* theIntArrayTypeInfo;
extern const TypeInfo* theLongArrayTypeInfo;
extern const TypeInfo* theNativePtrArrayTypeInfo;
extern const TypeInfo* theFloatArrayTypeInfo;
extern const TypeInfo* theForeignObjCObjectTypeInfo;
extern const TypeInfo* theFreezableAtomicReferenceTypeInfo;
extern const TypeInfo* theObjCObjectWrapperTypeInfo;
extern const TypeInfo* theOpaqueFunctionTypeInfo;
extern const TypeInfo* theShortArrayTypeInfo;
extern const TypeInfo* theStringTypeInfo;
extern const TypeInfo* theThrowableTypeInfo;
extern const TypeInfo* theUnitTypeInfo;
extern const TypeInfo* theWorkerBoundReferenceTypeInfo;
extern const TypeInfo* theCleanerImplTypeInfo;

KBoolean IsInstance(const ObjHeader* obj, const TypeInfo* type_info) RUNTIME_PURE;
KBoolean IsInstanceOfClassFast(const ObjHeader* obj, int32_t lo, int32_t hi) RUNTIME_PURE;
void CheckCast(const ObjHeader* obj, const TypeInfo* type_info);
KBoolean IsArray(KConstRef obj) RUNTIME_PURE;
bool IsSubInterface(const TypeInfo* thiz, const TypeInfo* other) RUNTIME_PURE;

#ifdef __cplusplus
}
#endif

#endif // RUNTIME_TYPES_H
