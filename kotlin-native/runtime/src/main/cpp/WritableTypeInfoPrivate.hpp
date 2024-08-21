/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#if KONAN_OBJC_INTEROP

#include "TypeInfoObjCExportAddition.hpp"

struct WritableTypeInfo {
    TypeInfoObjCExportAddition objCExport;
};

#endif
