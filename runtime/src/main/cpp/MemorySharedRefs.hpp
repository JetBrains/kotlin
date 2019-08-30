/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MEMORYSHAREDREFS_HPP
#define RUNTIME_MEMORYSHAREDREFS_HPP

#include "Memory.h"

class KRefSharedHolder {
 public:
  void initLocal(ObjHeader* obj);

  void init(ObjHeader* obj);

  ObjHeader* ref() const;

  void dispose() const;

 private:
  ObjHeader* obj_;
  ForeignRefContext context_;

  void ensureRefAccessible() const;
};

class BackRefFromAssociatedObject {
 public:
  void initAndAddRef(ObjHeader* obj);

  void addRef();

  bool tryAddRef();

  void releaseRef();

  ObjHeader* ref() const {
    ensureRefAccessible();
    return obj_;
  }

  inline bool permanent() const {
    return obj_->permanent(); // Safe to query from any thread.
  }

 private:
  ObjHeader* obj_;
  ForeignRefContext context_;
  volatile int refCount;

  void ensureRefAccessible() const;
};

#endif // RUNTIME_MEMORYSHAREDREFS_HPP
