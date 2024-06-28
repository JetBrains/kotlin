/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "Memory.h"
#include "Types.h"

namespace kotlin::mm {

// TODO(KT-67741): Unify different SpecialRefs

// Object if the given kotlin.native.internal.ref.ExternalRCRef is permanent object, nullptr otherwise.
KRef externalRCRefAsPermanentObject(void* ref) noexcept;

// kotlin.native.internal.ref.ExternalRCRef for the given permanent object.
void* permanentObjectAsExternalRCRef(KRef obj) noexcept;

}
