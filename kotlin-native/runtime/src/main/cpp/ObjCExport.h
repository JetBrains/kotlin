#ifndef RUNTIME_OBJCEXPORT_H
#define RUNTIME_OBJCEXPORT_H

#if KONAN_OBJC_INTEROP

#import <objc/runtime.h>
#import <Foundation/NSString.h>

#import "Types.h"
#import "Memory.h"

extern "C" id objc_retain(id self);
extern "C" id objc_retainBlock(id self);
extern "C" void objc_release(id self);

inline static id GetAssociatedObject(ObjHeader* obj) {
    return (id)obj->GetAssociatedObject();
}

// Note: this function shall not be used on shared objects.
inline static void SetAssociatedObject(ObjHeader* obj, id value) {
    obj->SetAssociatedObject((void*)value);
}

inline static id AtomicCompareAndSwapAssociatedObject(ObjHeader* obj, id expectedValue, id newValue) {
    id* location = reinterpret_cast<id*>(obj->GetAssociatedObjectLocation());
    return __sync_val_compare_and_swap(location, expectedValue, newValue);
}

inline static OBJ_GETTER(AllocInstanceWithAssociatedObject, const TypeInfo* typeInfo, id associatedObject) {
  ObjHeader* result = AllocInstance(typeInfo, OBJ_RESULT);
  SetAssociatedObject(result, associatedObject);
  return result;
}

extern "C" id Kotlin_ObjCExport_refToObjC(ObjHeader* obj);
extern "C" OBJ_GETTER(Kotlin_ObjCExport_refFromObjC, id obj);

extern "C" id Kotlin_Interop_CreateNSStringFromKString(KRef str);
extern "C" OBJ_GETTER(Kotlin_Interop_CreateKStringFromNSString, NSString* str);

#endif // KONAN_OBJC_INTEROP

#endif // RUNTIME_OBJCEXPORT_H
