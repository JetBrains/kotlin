/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Memory.h"
#include "Types.h"

namespace kotlin {

// TODO: Instead of KStd* provide allocator-customizable versions, to allow stack memory allocation.
// TODO: Model API as in upcoming https://en.cppreference.com/w/cpp/utility/basic_stacktrace

KStdVector<void*> GetCurrentStackTrace(int extraSkipFrames) noexcept;

// TODO: This is asking for a span.
KStdVector<KStdString> GetStackTraceStrings(void* const* stackTrace, size_t stackTraceSize) noexcept;

// It's not always safe to extract SourceInfo during unhandled exception termination.
void DisallowSourceInfo();

void PrintStackTraceStderr();

} // namespace kotlin
