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

#include "SourceInfo.h"

#ifdef KONAN_CORE_SYMBOLICATION
#include <KAssert.h>
#include <dlfcn.h>
#include <limits.h>
#include <stdint.h>
#include <unistd.h>

typedef struct _CSTypeRef {
  unsigned long type;
  void* contents;
} CSTypeRef;

typedef CSTypeRef CSSymbolicatorRef;
typedef CSTypeRef CSSymbolOwnerRef;
typedef CSTypeRef CSSymbolRef;
typedef CSTypeRef CSSourceInfoRef;

typedef struct _CSRange {
  unsigned long long location;
  unsigned long long length;
} CSRange;

typedef unsigned long long CSArchitecture;

#define kCSNow LLONG_MAX

namespace {

CSSymbolicatorRef (*CSSymbolicatorCreateWithPid)(pid_t pid);

CSSymbolOwnerRef (*CSSymbolicatorGetSymbolOwnerWithAddressAtTime)(
    CSSymbolicatorRef symbolicator,
    unsigned long long address,
    long long time
);

CSSourceInfoRef (*CSSymbolOwnerGetSourceInfoWithAddress)(
    CSSymbolOwnerRef owner,
    unsigned long long address
);


const char* (*CSSourceInfoGetPath)(CSSourceInfoRef info);

uint32_t (*CSSourceInfoGetLineNumber)(CSSourceInfoRef info);

uint32_t (*CSSourceInfoGetColumn)(CSSourceInfoRef info);

bool (*CSIsNull)(CSTypeRef);
CSSymbolRef (*CSSourceInfoGetSymbol)(CSSourceInfoRef info);

typedef int (^CSSourceInfoIterator)(CSSourceInfoRef);
int (*CSSymbolForeachSourceInfo)(CSSymbolRef, CSSourceInfoIterator);

CSRange (*CSSourceInfoGetRange)(CSSourceInfoRef);

CSSymbolRef (*CSSymbolOwnerGetSymbolWithAddress)(CSSymbolOwnerRef, unsigned long long);
CSSymbolicatorRef symbolicator;

bool TryInitializeCoreSymbolication() {
  void* cs = dlopen("/System/Library/PrivateFrameworks/CoreSymbolication.framework/CoreSymbolication", RTLD_LAZY);
  if (!cs) return false;

#define KONAN_CS_LOOKUP(name) name = (decltype(name)) dlsym(cs, #name); if (!name) return false;

  KONAN_CS_LOOKUP(CSSymbolicatorCreateWithPid)
  KONAN_CS_LOOKUP(CSSymbolicatorGetSymbolOwnerWithAddressAtTime)
  KONAN_CS_LOOKUP(CSSymbolOwnerGetSourceInfoWithAddress)
  KONAN_CS_LOOKUP(CSSourceInfoGetPath)
  KONAN_CS_LOOKUP(CSSourceInfoGetLineNumber)
  KONAN_CS_LOOKUP(CSSourceInfoGetColumn)
  KONAN_CS_LOOKUP(CSIsNull)
  KONAN_CS_LOOKUP(CSSourceInfoGetSymbol)
  KONAN_CS_LOOKUP(CSSymbolForeachSourceInfo)
  KONAN_CS_LOOKUP(CSSymbolOwnerGetSymbolWithAddress)
  KONAN_CS_LOOKUP(CSSourceInfoGetRange)
#undef KONAN_CS_LOOKUP

  symbolicator = CSSymbolicatorCreateWithPid(getpid());
  return !CSIsNull(symbolicator);
}

} // namespace

extern "C" struct SourceInfo Kotlin_getSourceInfo(void* addr) {
  __block SourceInfo result = { .fileName = nullptr, .lineNumber = -1, .column = -1 };

  static bool csIsAvailable = TryInitializeCoreSymbolication();

  if (csIsAvailable) {
    unsigned long long address = static_cast<unsigned long long>((uintptr_t)addr);

    CSSymbolOwnerRef symbolOwner = CSSymbolicatorGetSymbolOwnerWithAddressAtTime(symbolicator, address, kCSNow);
    if (CSIsNull(symbolOwner))
      return result;
    CSSymbolRef symbol = CSSymbolOwnerGetSymbolWithAddress(symbolOwner, address);
    if (CSIsNull(symbol))
      return result;

    CSSymbolForeachSourceInfo(symbol,
      ^(CSSourceInfoRef ref) {
          uint32_t lineNumber = CSSourceInfoGetLineNumber(ref);
          CSRange range = CSSourceInfoGetRange(ref);
          if (lineNumber != 0
              && address >= range.location
              && address < range.location + range.length) {
            const char* fileName = CSSourceInfoGetPath(ref);
            if (fileName != nullptr) {
              result.fileName = fileName;
              result.lineNumber = lineNumber;
              result.column = CSSourceInfoGetColumn(ref);
            }
       }
       return 0;
   });
  }
  return result;
}

#else // KONAN_CORE_SYMBOLICATION

extern "C" struct SourceInfo Kotlin_getSourceInfo(void* addr) {
  return (SourceInfo) { .fileName = nullptr, .lineNumber = -1, .column = -1 };
}

#endif // KONAN_CORE_SYMBOLICATION
