/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

#include "SourceInfo.h"

#include <CompilerConstants.hpp>
#include <CoreFoundation/CFUUID.h>
#include <KAssert.h>
#include <cstdint>
#include <dlfcn.h>
#include <limits>
#include <mach/mach.h>
#include <mach-o/loader.h>
#include <mach-o/getsect.h>
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

typedef int32_t cpu_type_t;
typedef int32_t cpu_subtype_t;

struct _CSArchitecture {
    cpu_type_t	cpu_type;
    cpu_subtype_t	cpu_subtype;
};

typedef struct _CSArchitecture CSArchitecture;

typedef struct _CSBinaryRelocationInformation {
    vm_address_t base;
    vm_address_t extent;
    char name[17];
} CSBinaryRelocationInformation;

typedef struct _CSBinaryImageInformation {
    vm_address_t base;
    vm_address_t extent;
    CFUUIDBytes uuid;
    CSArchitecture arch;
    const char *path;
    CSBinaryRelocationInformation *relocations;
    uint32_t relocationCount;
    uint32_t flags;
} CSBinaryImageInformation;

// It should be a block type, but we don't need to pass anything but `nullptr`,
// so there is no need to bother with specifying details.
typedef void* CSNotificationBlock;

constexpr CSTypeRef kCSNull = { 0, nullptr };

constexpr auto kCSNow = 1ull<<63;

#if defined (__x86_64__)
constexpr CSArchitecture hostArchitecture = {
        CPU_TYPE_X86_64, CPU_SUBTYPE_I386_ALL
};
#elif defined (__aarch64__)
constexpr CSArchitecture hostArchitecture = {
        CPU_TYPE_ARM64, CPU_SUBTYPE_ARM64_ALL
};
#else
#error "Unsupported architecture"
#endif

namespace {

CSSymbolicatorRef (*CSSymbolicatorCreateWithPid)(pid_t pid);

CSSymbolicatorRef (*CSSymbolicatorCreateWithBinaryImageList)(
    CSBinaryImageInformation* imageInfo,
    uint32_t imageInfoCount,
    uint32_t flags,
    CSNotificationBlock notificationBlock
);

CSSymbolOwnerRef (*CSSymbolicatorGetSymbolOwnerWithAddressAtTime)(
    CSSymbolicatorRef symbolicator,
    unsigned long long address,
    unsigned long long time
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


CFUUIDBytes* findUuid(mach_header_64* machHeader) {
  auto* loadCommand = reinterpret_cast<load_command*>(machHeader + 1);

  for (uint32_t index = 0; index < machHeader->ncmds; ++index) {
    if (loadCommand->cmd == LC_UUID) {
      auto* uuidCommand = reinterpret_cast<uuid_command*>(loadCommand);
      static_assert(sizeof(uuidCommand->uuid) == sizeof(CFUUIDBytes), "unexpected UUID size");
      return reinterpret_cast<CFUUIDBytes*>(&uuidCommand->uuid);
    }

    loadCommand = reinterpret_cast<load_command*>(reinterpret_cast<uintptr_t>(loadCommand) + loadCommand->cmdsize);
  }

  return nullptr;
}

/**
 * This function exists to workaround https://youtrack.jetbrains.com/issue/KT-75992.
 *
 * Normally we would create a symbolicator using `CSSymbolicatorCreateWithPid(getpid())`.
 * Unfortunately, when using simulators from Xcode 16.3, it doesn't work well. See more details in the issue.
 * TL;DR: CoreSymbolication can't create a symbolicator because enumerating loaded binary images with
 * `_dyld_process_info_create` fails.
 *
 * So, the workaround is to build an image list by other means and provide it to CoreSymbolication.
 * That's where things become tricky. There are a few ways to enumerate the images loaded to the process.
 * But none of them is easy:
 * - `_dyld_process_info_create` fails because of the bug we are trying to workaround here.
 * - `_dyld_image_count`/`_dyld_get_image_header(index)` are not thread-safe.
 * - `_dyld_register_func_for_add_image` is cumbersome and intrusive: there is no user data
 *   (= we need to store the results to globals), the callback can't be deregistered.
 *
 * So, in order to keep things simpler and more reliable, this function doesn't enumerate the loaded images at all,
 * but uses only the current image (where all Kotlin code resides).
 * This naturally leads to an issue:
 *   https://youtrack.jetbrains.com/issue/KT-76511/Native-Kotlin-stacktraces-dont-show-Swift-line-numbers
 * But it is considered the lesser evil.
 *
 * @warning This function allocates a little additional memory which is never reclaimed.
 *          Make sure to call it only once if that matters.
 */
CSSymbolicatorRef createSymbolicatorWithCurrentImage() {
  Dl_info dlInfo;
  if (dladdr(reinterpret_cast<void*>(&createSymbolicatorWithCurrentImage), &dlInfo) == 0) {
    return kCSNull;
  }

  // For simplicity, support only mach_header_64:
#ifndef __LP64__
#error "Unsupported architecture"
#endif

  auto* machHeader = reinterpret_cast<mach_header_64*>(dlInfo.dli_fbase);
  if (machHeader->magic != MH_MAGIC_64) {
    // Shouldn't happen, but let's be on the safe side:
    return kCSNull;
  }

  unsigned long textSegmentSize = 0;
  uint8_t* textSegmentData = getsegmentdata(machHeader, "__TEXT", &textSegmentSize);
  if (textSegmentData == nullptr) {
    // Shouldn't happen, but let's be on the safe side:
    return kCSNull;
  }

  CFUUIDBytes* uuidBytes = findUuid(machHeader);
  if (uuidBytes == nullptr) {
    // Shouldn't happen, but let's be on the safe side:
    return kCSNull;
  }

  // This memory is never reclaimed. This should be fine, because this function is called only once
  // (`The caller, TryInitializeCoreSymbolication`, initializes a `static` variable).
  auto* imageInfo = new CSBinaryImageInformation();
  auto* relocationInfo = new CSBinaryRelocationInformation();

  /*
   * The usage of `CSSymbolicatorCreateWithBinaryImageList` is inspired by Swift backtracing support:
   * - https://github.com/swiftlang/swift/blob/aedb869c69f8e13633496493a8237407ac9be7ed/stdlib/public/RuntimeModule/modules/OS/Darwin.h#L192
   * - https://github.com/swiftlang/swift/blob/daf8d97616c944f924e6a9a2a0428b9159c19a7f/stdlib/public/RuntimeModule/SymbolicatedBacktrace.swift#L296
   * - https://github.com/swiftlang/swift/blob/aedb869c69f8e13633496493a8237407ac9be7ed/stdlib/public/RuntimeModule/ImageMap%2BDarwin.swift#L80
   */
  imageInfo->base = reinterpret_cast<vm_address_t>(machHeader);
  imageInfo->extent = reinterpret_cast<vm_address_t>(textSegmentData + textSegmentSize);
  imageInfo->uuid = *uuidBytes;
  imageInfo->arch = hostArchitecture;
  imageInfo->path = dlInfo.dli_fname;
  imageInfo->relocations = relocationInfo;
  imageInfo->relocationCount = 1;
  imageInfo->flags = 0;

  static_assert(sizeof(relocationInfo->name) == 17, "relocationInfo.name buffer is too short for \"__TEXT\"");
  strncpy(relocationInfo->name, "__TEXT", sizeof(relocationInfo->name) - 1);
  relocationInfo->base = imageInfo->base;
  relocationInfo->extent = imageInfo->extent;

  return CSSymbolicatorCreateWithBinaryImageList(imageInfo, 1, 0, nullptr);
}

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

  if (kotlin::compiler::coreSymbolicationUseOnlyKotlinImage()) {
    KONAN_CS_LOOKUP(CSSymbolicatorCreateWithBinaryImageList)
    symbolicator = createSymbolicatorWithCurrentImage();
  } else {
    symbolicator = CSSymbolicatorCreateWithPid(getpid());
  }

#undef KONAN_CS_LOOKUP

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