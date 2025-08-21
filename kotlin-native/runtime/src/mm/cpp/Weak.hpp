/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "Memory.h"

namespace kotlin::mm {

OBJ_GETTER(createRegularWeakReferenceImpl, ObjHeader* object) noexcept;
void disposeRegularWeakReferenceImpl(ObjHeader* weakRef) noexcept;

ObjHeader* regularWeakReferenceImplBaseObjectUnsafe(ObjHeader* weakRef) noexcept;

} // namespace kotlin::mm
