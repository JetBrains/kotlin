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

#include "Atomic.h"
#include "Common.h"
#include "Exceptions.h"
#include "Memory.h"
#include "Types.h"

namespace {

struct AtomicReferenceLayout {
  ObjHeader header;
  KRef value_;
  KInt lock_;
  KInt cookie_;
};

inline AtomicReferenceLayout* asAtomicReference(KRef thiz) {
    return reinterpret_cast<AtomicReferenceLayout*>(thiz);
}

}  // namespace

extern "C" {



void Kotlin_AtomicReference_checkIfFrozen(KRef value) {
    if (!kotlin::compiler::freezingEnabled()) {
        return;
    }
    if (value != nullptr && !isPermanentOrFrozen(value)) {
        ThrowInvalidMutabilityException(value);
    }
}

OBJ_GETTER(Kotlin_AtomicReference_compareAndSwap, KRef thiz, KRef expectedValue, KRef newValue) {
    if (isPermanentOrFrozen(thiz)) {
        Kotlin_AtomicReference_checkIfFrozen(newValue);
    }
    // See Kotlin_AtomicReference_get() for explanations, why locking is needed.
    AtomicReferenceLayout* ref = asAtomicReference(thiz);
    RETURN_RESULT_OF(SwapHeapRefLocked, &ref->value_, expectedValue, newValue,
        &ref->lock_, &ref->cookie_);
}

KBoolean Kotlin_AtomicReference_compareAndSet(KRef thiz, KRef expectedValue, KRef newValue) {
    if (isPermanentOrFrozen(thiz)) {
        Kotlin_AtomicReference_checkIfFrozen(newValue);
    }
    // See Kotlin_AtomicReference_get() for explanations, why locking is needed.
    AtomicReferenceLayout* ref = asAtomicReference(thiz);
    ObjHolder holder;
    auto old = SwapHeapRefLocked(&ref->value_, expectedValue, newValue,
        &ref->lock_, &ref->cookie_, holder.slot());
    return old == expectedValue;
}

void Kotlin_AtomicReference_set(KRef thiz, KRef newValue) {
    if (isPermanentOrFrozen(thiz)) {
        Kotlin_AtomicReference_checkIfFrozen(newValue);
    }
    AtomicReferenceLayout* ref = asAtomicReference(thiz);
    SetHeapRefLocked(&ref->value_, newValue, &ref->lock_, &ref->cookie_);
}

OBJ_GETTER(Kotlin_AtomicReference_get, KRef thiz) {
    // Here we must take a lock to prevent race when value, while taken here, is CASed and immediately
    // destroyed by an another thread. AtomicReference no longer holds such an object, so if we got
    // rescheduled unluckily, between the moment value is read from the field and RC is incremented,
    // object may go away.
    AtomicReferenceLayout* ref = asAtomicReference(thiz);
    RETURN_RESULT_OF(ReadHeapRefLocked, &ref->value_, &ref->lock_, &ref->cookie_);
}

}  // extern "C"
