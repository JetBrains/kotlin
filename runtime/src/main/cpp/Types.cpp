#include "Types.h"
#include "Exceptions.h"

#ifdef __cplusplus
extern "C" {
#endif

KBoolean IsInstance(const ObjHeader* obj, const TypeInfo* type_info) {
  // We assume null check is handled by caller.
  RuntimeAssert(obj != nullptr, "must not be null");
  const TypeInfo* obj_type_info = obj->type_info();
  // If it is an interface - check in list of implemented interfaces.
  if (type_info->fieldsCount_ < 0) {
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

KBoolean IsArray(KConstRef obj) {
  RuntimeAssert(obj != nullptr, "Object must not be null");
  return obj->type_info()->instanceSize_ < 0;
}

void CheckInstance(const ObjHeader* obj, const TypeInfo* type_info) {
  if (IsInstance(obj, type_info)) {
    return;
  }
  ThrowClassCastException();
}

static struct InitNode* initHeadNode = nullptr;
static struct InitNode* initTailNode = nullptr;

void AppendToInitializersTail(struct InitNode *next) {
  if (initHeadNode == nullptr) {
    initHeadNode = next;
  } else {
    initTailNode->next = next;
  }
  initTailNode = next;
}

void InitGlobalVariables() {
    struct InitNode *currNode = initHeadNode;
    while(currNode != nullptr) {
        currNode->init();
        currNode = currNode->next;
    }
}
#ifdef __cplusplus
}
#endif
