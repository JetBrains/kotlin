#include <stdlib.h>

#include "Assert.h"
#include "Exceptions.h"
#include "Memory.h"

void FreeObject(ContainerHeader* header) {
  header->ref_count_ = CONTAINER_TAG_INVALID;
  free(header);
}

ArenaContainer::ArenaContainer(uint32_t size) {
    ArenaContainerHeader* header = reinterpret_cast<ArenaContainerHeader*>(
        calloc(size + sizeof(ArenaContainerHeader), 1));
    header_ = header;
    header->ref_count_ = CONTAINER_TAG_INCREMENT;
    header->current_ =
        reinterpret_cast<uint8_t*>(header_) + sizeof(ArenaContainerHeader);
    header->end_ = header->current_ + size;
}

void ObjectContainer::Init(const TypeInfo* type_info) {
  RuntimeAssert(type_info->instanceSize_ >= 0, "Must be an object");
   uint32_t alloc_size =
      sizeof(ContainerHeader) + sizeof(ObjHeader) + type_info->instanceSize_;
  header_ = reinterpret_cast<ContainerHeader*>(calloc(alloc_size, 1));
  if (header_) {
    header_->ref_count_ = CONTAINER_TAG_INCREMENT;
    SetMeta(GetPlace(), type_info);
  }
}

void ArrayContainer::Init(const TypeInfo* type_info, uint32_t elements) {
  RuntimeAssert(type_info->instanceSize_ < 0, "Must be an array");
  uint32_t alloc_size =
      sizeof(ContainerHeader) + sizeof(ArrayHeader) -
      type_info->instanceSize_ * elements;
  header_ = reinterpret_cast<ContainerHeader*>(calloc(alloc_size, 1));
  RuntimeAssert(header_ != nullptr, "Cannot alloc memory");
  if (header_) {
    header_->ref_count_ = CONTAINER_TAG_INCREMENT;
    GetPlace()->count_ = elements;
    SetMeta(GetPlace(), type_info);
  }
}

ObjHeader* ArenaContainer::PlaceObject(const TypeInfo* type_info) {
  RuntimeAssert(type_info->instanceSize_ >= 0, "must be an object");
  uint32_t size = type_info->instanceSize_ + sizeof(ObjHeader);
  ObjHeader* result = reinterpret_cast<ObjHeader*>(Place(size));
  if (!result) {
      return nullptr;
  }
  SetMeta(result, type_info);
  return result;
}

ArrayHeader* ArenaContainer::PlaceArray(const TypeInfo* type_info, int count) {
  RuntimeAssert(type_info->instanceSize_ < 0, "must be an array");
  uint32_t size = sizeof(ArrayHeader) - type_info->instanceSize_ * count;
  ArrayHeader* result = reinterpret_cast<ArrayHeader*>(Place(size));
  if (!result) {
    return nullptr;
  }
  SetMeta(result, type_info);
  result->count_ = count;
  return result;
}

#ifdef __cplusplus
extern "C" {
#endif

void InitMemory() {
  // TODO: initialize heap here.
}

// Now we ignore all placement hints and always allocate heap space for new object.
void* AllocInstance(const TypeInfo* type_info, PlacementHint hint) {
  RuntimeAssert(type_info->instanceSize_ >= 0, "must be an object");
  return ObjectContainer(type_info).GetPlace();
}

void* AllocArrayInstance(
    const TypeInfo* type_info, PlacementHint hint, uint32_t elements) {
  RuntimeAssert(type_info->instanceSize_ < 0, "must be an array");
  return ArrayContainer(type_info, elements).GetPlace();
}

#ifdef __cplusplus
}
#endif
