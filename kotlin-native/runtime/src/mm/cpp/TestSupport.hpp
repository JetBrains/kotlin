/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "../../main/cpp/TestSupport.hpp"

#include <ostream>

#include "MemoryPrivate.hpp"
#include "ObjectTestSupport.hpp"
#include "ThreadData.hpp"
#include "WeakRef.hpp"

namespace kotlin {

inline void RunInNewThread(std::function<void(mm::ThreadData&)> f) {
    kotlin::RunInNewThread([&f](MemoryState* state) {
        f(*state->GetThreadData());
    });
}

// Overload the << operator for ThreadState to allow the GTest runner
// to pretty print ThreadState constants.
std::ostream& operator<<(std::ostream& stream, ThreadState state);

namespace test_support {

RegularWeakReferenceImpl& InstallWeakReference(mm::ThreadData& threadData, ObjHeader* objHeader, ObjHeader** location);

} // namespace test_support

} // namespace kotlin
