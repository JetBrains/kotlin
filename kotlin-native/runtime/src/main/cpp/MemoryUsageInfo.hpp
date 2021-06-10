/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MEMORY_USAGE_INFO_H
#define RUNTIME_MEMORY_USAGE_INFO_H

#include <cstddef>

namespace kotlin {

size_t GetPeakResidentSetSizeBytes() noexcept;

}

#endif // RUNTIME_MEMORY_USAGE_INFO_H
