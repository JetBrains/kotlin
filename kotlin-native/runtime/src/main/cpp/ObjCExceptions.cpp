#if KONAN_REPORT_BACKTRACE_TO_IOS_CRASH_LOG

#include <dlfcn.h>
#include <inttypes.h>
#include <mach-o/loader.h>
#include <CoreFoundation/CFRunLoop.h>
#include "Natives.h"
#include "ObjCExceptions.h"
#include "Types.h"

extern "C" OBJ_GETTER(Kotlin_Throwable_getStackTrace, KRef throwable);

static void writeStackTraceToBuffer(KRef throwable, char* buffer, unsigned long bufferSize) {
  if (bufferSize < 2) return;

  ObjHolder stackTraceHolder;
  ArrayHeader* stackTrace = Kotlin_Throwable_getStackTrace(throwable, stackTraceHolder.slot())->array();

  char* bufferPointer = buffer;
  unsigned long remainingBytes = bufferSize;

  *(bufferPointer++) = '(';
  --remainingBytes;

  for (uint32_t index = 0; index < stackTrace->count_; ++index) {
    KNativePtr ptr = *PrimitiveArrayAddressOfElementAt<KNativePtr>(stackTrace, index);
    int bytes = snprintf(bufferPointer, remainingBytes, "0x%" PRIxPTR " ", reinterpret_cast<uintptr_t>(ptr));

    if (bytes < 0 || static_cast<unsigned long>(bytes) >= remainingBytes) {
      break;
    }

    bufferPointer += bytes;
    remainingBytes -= bytes;
  }

  *(bufferPointer - 1) = ')'; // Replace last space.
  *bufferPointer = '\0';
}

#if !defined(MACHSIZE)
#error "Define MACHSIZE to 32 or 64"
#endif

#if MACHSIZE == 32

typedef struct mach_header mach_header_target;
typedef struct segment_command segment_command_target;
typedef struct section section_target;
static const uint32_t MH_MAGIC_TARGET = MH_MAGIC;
static const uint32_t LC_SEGMENT_TARGET = LC_SEGMENT;

#elif MACHSIZE == 64

typedef struct mach_header_64 mach_header_target;
typedef struct segment_command_64 segment_command_target;
typedef struct section_64 section_target;
static const uint32_t MH_MAGIC_TARGET = MH_MAGIC_64;
static const uint32_t LC_SEGMENT_TARGET = LC_SEGMENT_64;

#else

#error "Impossible MACHSIZE"

#endif

static mach_header_target* findCoreFoundationMachHeader() {
  Dl_info info;
  if (dladdr(reinterpret_cast<void*>(&CFRunLoopRun), &info) == 0) return nullptr;

  return reinterpret_cast<mach_header_target*>(info.dli_fbase);
}

template<int n>
bool bufferEqualsString(const char (&buffer)[n], const char* str) {
  return strncmp(buffer, str, n) == 0;
}

static char* findExceptionBacktraceSection(unsigned long *size) {
  mach_header_target* header = findCoreFoundationMachHeader();
  if (header == nullptr) return nullptr;
  if (header->magic != MH_MAGIC_TARGET) return nullptr;

  uintptr_t textVmaddr = 0;

  load_command* loadCommand = reinterpret_cast<load_command*>(header + 1);
  for (uint32_t loadCommandIndex = 0; loadCommandIndex < header->ncmds; ++loadCommandIndex) {
    if (loadCommand->cmd == LC_SEGMENT_TARGET) {
      segment_command_target* segmentCommand = reinterpret_cast<segment_command_target*>(loadCommand);
      if (bufferEqualsString(segmentCommand->segname, "__TEXT")) {
        textVmaddr = segmentCommand->vmaddr;
      }

      if (bufferEqualsString(segmentCommand->segname, "__DATA")) {
        section_target* sections = reinterpret_cast<section_target*>(segmentCommand + 1);
        for (uint32_t sectionIndex = 0; sectionIndex < segmentCommand->nsects; ++sectionIndex) {
          section_target* section = &sections[sectionIndex];

          if (bufferEqualsString(section->sectname, "__cf_except_bt") && bufferEqualsString(section->segname, "__DATA")) {
            *size = section->size;
            return reinterpret_cast<char*>(reinterpret_cast<uintptr_t>(header) + section->addr - textVmaddr);
          }
        }
      }
    }

    loadCommand = reinterpret_cast<load_command*>(reinterpret_cast<uintptr_t>(loadCommand) + loadCommand->cmdsize);
  }

  return nullptr;
}

void ReportBacktraceToIosCrashLog(KRef throwable) {
  unsigned long bufferSize = 0;
  char* buffer = findExceptionBacktraceSection(&bufferSize);
  if (buffer == nullptr) return;

  // Note: access to this buffer is protected by a lock, but it is not easily accessible.
  // Instead assume that typically this buffer is accessed only during termination, and
  // rely on caller guaranteeing this code to be executed only before system termination handlers.

  writeStackTraceToBuffer(throwable, buffer, bufferSize);
}

#endif // KONAN_REPORT_BACKTRACE_TO_IOS_CRASH_LOG
