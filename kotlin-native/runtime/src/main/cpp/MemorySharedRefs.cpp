/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MemorySharedRefs.hpp"

#include "Types.h"

extern "C" {
// Returns a string describing object at `address` of type `typeInfo`.
OBJ_GETTER(DescribeObjectForDebugging, KConstNativePtr typeInfo, KConstNativePtr address);
}  // extern "C"

OBJ_GETTER0(KRefSharedHolder::describe) const {
  // Note: retrieving 'type_info()' is supposed to be correct even for unowned object.
  RETURN_RESULT_OF(DescribeObjectForDebugging, obj_->type_info(), obj_);
}

extern "C" {
RUNTIME_NOTHROW void KRefSharedHolder_initLocal(KRefSharedHolder* holder, ObjHeader* obj) {
  holder->initLocal(obj);
}

RUNTIME_NOTHROW void KRefSharedHolder_init(KRefSharedHolder* holder, ObjHeader* obj) {
  holder->init(obj);
}

RUNTIME_NOTHROW void KRefSharedHolder_dispose(KRefSharedHolder* holder) {
    holder->dispose();
}

RUNTIME_NOTHROW ObjHeader* KRefSharedHolder_ref(const KRefSharedHolder* holder) {
  return holder->ref<ErrorPolicy::kTerminate>();
}
} // extern "C"
