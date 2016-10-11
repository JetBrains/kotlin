#ifndef RUNTIME_TYPEINFO_H
#define RUNTIME_TYPEINFO_H

#include <cstdint>

#include "Names.h"

// An element of sorted by hash in-place array representing methods.
struct MethodTableRecord {
    MethodNameHash nameSignature_;
    void* methodEntryPoint_;
};

// An element of sorted by hash in-place array representing field offsets.
struct FieldTableRecord {
    FieldNameHash nameSignature_;
    int fieldOffset_;
};

// This struct represents runtime type information and by itself is compile time
// constant.
struct TypeInfo {
    ClassNameHash name_;
    int size_;
    const TypeInfo* superType_;
    const int* objOffsets_;
    int objOffsetsCount_;
    TypeInfo* const* implementedInterfaces_;
    int implementedInterfacesCount_;
    void* const* vtable_; // TODO: place vtable at the end of TypeInfo to eliminate the indirection
    const MethodTableRecord* methods_;
    int methodsCount_;
    const FieldTableRecord* fields_;
    int fieldsCount_;
};

#ifdef __cplusplus
extern "C" {
#endif
// Find offset of given hash in table.
int LookupFieldOffset(const TypeInfo* type_info, LocalHash hash);

// Find method by its hash.
void* LookupMethod(const TypeInfo* info, MethodNameHash nameSignature);

#ifdef __cplusplus
} // extern "C"
#endif

#endif // RUNTIME_TYPEINFO_H
