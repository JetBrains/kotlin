/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <cstddef>

namespace kotlin {

// An estimate of how much memory was committed by the process at its peak:
// * RSS on Linux
// * Memory Footprint on macOS
// * Working Set Size on Windows
// May return 0 if unimplemented on some platform, or in case of an error.
size_t peakResidentSetSizeBytes() noexcept;

}
