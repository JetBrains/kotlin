#include <stdlib.h>

#include <cassert>

#include "Memory.h"

void FreeObject(ContainerHeader* header) {
  free(header);
}

ArenaContainer::ArenaContainer(uint32_t size) {
    ArenaContainerHeader* header = reinterpret_cast<ArenaContainerHeader*>(
        calloc(size + sizeof(ArenaContainerHeader), 1));
    header_ = header;
    header->ref_count_ = 1;
    header->current_ = reinterpret_cast<uint8_t*>(header_) + sizeof(ArenaContainerHeader);
    header->end_ = header->current_ + size;
}

void ObjectContainer::Init(const TypeInfo* type_info, uint32_t elements) {
  header_ = reinterpret_cast<ContainerHeader*>(
      calloc(sizeof(ContainerHeader) + sizeof(ObjHeader) +
             type_info->size_ * elements, 1));
  header_->ref_count_ = 1;
  SetMeta(GetPlace(), type_info);
}

ObjHeader* ArenaContainer::PlaceObject(const TypeInfo* type_info) {
  int size = type_info->size_ + sizeof(ObjHeader);
  ObjHeader* result = reinterpret_cast<ObjHeader*>(Place(size));
  if (!result) {
      return nullptr;
  }
  SetMeta(result, type_info);
  return result;
}

ArrayHeader* ArenaContainer::PlaceArray(const TypeInfo* type_info, int count) {
  int size = sizeof(ArrayHeader) + type_info->size_ * count;
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
  return ObjectContainer(type_info).GetPlace();
}

void* AllocArrayInstance(const TypeInfo* type_info, PlacementHint hint, uint32_t elements) {
  return ObjectContainer(type_info, elements).GetPlace();
}

#ifdef __cplusplus
}
#endif
