/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "testlib_api.h"

#include <cassert>
#include <thread>

constexpr int kInitialValue = 0;
constexpr int kNewValue = 1;
constexpr int kErrorValue = 2;

int main() {
    std::thread main1([]() {
        assert(testlib_symbols()->kotlin.root.tryReadFromA(kErrorValue) == kInitialValue);
        testlib_symbols()->kotlin.root.writeToA(kNewValue);
        assert(testlib_symbols()->kotlin.root.tryReadFromA(kErrorValue) == kNewValue);
    });
    main1.join();

    std::thread main2([]() {
#if defined(IS_LEGACY)
        // Globals were reinitialized.
        assert(testlib_symbols()->kotlin.root.tryReadFromA(kErrorValue) == kInitialValue);
#else
        // Globals are not accessible.
        assert(testlib_symbols()->kotlin.root.tryReadFromA(kErrorValue) == kErrorValue);
#endif
    });
    main2.join();

    return 0;
}
