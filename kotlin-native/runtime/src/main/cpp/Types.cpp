/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Types.h"
#include "Exceptions.h"

extern "C" {

// Note: keeping it for compatibility with external tools only, will be deprecated and removed in the future.
RUNTIME_PURE RUNTIME_USED RUNTIME_WEAK KBoolean IsInstance(const ObjHeader* obj, const TypeInfo* type_info) {
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

KVector4f Kotlin_Interop_Vector4f_of(KFloat f0, KFloat f1, KFloat f2, KFloat f3) {
	return {f0, f1, f2, f3};
}

/*
 * In the current design all simd types are mapped internally to floating type, e.g. <4 x float>.
 * However, some platforms (ex. arm32) have different calling convention for <4 x float> and <4 x i32>.
 * To avoid illegal bitcast from/to function types the following function
 * return type MUST be <4 x float> and explicit type cast is done on the variable type.
 */
KVector4f Kotlin_Interop_Vector4i32_of(KInt f0, KInt f1, KInt f2, KInt f3) {
	KInt __attribute__ ((__vector_size__(16))) v4i = {f0, f1, f2, f3};
	return (KVector4f)v4i;
}

long Kotlin_longTypeProvider() {
    return 0;
}

}  // extern "C"
