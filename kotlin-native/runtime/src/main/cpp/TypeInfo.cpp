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

#include <string>

#include "KAssert.h"
#include "TypeInfo.h"
#include "Memory.h"
#include "Types.h"
#include "KString.h"

extern "C" {

// Seeks for the specified id. In case of failure returns a valid pointer to some record, never returns nullptr.
// It is the caller's responsibility to check if the search has succeeded or not.
InterfaceTableRecord const* LookupInterfaceTableRecord(InterfaceTableRecord const* interfaceTable,
                                                       int interfaceTableSize, ClassId interfaceId) {
  if (interfaceTableSize <= 8) {
    // Linear search.
    int i;
    for (i = 0; i < interfaceTableSize - 1 && interfaceTable[i].id < interfaceId; ++i);
    return interfaceTable + i;
  }
  int l = 0, r = interfaceTableSize - 1;
  while (l < r) {
    int m = (l + r) / 2;
    if (interfaceTable[m].id < interfaceId)
      l = m + 1;
    else r = m;
  }
  return interfaceTable + l;
}

RUNTIME_NOTHROW int Kotlin_internal_reflect_getObjectReferenceFieldsCount(ObjHeader* object) {
    auto *info = object->type_info();
    if (info->IsArray()) return 0;
    return info->objOffsetsCount_;
}

RUNTIME_NOTHROW OBJ_GETTER(Kotlin_internal_reflect_getObjectReferenceFieldByIndex, ObjHeader* object, int index) {
    RETURN_OBJ(*reinterpret_cast<ObjHeader**>(reinterpret_cast<uintptr_t>(object) + object->type_info()->objOffsets_[index]));
}

RUNTIME_NOTHROW const TypeInfo* Kotlin_native_internal_reflect_superClass(const TypeInfo* typeInfo) {
    return typeInfo->superType_;
}

}

std::string TypeInfo::fqName() const {
    std::string fqName = "";
    if (packageName_) {
        fqName += kotlin::to_string(packageName_->array());
        fqName += ".";
    }
    if (relativeName_) {
        fqName += kotlin::to_string(relativeName_->array());
    } else {
        fqName += "<anonymous>";
    }
    return fqName;
}
