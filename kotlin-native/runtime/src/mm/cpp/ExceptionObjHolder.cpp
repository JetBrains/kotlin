/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Memory.h"

#include "ScopedExternalRCRef.hpp"

using namespace kotlin;

namespace {

class ExceptionObjHolderImpl : public ExceptionObjHolder, private Pinned {
public:
    explicit ExceptionObjHolderImpl(ObjHeader* obj) noexcept : ref_(obj) {}

    ObjHeader* obj() noexcept { return *ref_; }

private:
    mm::ScopedExternalRCRef ref_;
};

} // namespace

// static
RUNTIME_NORETURN void ExceptionObjHolder::Throw(ObjHeader* exception) {
    throw ExceptionObjHolderImpl(exception);
}

ObjHeader* ExceptionObjHolder::GetExceptionObject() noexcept {
    return static_cast<ExceptionObjHolderImpl*>(this)->obj();
}
