/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <thread>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "Memory.h"
#include "TestSupport.hpp"
#include "ThreadData.hpp"
#include "Types.h"

using namespace kotlin;

namespace {

class ExceptionObjHolderTest : public ::testing::Test {
public:
    ~ExceptionObjHolderTest() {
        auto& stableRefs = mm::StableRefRegistry::Instance();
        stableRefs.ClearForTests();
    }

    static KStdVector<ObjHeader*> Collect(mm::ThreadData& threadData) {
        auto& stableRefs = mm::StableRefRegistry::Instance();
        stableRefs.ProcessThread(&threadData);
        stableRefs.ProcessDeletions();
        KStdVector<ObjHeader*> result;
        for (const auto& obj : stableRefs.Iter()) {
            result.push_back(obj);
        }
        return result;
    }

private:
};

} // namespace

TEST_F(ExceptionObjHolderTest, NothingByDefault) {
    mm::RunInNewThread([](mm::ThreadData& threadData) { EXPECT_THAT(Collect(threadData), testing::IsEmpty()); });
}

TEST_F(ExceptionObjHolderTest, Throw) {
    mm::RunInNewThread([](mm::ThreadData& threadData) {
        ASSERT_THAT(Collect(threadData), testing::IsEmpty());

        ObjHeader exception;
        try {
            ExceptionObjHolder::Throw(&exception);
        } catch (...) {
            EXPECT_THAT(Collect(threadData), testing::ElementsAre(&exception));
        }
        EXPECT_THAT(Collect(threadData), testing::IsEmpty());
    });
}

TEST_F(ExceptionObjHolderTest, ThrowInsideCatch) {
    mm::RunInNewThread([](mm::ThreadData& threadData) {
        ASSERT_THAT(Collect(threadData), testing::IsEmpty());

        ObjHeader exception1;
        try {
            ExceptionObjHolder::Throw(&exception1);
        } catch (...) {
            ObjHeader exception2;
            try {
                ExceptionObjHolder::Throw(&exception2);
            } catch (...) {
                EXPECT_THAT(Collect(threadData), testing::ElementsAre(&exception1, &exception2));
            }
            EXPECT_THAT(Collect(threadData), testing::ElementsAre(&exception1));
        }
        EXPECT_THAT(Collect(threadData), testing::IsEmpty());
    });
}

TEST_F(ExceptionObjHolderTest, StoreException) {
    mm::RunInNewThread([](mm::ThreadData& threadData) {
        ASSERT_THAT(Collect(threadData), testing::IsEmpty());

        ObjHeader exception1;
        std::exception_ptr storedException1;
        try {
            ExceptionObjHolder::Throw(&exception1);
        } catch (...) {
            storedException1 = std::current_exception();
        }
        EXPECT_THAT(Collect(threadData), testing::ElementsAre(&exception1));

        ObjHeader exception2;
        std::exception_ptr storedException2;
        try {
            ExceptionObjHolder::Throw(&exception2);
        } catch (...) {
            storedException2 = std::current_exception();
        }
        EXPECT_THAT(Collect(threadData), testing::ElementsAre(&exception1, &exception2));

        storedException1 = std::exception_ptr();
        EXPECT_THAT(Collect(threadData), testing::ElementsAre(&exception2));

        storedException2 = std::exception_ptr();
        EXPECT_THAT(Collect(threadData), testing::IsEmpty());
    });
}
