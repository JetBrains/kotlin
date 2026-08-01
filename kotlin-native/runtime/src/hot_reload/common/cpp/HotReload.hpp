/**
 * Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
*/

#ifndef HOTRELOAD_HPP
#define HOTRELOAD_HPP

#include "Memory.h"
#include "Natives.h"
#include "Runtime.h"
#include "Types.h"
#include "Common.h"
#include "Logging.hpp"

#define HRLogInfo(format, ...) RuntimeLogInfo({kotlin::kTagHotReload}, format, ##__VA_ARGS__)
#define HRLogDebug(format, ...) RuntimeLogDebug({kotlin::kTagHotReload}, format, ##__VA_ARGS__)
#define HRLogWarning(format, ...) RuntimeLogWarning({kotlin::kTagHotReload}, format, ##__VA_ARGS__)
#define HRLogError(format, ...) RuntimeLogError({kotlin::kTagHotReload}, format, ##__VA_ARGS__)

#endif