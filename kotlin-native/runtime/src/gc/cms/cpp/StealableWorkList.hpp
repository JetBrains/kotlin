#pragma once

#include "IntrusiveList.hpp"
#include "Porting.h"

namespace kotlin {

/**
 * A work list for thread-local use with a separate stealable part that can be shared with other threads.
 */
template<typename T, typename Traits = DefaultIntrusiveForwardListTraits<T>>
class StealableWorkList : Pinned {
    using ListImpl = intrusive_forward_list<T, Traits>;
public:
    using value_type = typename ListImpl::value_type;
    using size_type = typename ListImpl::size_type ;
    using reference = typename ListImpl::reference ;
    using pointer = typename ListImpl::pointer;

    StealableWorkList() = default;

    bool localEmpty() const {
        return local_.empty();
    }

    size_type localSize() const {
        return localSize_;
    }

    /**
     * Tries to add `value` to the local list.
     * See `intrusive_forward_list.try_push_front`.
     */
    bool tryPushLocal(reference value) {
        auto pushed = local_.try_push_front(value);
        if (pushed) ++localSize_;
        return pushed;
    }

    /**
     * Tries to pop a value from the local list.
     * See `intrusive_forward_list.try_pop_front`.
     */
    pointer tryPopLocal() {
        auto popped = local_.try_pop_front();
        if (popped) {
            --localSize_;
        } else {
            RuntimeAssert(localEmpty(), "Pop can only fail if the list is empty");
        }
        return popped;
    }
    
    void clearLocal() {
        local_.clear();
        localSize_ = 0;
    }

    bool sharedEmpty() const {
        return shared_.empty();
    }

    /**
     * Tries to move at most `maxAmount` elements from a victim's shared list into `this`'s local list.
     * In case some other thread is currently operating with the victim's shared list, returns `0`.
     * @return the number of elements stolen
     */
    size_type tryStealFrom(StealableWorkList<T, Traits>& victim, size_type maxAmount) { // TODO noexcept
        auto locked = victim.sharedLock_.tryLock(false);
        if (!locked) return 0;
        RuntimeAssert(!victim.sharedEmpty(), "Victim's shared was locked as non-empty");

        auto amount = local_.splice_after(local_.before_begin(), victim.shared_.before_begin(), victim.shared_.end(), maxAmount);
        victim.sharedSize_ -= amount;
        localSize_ += amount;

        victim.sharedLock_.release(victim.sharedEmpty());

        return amount;
    }

    /**
     * Moves all of the local items into own shared list.
     * @return `0` if the shared list is busy or non-empty, amount of newly shared items otherwise.
     */
    size_type shareAll() {
        RuntimeAssert(!localEmpty(), "Nothing to share");
        // don't bother with locked or non-empty shared_ -- take it another time
        auto locked = sharedLock_.tryLock(true);
        if (!locked) return 0;

        RuntimeAssert(sharedEmpty(), "Shared just have been locked as empty");
        shared_.swap(local_);
        std::swap(sharedSize_, localSize_);
        auto sharedAmount = sharedSize_;
        RuntimeAssert(!sharedEmpty(), "Must have shared at least something");

        sharedLock_.release(false);
        return sharedAmount;
    }

private:
    // TODO explain
    class TheftLock {
        static const std::size_t Empty = 0;
        static const std::size_t Available = 1;
        static const std::size_t Locked = 2;
    public:
        TheftLock() noexcept : status_(Empty) {}
        bool tryLock(bool asEmpty) noexcept {
            auto expected = asEmpty ? Empty : Available;
            auto desired = Locked;
            if (compiler::runtimeAssertsMode() != compiler::RuntimeAssertsMode::kIgnore) {
                desired |= (konan::currentThreadId() << 2);
            }
            return status_.compare_exchange_strong(expected, desired, std::memory_order_acquire, std::memory_order_relaxed);
        }
        void lock() noexcept {
            while (true) {
                auto status = status_.load(std::memory_order_relaxed);
                if (status == Empty || status == Available) {
                    auto locked = status_.compare_exchange_weak(status, Locked, std::memory_order_acquire, std::memory_order_relaxed);
                    if (locked) return;
                }
                std::this_thread::yield();
            }
        }
        void release(bool empty) noexcept {
            RuntimeAssert(status_ == (Locked | (konan::currentThreadId() << 2)), "Lock must be locked");
            auto releasedStatus = empty ? Empty : Available;
            status_.store(releasedStatus, std::memory_order_release);
        }
    private:
        std::atomic<std::size_t> status_;
    };

    // TODO consider removal?
    static constexpr size_t CACHE_LINE_SIZE = 128;

    alignas(CACHE_LINE_SIZE) ListImpl local_;
    size_type localSize_ = 0;

    alignas(CACHE_LINE_SIZE) ListImpl shared_;
    size_type sharedSize_ = 0;
    TheftLock sharedLock_;
};

}