/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <new>

namespace kotlin::std_support {

struct kalloc_t {};
inline constexpr kalloc_t kalloc = kotlin::std_support::kalloc_t{};

} // namespace kotlin::std_support

// TODO: Add align_val_t overloads once we make sure all targets support aligned allocation.
//       (also requires removing `-fno-aligned-allocation` compiler flag).

void* operator new(std::size_t count, kotlin::std_support::kalloc_t) noexcept;
void operator delete(void* ptr, kotlin::std_support::kalloc_t) noexcept;

namespace kotlin::std_support {

template <typename T>
void kdelete(T* ptr) noexcept {
    ptr->~T();
    ::operator delete(ptr, kalloc);
}

} // namespace kotlin::std_support
