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
    size_type tryStealFrom(StealableWorkList<T, Traits>& victim, size_type maxAmount) noexcept {
        bool wasLockedByOther = victim.sharedLocked_.test_and_set(std::memory_order_acquire);
        if (wasLockedByOther) return 0;
        if (victim.sharedEmpty()) {
            victim.sharedLocked_.clear(std::memory_order_relaxed);
            return 0;
        }

        auto amount = local_.splice_after(local_.before_begin(), victim.shared_.before_begin(), victim.shared_.end(), maxAmount);
        victim.sharedSize_ -= amount;
        localSize_ += amount;

        victim.sharedLocked_.clear(std::memory_order_release);

        return amount;
    }

    /**
     * Moves all of the local items into own shared list.
     * @return `0` if the shared list is busy, amount of newly shared items otherwise.
     */
    size_type shareAll() noexcept {
        RuntimeAssert(!localEmpty(), "Nothing to share");
        auto wasLockedByOther = sharedLocked_.test_and_set(std::memory_order_acquire);
        if (wasLockedByOther) return 0;

        auto amount = shared_.splice_after(shared_.before_begin(), local_.before_begin(), local_.end(), localSize_);
        sharedSize_ += amount;
        localSize_ -= amount;

        sharedLocked_.clear(std::memory_order_release);
        return amount;
    }

private:
    ListImpl local_;
    size_type localSize_ = 0;

    ListImpl shared_;
    size_type sharedSize_ = 0;
    std::atomic_flag sharedLocked_ = ATOMIC_FLAG_INIT;
};

}