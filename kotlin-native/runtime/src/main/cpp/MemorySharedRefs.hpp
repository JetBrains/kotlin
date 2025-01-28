/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MEMORYSHAREDREFS_HPP
#define RUNTIME_MEMORYSHAREDREFS_HPP

#include <type_traits>

#include "ManuallyScoped.hpp"
#include "Memory.h"
#include "ObjCBackRef.hpp"

class BackRefFromAssociatedObject {
 public:
  void initForPermanentObject(ObjHeader* obj);

  void initAndAddRef(ObjHeader* obj);

  // Returns true if initialized as permanent.
  bool initWithExternalRCRef(kotlin::mm::RawExternalRCRef* ref) noexcept;

  void addRef();

  bool tryAddRef();

  void releaseRef();

  void dealloc();

  ObjHeader* ref() const;

  ObjHeader* refPermanent() const;

  kotlin::mm::RawExternalRCRef* externalRCRef(bool permanent) const noexcept;

 private:
  union {
    kotlin::ManuallyScoped<kotlin::mm::ObjCBackRef> ref_; // Regular object.
    ObjHeader* permanentObj_; // Permanent object.
  };
};

static_assert(
        std::is_trivially_destructible_v<BackRefFromAssociatedObject>,
        "BackRefFromAssociatedObject destructor is not guaranteed to be called.");

#endif // RUNTIME_MEMORYSHAREDREFS_HPP
