/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "WritableTypeInfo.hpp"

#if KONAN_TYPE_INFO_HAS_WRITABLE_PART

#include "WritableTypeInfoPrivate.hpp"

using namespace kotlin;

WritableTypeInfo* kotlin::allocateWritableTypeInfo() noexcept {
    return new WritableTypeInfo();
}

#if KONAN_OBJC_INTEROP

TypeInfoObjCExportAddition& kotlin::objCExport(const TypeInfo* typeInfo) noexcept {
    return typeInfo->writableInfo_->objCExport;
}

#endif

#endif
