/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Memory.h"

#include "StableRef.hpp"
#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"

using namespace kotlin;

namespace {

#if !KONAN_NO_EXCEPTIONS
class ExceptionObjHolderImpl : public ExceptionObjHolder, private Pinned {
public:
    explicit ExceptionObjHolderImpl(ObjHeader* obj) noexcept : stableRef_(mm::StableRef::create(obj)) {}

    ~ExceptionObjHolderImpl() override { std::move(stableRef_).dispose(); }

    ObjHeader* obj() noexcept { return *stableRef_; }

private:
    mm::StableRef stableRef_;
};
#endif

} // namespace

#if !KONAN_NO_EXCEPTIONS
// static
RUNTIME_NORETURN void ExceptionObjHolder::Throw(ObjHeader* exception) {
    throw ExceptionObjHolderImpl(exception);
}

ObjHeader* ExceptionObjHolder::GetExceptionObject() noexcept {
    return static_cast<ExceptionObjHolderImpl*>(this)->obj();
}
#endif
