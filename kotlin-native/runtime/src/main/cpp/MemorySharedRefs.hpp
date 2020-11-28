/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MEMORYSHAREDREFS_HPP
#define RUNTIME_MEMORYSHAREDREFS_HPP

#include <type_traits>

#include "CppSupport.hpp"
#include "Memory.h"

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

  void dispose() const;

  OBJ_GETTER0(describe) const;

 private:
  ObjHeader* obj_;
  ForeignRefContext context_;
};

static_assert(
        kotlin::std_support::is_trivially_destructible_v<KRefSharedHolder>, "KRefSharedHolder destructor is not guaranteed to be called.");

class BackRefFromAssociatedObject {
 public:
  void initAndAddRef(ObjHeader* obj);

  // Error if refCount is zero and it's called from the wrong worker with non-frozen obj_.
  template <ErrorPolicy errorPolicy>
  void addRef();

  // Error if called from the wrong worker with non-frozen obj_.
  template <ErrorPolicy errorPolicy>
  bool tryAddRef();

  void releaseRef();

  void detach();

  // Error if called from the wrong worker with non-frozen obj_.
  template <ErrorPolicy errorPolicy>
  ObjHeader* ref() const;

 private:
  ObjHeader* obj_; // May be null before [initAndAddRef] or after [detach].
  ForeignRefContext context_;
  volatile int refCount;
};

static_assert(
        kotlin::std_support::is_trivially_destructible_v<BackRefFromAssociatedObject>,
        "BackRefFromAssociatedObject destructor is not guaranteed to be called.");

#endif // RUNTIME_MEMORYSHAREDREFS_HPP
