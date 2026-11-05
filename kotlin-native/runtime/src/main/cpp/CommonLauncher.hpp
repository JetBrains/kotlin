/*
 * Copyright 2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef COMMON_LAUNCHER_H
#define COMMON_LAUNCHER_H

#include "Types.h"

namespace kotlin {

int executableEntryPoint(int argc, const char** argv, KInt (*entryPoint)(const ObjHeader*));

}

#endif
