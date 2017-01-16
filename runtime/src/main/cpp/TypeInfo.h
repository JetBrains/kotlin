#ifndef RUNTIME_TYPEINFO_H
#define RUNTIME_TYPEINFO_H

#include <cstdint>

#include "Common.h"
#include "Names.h"

// An element of sorted by hash in-place array representing methods.
// For systems where introspection is not needed - only open methods are in
// this table.
struct MethodTableRecord {
    MethodNameHash nameSignature_;
    void* methodEntryPoint_;
};

// An element of sorted by hash in-place array representing field offsets.
struct FieldTableRecord {
    FieldNameHash nameSignature_;
    int fieldOffset_;
};

// This struct represents runtime type information and by itself is the compile time
// constant.
struct TypeInfo {
    ClassNameHash name_;
    // Negative value marks array class/string, and it is negated element size.
    int32_t instanceSize_;
    // Must be pointer to Any for array classes, and null for Any.
    const TypeInfo* superType_;
    // All object references inside this object.
    const int32_t* objOffsets_;
    int32_t objOffsetsCount_;
    const TypeInfo* const* implementedInterfaces_;
    int32_t implementedInterfacesCount_;
    // Null for abstract classes and interfaces.
    // TODO: place vtable at the end of TypeInfo to eliminate the indirection.
    void* const* vtable_;
    // Null for abstract classes and interfaces.
    const MethodTableRecord* openMethods_;
    uint32_t openMethodsCount_;
    const FieldTableRecord* fields_;
    // Is negative to mark an interface.
    int32_t fieldsCount_;
};

#ifdef __cplusplus
extern "C" {
#endif
// Find offset of given hash in table.
// Note, that we use attribute const, which assumes function doesn't
// dereference global memory, while this function does. However, it seems
// to be safe, as actual result of this computation depends only on 'type_info'
// and 'hash' numeric values and doesn't really depends on global memory state
// (as TypeInfo is compile time constant and type info pointers are stable).
int LookupFieldOffset(const TypeInfo* type_info, FieldNameHash hash) RUNTIME_CONST;

// Find open method by its hash. Other methods are resolved in compile-time.
// See comment in LookupFieldOffset().
void* LookupOpenMethod(const TypeInfo* info, MethodNameHash nameSignature) RUNTIME_CONST;

#ifdef __cplusplus
} // extern "C"
#endif

#endif // RUNTIME_TYPEINFO_H
