/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#if !KONAN_HAS_UIKIT_FRAMEWORK

#include "AppStateTracking.hpp"

using namespace kotlin;

class mm::AppStateTracking::Impl {};

mm::AppStateTracking::AppStateTracking() noexcept = default;

mm::AppStateTracking::~AppStateTracking() = default;

#endif
