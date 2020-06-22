/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "WorkerBoundReference.h"

#include "Alloc.h"
#include "Memory.h"
#include "MemorySharedRefs.hpp"

namespace {

struct WorkerBoundReference {
  ObjHeader header;
  KRefSharedHolder* holder;
};

WorkerBoundReference* asWorkerBoundReference(KRef thiz) {
  return reinterpret_cast<WorkerBoundReference*>(thiz);
}

}  // namespace

RUNTIME_NOTHROW void DisposeWorkerBoundReference(KRef thiz) {
  // DisposeSharedRef is only called when all references to thiz are gone.
  auto* holder = asWorkerBoundReference(thiz)->holder;
  holder->dispose();
  konanDestructInstance(holder);
}

extern "C" {

KNativePtr Kotlin_WorkerBoundReference_create(KRef value) {
  auto* holder = konanConstructInstance<KRefSharedHolder>();
  holder->init(value);
  return holder;
}

OBJ_GETTER(Kotlin_WorkerBoundReference_deref, KNativePtr holder) {
  RETURN_OBJ(reinterpret_cast<KRefSharedHolder*>(holder)->ref<ErrorPolicy::kDefaultValue>());
}

OBJ_GETTER(Kotlin_WorkerBoundReference_describe, KNativePtr holder) {
  RETURN_RESULT_OF0(reinterpret_cast<KRefSharedHolder*>(holder)->describe);
}

}
