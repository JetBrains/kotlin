/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <memory>
#include <utility>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "Types.h"
#include "Utils.hpp"

namespace kotlin {
namespace test_support {

namespace internal {

extern testing::MockFunction<KInt()>* createCleanerWorkerMock;
extern testing::MockFunction<void(KInt, bool)>* shutdownCleanerWorkerMock;
extern testing::MockFunction<void(KRef)>* reportUnhandledExceptionMock;
extern testing::MockFunction<void(KRef)>* Kotlin_runUnhandledExceptionHookMock;

} // namespace internal

template <class F, bool Strict = true>
class ScopedMockFunction : private kotlin::MoveOnly {
public:
    using Mock = typename std::conditional_t<Strict,
                                             testing::StrictMock<testing::MockFunction<F>>,
                                             testing::NiceMock<testing::MockFunction<F>>>;

    explicit ScopedMockFunction(testing::MockFunction<F>** globalMockLocation) : globalMockLocation_(globalMockLocation) {
        RuntimeCheck(globalMockLocation != nullptr, "ScopedMockFunction needs non-null global mock location");
        RuntimeCheck(*globalMockLocation == nullptr, "ScopedMockFunction needs null global mock");
        mock_ = make_unique<Mock>();
        *globalMockLocation_ = mock_.get();
    }

    ScopedMockFunction(ScopedMockFunction&& rhs) : globalMockLocation_(rhs.globalMockLocation_), mock_(std::move(rhs.mock_)) {
        rhs.globalMockLocation_ = nullptr;
    }

    ScopedMockFunction& operator=(ScopedMockFunction&& rhs) {
        ScopedMockFunction tmp(std::move(rhs));
        swap(tmp);
        return *this;
    }

    ~ScopedMockFunction() {
        if (!globalMockLocation_) return;

        RuntimeCheck(*globalMockLocation_ == mock_.get(), "unexpected global mock location");

        mock_.reset();

        *globalMockLocation_ = nullptr;
    }

    void swap(ScopedMockFunction& other) {
        std::swap(globalMockLocation_, other.globalMockLocation_);
        std::swap(mock_, other.mock_);
    }

    Mock& get() { return *mock_; }
    Mock& operator*() { return *mock_; }

private:
    // Can be null if moved-out of.
    testing::MockFunction<F>** globalMockLocation_;
    KStdUniquePtr<Mock> mock_;
};

template<bool Strict = true>
ScopedMockFunction<KInt(), Strict> ScopedCreateCleanerWorkerMock() {
    return ScopedMockFunction<KInt(), Strict>(&internal::createCleanerWorkerMock);
}

template<bool Strict = true>
ScopedMockFunction<void(KInt, bool), Strict> ScopedShutdownCleanerWorkerMock() {
    return ScopedMockFunction<void(KInt, bool), Strict>(&internal::shutdownCleanerWorkerMock);
}

template<bool Strict = true>
ScopedMockFunction<void(KRef), Strict> ScopedReportUnhandledExceptionMock() {
    return ScopedMockFunction<void(KRef), Strict>(&internal::reportUnhandledExceptionMock);
}

template<bool Strict = true>
ScopedMockFunction<void(KRef), Strict> ScopedKotlin_runUnhandledExceptionHookMock() {
    return ScopedMockFunction<void(KRef), Strict>(&internal::Kotlin_runUnhandledExceptionHookMock);
}

} // namespace test_support
} // namespace kotlin