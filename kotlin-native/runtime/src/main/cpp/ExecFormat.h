/*
 * Copyright 2010-2026 JetBrains s.r.o.
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

#ifndef RUNTIME_EXECFORMAT_H
#define RUNTIME_EXECFORMAT_H

#include <stddef.h>

#if __has_include("dlfcn.h")
#include <dlfcn.h>
#endif

extern "C" {

bool AddressToSymbol(const void* address, char* resultBuffer, size_t resultBufferSize, ptrdiff_t &resultOffset);

}  // extern "C"

#if defined(__cplusplus) && __has_include("dlfcn.h")
bool AddressToSymbolWithDlInfo(const void* address, const Dl_info& info, char* resultBuffer, size_t resultBufferSize, ptrdiff_t& resultOffset);
#endif

#endif  // RUNTIME_EXECFORMAT_H
