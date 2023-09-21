/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#import "Types.h"
#import "Memory.h"
#include "ObjCInterop.h"
#include "KString.h"
#include "TypeInfo.h"
#include <vector>

#if KONAN_OBJC_INTEROP

#import <mutex>

#import <Foundation/Foundation.h>

#import "SwiftExport.h"
#import "ObjCExport.h"
#import "Memory.h"
#import "ObjCExportPrivate.h"

using namespace kotlin;

extern "C" id Kotlin_SwiftExport_refToSwiftObject(ObjHeader *obj) {
    return Kotlin_ObjCExport_refToObjC(obj); // FIXME: For now, we just return objc counterparts
}

namespace {

    template <typename T>
    inline T* konanAllocArray(size_t length) {
        if (length == 0) {
            return nullptr;
        }
        return reinterpret_cast<T*>(std::calloc(length, sizeof(T)));
    }

}

static const TypeInfo *createTypeInfo(
        const char *className,
        const TypeInfo *superType,
        const std::vector<VTableElement>& vtable
) {
    size_t vtableSize = vtable.size() * sizeof(void*);
    TypeInfo* result = (TypeInfo*) std::calloc(1, sizeof(TypeInfo) + vtableSize);
    result->typeInfo_ = result;
    result->instanceSize_ = superType->instanceSize_;
    result->objOffsets_ = superType->objOffsets_;
    result->objOffsetsCount_ = superType->objOffsetsCount_;
    if ((superType->flags_ & TF_IMMUTABLE) != 0) {
        result->flags_ |= TF_IMMUTABLE;
    }

    const TypeInfo** implementedInterfaces_ = konanAllocArray<const TypeInfo*>(0);
    result->implementedInterfaces_ = implementedInterfaces_;
    result->implementedInterfacesCount_ = 0;

    result->processObjectInMark = superType->processObjectInMark;
    result->classId_ = superType->classId_;
    result->packageName_ = nullptr; // TODO: We can do something smart here
    result->relativeName_ = CreatePermanentStringFromCString(className);
    result->writableInfo_ = (WritableTypeInfo*)std::calloc(1, sizeof(WritableTypeInfo));
    for (size_t i = 0; i < vtable.size(); ++i) result->vtable()[i] = vtable[i];
    return result;
}

const TypeInfo *getOrCreateTypeInfoForSwiftValue(Class objectClass) {
    const char *className = class_getName(objectClass);
    const TypeInfo *typeInfo = Kotlin_ObjCExport_getAssociatedTypeInfo(objectClass);
    if (typeInfo != nullptr) {
        return typeInfo;
    }
    Class superClass = class_getSuperclass(objectClass);
    const TypeInfo *superTypeInfo = superClass == nullptr ?
                                    theSwiftValueTypeInfo : getOrCreateTypeInfoForSwiftValue(superClass);

    VTableElement const *superVTablePtr = superTypeInfo->vtable();
    int superVTableSize = 3; // TODO: For now, it's just Any methods.
    std::vector<const void*> vtable(superVTablePtr, superVTablePtr + superVTableSize);

    const TypeInfo *result = createTypeInfo(className, superTypeInfo, vtable);
    setAssociatedTypeInfo(objectClass, result);
    return result;
}

extern "C" OBJ_GETTER(Kotlin_SwiftExport_swiftObjectToRef, id obj) {
    // TODO: After adding Swift/C++ interop to runtime, replace objc runtime functions with the swift ones.
    Class objectClass = object_getClass(obj);
    Class kotlinBase = objc_getClass("KotlinBase");
    Class currentClass = objectClass;
    while (currentClass != nullptr) {
        if (currentClass == kotlinBase) {
            // This is a Kotlin object, we can just unwrap it.
            RETURN_RESULT_OF(Kotlin_ObjCExport_refFromObjC, obj);
        }
        currentClass = class_getSuperclass(currentClass);
    }
    // This is a pure Swift/Objective-C type. Let's wrap it into a SwiftValue object.
    const TypeInfo *typeInfo = getOrCreateTypeInfoForSwiftValue(objectClass);
    // TODO: Is it OK to use objc_retain here?
    RETURN_RESULT_OF(AllocInstanceWithAssociatedObject, typeInfo, objc_retain(obj));
}

#endif // KONAN_OBJC_INTEROP
