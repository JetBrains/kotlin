/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MEMORYSHAREDREFS_HPP
#define RUNTIME_MEMORYSHAREDREFS_HPP

#include <variant>

#include "Memory.h"
#include "ObjCBackRef.hpp"

class BackRefFromAssociatedObject {
 public:
  void initForPermanentObject(ObjHeader* obj);

  void initAndAddRef(ObjHeader* obj);

  void initWithExternalRCRef(kotlin::mm::RawExternalRCRef* ref) noexcept;

  void addRef();

  bool tryAddRef();

  void releaseRef();

  ObjHeader* ref() const;

  kotlin::mm::RawExternalRCRef* externalRCRef() const noexcept;

  bool isPermanent() const noexcept { return std::holds_alternative<PermanentRef>(ref_); }

 private:
  using PermanentRef = KRef;
  using RegularRef = kotlin::mm::ObjCBackRef;
  std::variant<RegularRef, PermanentRef> ref_;
};

#endif // RUNTIME_MEMORYSHAREDREFS_HPP
