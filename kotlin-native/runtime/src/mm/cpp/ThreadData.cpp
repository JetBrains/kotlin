/*
* Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ThreadData.hpp"

using namespace kotlin;

// mm::ThreadData::ThreadData(uintptr_t threadId) noexcept :
//     threadId_(threadId),
//     globalsThreadQueue_(GlobalsRegistry::Instance()),
//     externalRCRefRegistry_(ExternalRCRefRegistry::instance()),
//     gcScheduler_(GlobalData::Instance().gcScheduler(), *this),
//     allocator_(GlobalData::Instance().allocator()),
//     gc_(GlobalData::Instance().gc(), *this),
//     suspensionData_(ThreadState::kNative, *this) {
//
// }
//
// mm::ThreadData::~ThreadData() noexcept {
//
// }
