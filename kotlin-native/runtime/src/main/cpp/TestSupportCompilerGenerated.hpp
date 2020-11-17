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

template <class F>
class ScopedStrictMockFunction : private kotlin::MoveOnly {
public:
    using Mock = testing::StrictMock<testing::MockFunction<F>>;

    explicit ScopedStrictMockFunction(Mock** globalMockLocation) : globalMockLocation_(globalMockLocation) {
        RuntimeCheck(globalMockLocation != nullptr, "ScopedStrictMockFunction needs non-null global mock location");
        RuntimeCheck(*globalMockLocation == nullptr, "ScopedStrictMockFunction needs null global mock");
        // TODO: Use make_unique when sysroots on Linux get updated.
        mock_ = std::unique_ptr<Mock>(new Mock());
        *globalMockLocation_ = mock_.get();
    }

    ScopedStrictMockFunction(ScopedStrictMockFunction&& rhs) : globalMockLocation_(rhs.globalMockLocation_), mock_(std::move(rhs.mock_)) {
        rhs.globalMockLocation_ = nullptr;
    }

    ScopedStrictMockFunction& operator=(ScopedStrictMockFunction&& rhs) {
        ScopedStrictMockFunction tmp(std::move(rhs));
        swap(tmp);
        return *this;
    }

    ~ScopedStrictMockFunction() {
        if (!globalMockLocation_) return;

        RuntimeCheck(*globalMockLocation_ == mock_.get(), "unexpected global mock location");

        testing::Mock::VerifyAndClear(mock_.get());
        mock_.reset();

        *globalMockLocation_ = nullptr;
    }

    void swap(ScopedStrictMockFunction& other) {
        std::swap(globalMockLocation_, other.globalMockLocation_);
        std::swap(mock_, other.mock_);
    }

    Mock& get() { return *mock_; }
    Mock& operator*() { return *mock_; }

private:
    // Can be null if moved-out of.
    Mock** globalMockLocation_;
    std::unique_ptr<Mock> mock_;
};

ScopedStrictMockFunction<KInt()> ScopedCreateCleanerWorkerMock();
ScopedStrictMockFunction<void(KInt, bool)> ScopedShutdownCleanerWorkerMock();
