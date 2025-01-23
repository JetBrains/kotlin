/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ObjCBackRef.hpp"

#include "SpecialRefRegistry.hpp"
#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"

using namespace kotlin;

// static
mm::ObjCBackRef mm::ObjCBackRef::create(ObjHeader* obj) noexcept {
    RuntimeAssert(obj != nullptr, "Creating ObjCBackRef for null object");
    return mm::ObjCBackRef(&mm::ThreadRegistry::Instance().CurrentThreadData()->specialRefRegistry().createExternalRCRefImpl(obj, 1));
}
