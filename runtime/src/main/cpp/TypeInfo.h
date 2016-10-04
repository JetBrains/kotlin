#ifndef RUNTIME_TYPEINFO_H
#define RUNTIME_TYPEINFO_H

#include <cstdint>

// All names in system are stored as hashes (or maybe, for debug builds,
// as pointers to uniqued C strings containing names?).
typedef int64_t NameHash;

// An element of sorted by hash in-place array representing methods.
struct MethodTableRecord {
    NameHash nameSignature;
    void* methodEntryPoint;
};

// An element of sorted by hash in-place array representing field offsets.
struct FieldTableRecord {
    NameHash nameSignature;
    int fieldOffset;
};

// This struct represents runtime type information and by itself is compile time
// constant.
struct TypeInfo {
    NameHash name;
    int size;
    const TypeInfo* superType;
    const int* objOffsets;
    int objOffsetsCount;
    TypeInfo* const* implementedInterfaces;
    int implementedInterfacesCount;
    void* const* vtable; // TODO: place vtable at the end of TypeInfo to eliminate the indirection
    const MethodTableRecord* methods;
    int methodsCount;
    const FieldTableRecord* fields;
    int fieldsCount;
};


#endif //RUNTIME_TYPEINFO_H
