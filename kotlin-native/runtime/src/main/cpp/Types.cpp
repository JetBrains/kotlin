/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Types.h"
#include "Exceptions.h"

extern "C" {

// Note: keeping it for compatibility with external tools only, will be deprecated and removed in the future.
RUNTIME_PURE RUNTIME_EXPORT RUNTIME_WEAK KBoolean IsInstance(const ObjHeader* obj, const TypeInfo* type_info) {
  return IsInstanceInternal(obj, type_info);
}

KBoolean IsInstanceInternal(const ObjHeader* obj, const TypeInfo* type_info) {
  // We assume null check is handled by caller.
  RuntimeAssert(obj != nullptr, "must not be null");
  const TypeInfo* obj_type_info = obj->type_info();
  return IsSubtype(obj_type_info, type_info);
}

KBoolean IsSubtype(const TypeInfo* obj_type_info, const TypeInfo* type_info) {
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

KBoolean IsSubclassFast(const TypeInfo* obj_type_info, int32_t lo, int32_t hi) {
  // Super type's interval should contain our interval.
  return obj_type_info->classId_ >= lo && obj_type_info->classId_ <= hi;
}

KBoolean IsArray(KConstRef obj) {
  RuntimeAssert(obj != nullptr, "Object must not be null");
  return obj->type_info()->instanceSize_ < 0;
}

KBoolean Kotlin_TypeInfo_isInstance(KConstRef obj, KNativePtr typeInfo) {
  return IsInstanceInternal(obj, reinterpret_cast<const TypeInfo*>(typeInfo));
}

OBJ_GETTER(Kotlin_TypeInfo_getPackageName, KNativePtr typeInfo, KBoolean checkFlags) {
  const TypeInfo* type_info = reinterpret_cast<const TypeInfo*>(typeInfo);
  if (!checkFlags || type_info->flags_ & TF_REFLECTION_SHOW_PKG_NAME) {
    RETURN_OBJ(type_info->packageName_);
  } else {
    return NULL;
  }
}

OBJ_GETTER(Kotlin_TypeInfo_getRelativeName, KNativePtr typeInfo, KBoolean checkFlags) {
  const TypeInfo* type_info = reinterpret_cast<const TypeInfo*>(typeInfo);
  if (!checkFlags || type_info->flags_ & TF_REFLECTION_SHOW_REL_NAME) {
    RETURN_OBJ(type_info->relativeName_);
  } else {
    return NULL;
  }
}

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

long Kotlin_longTypeProvider() {
    return 0;
}

}  // extern "C"
