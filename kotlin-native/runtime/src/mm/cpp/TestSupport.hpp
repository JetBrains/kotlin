/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "../../main/cpp/TestSupport.hpp"

#include "MemoryPrivate.hpp"
#include "ThreadData.hpp"

namespace kotlin {

inline void RunInNewThread(std::function<void(mm::ThreadData&)> f) {
    kotlin::RunInNewThread([&f](MemoryState* state) {
        f(*state->GetThreadData());
    });
}

} // namespace kotlin
