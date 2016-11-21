#include "Assert.h"
#include "TypeInfo.h"

extern "C" {

int LookupFieldOffset(const TypeInfo* info, FieldNameHash nameSignature) {
  // TODO: make it binary search?
  for (int i = 0; i < info->fieldsCount_; ++i) {
    if (info->fields_[i].nameSignature_ == nameSignature) {
      return info->fields_[i].fieldOffset_;
    }
  }
  RuntimeAssert(false, "Unknown field");
  return -1;
}

void* LookupOpenMethod(const TypeInfo* info, MethodNameHash nameSignature) {
  // TODO: make it binary search?
  for (int i = 0; i < info->openMethodsCount_; ++i) {
    if (info->openMethods_[i].nameSignature_ == nameSignature) {
      return info->openMethods_[i].methodEntryPoint_;
    }
  }
  RuntimeAssert(false, "Unknown open method");
  return nullptr;
}

}
