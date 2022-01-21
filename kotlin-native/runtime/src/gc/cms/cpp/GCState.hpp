/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <Logging.hpp>
#include <condition_variable>
#include <mutex>
#include <atomic>
#include <optional>

#include <stdio.h>

#include <thread>

using namespace std::chrono_literals;

namespace {
    int64_t getTime() {
        auto now = std::chrono::high_resolution_clock::now().time_since_epoch();
        return std::chrono::duration_cast<std::chrono::milliseconds>(now).count();
    }
}

class GCStateHolder {
public:
    int64_t schedule() {
        std::unique_lock lock(mutex_);
        // RuntimeLogDebug({"race"}, "schedule: scheduled=%lld, started=%lld, finished=%lld", scheduledEpoch, startedEpoch, finishedEpoch);
        if (scheduledEpoch <= startedEpoch) {
            scheduledEpoch = startedEpoch + 1;
            printf("%lld. Schedule by thread %lld. scheduled=%lld, started=%lld, finished=%lld\n",
                   getTime(), pthread_self(), scheduledEpoch, startedEpoch, finishedEpoch);

            cond_.notify_all();
        }
        return scheduledEpoch;
    }

    void shutdown() {
        std::unique_lock lock(mutex_);
        shutdownFlag_ = true;
        cond_.notify_all();
    }

    void start(int64_t epoch) {
        std::unique_lock lock(mutex_);
        printf("%lld. Start by GC thread (%lld). scheduled=%lld, started=%lld -> %lld, finished=%lld\n",
               getTime(), pthread_self(), scheduledEpoch, startedEpoch, epoch, finishedEpoch);
        printWaiting();

        startedEpoch = epoch;
        cond_.notify_all();
    }

    void finish(int64_t epoch) {
        std::unique_lock lock(mutex_);
        int64_t finishedBefore = finishedEpoch;

        finishedEpoch = epoch;

        printf("%lld. Finish by GC thread (%lld): scheduled=%lld, started=%lld, finished=%lld -> %lld\n",
               getTime(), pthread_self(), scheduledEpoch, startedEpoch, finishedBefore, finishedEpoch);
        printWaiting();

        cond_.notify_all();

    }

    void finalized(int64_t epoch) {
        std::unique_lock lock(mutex_);

        int64_t finalizedBefore = finalizedEpoch;

        finalizedEpoch = epoch;

        printf("%lld. Finalized by GC thread (%lld): scheduled=%lld, started=%lld, finalized=%lld -> %lld\n",
               getTime(), pthread_self(), scheduledEpoch, startedEpoch, finalizedBefore, finalizedEpoch);

        cond_.notify_all();
    }

    void waitEpochFinished(int64_t epoch) {
        std::unique_lock lock(mutex_);

        auto stopWaiting = [this, epoch] { return finishedEpoch >= epoch || shutdownFlag_; };

        int64_t scheduledBefore = scheduledEpoch;
        int64_t startedBefore = startedEpoch;
        int64_t finishedBefore = finishedEpoch;

        int checkCount = 0;

        while (!stopWaiting()) {
            printf("%lld. Mutator %lld starts to wait for finish\n", getTime(), pthread_self());

            waitingThreads.insert(pthread_self());
            auto result = cond_.wait_for(lock, 5000ms);
            waitingThreads.erase(pthread_self());

            if (result == std::cv_status::timeout) {
                printf("%lld. Mutator %lld wake up with timeout in waitEpochFinished. Epoch=%lld, Checks: %d\n"
                                "State before: scheduled=%lld, started=%lld, finished=%lld\n"
                                "State after: scheduled=%lld, started=%lld, finished=%lld\n",
                                getTime(), pthread_self(), epoch, checkCount,
                                scheduledBefore, startedBefore, finishedBefore,
                                scheduledEpoch, startedEpoch, finishedEpoch);

            } else {
                printf("%lld. Mutator %lld wakes up. Epoch=%lld\n"
                       "State: scheduled=%lld, started=%lld, finished=%lld\n",
                       getTime(), pthread_self(), epoch,
                       scheduledEpoch, startedEpoch, finishedEpoch);
                checkCount++;
            }
        }
    }

    void waitEpochFinalized(int64_t epoch) {
        std::unique_lock lock(mutex_);

        auto stopWaiting = [this, epoch] { return finishedEpoch >= epoch || shutdownFlag_; };

        int64_t scheduledBefore = scheduledEpoch;
        int64_t startedBefore = startedEpoch;
        int64_t finishedBefore = finishedEpoch;

        while (!stopWaiting()) {

            waitingThreads.insert(pthread_self());
            auto result = cond_.wait_for(lock, 5000ms);
            waitingThreads.erase(pthread_self());

            if (result == std::cv_status::timeout) {
                printf("%lld. Mutator %lld wake up with timeout in waitEpochFinalized. Epoch=%lld\n"
                       "State before: scheduled=%lld, started=%lld, finished=%lld\n"
                       "State after: scheduled=%lld, started=%lld, finished=%lld\n",
                       getTime(), pthread_self(), epoch,
                       scheduledBefore, startedBefore, finishedBefore,
                       scheduledEpoch, startedEpoch, finishedEpoch);

            }
        }
    }

    std::optional<int64_t> waitScheduled() {
        std::unique_lock lock(mutex_);

        auto stopWaiting = [this] { return scheduledEpoch > finishedEpoch || shutdownFlag_; };

        int64_t scheduledBefore = scheduledEpoch;
        int64_t startedBefore = startedEpoch;
        int64_t finishedBefore = finishedEpoch;

        while (!stopWaiting()) {
            printf("%lld. GC thread (%lld) starts to wait for schedule.\n"
                   "State: scheduled=%lld, started=%lld, finished=%lld\n",
                   getTime(), pthread_self(),
                   scheduledEpoch, startedEpoch, finishedEpoch);

            waitingThreads.insert(pthread_self());
            auto result = cond_.wait_for(lock, 7000ms);
            waitingThreads.erase(pthread_self());

            if (result == std::cv_status::timeout) {
                printf("%lld. GC thread (%lld) wake up with timeout in waitScheduled.\n"
                                "State before: scheduled=%lld, started=%lld, finished=%lld\n"
                                "State after: scheduled=%lld, started=%lld, finished=%lld\n",
                                getTime(), pthread_self(),
                                scheduledBefore, startedBefore, finishedBefore,
                                scheduledEpoch, startedEpoch, finishedEpoch);

            } else {
                printf("%lld. GC thread (%lld) wakes up when waiting for schedule.\n"
                       "State: scheduled=%lld, started=%lld, finished=%lld\n",
                       getTime(), pthread_self(),
                       scheduledEpoch, startedEpoch, finishedEpoch);
            }
        }



        if (shutdownFlag_) return std::nullopt;
        return scheduledEpoch;
    }

private:
    void printWaiting() {
        printf("Waiting threads: ");
        for (auto t : waitingThreads) {
            printf("%lld, ", t);
        }
        printf("\n");
    }

    std::unordered_set<pthread_t> waitingThreads;

    std::mutex mutex_;
    std::condition_variable cond_;
    int64_t startedEpoch = 0;
    int64_t finishedEpoch = 0;
    int64_t scheduledEpoch = 0;
    int64_t finalizedEpoch = 0;
    bool shutdownFlag_ = false;
};