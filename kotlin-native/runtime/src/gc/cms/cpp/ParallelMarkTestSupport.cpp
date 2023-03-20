#include "ParallelMarkTestSupport.hpp"
#include "ParallelMark.hpp"

using namespace kotlin;

std::unique_ptr<gc::mark::MarkDispatcher> gc::mark::test::ParallelMarkTestSupport::fakeDispatcher(std::size_t expectedJobs) {
    auto dispatcher = std::make_unique<MarkDispatcher>(0, false);
    dispatcher->expectedJobs_ = expectedJobs;
    dispatcher->expandJobsArrayIfNeeded();
    dispatcher->gcHandle_ = GCHandle::createFakeForTests();
    dispatcher->pacer_.beginEpoch(dispatcher->gcHandle().getEpoch());
    return dispatcher;
}

void gc::mark::test::ParallelMarkTestSupport::registerTask(gc::mark::MarkDispatcher& dispatcher,
                                                           gc::mark::MarkDispatcher::MarkJob& job) {
    dispatcher.registerTask(job);
}

std::size_t gc::mark::test::ParallelMarkTestSupport::registeredTasks(gc::mark::MarkDispatcher& dispatcher) {
    return dispatcher.registeredJobs_;
}

bool gc::mark::test::ParallelMarkTestSupport::shareWork(MarkDispatcher::MarkJob& job) {
    return job.shareWork();
}

StealableWorkList<gc::mark::ObjectData>& gc::mark::test::ParallelMarkTestSupport::workList(gc::mark::MarkDispatcher::MarkJob& job) {
    return job.workList_;
}

void gc::mark::test::ParallelMarkTestSupport::performWork(gc::mark::MarkDispatcher::MarkJob& job) {
    auto markHandle = job.dispatcher_.gcHandle_.mark();
    job.performWork(markHandle);
}

bool gc::mark::test::ParallelMarkTestSupport::tryAcquireWork(MarkDispatcher::MarkJob& job) {
    return job.tryAcquireWork();
}

