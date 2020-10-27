/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "Weak.h"

#include "Memory.h"
#include "Types.h"

namespace {

// TODO: an ugly hack with fixed layout.
struct WeakReferenceCounter {
  ObjHeader header;
  KRef referred;
  KInt lock;
  KInt cookie;
};

inline WeakReferenceCounter* asWeakReferenceCounter(ObjHeader* obj) {
  return reinterpret_cast<WeakReferenceCounter*>(obj);
}

#if !KONAN_NO_THREADS

inline void lock(int32_t* address) {
    RuntimeAssert(*address == 0 || *address == 1, "Incorrect lock state");
    while (__sync_val_compare_and_swap(address, 0, 1) == 1);
}

inline void unlock(int32_t* address) {
    int old = __sync_val_compare_and_swap(address, 1, 0);
    RuntimeAssert(old == 1, "Incorrect lock state");
}

#endif

}  // namespace

extern "C" {

OBJ_GETTER(makeWeakReferenceCounter, void*);
OBJ_GETTER(makeObjCWeakReferenceImpl, void*);
OBJ_GETTER(makePermanentWeakReferenceImpl, ObjHeader*);

// See Weak.kt for implementation details.
// Retrieve link on the counter object.
OBJ_GETTER(Konan_getWeakReferenceImpl, ObjHeader* referred) {
    if (referred->permanent()) {
        RETURN_RESULT_OF(makePermanentWeakReferenceImpl, referred);
    }

#if KONAN_OBJC_INTEROP
  if (IsInstance(referred, theObjCObjectWrapperTypeInfo)) {
      RETURN_RESULT_OF(makeObjCWeakReferenceImpl, referred->GetAssociatedObject());
  }
#endif // KONAN_OBJC_INTEROP

  ObjHeader** weakCounterLocation = referred->GetWeakCounterLocation();
  if (*weakCounterLocation == nullptr) {
      ObjHolder counterHolder;
      // Cast unneeded, just to emphasize we store an object reference as void*.
      ObjHeader* counter = makeWeakReferenceCounter(reinterpret_cast<void*>(referred), counterHolder.slot());
      UpdateHeapRefIfNull(weakCounterLocation, counter);
  }
  RETURN_OBJ(*weakCounterLocation);
}

// Materialize a weak reference to either null or the real reference.
OBJ_GETTER(Konan_WeakReferenceCounter_get, ObjHeader* counter) {
  ObjHeader** referredAddress = &asWeakReferenceCounter(counter)->referred;
#if KONAN_NO_THREADS
  RETURN_OBJ(*referredAddress);
#else
  auto* weakCounter = asWeakReferenceCounter(counter);
  RETURN_RESULT_OF(ReadHeapRefLocked, referredAddress,  &weakCounter->lock,  &weakCounter->cookie);
#endif
}

void WeakReferenceCounterClear(ObjHeader* counter) {
  ObjHeader** referredAddress = &asWeakReferenceCounter(counter)->referred;
  // Note, that we don't do UpdateRef here, as reference is weak.
#if KONAN_NO_THREADS
  *referredAddress = nullptr;
#else
  int32_t* lockAddress = &asWeakReferenceCounter(counter)->lock;
  // Spinlock.
  lock(lockAddress);
  *referredAddress = nullptr;
  unlock(lockAddress);
#endif
}

}  // extern "C"
