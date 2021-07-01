/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Memory.h"
#include "Types.h"

namespace kotlin {

OBJ_GETTER(GetStackTraceStrings, KConstRef stackTrace);

// It's not always safe to extract SourceInfo during unhandled exception termination.
void DisallowSourceInfo();

void PrintStackTraceStderr();

} // namespace kotlin

// Returns current stacktrace as Array<String>.
extern "C" OBJ_GETTER0(Kotlin_getCurrentStackTrace);
