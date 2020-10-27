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

#include "Porting.h"
#include "Types.h"

typedef KInt Arena;
typedef KInt Object;
typedef KInt Pointer;

#ifndef KONAN_WASM

extern "C" {

// These functions are implemented in JS file for WASM and are not available on other platforms.
RUNTIME_NORETURN Arena Konan_js_allocateArena() {
  RuntimeAssert(false, "JavaScript interop is disabled");
  konan::abort();
}

RUNTIME_NORETURN void Konan_js_freeArena(Arena arena) {
  RuntimeAssert(false, "JavaScript interop is disabled");
  konan::abort();
}

RUNTIME_NORETURN void Konan_js_pushIntToArena(Arena arena, KInt value) {
  RuntimeAssert(false, "JavaScript interop is disabled");
  konan::abort();
}

RUNTIME_NORETURN KInt Konan_js_getInt(Arena arena,
                                      Object obj,
                                      Pointer propertyPtr,
                                      KInt propertyLen) {
  RuntimeAssert(false, "JavaScript interop is disabled");
  konan::abort();
}

RUNTIME_NORETURN KInt Konan_js_getProperty(Arena arena,
                                           Object obj,
                                           Pointer propertyPtr,
                                           KInt propertyLen) {
  RuntimeAssert(false, "JavaScript interop is disabled");
  konan::abort();
}

RUNTIME_NORETURN void Konan_js_setFunction(Arena arena,
                                           Object obj,
                                           Pointer propertyName,
                                           KInt propertyLength,
                                           KInt function) {
  RuntimeAssert(false, "JavaScript interop is disabled");
  konan::abort();
}

RUNTIME_NORETURN void Konan_js_setString(Arena arena,
                                         Object obj,
                                         Pointer propertyName,
                                         KInt propertyLength,
                                         Pointer stringPtr,
                                         KInt stringLength) {
  RuntimeAssert(false, "JavaScript interop is disabled");
  konan::abort();
}

}; // extern "C"

#endif // #ifndef KONAN_WASM
