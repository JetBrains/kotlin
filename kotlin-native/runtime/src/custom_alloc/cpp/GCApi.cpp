/*
 * Copyright 2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GCApi.hpp"

#include <limits>

#include "ConcurrentMarkAndSweep.hpp"
#include "CustomLogging.hpp"
#include "ObjectFactory.hpp"

namespace kotlin::alloc {

bool TryResetMark(void* ptr) noexcept {
    using Node = typename kotlin::mm::ObjectFactory<kotlin::gc::ConcurrentMarkAndSweep>::Storage::Node;
    using NodeRef = typename kotlin::mm::ObjectFactory<kotlin::gc::ConcurrentMarkAndSweep>::NodeRef;
    Node& node = Node::FromData(ptr);
    NodeRef ref = NodeRef(node);
    auto& objectData = ref.ObjectData();
    if (!objectData.tryResetMark()) {
        auto* objHeader = ref.GetObjHeader();
        if (HasFinalizers(objHeader)) {
            CustomAllocWarning("FINALIZER IGNORED");
        }
        return false;
    }
    return true;
}

void* SafeAlloc(uint64_t size) noexcept {
    void* memory;
    if (size > std::numeric_limits<size_t>::max() || !(memory = std_support::malloc(size))) {
        konan::consoleErrorf("Out of memory trying to allocate %" PRIu64 "bytes. Aborting.\n", size);
        konan::abort();
    }
    return memory;
}

} // namespace kotlin::alloc
