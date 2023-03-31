/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MEMORYSHAREDREFS_HPP
#define RUNTIME_MEMORYSHAREDREFS_HPP

#include <type_traits>

#include "ManuallyScoped.hpp"
#include "Memory.h"
#include "Mutex.hpp"

// TODO: Generalize for uses outside this file.
enum class ErrorPolicy {
  kIgnore,  // Ignore any errors. (i.e. unsafe mode)
  kDefaultValue,  // Return the default value from the function when an error happens.
  kThrow,  // Throw a Kotlin exception when an error happens. The exact exception is chosen by the callee.
  kTerminate,  // Terminate immediately when an error happens.
};

class KRefSharedHolder {
 public:
  void initLocal(ObjHeader* obj);

  void init(ObjHeader* obj);

  // Error if called from the wrong worker with non-frozen obj_.
  template <ErrorPolicy errorPolicy>
  ObjHeader* ref() const;

  void dispose();

  OBJ_GETTER0(describe) const;

 private:
  ObjHeader* obj_;
  union {
    ForeignRefContext context_; // Legacy MM.
    kotlin::mm::RawSpecialRef* ref_; // New MM.
  };
};

static_assert(std::is_trivially_destructible_v<KRefSharedHolder>, "KRefSharedHolder destructor is not guaranteed to be called.");

class BackRefFromAssociatedObject {
 public:
  void initForPermanentObject(ObjHeader* obj);

  void initAndAddRef(ObjHeader* obj);

  // Error if refCount is zero and it's called from the wrong worker with non-frozen obj_.
  template <ErrorPolicy errorPolicy>
  void addRef();

  // Error if called from the wrong worker with non-frozen obj_.
  template <ErrorPolicy errorPolicy>
  bool tryAddRef();

  void releaseRef();

  // This does nothing with the new MM.
  void detach();

  // This does nothing with legacy MM.
  void dealloc();

  // Error if called from the wrong worker with non-frozen obj_.
  template <ErrorPolicy errorPolicy>
  ObjHeader* ref() const;

  ObjHeader* refPermanent() const;

 private:
  union {
    struct {
      ObjHeader* obj_; // May be null before [initAndAddRef] or after [detach].
      ForeignRefContext context_;
      volatile int refCount;
    }; // Legacy MM
    struct {
      kotlin::mm::RawSpecialRef* ref_;
      kotlin::ManuallyScoped<kotlin::RWSpinLock<kotlin::MutexThreadStateHandling::kIgnore>> deallocMutex_;
    }; // New MM. Regular object.
    ObjHeader* permanentObj_; // New MM. Permanent object.
  };
};

static_assert(
        std::is_trivially_destructible_v<BackRefFromAssociatedObject>,
        "BackRefFromAssociatedObject destructor is not guaranteed to be called.");

extern "C" {
RUNTIME_NOTHROW void KRefSharedHolder_initLocal(KRefSharedHolder* holder, ObjHeader* obj);
RUNTIME_NOTHROW void KRefSharedHolder_init(KRefSharedHolder* holder, ObjHeader* obj);
RUNTIME_NOTHROW void KRefSharedHolder_dispose(KRefSharedHolder* holder);
RUNTIME_NOTHROW ObjHeader* KRefSharedHolder_ref(const KRefSharedHolder* holder);
} // extern "C"

#endif // RUNTIME_MEMORYSHAREDREFS_HPP
