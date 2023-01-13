/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef CUSTOM_ALLOC_CPP_CUSTOMLOGGING_HPP_
#define CUSTOM_ALLOC_CPP_CUSTOMLOGGING_HPP_

#include "Logging.hpp"
#include "Porting.h"

#define CustomAllocInfo(format, ...) RuntimeLogInfo({"alloc"}, "t%u " format, konan::currentThreadId(), ##__VA_ARGS__)
#define CustomAllocDebug(format, ...) RuntimeLogDebug({"alloc"}, "t%u " format, konan::currentThreadId(), ##__VA_ARGS__)
#define CustomAllocWarning(format, ...) RuntimeLogWarning({"alloc"}, "t%u " format, konan::currentThreadId(), ##__VA_ARGS__)
#define CustomAllocError(format, ...) RuntimeLogError({"alloc"}, "t%u " format, konan::currentThreadId(), ##__VA_ARGS__)

#endif
