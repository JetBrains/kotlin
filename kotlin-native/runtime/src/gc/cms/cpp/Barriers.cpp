#include "Barriers.hpp"

#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"
#include "GCImpl.hpp"
#include <atomic>

using namespace kotlin;

namespace {

[[clang::no_destroy]] std::atomic<bool> weakRefBarriersEnabled = false;

template<typename Iterable, typename Pred>
bool forall(Iterable& iterable, Pred&& pred) {
    for (auto& item : iterable) {
        if (!pred(item)) return false;
    }
    return true;
}

void checkpointAction(mm::ThreadData& thread) {
    thread.gc().impl().gc().barriers().onCheckpoint();
}

void waitForThreadsToReachCheckpoint() {
    // resetCheckpoint
    for (auto& thr: mm::ThreadRegistry::Instance().LockForIter()) {
        thr.gc().impl().gc().barriers().resetCheckpoint();
    }

    // requestCheckpoint
    bool safePointSet = mm::TrySetSafePointAction(checkpointAction);
    RuntimeAssert(safePointSet, "Only the GC thread can request safepoint actions, and STW must have already finished");

    // waitForAllThreadsToVisitCheckpoint
    auto threads = mm::ThreadRegistry::Instance().LockForIter();
    while (!forall(threads, [](mm::ThreadData& thr) { return thr.gc().impl().gc().barriers().visitedCheckpoint() || thr.suspensionData().suspendedOrNative(); })) {
        std::this_thread::yield();
    }

    //unsetSafePointAction
    mm::UnsetSafePointAction();
}

}

void gc::BarriersThreadData::onCheckpoint() {
    visitedCheckpoint_.store(true, std::memory_order_seq_cst);
}

void gc::BarriersThreadData::resetCheckpoint() {
    visitedCheckpoint_.store(false, std::memory_order_seq_cst);
}

bool gc::BarriersThreadData::visitedCheckpoint() const {
    return visitedCheckpoint_.load(std::memory_order_relaxed);
}

void gc::EnableWeakRefBarriers(bool inSTW) {
    weakRefBarriersEnabled.store(true, std::memory_order_seq_cst);
    if (!inSTW) {
        waitForThreadsToReachCheckpoint();
    }
}

void gc::DisableWeakRefBarriers(bool inSTW) {
    weakRefBarriersEnabled.store(false, std::memory_order_seq_cst);
    if (!inSTW) {
        waitForThreadsToReachCheckpoint();
    }
}

OBJ_GETTER(kotlin::gc::WeakRefRead, ObjHeader* weakReferee) noexcept {
    if (compiler::concurrentWeakSweep()) {
        if (weakReferee != nullptr) {
            // weakRefBarriersEnabled changes are synchronized with checkpoints or STW
            if (weakRefBarriersEnabled.load(std::memory_order_relaxed)) {
                // When weak ref barriers are enabled, marked state cannot change and the
                // object cannot be deleted.
                if (!gc::isMarked(weakReferee)) {
                    RETURN_OBJ(nullptr);
                }
            }
        }
    }
    RETURN_OBJ(weakReferee);
}

