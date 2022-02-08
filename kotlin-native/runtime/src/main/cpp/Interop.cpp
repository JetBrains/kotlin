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

#include <assert.h>
#include <stdio.h>
#include <stdint.h>
#include <string.h>

#include "Alloc.h"
#include "KString.h"
#include "Memory.h"
#include "MemorySharedRefs.hpp"
#include "Types.h"

extern "C" {

KNativePtr Kotlin_Interop_createStablePointer(KRef any) {
  KRefSharedHolder* holder = konanConstructInstance<KRefSharedHolder>();
  holder->init(any);
  return holder;
}

void Kotlin_Interop_disposeStablePointer(KNativePtr pointer) {
  KRefSharedHolder* holder = reinterpret_cast<KRefSharedHolder*>(pointer);
  holder->dispose();
  konanDestructInstance(holder);
}

OBJ_GETTER(Kotlin_Interop_derefStablePointer, KNativePtr pointer) {
  KRefSharedHolder* holder = reinterpret_cast<KRefSharedHolder*>(pointer);
  RETURN_OBJ(holder->ref<ErrorPolicy::kThrow>());
}

OBJ_GETTER(Kotlin_CString_toKStringFromUtf8Impl, const char* cstring) {
  RETURN_RESULT_OF(StringFromUtf8Buffer, cstring, strlen(cstring));
}

}
