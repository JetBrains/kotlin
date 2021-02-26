/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Memory.h"

#include "StableRefRegistry.hpp"
#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"

using namespace kotlin;

namespace {

#if !KONAN_NO_EXCEPTIONS
class ExceptionObjHolderImpl : public ExceptionObjHolder {
public:
    explicit ExceptionObjHolderImpl(ObjHeader* obj) noexcept {
        auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
        stableRef_ = threadData->stableRefThreadQueue().Insert(obj);
    }

    ~ExceptionObjHolderImpl() override {
        auto* threadData = mm::ThreadRegistry::Instance().CurrentThreadData();
        threadData->stableRefThreadQueue().Erase(stableRef_);
    }

    ObjHeader* obj() noexcept { return **stableRef_; }

private:
    mm::StableRefRegistry::Node* stableRef_;
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
