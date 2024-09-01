/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#if KONAN_OBJC_INTEROP

#include <objc/runtime.h>

#include "TypeInfo.h"

struct ObjCToKotlinMethodAdapter {
  const char* selector;
  const char* encoding;
  IMP imp;
};

struct KotlinToObjCMethodAdapter {
  const char* selector;
  ClassId interfaceId;
  int itableSize;
  int itableIndex;
  int vtableIndex;
  const void* kotlinImpl;
};

struct ObjCTypeAdapter {
  const TypeInfo* kotlinTypeInfo;

  const void * const * kotlinVtable;
  int kotlinVtableSize;

  const InterfaceTableRecord* kotlinItable;
  int kotlinItableSize;

  const char* objCName;

  const ObjCToKotlinMethodAdapter* directAdapters;
  int directAdapterNum;

  const ObjCToKotlinMethodAdapter* classAdapters;
  int classAdapterNum;

  const ObjCToKotlinMethodAdapter* virtualAdapters;
  int virtualAdapterNum;

  const KotlinToObjCMethodAdapter* reverseAdapters;
  int reverseAdapterNum;
};

using convertReferenceToRetainedObjC = id (*)(ObjHeader* obj);

struct TypeInfoObjCExportAddition {
  /*convertReferenceToRetainedObjC*/ void* convertToRetained;
  Class objCClass;
  const ObjCTypeAdapter* typeAdapter;
};

#endif
