/*
 * Copyright 2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <thread>
#include <cstring>

#include "Utils.hpp"
#include "Memory.h"
#include "Types.h"
#include "ExternalRCRef.hpp"
#include "Runtime.h"

using namespace kotlin;

extern "C" {

void Kotlin_internal_executeAndRelease(mm::RawExternalRCRef* action);

void Kotlin_native_concurrent_startThreadImpl(mm::RawExternalRCRef* routine) {
    kotlin::ThreadStateGuard guard(ThreadState::kNative);
    std::thread thread{[routine]() {
        kotlin::CalledFromNativeGuard guard;
        Kotlin_internal_executeAndRelease(routine);
    }};
    thread.detach();
}

KULong Kotlin_native_concurrent_currentThreadId() {
    auto tid = std::this_thread::get_id();
    static_assert(sizeof(tid) <= sizeof(KULong), "Casting pthread_t to ULong will lose data");
    // That's almost std::bit_cast. The latter requires sizeof equality of types.
    KULong result = 0;
    std::memcpy(&result, &tid, sizeof(tid));
    return result;
}

}
