/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "KAssert.h"
#include "Porting.h"
#include "Common.h"

#if KONAN_LINUX || KONAN_WINDOWS
// This function replaces `__cxa_demangle` defined in GNU libstdc++
// by adding `--defsym` flag in `konan.properties`.
// This allows to avoid linking `__cxa_demangle` and its dependencies, thus reducing binary size.
RUNTIME_USED RUNTIME_WEAK extern "C" char* Konan_cxa_demangle(
    const char* __mangled_name, char* __output_buffer,
    size_t* __length, int* __status
) {
  *__status = -2; // __mangled_name is not a valid name under the C++ ABI mangling rules.
  return nullptr;
}

namespace std {
RUNTIME_WEAK void __throw_length_error(const char* __s __attribute__((unused))) {
  RuntimeCheck(false, "%s", __s);
}

}  // namespace std

#endif // KONAN_LINUX || KONAN_WINDOWS

#if KONAN_LINUX

#include <system_error>

// Since GCC 4.8.5 mangling for std::system_category has changed from _ZNSt3_V215system_categoryEv to _ZSt15system_categoryv.
// It causes linkage problems in case of kotlinx-datetime:0.1.1 usage, because its C++ dependency is compiled with old GCC 4.8.5.
// So to avoid nasty linkage problems when users migrate their projects with kotlinx-datetime:0.1.1 dependency from 1.4.x to 1.5.x
// we provide this compatibility layer.
RUNTIME_WEAK
const std::error_category& std_system_category_backward_compatibility_with_gcc_4_8_5() noexcept __asm("_ZSt15system_categoryv");

const std::error_category& std_system_category_backward_compatibility_with_gcc_4_8_5() noexcept {
    return std::system_category();
}

#endif // KONAN_LINUX
