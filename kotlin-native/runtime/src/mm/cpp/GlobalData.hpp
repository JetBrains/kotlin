/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_GLOBAL_DATA_H
#define RUNTIME_MM_GLOBAL_DATA_H

#include "GlobalsRegistry.hpp"
#include "ThreadRegistry.hpp"
#include "Utils.hpp"

namespace kotlin {
namespace mm {

// Global (de)initialization is undefined in C++. Use single global singleton to define it for simplicity.
class GlobalData : private Pinned {
public:
    static GlobalData& Instance() noexcept { return instance_; }

    ThreadRegistry& threadRegistry() { return threadRegistry_; }
    GlobalsRegistry& globalsRegistry() { return globalsRegistry_; }

private:
    GlobalData();
    ~GlobalData();

    static GlobalData instance_;

    ThreadRegistry threadRegistry_;
    GlobalsRegistry globalsRegistry_;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_GLOBAL_DATA_H
