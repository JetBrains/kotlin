/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Memory.h"

#include "ExternalRCRef.hpp"

using namespace kotlin;

// Define the destructor here to make it the "key function" for ExceptionObjHolder.
// This ensures the typeinfo (__ZTI18ExceptionObjHolder) is emitted in this translation
// unit with proper linkage and visibility, rather than as a weak symbol in every TU.
ExceptionObjHolder::~ExceptionObjHolder() = default;

namespace {

class ExceptionObjHolderImpl : public ExceptionObjHolder, private Pinned {
public:
    explicit ExceptionObjHolderImpl(ObjHeader* obj) noexcept : ref_(obj) {}

    ObjHeader* obj() noexcept { return *ref_; }

private:
    mm::OwningExternalRCRef ref_;
};

} // namespace

// static
RUNTIME_NORETURN void ExceptionObjHolder::Throw(ObjHeader* exception) {
    throw ExceptionObjHolderImpl(exception);
}

ObjHeader* ExceptionObjHolder::GetExceptionObject() noexcept {
    return static_cast<ExceptionObjHolderImpl*>(this)->obj();
}
