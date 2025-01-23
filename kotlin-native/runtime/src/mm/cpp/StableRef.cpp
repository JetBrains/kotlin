/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "StableRef.hpp"

#include "ExternalRCRefRegistry.hpp"
#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"

using namespace kotlin;

// static
mm::StableRef mm::StableRef::create(ObjHeader* obj) noexcept {
    RuntimeAssert(obj != nullptr, "Creating StableRef for null object");
    return mm::StableRef(&mm::ThreadRegistry::Instance().CurrentThreadData()->externalRCRefRegistry().createExternalRCRefImpl(obj, 1));
}
