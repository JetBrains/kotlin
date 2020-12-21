/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_GLOBAL_DATA_H
#define RUNTIME_MM_GLOBAL_DATA_H

#include "ObjectFactory.hpp"
#include "GlobalsRegistry.hpp"
#include "StableRefRegistry.hpp"
#include "ThreadRegistry.hpp"
#include "Utils.hpp"

namespace kotlin {
namespace mm {

// Global (de)initialization is undefined in C++. Use single global singleton to define it for simplicity.
class GlobalData : private Pinned {
public:
    static GlobalData& Instance() noexcept { return instance_; }

    ThreadRegistry& threadRegistry() noexcept { return threadRegistry_; }
    GlobalsRegistry& globalsRegistry() noexcept { return globalsRegistry_; }
    StableRefRegistry& stableRefRegistry() noexcept { return stableRefRegistry_; }
    ObjectFactory& objectFactory() noexcept { return objectFactory_; }

private:
    GlobalData();
    ~GlobalData();

    static GlobalData instance_;

    ThreadRegistry threadRegistry_;
    GlobalsRegistry globalsRegistry_;
    StableRefRegistry stableRefRegistry_;
    ObjectFactory objectFactory_;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_GLOBAL_DATA_H
