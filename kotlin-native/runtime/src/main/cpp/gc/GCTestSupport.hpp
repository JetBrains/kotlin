/*
 * Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include "gc/GC.hpp"

namespace kotlin::gc::test_support {

bool tryMark(ObjHeader* object) noexcept;

}