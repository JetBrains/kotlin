/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#if KONAN_OBJC_INTEROP

#include <objc/runtime.h>

#include "TypeInfo.h"

namespace kotlin::swiftExportRuntime {

Class bestFittingObjCClassFor(const TypeInfo* typeInfo) noexcept;

} // namespace kotlin::swiftExportRuntime

#endif
