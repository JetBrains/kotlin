/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "CompilerConstants.hpp"
#include "KAssert.h"
#include "Logging.hpp"
#include "Utils.hpp"
#include "Porting.h"
#include "BoundedQueue.hpp"

namespace kotlin {

/**
 * Coordinates a group of workers working in parallel on a large amounts of identical tasks.
 * The dispatcher will try to balance the work among the workers.
 *
 * Requirements:
 * -  Every instantiated worker must execute `tryPop` sooner or later;
 * -  Every instantiated worker must finish execution before the destruction of the processor;
 */
template <typename ListImpl, std::size_t kBatchSize, std::size_t kBatchesPoolSize>
class ParallelProcessor : private Pinned {
private:
    class Batch {
    public:
        PERFORMANCE_INLINE bool empty() const noexcept {
            return elems_.empty();
        }

        PERFORMANCE_INLINE bool full() const noexcept {
            return elemsCount_ == kBatchSize;
        }

        PERFORMANCE_INLINE std::size_t elementsCount() const noexcept {
            return elemsCount_;
        }

        PERFORMANCE_INLINE bool tryPush(typename ListImpl::reference value) noexcept {
            RuntimeAssert(!full(), "Batch overflow");
            const bool pushed = elems_.try_push_front(value);
            elemsCount_ += static_cast<int>(pushed);
            return pushed;
        }

        PERFORMANCE_INLINE typename ListImpl::pointer tryPop() noexcept {
            auto popped = elems_.try_pop_front();
            if (popped) {
                --elemsCount_;
            }
            return popped;
        }

        void transferAllInto(ListImpl& dst) noexcept {
            dst.splice_after(dst.before_begin(), elems_.before_begin(), elems_.end(), std::numeric_limits<typename ListImpl::size_type>::max());
            RuntimeAssert(empty(), "All the elements must be transferred");
            elemsCount_ = 0;
        }

        void fillFrom(ListImpl& src) noexcept {
            auto spliced = elems_.splice_after(elems_.before_begin(), src.before_begin(), src.end(), kBatchSize);
            elemsCount_ = spliced;
        }

        void clear() noexcept {
            elems_.clear();
            elemsCount_ = 0;
        }

    private:
        ListImpl elems_;
        std::size_t elemsCount_ = 0;
    };

    class LocalQueue : private Pinned {
    public:
        PERFORMANCE_INLINE bool localEmpty() const noexcept {
            return localQueue_.empty();
        }

        PERFORMANCE_INLINE bool tryPushLocal(typename ListImpl::reference value) noexcept {
            return localQueue_.try_push_front(value);
        }

        PERFORMANCE_INLINE typename ListImpl::pointer tryPopLocal() noexcept {
            return localQueue_.try_pop_front();
        }

    protected:
        ListImpl localQueue_;
    };

public:
    class WorkSource : public LocalQueue {
        friend ParallelProcessor;
    public:
        explicit WorkSource(ParallelProcessor& dispatcher) : dispatcher_(dispatcher) {}

        ~WorkSource() noexcept {
            RuntimeAssert(retainsNoWork(), "A queue must be empty");
        }

        PERFORMANCE_INLINE bool retainsNoWork() const noexcept {
            return batch_.empty() && overflowList().empty();
        }

        PERFORMANCE_INLINE bool tryPush(typename ListImpl::reference value) noexcept {
            if (batch_.full()) {
                bool released = dispatcher_.releaseBatch(std::move(batch_));
                if (!released) {
                    RuntimeLogDebug({ kTagBalancing }, "Batches pool overflow");
                    batch_.transferAllInto(overflowList());
                }
                batch_ = Batch{};
            }
            return batch_.tryPush(value);
        }

        /**
         * Tries to transfer all the tasks stored in this WorkSource locally into the shared ParallelProcessor's storage.
         * @return `true` iff this WorkSource doesn't contain any local tasks anymore.
         */
        PERFORMANCE_INLINE bool forceFlush() noexcept {
            while (true) {
                if (!batch_.empty()) {
                    bool released = dispatcher_.releaseBatch(std::move(batch_));
                    if (released) {
                        RuntimeLogDebug({ kTagBalancing }, "Work batch flushed");
                        batch_ = Batch{};
                    } else {
                        RuntimeLogDebug({ kTagBalancing }, "Failed to force flush work queue");
                        return false;
                    };
                }
                RuntimeAssert(batch_.empty(), "Now must be empty");
                if (overflowList().empty()) {
                    return true;
                } else {
                    RuntimeLogDebug({ kTagBalancing }, "Refilling batch from overflow list");
                    batch_.fillFrom(overflowList());
                }
            }
        }

        void clear() noexcept {
            batch_.clear();
            overflowList().clear();
        }

    protected:
        ListImpl& overflowList() noexcept {
            return this->localQueue_;
        }

        const ListImpl& overflowList() const noexcept {
            return this->localQueue_;
        }

        ParallelProcessor& dispatcher_;
        Batch batch_;
    };

    class Worker : public WorkSource {
        friend ParallelProcessor;
    public:
        explicit Worker(ParallelProcessor& dispatcher) : WorkSource(dispatcher) {
            this->dispatcher_.registeredWorkers_.fetch_add(1, std::memory_order_relaxed);
            RuntimeLogDebug({ kTagBalancing }, "Worker registered");
        }

        PERFORMANCE_INLINE typename ListImpl::pointer tryPop() noexcept {
            if (this->batch_.empty()) {
                while (true) {
                    bool acquired = this->dispatcher_.acquireBatch(this->batch_);
                    if (!acquired) {
                        if (!this->overflowList().empty()) {
                            this->batch_.fillFrom(this->overflowList());
                            RuntimeLogDebug({ kTagBalancing }, "Acquired %zu elements from the overflow list", this->batch_.elementsCount());
                        } else {
                            bool newWorkAvailable = waitForMoreWork();
                            if (newWorkAvailable) continue;
                            return nullptr;
                        }
                    }
                    RuntimeAssert(!this->batch_.empty(), "Must have acquired some elements");
                    break;
                }
            }

            return this->batch_.tryPop();
        }

    private:
        bool waitForMoreWork() noexcept {
            RuntimeAssert(this->batch_.empty(), "Local batch must be depleted before waiting for shared work");
            RuntimeAssert(this->overflowList().empty(), "Local overflow list must be depleted before waiting for shared work");

            std::unique_lock lock(this->dispatcher_.waitMutex_);

            // seq_cst (not relaxed): consumer side of the SC pivot point with releaseBatch.
            // This SC pivot puts these two operations (this fetch_add and the load in releaseBranch) into a single total order.
            // We have two cases here:
            //  - fetch_add is first, load is second. Then releaseBatch will read > 0 and therefore will call notify_one();
            //  - load is first, fetch_add is second. Then the below call to tryPeek() will likely (memory coherence) return true
            //    and this thread won't park. (also see the explanation below why the case when tryPeek() returns false is OK as well).
            // In both cases we would have some progress made - no situation when all threads are parked while there is some pending work.
            //
            // One note: all decrements of waitingWorkers_ below are relaxed as
            // the SC pivot described above is enough for correct synchronization.
            auto nowWaiting = this->dispatcher_.waitingWorkers_.fetch_add(1, std::memory_order_seq_cst) + 1;
            RuntimeLogDebug({ kTagBalancing }, "Worker goes to sleep (now sleeping %zu of %zu)",
                            nowWaiting, this->dispatcher_.registeredWorkers_.load(std::memory_order_relaxed));

            // Predicate re-check under the lock, AFTER registering as a waiter.
            // If a batch was published (and, via the SC pivot, our registration is visible to its producer),
            // either the producer will notify us, or we observe the batch right here and don't park.
            // There is one rare case when this call returns false even though load has read 0 and the queue is not empty.
            // But in such a case the current thread can never be the last one: if the queue is not empty, there is a batch B
            // enqueued by some thread T1. The last thread T2 must have its fetch_add read-from T1's increment of waitingWorkers_
            // (it's an RMW operation). Since the increments are acq_rel (as seq_cst supersedes it), that read-from forms
            // a SW edge between the two increments. Combined with program order - T1.enqueue is sequenced before T1.increment,
            // and T2.fetch_add is sequenced before T2.tryPeek() - this gives a HB edge T1.enqueue -> T2.tryPeek.
            // Hence, T2 must observe B and will not go to the waitingWorkers_ == registeredWorkers_ branch.
            if (this->dispatcher_.sharedBatches_.tryPeek()) {
                this->dispatcher_.waitingWorkers_.fetch_sub(1, std::memory_order_relaxed);
                return true; // tryPop will retry acquireBatch
            }

            if (this->dispatcher_.allDone_) {
                this->dispatcher_.waitingWorkers_.fetch_sub(1, std::memory_order_relaxed);
                return false;
            }

            if (nowWaiting == this->dispatcher_.registeredWorkers_.load(std::memory_order_relaxed)) {
                // we are the last ones awake
                RuntimeLogDebug({ kTagBalancing }, "Worker has detected termination");
                this->dispatcher_.allDone_ = true;
                this->dispatcher_.waitingWorkers_.fetch_sub(1, std::memory_order_relaxed);
                lock.unlock();
                this->dispatcher_.waitCV_.notify_all();
                return false;
            }

            this->dispatcher_.waitCV_.wait(lock);
            this->dispatcher_.waitingWorkers_.fetch_sub(1, std::memory_order_relaxed);
            if (this->dispatcher_.allDone_) {
                return false;
            }
            RuntimeLogDebug({ kTagBalancing }, "Worker woke up");

            return true;
        }
    };

    ParallelProcessor() = default;

    ~ParallelProcessor() {
        // The shared mark queue must be fully drained before we tear it down. A non-empty queue here
        // means a batch of live objects was stranded and would be swept while still reachable.
        RuntimeAssert(sharedBatches_.empty(), "Shared mark queue stranded at end of mark epoch");
        RuntimeAssert(waitingWorkers_.load() == 0, "All the workers must terminate before dispatcher destruction");
    }

    size_t registeredWorkers() const noexcept {
        return registeredWorkers_.load(std::memory_order_relaxed);
    }

    /** Prepare for a new round of work processing. Must be called only after a previous round is fully finished. */
    void resetForNewWork() noexcept {
        RuntimeAssert(allDone_, "A work processing iteration must be finished");
        RuntimeAssert(waitingWorkers_ == 0, "There must be no workers sleeping inside processing loop");
        allDone_ = false;
    }

    /** Returns a total cumulatiove number of bathces that have ever been shared by any of the work sources so far. */
    size_t batchesEverShared() const noexcept {
        return sharedBatches_.cumulativeThroughput();
    }

    bool sharedBatchesEmpty() const noexcept { return sharedBatches_.empty(); }

private:
    bool releaseBatch(Batch&& batch) {
        RuntimeAssert(!batch.empty(), "A batch to release into shared pool must be non-empty");
        RuntimeLogDebug({ kTagBalancing }, "Releasing batch of %zu elements", batch.elementsCount());
        bool shared = sharedBatches_.enqueue(std::move(batch));
        if (shared) {
            // seq_cst (not relaxed): second part of the SC pivot point (pairs with the seq_cst fetch_add in waitForMoreWork).
            // Guarantees a producer that publishes a batch cannot also miss a worker that has already registered itself as waiting.
            if (waitingWorkers_.load(std::memory_order_seq_cst) > 0) {
                waitCV_.notify_one();
            }
        }
        return shared;
    }

    bool acquireBatch(Batch& dst) {
        RuntimeAssert(dst.empty(), "Destination batch must be already depleted");
        auto acquired = sharedBatches_.dequeue();
        if (acquired) {
            dst = std::move(*acquired);
            RuntimeLogDebug({ kTagBalancing }, "Acquired a batch of %zu elements", dst.elementsCount());
            return true;
        }
        return false;
    }

    BoundedQueue<Batch, kBatchesPoolSize> sharedBatches_;

    std::atomic<size_t> registeredWorkers_ = 0;
    std::atomic<size_t> waitingWorkers_ = 0;

    std::atomic<bool> allDone_ = false;
    mutable std::mutex waitMutex_;
    mutable std::condition_variable waitCV_;
};

}
