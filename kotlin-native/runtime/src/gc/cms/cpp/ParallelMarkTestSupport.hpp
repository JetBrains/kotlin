#pragma once

#include "ParallelMark.hpp"
#include "ConcurrentMarkAndSweep.hpp"

namespace kotlin::gc::mark {

namespace test {

class ParallelMarkTestSupport {
public:
    static std::unique_ptr<MarkDispatcher> fakeDispatcher(std::size_t expectedTasks);
    static void registerTask(MarkDispatcher& dispatcher, MarkDispatcher::MarkJob& job);
    static std::size_t registeredTasks(MarkDispatcher& dispatcher);
    static void performWork(MarkDispatcher::MarkJob& job);
    static bool shareWork(MarkDispatcher::MarkJob& job);
    static CooperativeIntrusiveList<ObjectData>& workList(MarkDispatcher::MarkJob& job);
    static bool tryAcquireWork(MarkDispatcher::MarkJob& job);
};

}
}
