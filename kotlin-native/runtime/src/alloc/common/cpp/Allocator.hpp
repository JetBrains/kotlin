/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

namespace kotlin::alloc {

// TODO: Build `Allocator`, `Allocator::ThreadData` like with `gc`, `gcScheduler`,
//       and move allocator-specific data there.

void initObjectPool() noexcept;
void compactObjectPoolInCurrentThread() noexcept;

}
