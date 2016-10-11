#include <cassert>
#include "TypeInfo.h"

extern "C" {

int LookupFieldOffset(const TypeInfo* info, FieldNameHash nameSignature) {
  assert(false); // not implemented yet
  return -1;
}

void* LookupMethod(const TypeInfo* info, MethodNameHash nameSignature) {
  assert(false); // not implemented yet
  return nullptr;
}

}
