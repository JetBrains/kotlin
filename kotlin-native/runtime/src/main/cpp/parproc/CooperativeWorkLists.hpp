/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "../Utils.hpp"
#include "SplitSharedList.hpp"
#include "Porting.h"
#include "KAssert.h"
#include "Logging.hpp"
#include "PushOnlyAtomicArray.hpp"

namespace kotlin {

template<typename ParallelProcessor, typename ListImpl>
struct NonSharing  {
    class CommonStorage : private Pinned {
    public:
        explicit CommonStorage(ParallelProcessor&) {};
        bool empty() const noexcept { return true; }
    };

    class LocalQueue : private Pinned {
    public:
        explicit LocalQueue(CommonStorage&) {};

        bool empty() const noexcept {
            return list_.empty();
        }

        bool tryPush(typename ListImpl::reference value) noexcept {
            return list_.try_push_front(value);
        }

        typename ListImpl::pointer tryPop() noexcept {
            return list_.try_pop_front();
        }

        bool tryAcquireWork() noexcept {
            return !empty();
        }
    private:
        ListImpl list_;
    };
};

namespace internal {
enum class ShareOn { kPush, kPop };
}

template<typename ParallelProcessor, typename ListImpl,
        std::size_t kMinSizeToShare, std::size_t kMaxSizeToSteal = kMinSizeToShare / 2,
        internal::ShareOn kShareOn = internal::ShareOn::kPush>
struct SharableQueuePerWorker {
    /** A worker will iterate over other workers this number of times searching for a victim to steal tasks from. */
    static const std::size_t kStealingAttemptCyclesBeforeWait = 4;

    class CommonStorage : private Pinned {
    public:
        explicit CommonStorage(ParallelProcessor& workProcessor) : workProcessor_(workProcessor) {}

        bool empty() const noexcept { return true; }

        ParallelProcessor& workProcessor() noexcept {
            return workProcessor_;
        }
    private:
        ParallelProcessor& workProcessor_;
    };

    class LocalQueue : private Pinned {
    public:
        explicit LocalQueue(CommonStorage& common) : common_(common) {}

        bool empty() const noexcept {
            return list_.localEmpty() && list_.sharedEmpty();
        }

        bool tryPush(typename ListImpl::reference value) noexcept {
            bool pushed = list_.tryPushLocal(value);
            if (pushed && kShareOn == internal::ShareOn::kPush) {
                shareAll();
            }
            return pushed;
        }

        typename ListImpl::pointer tryPop() noexcept {
            auto popped = list_.tryPopLocal();
            if (popped && kShareOn == internal::ShareOn::kPop) {
                shareAll();
            }
            return popped;
        }

        bool tryAcquireWork() noexcept {
            if (!list_.localEmpty()) {
                return true;
            }

            // check own shared queue first
            auto selfStolen = list_.tryTransferFrom(list_, kMaxSizeToSteal);
            if (selfStolen > 0) {
                RuntimeLogDebug({"balancing"}, "Worker has acquired %zu tasks from itself", selfStolen);
                return true;
            }

            for (size_t i = 0; i < kStealingAttemptCyclesBeforeWait; ++i) {
                auto&& workers = common_.workProcessor().workerQueues();
                for (auto& from : workers) {
                    auto stolen = list_.tryTransferFrom(from.list_, kMaxSizeToSteal);
                    if (stolen > 0) {
                        RuntimeLogDebug({"balancing"}, "Worker has acquired %zu tasks from %d", stolen, from.carrierThreadId_);
                        return true;
                    }
                }
                std::this_thread::yield();
            }
            RuntimeLogDebug({"balancing"}, "Worker has not found a victim to steal from :(");

            return false;
        }

    private:
        void shareAll() noexcept {
            if (list_.localSize() > kMinSizeToShare) {
                auto shared = list_.shareAllWith(list_);
                if (shared > 0) {
                    common_.workProcessor().onShare(shared);
                }
            }
        }

        const int carrierThreadId_ = konan::currentThreadId();
        CommonStorage& common_;
        SplitSharedList<ListImpl> list_;
    };
};

template<typename ParallelProcessor, typename ListImpl,
        std::size_t kMinSizeToShare, std::size_t kMaxSizeToSteal = kMinSizeToShare / 2,
        internal::ShareOn kShareOn = internal::ShareOn::kPush>
struct SharedGlobalQueue {
    class CommonStorage : private Pinned {
    public:
        explicit CommonStorage(ParallelProcessor& workProcessor) : workProcessor_(workProcessor) {}

        bool empty() const noexcept {
            RuntimeAssert(sharedList_.localEmpty(), "Global list's local part must not be used");
            return sharedList_.sharedEmpty();
        }

        ParallelProcessor& workProcessor() noexcept {
            return workProcessor_;
        }

        SplitSharedList<ListImpl>& sharedList() noexcept {
            return sharedList_;
        }
    private:
        ParallelProcessor& workProcessor_;
        SplitSharedList<ListImpl> sharedList_;
    };

    class LocalQueue : private Pinned {
    public:
        explicit LocalQueue(CommonStorage& common) : common_(common), list_() {}

        bool empty() const noexcept {
            RuntimeAssert(list_.localEmpty(), "Local list's shared part must not be used");
            return list_.localEmpty();
        }

        bool tryPush(typename ListImpl::reference value) noexcept {
            bool pushed = list_.tryPushLocal(value);
            if (pushed && kShareOn == internal::ShareOn::kPush) {
                shareAll();
            }
            return pushed;
        }

        typename ListImpl::pointer tryPop() noexcept {
            auto popped = list_.tryPopLocal();
            if (popped && kShareOn == internal::ShareOn::kPop) {
                shareAll();
            }
            return popped;
        }

        bool tryAcquireWork() noexcept {
            if (!list_.localEmpty()) {
                return true;
            }

            // check own shared queue first
            auto stolen = list_.tryTransferFrom(common_.sharedList(), kMaxSizeToSteal);
            if (stolen > 0) {
                RuntimeLogDebug({"balancing"}, "Worker has acquired %zu tasks from the shared queue", stolen);
                return true;
            }
            return false;
        }

    private:
        void shareAll() noexcept {
            if (list_.localSize() > kMinSizeToShare) {
                auto shared = list_.shareAllWith(common_.sharedList());
                if (shared > 0) {
                    common_.workProcessor().onShare(shared);
                }
            }
        }

        CommonStorage& common_;
        SplitSharedList<ListImpl> list_;
    };
};

} // namespace kotlin::gc::mark

