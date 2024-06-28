/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "migrating_main_thread_api.h"

#include <cassert>
#include <thread>

constexpr int kInitialValue = 0;
constexpr int kNewValue = 1;
constexpr int kErrorValue = 2;

int main() {
    auto* symbols = migrating_main_thread_symbols();
    std::thread main1([symbols]() {
        assert(symbols->kotlin.root.tryReadFromA(kErrorValue) == kInitialValue);
        symbols->kotlin.root.writeToA(kNewValue);
        assert(symbols->kotlin.root.tryReadFromA(kErrorValue) == kNewValue);
    });
    main1.join();

    std::thread main2([symbols]() {
        // Globals are preserved.
        assert(symbols->kotlin.root.tryReadFromA(kErrorValue) == kNewValue);

    });
    main2.join();

    return 0;
}
