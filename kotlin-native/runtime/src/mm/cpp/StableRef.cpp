/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "StableRef.hpp"

#include "MemoryPrivate.hpp"
#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"
#include "ThreadState.hpp"

using namespace kotlin;

// static
mm::StableRef mm::StableRef::create(ObjHeader* obj) noexcept {
    RuntimeAssert(obj != nullptr, "Creating StableRef for null object");
    return mm::ThreadRegistry::Instance().CurrentThreadData()->specialRefRegistry().createStableRef(obj);
}

// static
void mm::StableRef::tryToDeleteImmediately(raw_ptr<SpecialRefRegistry::Node> node) noexcept {
    // When we're on the registered thread, perform oportunistic quick deletion.
    if (auto* threadNode = mm::ThreadRegistry::Instance().CurrentThreadDataNodeOrNull()) {
        tryToDeleteImmediately(*threadNode->Get(), std::move(node));
    }
}

// static
void mm::StableRef::tryToDeleteImmediately(mm::ThreadData& thread, raw_ptr<SpecialRefRegistry::Node> node) noexcept {
    auto lastState = SwitchThreadState(&thread, ThreadState::kRunnable, /* reentrant = */ true);
    thread.specialRefRegistry().deleteNodeIfLocal(*node);
    SwitchThreadState(&thread, lastState, /* reentrant = */ true);
}
