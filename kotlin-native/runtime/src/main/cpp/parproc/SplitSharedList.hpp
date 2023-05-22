/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <mutex>
#include "IntrusiveList.hpp"
#include "Porting.h"
#include "Mutex.hpp"

namespace kotlin {

/**
 * A list for thread-local use with a separate shared part that can be accessed by other threads.
 */
template<typename ListImpl>
class SplitSharedList : private Pinned {
public:
    using value_type = typename ListImpl::value_type;
    using size_type = typename ListImpl::size_type;
    using reference = typename ListImpl::reference;
    using pointer = typename ListImpl::pointer;

    SplitSharedList() = default;

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
    bool tryPushLocal(reference value) noexcept {
        auto pushed = local_.try_push_front(value);
        if (pushed) ++localSize_;
        return pushed;
    }

    /**
     * Tries to pop a value from the local list.
     * See `intrusive_forward_list.try_pop_front`.
     */
    pointer tryPopLocal() noexcept {
        auto popped = local_.try_pop_front();
        if (popped) {
            --localSize_;
        } else {
            RuntimeAssert(localEmpty(), "Pop can only fail if the list is empty");
        }
        return popped;
    }

    bool sharedEmpty() const {
        return shared_.empty();
    }

    /**
     * Tries to move at most `maxAmount` elements from a from's shared list into `this`'s local list.
     * In case some other thread is currently operating with the from's shared list, returns `0`.
     * @return the number of elements stolen
     */
    size_type tryTransferFrom(SplitSharedList& from, size_type maxAmount) noexcept {
        std::unique_lock guard(from.sharedLock_, std::try_to_lock);
        if (!guard || from.sharedEmpty()) {
            return 0;
        }

        auto amount = local_.splice_after(local_.before_begin(),
                                          from.shared_.before_begin(),
                                          from.shared_.end(),
                                          maxAmount);
        from.sharedSize_ -= amount;
        localSize_ += amount;

        return amount;
    }

    /**
     * Moves all of the local items into own shared list.
     * @return `0` if the shared list is busy, amount of newly shared items otherwise.
     */
    size_type shareAllWith(SplitSharedList& with) noexcept {
        RuntimeAssert(!localEmpty(), "Nothing to share");
        std::unique_lock guard(with.sharedLock_, std::try_to_lock);
        if (!guard) return 0;

        auto amount = with.shared_.splice_after(with.shared_.before_begin(), local_.before_begin(), local_.end(), localSize_);
        with.sharedSize_ += amount;
        localSize_ -= amount;

        return amount;
    }

private:
    ListImpl local_;
    size_type localSize_ = 0;

    ListImpl shared_;
    size_type sharedSize_ = 0;
    SpinLock<MutexThreadStateHandling::kIgnore> sharedLock_;
};

}