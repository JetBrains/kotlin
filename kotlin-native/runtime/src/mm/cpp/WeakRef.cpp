/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "WeakRef.hpp"

#include "ThreadData.hpp"

using namespace kotlin;

// static
mm::WeakRef mm::WeakRef::create(ObjHeader* obj) noexcept {
    RuntimeAssert(obj != nullptr, "Creating WeakRef for null object");
    return mm::ThreadRegistry::Instance().CurrentThreadData()->specialRefRegistry().createWeakRef(obj);
}
