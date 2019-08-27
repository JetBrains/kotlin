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
#include "Exceptions.h"

extern "C" {

KBoolean IsInstance(const ObjHeader* obj, const TypeInfo* type_info) {
  // We assume null check is handled by caller.
  RuntimeAssert(obj != nullptr, "must not be null");
  const TypeInfo* obj_type_info = obj->type_info();
  // If it is an interface - check in list of implemented interfaces.
  if ((type_info->flags_ & TF_INTERFACE) != 0) {
    for (int i = 0; i < obj_type_info->implementedInterfacesCount_; ++i) {
      if (obj_type_info->implementedInterfaces_[i] == type_info) {
        return 1;
      }
    }
    return 0;
  }
  while (obj_type_info != nullptr && obj_type_info != type_info) {
    obj_type_info = obj_type_info->superType_;
  }
  return obj_type_info != nullptr;
}

KBoolean IsInstanceOfClassFast(const ObjHeader* obj, int32_t lo, int32_t hi) {
  // We assume null check is handled by caller.
  RuntimeAssert(obj != nullptr, "must not be null");
  const TypeInfo* obj_type_info = obj->type_info();
  // Super type's interval should contain our interval.
  return obj_type_info->classId_ >= lo && obj_type_info->classId_ <= hi;
}

KBoolean IsArray(KConstRef obj) {
  RuntimeAssert(obj != nullptr, "Object must not be null");
  return obj->type_info()->instanceSize_ < 0;
}

KBoolean Kotlin_TypeInfo_isInstance(KConstRef obj, KNativePtr typeInfo) {
  return IsInstance(obj, reinterpret_cast<const TypeInfo*>(typeInfo));
}

OBJ_GETTER(Kotlin_TypeInfo_getPackageName, KNativePtr typeInfo) {
  RETURN_OBJ(reinterpret_cast<const TypeInfo*>(typeInfo)->packageName_);
}

OBJ_GETTER(Kotlin_TypeInfo_getRelativeName, KNativePtr typeInfo) {
  RETURN_OBJ(reinterpret_cast<const TypeInfo*>(typeInfo)->relativeName_);
}

struct AssociatedObjectTableRecord {
  const TypeInfo* key;
  OBJ_GETTER0((*getAssociatedObjectInstance));
};

OBJ_GETTER(Kotlin_TypeInfo_findAssociatedObject, KNativePtr typeInfo, KNativePtr key) {
  const AssociatedObjectTableRecord* associatedObjects = reinterpret_cast<const TypeInfo*>(typeInfo)->associatedObjects;
  if (associatedObjects == nullptr) {
    RETURN_OBJ(nullptr);
  }

  for (int index = 0; associatedObjects[index].key != nullptr; ++index) {
    if (associatedObjects[index].key == key) {
      RETURN_RESULT_OF0(associatedObjects[index].getAssociatedObjectInstance);
    }
  }

  RETURN_OBJ(nullptr);
}

bool IsSubInterface(const TypeInfo* thiz, const TypeInfo* other) {
  for (int i = 0; i < thiz->implementedInterfacesCount_; ++i) {
    if (thiz->implementedInterfaces_[i] == other) {
      return true;
    }
  }

  return false;
}

}  // extern "C"
