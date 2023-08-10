/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ClockTestSupport.hpp"

using namespace kotlin;

// static
std::atomic<test_support::manual_clock::time_point> test_support::manual_clock::now_ = test_support::manual_clock::time_point::min();

// static
std::mutex test_support::manual_clock::pendingWaitsMutex_;

// static
std::multiset<test_support::manual_clock::time_point> test_support::manual_clock::pendingWaits_;
