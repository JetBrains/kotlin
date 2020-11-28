/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GlobalData.hpp"

using namespace kotlin;

mm::GlobalData::GlobalData() = default;
mm::GlobalData::~GlobalData() = default;

// static
mm::GlobalData mm::GlobalData::instance_;
