/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MEMORYSHAREDREFS_HPP
#define RUNTIME_MEMORYSHAREDREFS_HPP

#include <type_traits>

#include "ManuallyScoped.hpp"
#include "Memory.h"
#include "concurrent/Mutex.hpp"

class KRefSharedHolder {
 public:
  void initLocal(ObjHeader* obj);

  void init(ObjHeader* obj);

  ObjHeader* ref() const;

  void dispose();

  OBJ_GETTER0(describe) const;

 private:
  ObjHeader* obj_;
  kotlin::mm::RawSpecialRef* ref_;
};

static_assert(std::is_trivially_destructible_v<KRefSharedHolder>, "KRefSharedHolder destructor is not guaranteed to be called.");

class BackRefFromAssociatedObject {
 public:
  void initForPermanentObject(ObjHeader* obj);

  void initAndAddRef(ObjHeader* obj);

  // Returns true if initialized as permanent.
  bool initWithExternalRCRef(void* ref) noexcept;

  void addRef();

  bool tryAddRef();

  void releaseRef();

  void dealloc();

  ObjHeader* ref() const;

  ObjHeader* refPermanent() const;

  void* externalRCRef(bool permanent) const noexcept;

 private:
  union {
    struct {
      kotlin::mm::RawSpecialRef* ref_;
      kotlin::ManuallyScoped<kotlin::RWSpinLock<kotlin::MutexThreadStateHandling::kIgnore>> deallocMutex_;
    }; // Regular object.
    ObjHeader* permanentObj_; // Permanent object.
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
