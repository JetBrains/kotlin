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
#include <cstdint>
#include <dlfcn.h>
#include <limits>
#include <string.h>
#include <unistd.h>

#define TRACE_SYMBOLICATION 0
#if TRACE_SYMBOLICATION
#include <stdio.h>
#define SYM_LOG(...) fprintf(stderr, __VA_ARGS__)
#define SYM_DUMP(p) CSShow((p))
#else
#define SYM_LOG(...)
#define SYM_DUMP(p)
#endif

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

constexpr auto kCSNow = std::numeric_limits<long long>::max();

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
/**
 * Function used for debug.
 */
#if TRACE_SYMBOLICATION
void (*CSShow)(CSTypeRef);
#endif

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

#if TRACE_SYMBOLICATION
  KONAN_CS_LOOKUP(CSShow)
#endif
#undef KONAN_CS_LOOKUP

  symbolicator = CSSymbolicatorCreateWithPid(getpid());
  return !CSIsNull(symbolicator);
}

} // namespace

typedef struct {
  const char * fileName;
  int start;
  int end;
} SymbolSourceInfoLimits;

extern "C" struct SourceInfo Kotlin_getSourceInfo(void* addr) {
  __block SourceInfo result = { .fileName = nullptr, .lineNumber = -1, .column = -1 };
  __block bool continueUpdateResult = true;
  __block SymbolSourceInfoLimits limits = {.start = -1, .end = -1};

  static bool csIsAvailable = TryInitializeCoreSymbolication();

  if (csIsAvailable) {
    unsigned long long address = static_cast<unsigned long long>((uintptr_t)addr);

    CSSymbolOwnerRef symbolOwner = CSSymbolicatorGetSymbolOwnerWithAddressAtTime(symbolicator, address, kCSNow);
    if (CSIsNull(symbolOwner))
      return result;
    CSSymbolRef symbol = CSSymbolOwnerGetSymbolWithAddress(symbolOwner, address);
    if (CSIsNull(symbol))
      return result;
    SYM_LOG("Kotlin_getSourceInfo: address: %p\n", addr);
    SYM_DUMP(symbol);


    /**
     * ASSUMPTION: we assume that the _first_ and the _last_ source infos should belong to real function(symbol) the rest might belong to
     * inlined functions.
     */
    CSSymbolForeachSourceInfo(symbol,
      ^(CSSourceInfoRef ref) {
        // Expecting CSSourceInfoGetLineNumber not to overflow int32_t max value.
        int32_t lineNumber = CSSourceInfoGetLineNumber(ref);
        if (lineNumber == 0)
          return 0;
        if (limits.start == -1) {
          limits.start = lineNumber;
          limits.fileName = CSSourceInfoGetPath(ref);
        } else {
          limits.end = lineNumber;
        }
        return 0;
    });

    SYM_LOG("limits: {%s %d..%d}\n", limits.fileName, limits.start, limits.end);
    result.fileName = limits.fileName;

    CSSymbolForeachSourceInfo(symbol,
      ^(CSSourceInfoRef ref) {
          // Expecting CSSourceInfoGetLineNumber not to overflow int32_t max value.
          int32_t lineNumber = CSSourceInfoGetLineNumber(ref);
          if (lineNumber == 0)
            return 0;
          SYM_DUMP(ref);
          CSRange range = CSSourceInfoGetRange(ref);
          const char* fileName = CSSourceInfoGetPath(ref);
          /**
           * We need to change API fo Kotlin_getSourceInfo to return information about inlines,
           * but for a moment we have to track that we updating result info _only_ for upper level or _inlined at_ and
           * don't go deeper. at deeper level we check only that we at the right _inlined at_ position.
           */
          if (continueUpdateResult
              && strcmp(limits.fileName, fileName) == 0
              && lineNumber >= limits.start
              && lineNumber <= limits.end) {
            result.lineNumber = lineNumber;
            result.column = CSSourceInfoGetColumn(ref);
          }
          /**
           * if found right inlined function don't bother with
           * updating high level inlined _at_ source info
           */
          if (continueUpdateResult &&  (address >= range.location
                                        && address < range.location + range.length))
             continueUpdateResult = false;

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
