/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

#include "Types.h"

#ifdef KONAN_WASM

#ifdef __cplusplus
extern "C" {
#endif

KDouble ReturnSlot_getDouble();
void ReturnSlot_setDouble(KInt upper, KInt lower);

#ifdef __cplusplus
}  // extern "C"
#endif

#endif  // KONAN_WASM
