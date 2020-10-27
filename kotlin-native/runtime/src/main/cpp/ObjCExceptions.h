#ifndef RUNTIME_OBJCEXCEPTIONS_H
#define RUNTIME_OBJCEXCEPTIONS_H

#if KONAN_REPORT_BACKTRACE_TO_IOS_CRASH_LOG

#include "Common.h"
#include "Types.h"

void ReportBacktraceToIosCrashLog(KRef throwable);

#endif // KONAN_REPORT_BACKTRACE_TO_IOS_CRASH_LOG

#endif // RUNTIME_OBJCEXCEPTIONS_H