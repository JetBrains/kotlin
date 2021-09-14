/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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

extern "C" int Kotlin_getSourceInfo_core_symbolication(void* addr, SourceInfo *result_buffer, int result_size) {
  if (result_size == 0) return 0;
  __block SourceInfo result;
  __block bool continueUpdateResult = true;
  __block SymbolSourceInfoLimits limits = {.start = -1, .end = -1};

  static bool csIsAvailable = TryInitializeCoreSymbolication();

  if (csIsAvailable) {
    unsigned long long address = static_cast<unsigned long long>((uintptr_t)addr);

    CSSymbolOwnerRef symbolOwner = CSSymbolicatorGetSymbolOwnerWithAddressAtTime(symbolicator, address, kCSNow);
    if (CSIsNull(symbolOwner))
      return 0;
    CSSymbolRef symbol = CSSymbolOwnerGetSymbolWithAddress(symbolOwner, address);
    if (CSIsNull(symbol))
      return 0;
    SYM_LOG("Kotlin_getSourceInfo: address: (%p) {\n", addr);
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
    result.setFilename(limits.fileName);

    CSSymbolForeachSourceInfo(symbol,
      ^(CSSourceInfoRef ref) {
          // Expecting CSSourceInfoGetLineNumber not to overflow int32_t max value.
          int32_t lineNumber = CSSourceInfoGetLineNumber(ref);
          if (lineNumber == 0)
            return 0;
          CSRange range = CSSourceInfoGetRange(ref);
          SYM_LOG("ref(%p .. %p) [{\n", (void *)range.location,  (void *)(range.location + range.length));
          SYM_DUMP(ref);
          SYM_DUMP(CSSourceInfoGetSymbol(ref));
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
          SYM_LOG("}]\n");
          return 0;
   });
  }
  SYM_LOG("}\n");
  result_buffer[0] = result;
  return 1;
}
#endif // KONAN_CORE_SYMBOLICATION