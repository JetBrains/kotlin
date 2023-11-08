/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

/*
 * An implementation of Dmitry Vyukov's Bounded Multi-producer/multi-consumer bounded queue.
 *
 *  Copyright (c) 2010-2011, Dmitry Vyukov. All rights reserved.
 *  Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *     1. Redistributions of source code must retain the above copyright notice, this list of
 *        conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright notice, this list
 *        of conditions and the following disclaimer in the documentation and/or other materials
 *        provided with the distribution.
 *  THIS SOFTWARE IS PROVIDED BY DMITRY VYUKOV "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 *  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 *  DMITRY VYUKOV OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 *  HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  The views and conclusions contained in the software and documentation are those of the authors and should not be interpreted
 *  as representing official policies, either expressed or implied, of Dmitry Vyukov.
 */

#pragma once

#include <atomic>
#include <optional>

#include "Utils.hpp"
#include "ManuallyScoped.hpp"

namespace kotlin {

/**
 * A fixed-size concurrent multi-producer/multi-consumer queue.
 * @tparam kCapacity is suggested to set to a power of 2.
 */
template<typename T, std::size_t kCapacity>
class BoundedQueue : private Pinned {
public:
    BoundedQueue() {
        for (size_t i = 0; i < kCapacity; ++i) {
            buffer_[i].sequence_.store(i, std::memory_order_relaxed);
        }
        enqueuePos_.store(0, std::memory_order_relaxed);
        dequeuePos_.store(0, std::memory_order_relaxed);
    }

    bool enqueue(T&& value) {
        Cell* cell;
        std::size_t pos = enqueuePos_.load(std::memory_order_relaxed);
        while (true) {
            cell = &buffer_[pos % kCapacity];
            std::size_t seq = cell->sequence_.load(std::memory_order_acquire);
            std::intptr_t dif = static_cast<std::intptr_t>(seq) - static_cast<std::intptr_t>(pos);
            if (dif == 0) {
                if (enqueuePos_.compare_exchange_weak(pos, pos + 1, std::memory_order_relaxed)) {
                    break;
                }
            } else if (dif < 0) {
                return false;
            } else {
                pos = enqueuePos_.load(std::memory_order_relaxed);
            }
        }
        cell->data_.construct(std::move(value));
        cell->sequence_.store(pos + 1, std::memory_order_release);
        return true;
    }

    std::optional<T> dequeue() {
        Cell* cell;
        std::size_t pos = dequeuePos_.load(std::memory_order_relaxed);
        while (true) {
            cell = &buffer_[pos % kCapacity];
            std::size_t seq = cell->sequence_.load(std::memory_order_acquire);
            std::intptr_t dif = static_cast<std::intptr_t>(seq) - static_cast<std::intptr_t>(pos + 1);
            if (dif == 0) {
                if (dequeuePos_.compare_exchange_weak(pos, pos + 1, std::memory_order_relaxed)) {
                    break;
                }
            } else if (dif < 0) {
                return std::nullopt;
            } else {
                pos = dequeuePos_.load(std::memory_order_relaxed);
            }
        }
        auto result = std::move(*cell->data_);
        cell->data_.destroy();
        cell->sequence_.store(pos + kCapacity, std::memory_order_release);
        return std::move(result);
    }

private:
    struct Cell {
        // TODO describe
        std::atomic<size_t> sequence_;
        ManuallyScoped<T> data_;
    };

    constexpr static auto kCacheLineSize = 128;

    alignas(kCacheLineSize) Cell buffer_[kCapacity];
    alignas(kCacheLineSize) std::atomic<size_t> enqueuePos_;
    alignas(kCacheLineSize) std::atomic<size_t> dequeuePos_;
};

} // namespace kotlin
