/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "Utils.hpp"

namespace kotlin::objc_support {

class AutoreleasePool : private Pinned {
public:
    AutoreleasePool() noexcept;
    ~AutoreleasePool();

private:
#if KONAN_OBJC_INTEROP
    void* handle_;
#endif
};

} // namespace kotlin::objc_support