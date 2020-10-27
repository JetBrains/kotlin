/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "testlib_api.h"

#include <thread>

int main() {
    std::thread t([]() { testlib_symbols()->kotlin.root.createCleaner(); });
    t.join();
    testlib_symbols()->kotlin.root.performGC();
    return 0;
}
