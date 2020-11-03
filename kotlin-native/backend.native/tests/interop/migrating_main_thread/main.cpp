/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "testlib_api.h"

#include <cassert>
#include <thread>

int main() {
    std::thread main1([]() {
        assert(testlib_symbols()->kotlin.root.readFromA() == 0);
        testlib_symbols()->kotlin.root.writeToA(1);
        assert(testlib_symbols()->kotlin.root.readFromA() == 1);
    });
    main1.join();

    std::thread main2([]() {
        // Globals were reinitialized.
        assert(testlib_symbols()->kotlin.root.readFromA() == 0);
    });
    main2.join();

    return 0;
}
