/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

#import "Types.h"
#import "Memory.h"
#include "Natives.h"
#include "ObjCInterop.h"

#if KONAN_OBJC_INTEROP

#import <Foundation/NSObject.h>
#import <Foundation/NSValue.h>
#import <Foundation/NSString.h>
#import <Foundation/NSMethodSignature.h>
#import <Foundation/NSError.h>
#import <Foundation/NSException.h>
#import <Foundation/NSDecimalNumber.h>
#import <Foundation/NSDictionary.h>
#import <objc/message.h>
#import <objc/runtime.h>
#import <objc/objc-exception.h>
#import <dispatch/dispatch.h>

#import "ObjCExport.h"
#import "ObjCExportInit.h"
#import "ObjCExportPrivate.h"
#import "ObjCMMAPI.h"
#import "Runtime.h"
#import "Mutex.hpp"
#import "Exceptions.h"

struct ObjCToKotlinMethodAdapter {
  const char* selector;
  const char* encoding;
  IMP imp;
};

struct KotlinToObjCMethodAdapter {
  const char* selector;
  MethodNameHash nameSignature;
  ClassId interfaceId;
  int itableIndex;
  int vtableIndex;
  const void* kotlinImpl;
};

struct ObjCTypeAdapter {
  const TypeInfo* kotlinTypeInfo;

  const void * const * kotlinVtable;
  int kotlinVtableSize;

  const MethodTableRecord* kotlinMethodTable;
  int kotlinMethodTableSize;

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

typedef id (*convertReferenceToObjC)(ObjHeader* obj);
typedef OBJ_GETTER((*convertReferenceFromObjC), id obj);

struct TypeInfoObjCExportAddition {
  /*convertReferenceToObjC*/ void* convert;
  Class objCClass;
  const ObjCTypeAdapter* typeAdapter;
};

struct WritableTypeInfo {
  TypeInfoObjCExportAddition objCExport;
};


static char associatedTypeInfoKey;

extern "C" const TypeInfo* Kotlin_ObjCExport_getAssociatedTypeInfo(Class clazz) {
  return (const TypeInfo*)[objc_getAssociatedObject(clazz, &associatedTypeInfoKey) pointerValue];
}

static void setAssociatedTypeInfo(Class clazz, const TypeInfo* typeInfo) {
  objc_setAssociatedObject(clazz, &associatedTypeInfoKey, [NSValue valueWithPointer:typeInfo], OBJC_ASSOCIATION_RETAIN);
}

extern "C" id Kotlin_ObjCExport_GetAssociatedObject(ObjHeader* obj) {
  return GetAssociatedObject(obj);
}

extern "C" OBJ_GETTER(Kotlin_ObjCExport_AllocInstanceWithAssociatedObject,
                      const TypeInfo* typeInfo, id associatedObject) RUNTIME_NOTHROW;

extern "C" OBJ_GETTER(Kotlin_ObjCExport_AllocInstanceWithAssociatedObject,
                            const TypeInfo* typeInfo, id associatedObject) {
  RETURN_RESULT_OF(AllocInstanceWithAssociatedObject, typeInfo, associatedObject);
}

static Class getOrCreateClass(const TypeInfo* typeInfo);
static void initializeClass(Class clazz);

extern "C" id objc_retainAutoreleaseReturnValue(id self);

extern "C" ALWAYS_INLINE void Kotlin_ObjCExport_releaseAssociatedObject(void* associatedObject) {
  if (associatedObject != nullptr) {
    auto msgSend = reinterpret_cast<void (*)(void* self, SEL cmd)>(&objc_msgSend);
    msgSend(associatedObject, Kotlin_ObjCExport_releaseAsAssociatedObjectSelector);
  }
}

extern "C" id Kotlin_ObjCExport_convertUnit(ObjHeader* unitInstance) {
  static dispatch_once_t onceToken;
  static id instance = nullptr;
  dispatch_once(&onceToken, ^{
    Class unitClass = getOrCreateClass(unitInstance->type_info());
    instance = [[unitClass createWrapper:unitInstance] retain];
  });
  return instance;
}

extern "C" id Kotlin_ObjCExport_CreateNSStringFromKString(ObjHeader* str) {
  KChar* utf16Chars = CharArrayAddressOfElementAt(str->array(), 0);
  auto numBytes = str->array()->count_ * sizeof(KChar);

  if (str->permanent()) {
    return [[[NSString alloc] initWithBytesNoCopy:utf16Chars
        length:numBytes
        encoding:NSUTF16LittleEndianStringEncoding
        freeWhenDone:NO] autorelease];
  } else {
    // TODO: consider making NSString subclass to avoid copying here.
    NSString* candidate = [[NSString alloc] initWithBytes:utf16Chars
      length:numBytes
      encoding:NSUTF16LittleEndianStringEncoding];

    if (!isShareable(str)) {
      SetAssociatedObject(str, candidate);
    } else {
      id old = AtomicCompareAndSwapAssociatedObject(str, nullptr, candidate);
      if (old != nullptr) {
        objc_release(candidate);
        return objc_retainAutoreleaseReturnValue(old);
      }
    }

    return objc_retainAutoreleaseReturnValue(candidate);
  }
}
static const ObjCTypeAdapter* findAdapterByName(
      const char* name,
      const ObjCTypeAdapter** sortedAdapters,
      int adapterNum
) {

  int left = 0, right = adapterNum - 1;

  while (right >= left) {
    int mid = (left + right) / 2;
    int cmp = strcmp(name, sortedAdapters[mid]->objCName);
    if (cmp < 0) {
      right = mid - 1;
    } else if (cmp > 0) {
      left = mid + 1;
    } else {
      return sortedAdapters[mid];
    }
  }

  return nullptr;
}

__attribute__((weak)) const ObjCTypeAdapter** Kotlin_ObjCExport_sortedClassAdapters = nullptr;
__attribute__((weak)) int Kotlin_ObjCExport_sortedClassAdaptersNum = 0;

__attribute__((weak)) const ObjCTypeAdapter** Kotlin_ObjCExport_sortedProtocolAdapters = nullptr;
__attribute__((weak)) int Kotlin_ObjCExport_sortedProtocolAdaptersNum = 0;

__attribute__((weak)) bool Kotlin_ObjCExport_initTypeAdapters = false;

static const ObjCTypeAdapter* findClassAdapter(Class clazz) {
  return findAdapterByName(class_getName(clazz),
        Kotlin_ObjCExport_sortedClassAdapters,
        Kotlin_ObjCExport_sortedClassAdaptersNum
  );
}

static const ObjCTypeAdapter* findProtocolAdapter(Protocol* prot) {
  return findAdapterByName(protocol_getName(prot),
        Kotlin_ObjCExport_sortedProtocolAdapters,
        Kotlin_ObjCExport_sortedProtocolAdaptersNum
  );
}

static const ObjCTypeAdapter* getTypeAdapter(const TypeInfo* typeInfo) {
  return typeInfo->writableInfo_->objCExport.typeAdapter;
}

static void addProtocolForAdapter(Class clazz, const ObjCTypeAdapter* protocolAdapter) {
  Protocol* protocol = objc_getProtocol(protocolAdapter->objCName);
  if (protocol != nullptr) {
    class_addProtocol(clazz, protocol);
    class_addProtocol(object_getClass(clazz), protocol);
  } else {
    // TODO: construct the protocol in compiler instead, because this case can't be handled easily.
  }
}

static void addProtocolForInterface(Class clazz, const TypeInfo* interfaceInfo) {
  const ObjCTypeAdapter* protocolAdapter = getTypeAdapter(interfaceInfo);
  if (protocolAdapter != nullptr) {
    addProtocolForAdapter(clazz, protocolAdapter);
  }
}

extern "C" const TypeInfo* Kotlin_ObjCInterop_getTypeInfoForClass(Class clazz) {
  const TypeInfo* candidate = Kotlin_ObjCExport_getAssociatedTypeInfo(clazz);

  if (candidate != nullptr && (candidate->flags_ & TF_OBJC_DYNAMIC) == 0) {
    return candidate;
  } else {
    return nullptr;
  }
}

extern "C" const TypeInfo* Kotlin_ObjCInterop_getTypeInfoForProtocol(Protocol* protocol) {
  const ObjCTypeAdapter* typeAdapter = findProtocolAdapter(protocol);

  return (typeAdapter != nullptr) ? typeAdapter->kotlinTypeInfo : nullptr;
}

static const TypeInfo* getOrCreateTypeInfo(Class clazz);

extern "C" void Kotlin_ObjCExport_initializeClass(Class clazz) {
  const ObjCTypeAdapter* typeAdapter = findClassAdapter(clazz);
  if (typeAdapter == nullptr) {
    getOrCreateTypeInfo(clazz);
    return;
  }

  const TypeInfo* typeInfo = typeAdapter->kotlinTypeInfo;
  bool isClassForPackage = typeInfo == nullptr;
  if (!isClassForPackage) {
    setAssociatedTypeInfo(clazz, typeInfo);
  }

  for (int i = 0; i < typeAdapter->directAdapterNum; ++i) {
    const ObjCToKotlinMethodAdapter* adapter = typeAdapter->directAdapters + i;
    SEL selector = sel_registerName(adapter->selector);
    BOOL added = class_addMethod(clazz, selector, adapter->imp, adapter->encoding);
    RuntimeAssert(added, "Unexpected selector clash");
  }

  for (int i = 0; i < typeAdapter->classAdapterNum; ++i) {
    const ObjCToKotlinMethodAdapter* adapter = typeAdapter->classAdapters + i;
    SEL selector = sel_registerName(adapter->selector);
    BOOL added = class_addMethod(object_getClass(clazz), selector, adapter->imp, adapter->encoding);
    RuntimeAssert(added, "Unexpected selector clash");
  }

  if (isClassForPackage) return;

  for (int i = 0; i < typeInfo->implementedInterfacesCount_; ++i) {
    addProtocolForInterface(clazz, typeInfo->implementedInterfaces_[i]);
  }

}

extern "C" ALWAYS_INLINE OBJ_GETTER(Kotlin_ObjCExport_convertUnmappedObjCObject, id obj) {
  const TypeInfo* typeInfo = getOrCreateTypeInfo(object_getClass(obj));
  RETURN_RESULT_OF(AllocInstanceWithAssociatedObject, typeInfo, objc_retain(obj));
}

// Initialized by [ObjCExportClasses.mm].
extern "C" SEL Kotlin_ObjCExport_toKotlinSelector = nullptr;
extern "C" SEL Kotlin_ObjCExport_releaseAsAssociatedObjectSelector = nullptr;

static OBJ_GETTER(blockToKotlinImp, id self, SEL cmd);
static OBJ_GETTER(boxedBooleanToKotlinImp, NSNumber* self, SEL cmd);

static OBJ_GETTER(SwiftObject_toKotlinImp, id self, SEL cmd);
static void SwiftObject_releaseAsAssociatedObjectImp(id self, SEL cmd);

static void initTypeAdaptersFrom(const ObjCTypeAdapter** adapters, int count) {
  for (int index = 0; index < count; ++index) {
    const ObjCTypeAdapter* adapter = adapters[index];
    const TypeInfo* typeInfo = adapter->kotlinTypeInfo;
    if (typeInfo != nullptr) {
      typeInfo->writableInfo_->objCExport.typeAdapter = adapter;
    }
  }
}

static void initTypeAdapters() {
  if (!Kotlin_ObjCExport_initTypeAdapters) return;

  initTypeAdaptersFrom(Kotlin_ObjCExport_sortedClassAdapters, Kotlin_ObjCExport_sortedClassAdaptersNum);
  initTypeAdaptersFrom(Kotlin_ObjCExport_sortedProtocolAdapters, Kotlin_ObjCExport_sortedProtocolAdaptersNum);
}

static void Kotlin_ObjCExport_initializeImpl() {
  RuntimeCheck(Kotlin_ObjCExport_toKotlinSelector != nullptr, "unexpected initialization order");
  RuntimeCheck(Kotlin_ObjCExport_releaseAsAssociatedObjectSelector != nullptr, "unexpected initialization order");

  initTypeAdapters();

  SEL toKotlinSelector = Kotlin_ObjCExport_toKotlinSelector;
  Method toKotlinMethod = class_getClassMethod([NSObject class], toKotlinSelector);
  RuntimeAssert(toKotlinMethod != nullptr, "");
  const char* toKotlinTypeEncoding = method_getTypeEncoding(toKotlinMethod);

  SEL releaseAsAssociatedObjectSelector = Kotlin_ObjCExport_releaseAsAssociatedObjectSelector;
  Method releaseAsAssociatedObjectMethod = class_getClassMethod([NSObject class], releaseAsAssociatedObjectSelector);
  RuntimeAssert(releaseAsAssociatedObjectMethod != nullptr, "");
  const char* releaseAsAssociatedObjectTypeEncoding = method_getTypeEncoding(releaseAsAssociatedObjectMethod);

  Class nsBlockClass = objc_getClass("NSBlock");
  RuntimeAssert(nsBlockClass != nullptr, "NSBlock class not found");

  // Note: can't add it with category, because it would be considered as private API usage.
  BOOL added = class_addMethod(nsBlockClass, toKotlinSelector, (IMP)blockToKotlinImp, toKotlinTypeEncoding);
  RuntimeAssert(added, "Unable to add 'toKotlin:' method to NSBlock class");

  // Note: __NSCFBoolean is not visible to linker, so this case can't be handled with a category too.
  Class booleanClass = objc_getClass("__NSCFBoolean");
  RuntimeAssert(booleanClass != nullptr, "__NSCFBoolean class not found");

  added = class_addMethod(booleanClass, toKotlinSelector, (IMP)boxedBooleanToKotlinImp, toKotlinTypeEncoding);
  RuntimeAssert(added, "Unable to add 'toKotlin:' method to __NSCFBoolean class");

  for (const char* swiftRootClassName : { "SwiftObject", "_TtCs12_SwiftObject" }) {
    Class swiftRootClass = objc_getClass(swiftRootClassName);
    if (swiftRootClass != nullptr) {
      added = class_addMethod(swiftRootClass, toKotlinSelector, (IMP)SwiftObject_toKotlinImp, toKotlinTypeEncoding);
      RuntimeAssert(added, "Unable to add 'toKotlin:' method to SwiftObject class");

      added = class_addMethod(
        swiftRootClass, releaseAsAssociatedObjectSelector,
        (IMP)SwiftObject_releaseAsAssociatedObjectImp, releaseAsAssociatedObjectTypeEncoding
      );
      RuntimeAssert(added, "Unable to add 'releaseAsAssociatedObject' method to SwiftObject class");
    }
  }
}

// Initializes ObjCExport for current process (if not initialized yet).
// Generally this is equal to some "binary patching" (which is usually done at link time
// but postponed until runtime here due to various reasons):
// adds methods to Objective-C classes, initializes static memory with "constant" values etc.
extern "C" void Kotlin_ObjCExport_initialize() {
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    Kotlin_ObjCExport_initializeImpl();
  });
}

static OBJ_GETTER(SwiftObject_toKotlinImp, id self, SEL cmd) {
  RETURN_RESULT_OF(Kotlin_ObjCExport_convertUnmappedObjCObject, self);
}

static void SwiftObject_releaseAsAssociatedObjectImp(id self, SEL cmd) {
  objc_release(self);
}


extern "C" OBJ_GETTER(Kotlin_boxBoolean, KBoolean value);

static OBJ_GETTER(boxedBooleanToKotlinImp, NSNumber* self, SEL cmd) {
  RETURN_RESULT_OF(Kotlin_boxBoolean, self.boolValue);
}

struct Block_descriptor_1;

// Based on https://clang.llvm.org/docs/Block-ABI-Apple.html and libclosure source.
struct Block_literal_1 {
    void *isa; // initialized to &_NSConcreteStackBlock or &_NSConcreteGlobalBlock
    int flags;
    int reserved;
    void (*invoke)(void *, ...);
    struct Block_descriptor_1  *descriptor; // IFF (1<<25)
    // Or:
    // struct Block_descriptor_1_without_helpers* descriptor // if hasn't (1<<25).

    // imported variables
};

struct Block_literal_1 exportBlockLiteral;

struct Block_descriptor_1 {
    unsigned long int reserved;         // NULL
    unsigned long int size;             // sizeof(struct Block_literal_1)

    // optional helper functions
    void (*copy_helper)(void *dst, void *src);
    void (*dispose_helper)(void *src);
    // required ABI.2010.3.16
    const char *signature;                         // IFF (1<<30)
    const void* layout;                            // IFF (1<<31)
};

struct Block_descriptor_1_without_helpers {
    unsigned long int reserved;         // NULL
    unsigned long int size;             // sizeof(struct Block_literal_1)

    // required ABI.2010.3.16
    const char *signature;                         // IFF (1<<30)
    const void* layout;                            // IFF (1<<31)
};

static const char* getBlockEncoding(id block) {
  Block_literal_1* literal = reinterpret_cast<Block_literal_1*>(block);

  int flags = literal->flags;
  RuntimeAssert((flags & (1 << 30)) != 0, "block has no signature stored");
  return (flags & (1 << 25)) != 0 ?
      literal->descriptor->signature :
      reinterpret_cast<struct Block_descriptor_1_without_helpers*>(literal->descriptor)->signature;
}

// Note: defined by compiler.
extern "C" convertReferenceFromObjC* Kotlin_ObjCExport_blockToFunctionConverters;
extern "C" int Kotlin_ObjCExport_blockToFunctionConverters_size;

static OBJ_GETTER(blockToKotlinImp, id block, SEL cmd) {
  const char* encoding = getBlockEncoding(block);

  // TODO: optimize:
  NSMethodSignature *signature = [NSMethodSignature signatureWithObjCTypes:encoding];
  int parameterCount = signature.numberOfArguments - 1; // 1 for the block itself.

  for (int i = 1; i <= parameterCount; ++i) {
    const char* argEncoding = [signature getArgumentTypeAtIndex:i];
    if (argEncoding[0] != '@') {
      [NSException raise:NSGenericException
            format:@"Blocks with non-reference-typed arguments aren't supported (%s)", argEncoding];
    }
  }

  const char* returnTypeEncoding = signature.methodReturnType;
  if (returnTypeEncoding[0] != '@') {
    [NSException raise:NSGenericException
          format:@"Blocks with non-reference-typed return value aren't supported (%s)", returnTypeEncoding];
  }

  auto converter = parameterCount < Kotlin_ObjCExport_blockToFunctionConverters_size
          ? Kotlin_ObjCExport_blockToFunctionConverters[parameterCount]
          : nullptr;

  if (converter != nullptr) {
    RETURN_RESULT_OF(converter, block);
  } else {
    // There is no function class for this arity, so resulting object will not be cast to FunctionN class,
    // and it is enough to convert block to arbitrary object conforming Function.
    RETURN_RESULT_OF(AllocInstanceWithAssociatedObject, theOpaqueFunctionTypeInfo, objc_retainBlock(block));
  }
}

static id Kotlin_ObjCExport_refToObjC_slowpath(ObjHeader* obj);

template <bool retainAutorelease>
static ALWAYS_INLINE id Kotlin_ObjCExport_refToObjCImpl(ObjHeader* obj) {
  if (obj == nullptr) return nullptr;

  id associatedObject = GetAssociatedObject(obj);
  if (associatedObject != nullptr) {
    return retainAutorelease ? objc_retainAutoreleaseReturnValue(associatedObject) : associatedObject;
  }

  // TODO: propagate [retainAutorelease] to the code below.

  convertReferenceToObjC converter = (convertReferenceToObjC)obj->type_info()->writableInfo_->objCExport.convert;
  if (converter != nullptr) {
    return converter(obj);
  }

  return Kotlin_ObjCExport_refToObjC_slowpath(obj);
}

extern "C" id Kotlin_ObjCExport_refToObjC(ObjHeader* obj) {
  // TODO: in some cases (e.g. when converting a bridge argument) performing retain-autorelease is not necessary.
  return Kotlin_ObjCExport_refToObjCImpl<true>(obj);
}

extern "C" ALWAYS_INLINE id Kotlin_Interop_refToObjC(ObjHeader* obj) {
  return Kotlin_ObjCExport_refToObjCImpl<false>(obj);
}

extern "C" ALWAYS_INLINE OBJ_GETTER(Kotlin_Interop_refFromObjC, id obj) {
  // TODO: consider removing this function.
  RETURN_RESULT_OF(Kotlin_ObjCExport_refFromObjC, obj);
}

extern "C" OBJ_GETTER(Kotlin_Interop_CreateObjCObjectHolder, id obj) {
  RuntimeAssert(obj != nullptr, "wrapped object must not be null");
  const TypeInfo* typeInfo = theForeignObjCObjectTypeInfo;
  RETURN_RESULT_OF(AllocInstanceWithAssociatedObject, typeInfo, objc_retain(obj));
}

extern "C" OBJ_GETTER(Kotlin_ObjCExport_refFromObjC, id obj) {
  if (obj == nullptr) RETURN_OBJ(nullptr);
  auto msgSend = reinterpret_cast<ObjHeader* (*)(id self, SEL cmd, ObjHeader** slot)>(&objc_msgSend);
  RETURN_RESULT_OF(msgSend, obj, Kotlin_ObjCExport_toKotlinSelector);
}

static id convertKotlinObject(ObjHeader* obj) {
  Class clazz = obj->type_info()->writableInfo_->objCExport.objCClass;
  RuntimeAssert(clazz != nullptr, "");
  return [clazz createWrapper:obj];
}

static convertReferenceToObjC findConverterFromInterfaces(const TypeInfo* typeInfo) {
  const TypeInfo* foundTypeInfo = nullptr;

  for (int i = 0; i < typeInfo->implementedInterfacesCount_; ++i) {
    const TypeInfo* interfaceTypeInfo = typeInfo->implementedInterfaces_[i];
    if ((interfaceTypeInfo->flags_ & TF_SUSPEND_FUNCTION) != 0) {
      // interfaceTypeInfo is a SuspendFunction$N interface.
      // So any instance of typeInfo is a suspend lambda or a suspend callable reference
      // (user-defined Kotlin classes implementing SuspendFunction$N are prohibited by the compiler).
      //
      // Such types also actually implement Function${N+1} interface as an optimization
      // (see e.g. [startCoroutineUninterceptedOrReturn implementation).
      // This fact is not user-visible, so ignoring Function${N+1} interface here
      // (and thus not converting such objects to Obj-C blocks) should be safe enough
      // (because such objects aren't expected to be passed from Kotlin to Swift
      // under formal Function${N+1} type).
      //
      // On the other hand, this fixes support for SuspendFunction$N type: it is mapped as
      // regular Kotlin interface, so its instances should be converted on a general basis
      // (i.e. to objects implementing Obj-C representation of SuspendFunction$N, not to Obj-C blocks).
      //
      // "If typeInfo is a suspend lambda or callable reference type, convert its instances on a regular basis":
      return nullptr;
    }

    if (interfaceTypeInfo->writableInfo_->objCExport.convert != nullptr) {
      if (foundTypeInfo == nullptr || IsSubInterface(interfaceTypeInfo, foundTypeInfo)) {
        foundTypeInfo = interfaceTypeInfo;
      } else if (!IsSubInterface(foundTypeInfo, interfaceTypeInfo)) {
        [NSException raise:NSGenericException
            format:@"Can't convert to Objective-C Kotlin object that is '%@' and '%@' and the same time",
            Kotlin_Interop_CreateNSStringFromKString(foundTypeInfo->relativeName_),
            Kotlin_Interop_CreateNSStringFromKString(interfaceTypeInfo->relativeName_)];
      }
    }
  }

  return foundTypeInfo == nullptr ?
    nullptr :
    (convertReferenceToObjC)foundTypeInfo->writableInfo_->objCExport.convert;
}

static id Kotlin_ObjCExport_refToObjC_slowpath(ObjHeader* obj) {
  const TypeInfo* typeInfo = obj->type_info();
  convertReferenceToObjC converter = nullptr;

  converter = findConverterFromInterfaces(typeInfo);

  if (converter == nullptr) {
    getOrCreateClass(typeInfo);
    converter = (typeInfo == theUnitTypeInfo) ? &Kotlin_ObjCExport_convertUnit : &convertKotlinObject;
  }

  typeInfo->writableInfo_->objCExport.convert = (void*)converter;

  return converter(obj);
}

static void buildITable(TypeInfo* result, const KStdOrderedMap<ClassId, KStdVector<VTableElement>>& interfaceVTables) {
  // Check if can use fast optimistic version - check if the size of the itable could be 2^k and <= 32.
  bool useFastITable;
  int itableSize = 1;
  for (; itableSize <= 32; itableSize <<= 1) {
    useFastITable = true;
    bool used[32];
    memset(used, 0, sizeof(used));
    for (auto& pair : interfaceVTables) {
      auto interfaceId = pair.first;
      auto index = interfaceId & (itableSize - 1);
      if (used[index]) {
        useFastITable = false;
        break;
      }
      used[index] = true;
    }
    if (useFastITable) break;
  }
  if (!useFastITable)
    itableSize = interfaceVTables.size();

  auto itable_ = konanAllocArray<InterfaceTableRecord>(itableSize);
  result->interfaceTable_ = itable_;
  result->interfaceTableSize_ = useFastITable ? itableSize - 1 : -itableSize;

  if (useFastITable) {
    for (auto& pair : interfaceVTables) {
      auto interfaceId = pair.first;
      auto index = interfaceId & (itableSize - 1);
      itable_[index].id = interfaceId;
    }
  } else {
    // Otherwise: conservative version.
    // The table will be sorted since we're using KStdOrderedMap.
    int index = 0;
    for (auto& pair : interfaceVTables) {
      auto interfaceId = pair.first;
      itable_[index++].id = interfaceId;
    }
  }

  for (int i = 0; i < itableSize; ++i) {
    auto interfaceId = itable_[i].id;
    if (interfaceId == kInvalidInterfaceId) continue;
    auto interfaceVTableIt = interfaceVTables.find(interfaceId);
    RuntimeAssert(interfaceVTableIt != interfaceVTables.end(), "");
    auto const& interfaceVTable = interfaceVTableIt->second;
    int interfaceVTableSize = interfaceVTable.size();
    auto interfaceVTable_ = interfaceVTableSize == 0 ? nullptr : konanAllocArray<VTableElement>(interfaceVTableSize);
    for (int j = 0; j < interfaceVTableSize; ++j)
      interfaceVTable_[j] = interfaceVTable[j];
    itable_[i].vtable = interfaceVTable_;
    itable_[i].vtableSize = interfaceVTableSize;
  }
}

static const TypeInfo* createTypeInfo(
  const TypeInfo* superType,
  const KStdVector<const TypeInfo*>& superInterfaces,
  const KStdVector<VTableElement>& vtable,
  const KStdVector<MethodTableRecord>& methodTable,
  const KStdOrderedMap<ClassId, KStdVector<VTableElement>>& interfaceVTables,
  const InterfaceTableRecord* superItable,
  int superItableSize,
  bool itableEqualsSuper,
  const TypeInfo* fieldsInfo
) {
  TypeInfo* result = (TypeInfo*)konanAllocMemory(sizeof(TypeInfo) + vtable.size() * sizeof(void*));
  result->typeInfo_ = result;

  result->flags_ = TF_OBJC_DYNAMIC;

  result->superType_ = superType;
  if (fieldsInfo == nullptr) {
    result->instanceSize_ = superType->instanceSize_;
    result->objOffsets_ = superType->objOffsets_;
    result->objOffsetsCount_ = superType->objOffsetsCount_; // So TF_IMMUTABLE can also be inherited:
    if ((superType->flags_ & TF_IMMUTABLE) != 0) {
      result->flags_ |= TF_IMMUTABLE;
    }
  } else {
    result->instanceSize_ = fieldsInfo->instanceSize_;
    result->objOffsets_ = fieldsInfo->objOffsets_;
    result->objOffsetsCount_ = fieldsInfo->objOffsetsCount_;
  }

  result->classId_ = superType->classId_;

  KStdVector<const TypeInfo*> implementedInterfaces(
    superType->implementedInterfaces_, superType->implementedInterfaces_ + superType->implementedInterfacesCount_
  );
  KStdUnorderedSet<const TypeInfo*> usedInterfaces(implementedInterfaces.begin(), implementedInterfaces.end());

  for (const TypeInfo* interface : superInterfaces) {
    if (usedInterfaces.insert(interface).second) {
      implementedInterfaces.push_back(interface);
    }
  }

  const TypeInfo** implementedInterfaces_ = konanAllocArray<const TypeInfo*>(implementedInterfaces.size());
  for (size_t i = 0; i < implementedInterfaces.size(); ++i) {
    implementedInterfaces_[i] = implementedInterfaces[i];
  }

  result->implementedInterfaces_ = implementedInterfaces_;
  result->implementedInterfacesCount_ = implementedInterfaces.size();
  if (superItable != nullptr) {
    if (itableEqualsSuper) {
      result->interfaceTableSize_ = superItableSize;
      result->interfaceTable_ = superItable;
    } else {
      buildITable(result, interfaceVTables);
    }
  }

  MethodTableRecord* openMethods_ = konanAllocArray<MethodTableRecord>(methodTable.size());
  for (size_t i = 0; i < methodTable.size(); ++i) openMethods_[i] = methodTable[i];

  result->openMethods_ = openMethods_;
  result->openMethodsCount_ = methodTable.size();

  result->packageName_ = nullptr;
  result->relativeName_ = nullptr; // TODO: add some info.
  result->writableInfo_ = (WritableTypeInfo*)konanAllocMemory(sizeof(WritableTypeInfo));

  for (size_t i = 0; i < vtable.size(); ++i) result->vtable()[i] = vtable[i];

  return result;
}

static void addDefinedSelectors(Class clazz, KStdUnorderedSet<SEL>& result) {
  unsigned int objcMethodCount;
  Method* objcMethods = class_copyMethodList(clazz, &objcMethodCount);

  for (unsigned int i = 0; i < objcMethodCount; ++i) {
    result.insert(method_getName(objcMethods[i]));
  }

  if (objcMethods != nullptr) free(objcMethods);
}

static KStdVector<const TypeInfo*> getProtocolsAsInterfaces(Class clazz) {
  KStdVector<const TypeInfo*> result;
  KStdUnorderedSet<Protocol*> handledProtocols;
  KStdVector<Protocol*> protocolsToHandle;

  {
    unsigned int protocolCount;
    Protocol** protocols = class_copyProtocolList(clazz, &protocolCount);
    if (protocols != nullptr) {
      protocolsToHandle.insert(protocolsToHandle.end(), protocols, protocols + protocolCount);
      free(protocols);
    }
  }

  while (!protocolsToHandle.empty()) {
    Protocol* proto = protocolsToHandle[protocolsToHandle.size() - 1];
    protocolsToHandle.pop_back();

    if (handledProtocols.insert(proto).second) {
      const ObjCTypeAdapter* typeAdapter = findProtocolAdapter(proto);
      if (typeAdapter != nullptr) result.push_back(typeAdapter->kotlinTypeInfo);

      unsigned int protocolCount;
      Protocol** protocols = protocol_copyProtocolList(proto, &protocolCount);
      if (protocols != nullptr) {
        protocolsToHandle.insert(protocolsToHandle.end(), protocols, protocols + protocolCount);
        free(protocols);
      }
    }
  }

  return result;
}

static const TypeInfo* getMostSpecificKotlinClass(const TypeInfo* typeInfo) {
  const TypeInfo* result = typeInfo;
  while (getTypeAdapter(result) == nullptr) {
    result = result->superType_;
    RuntimeAssert(result != nullptr, "");
  }

  return result;
}

static int getVtableSize(const TypeInfo* typeInfo) {
  for (const TypeInfo* current = typeInfo; current != nullptr; current = current->superType_) {
    auto typeAdapter = getTypeAdapter(current);
    if (typeAdapter != nullptr) return typeAdapter->kotlinVtableSize;
  }

  RuntimeAssert(false, "");
  return -1;
}

static void insertOrReplace(KStdVector<MethodTableRecord>& methodTable, MethodNameHash nameSignature, void* entryPoint) {
  MethodTableRecord record = {nameSignature, entryPoint};

  for (int i = methodTable.size() - 1; i >= 0; --i) {
    if (methodTable[i].nameSignature_ == nameSignature) {
      methodTable[i].methodEntryPoint_ = entryPoint;
      return;
    } else if (methodTable[i].nameSignature_ < nameSignature) {
      methodTable.insert(methodTable.begin() + (i + 1), record);
      return;
    }
  }

  methodTable.insert(methodTable.begin(), record);
}

static void throwIfCantBeOverridden(Class clazz, const KotlinToObjCMethodAdapter* adapter) {
  if (adapter->kotlinImpl == nullptr) {
    NSString* reason;
    switch (adapter->vtableIndex) {
      case -1: reason = @"it is final"; break;
      case -2: reason = @"original Kotlin method has more than one selector"; break;
      default: reason = @""; break;
    }
    [NSException raise:NSGenericException
        format:@"[%s %s] can't be overridden: %@",
        class_getName(clazz), adapter->selector, reason];
  }
}

static const TypeInfo* createTypeInfo(Class clazz, const TypeInfo* superType, const TypeInfo* fieldsInfo) {
  KStdUnorderedSet<SEL> definedSelectors;
  addDefinedSelectors(clazz, definedSelectors);

  const ObjCTypeAdapter* superTypeAdapter = getTypeAdapter(superType);

  const void * const * superVtable = nullptr;
  int superVtableSize = getVtableSize(superType);

  const MethodTableRecord* superMethodTable = nullptr;
  int superMethodTableSize = 0;

  InterfaceTableRecord const* superITable = nullptr;
  int superITableSize = 0;

  if (superTypeAdapter != nullptr) {
    // Then super class is Kotlin class.

    // And if it is abstract, then vtable and method table are not available from TypeInfo,
    // but present in type adapter instead:
    superVtable = superTypeAdapter->kotlinVtable;
    superMethodTable = superTypeAdapter->kotlinMethodTable;
    superMethodTableSize = superTypeAdapter->kotlinMethodTableSize;
    superITable = superTypeAdapter->kotlinItable;
    superITableSize = superTypeAdapter->kotlinItableSize;
  }

  if (superVtable == nullptr) superVtable = superType->vtable();
  if (superMethodTable == nullptr) {
    superMethodTable = superType->openMethods_;
    superMethodTableSize = superType->openMethodsCount_;
  }

  KStdVector<const void*> vtable(
        superVtable,
        superVtable + superVtableSize
  );

  KStdVector<MethodTableRecord> methodTable(
        superMethodTable, superMethodTable + superMethodTableSize
  );

  if (superITable == nullptr) {
    superITable = superType->interfaceTable_;
    superITableSize = superType->interfaceTableSize_;
  }
  KStdOrderedMap<ClassId, KStdVector<VTableElement>> interfaceVTables;
  if (superITable != nullptr) {
    int actualItableSize = superITableSize >= 0 ? superITableSize + 1 : -superITableSize;
    for (int i = 0; i < actualItableSize; ++i) {
      auto& record = superITable[i];
      auto interfaceId = record.id;
      if (interfaceId == kInvalidInterfaceId) continue;
      int vtableSize = record.vtableSize;
      KStdVector<VTableElement> interfaceVTable(vtableSize);
      for (int j = 0; j < vtableSize; ++j)
        interfaceVTable[j] = record.vtable[j];
      interfaceVTables.emplace(interfaceId, std::move(interfaceVTable));
    }
  }

  KStdVector<const TypeInfo*> addedInterfaces = getProtocolsAsInterfaces(clazz);

  KStdVector<const TypeInfo*> supers(
        superType->implementedInterfaces_,
        superType->implementedInterfaces_ + superType->implementedInterfacesCount_
  );

  for (const TypeInfo* t = superType; t != nullptr; t = t->superType_) {
    supers.push_back(t);
  }

  auto addToITable = [&interfaceVTables](ClassId interfaceId, int methodIndex, VTableElement entry) {
    RuntimeAssert(interfaceId != kInvalidInterfaceId, "");
    auto interfaceVTableIt = interfaceVTables.find(interfaceId);
    RuntimeAssert(interfaceVTableIt != interfaceVTables.end(), "");
    auto& interfaceVTable = interfaceVTableIt->second;
    RuntimeAssert(methodIndex >= 0 && static_cast<size_t>(methodIndex) < interfaceVTable.size(), "");
    interfaceVTable[methodIndex] = entry;
  };

  bool itableEqualsSuper = true;
  for (const TypeInfo* t : supers) {
    const ObjCTypeAdapter* typeAdapter = getTypeAdapter(t);
    if (typeAdapter == nullptr) continue;

    for (int i = 0; i < typeAdapter->reverseAdapterNum; ++i) {
      const KotlinToObjCMethodAdapter* adapter = &typeAdapter->reverseAdapters[i];
      if (definedSelectors.find(sel_registerName(adapter->selector)) == definedSelectors.end()) continue;

      throwIfCantBeOverridden(clazz, adapter);

      itableEqualsSuper = false;
      insertOrReplace(methodTable, adapter->nameSignature, const_cast<void*>(adapter->kotlinImpl));
      if (adapter->vtableIndex != -1) vtable[adapter->vtableIndex] = adapter->kotlinImpl;

      if (adapter->itableIndex != -1 && superITable != nullptr)
        addToITable(adapter->interfaceId, adapter->itableIndex, adapter->kotlinImpl);
    }
  }

  for (const TypeInfo* typeInfo : addedInterfaces) {
    const ObjCTypeAdapter* typeAdapter = getTypeAdapter(typeInfo);

    if (typeAdapter == nullptr) continue;

    if (superITable != nullptr) {
      auto interfaceId = typeInfo->classId_;
      int interfaceVTableSize = typeAdapter->kotlinItableSize;
      RuntimeAssert(interfaceVTableSize >= 0, "");
      auto interfaceVTablesIt = interfaceVTables.find(interfaceId);
      if (interfaceVTablesIt == interfaceVTables.end()) {
        itableEqualsSuper = false;
        interfaceVTables.emplace(interfaceId, KStdVector<VTableElement>(interfaceVTableSize));
      } else {
        auto const& interfaceVTable = interfaceVTablesIt->second;
        RuntimeAssert(interfaceVTable.size() == static_cast<size_t>(interfaceVTableSize), "");
      }
    }

    for (int i = 0; i < typeAdapter->reverseAdapterNum; ++i) {
      itableEqualsSuper = false;
      const KotlinToObjCMethodAdapter* adapter = &typeAdapter->reverseAdapters[i];
      throwIfCantBeOverridden(clazz, adapter);

      insertOrReplace(methodTable, adapter->nameSignature, const_cast<void*>(adapter->kotlinImpl));
      RuntimeAssert(adapter->vtableIndex == -1, "");

      if (adapter->itableIndex != -1 && superITable != nullptr)
        addToITable(adapter->interfaceId, adapter->itableIndex, adapter->kotlinImpl);
    }
  }

  // TODO: consider forbidding the class being abstract.

  const TypeInfo* result = createTypeInfo(superType, addedInterfaces, vtable, methodTable,
                                          interfaceVTables, superITable, superITableSize, itableEqualsSuper,
                                          fieldsInfo);

  // TODO: it will probably never be requested, since such a class can't be instantiated in Kotlin.
  result->writableInfo_->objCExport.objCClass = clazz;
  return result;
}

static SimpleMutex typeInfoCreationMutex;

static const TypeInfo* getOrCreateTypeInfo(Class clazz) {
  const TypeInfo* result = Kotlin_ObjCExport_getAssociatedTypeInfo(clazz);
  if (result != nullptr) {
    return result;
  }

  Class superClass = class_getSuperclass(clazz);

  const TypeInfo* superType = superClass == nullptr ?
    theForeignObjCObjectTypeInfo :
    getOrCreateTypeInfo(superClass);

  LockGuard<SimpleMutex> lockGuard(typeInfoCreationMutex);

  result = Kotlin_ObjCExport_getAssociatedTypeInfo(clazz); // double-checking.
  if (result == nullptr) {
    result = createTypeInfo(clazz, superType, nullptr);
    setAssociatedTypeInfo(clazz, result);
  }

  return result;
}

const TypeInfo* Kotlin_ObjCExport_createTypeInfoWithKotlinFieldsFrom(Class clazz, const TypeInfo* fieldsInfo) {
  Class superClass = class_getSuperclass(clazz);
  RuntimeCheck(superClass != nullptr, "");

  const TypeInfo* superType = getOrCreateTypeInfo(superClass);

  return createTypeInfo(clazz, superType, fieldsInfo);
}

static SimpleMutex classCreationMutex;
static int anonymousClassNextId = 0;

static void addVirtualAdapters(Class clazz, const ObjCTypeAdapter* typeAdapter) {
  for (int i = 0; i < typeAdapter->virtualAdapterNum; ++i) {
    const ObjCToKotlinMethodAdapter* adapter = typeAdapter->virtualAdapters + i;
    SEL selector = sel_registerName(adapter->selector);

    class_addMethod(clazz, selector, adapter->imp, adapter->encoding);
  }
}

static Class createClass(const TypeInfo* typeInfo, Class superClass) {
  RuntimeAssert(typeInfo->superType_ != nullptr, "");

  int classIndex = (anonymousClassNextId++);
  KStdString className = Kotlin_ObjCInterop_getUniquePrefix();
  className += "_kobjcc";
  className += std::to_string(classIndex);

  Class result = objc_allocateClassPair(superClass, className.c_str(), 0);
  RuntimeCheck(result != nullptr, "");

  // TODO: optimize by adding virtual adapters only for overridden methods.

  if (getTypeAdapter(typeInfo->superType_) == nullptr) {
    // class for super type is also synthesized, no need to add class adapters;
  } else {
    for (const TypeInfo* superType = typeInfo->superType_; superType != nullptr; superType = superType->superType_) {
      const ObjCTypeAdapter* typeAdapter = getTypeAdapter(superType);
      if (typeAdapter != nullptr) {
        addVirtualAdapters(result, typeAdapter);
      }
    }
  }

  KStdUnorderedSet<const TypeInfo*> superImplementedInterfaces(
          typeInfo->superType_->implementedInterfaces_,
          typeInfo->superType_->implementedInterfaces_ + typeInfo->superType_->implementedInterfacesCount_
  );

  for (int i = 0; i < typeInfo->implementedInterfacesCount_; ++i) {
    const TypeInfo* interface = typeInfo->implementedInterfaces_[i];
    if (superImplementedInterfaces.find(interface) == superImplementedInterfaces.end()) {
      const ObjCTypeAdapter* typeAdapter = getTypeAdapter(interface);
      if (typeAdapter != nullptr) {
        addVirtualAdapters(result, typeAdapter);
        addProtocolForAdapter(result, typeAdapter);
      }
    }
  }

  objc_registerClassPair(result);

  // TODO: it will probably never be requested, since such a class can't be instantiated in Objective-C.
  setAssociatedTypeInfo(result, typeInfo);

  return result;
}

static Class getOrCreateClass(const TypeInfo* typeInfo) {
  Class result = typeInfo->writableInfo_->objCExport.objCClass;
  if (result != nullptr) {
    return result;
  }

  const ObjCTypeAdapter* typeAdapter = getTypeAdapter(typeInfo);
  if (typeAdapter != nullptr) {
    result = objc_getClass(typeAdapter->objCName);
    RuntimeAssert(result != nullptr, "");
    typeInfo->writableInfo_->objCExport.objCClass = result;
  } else {
    Class superClass = getOrCreateClass(typeInfo->superType_);

    LockGuard<SimpleMutex> lockGuard(classCreationMutex); // Note: non-recursive

    result = typeInfo->writableInfo_->objCExport.objCClass; // double-checking.
    if (result == nullptr) {
      result = createClass(typeInfo, superClass);
      RuntimeAssert(result != nullptr, "");
      typeInfo->writableInfo_->objCExport.objCClass = result;
    }
  }

  return result;
}

extern "C" void Kotlin_ObjCExport_AbstractMethodCalled(id self, SEL selector) {
  [NSException raise:NSGenericException
        format:@"[%s %s] is abstract",
        class_getName(object_getClass(self)), sel_getName(selector)];
}

#else

extern "C" ALWAYS_INLINE void* Kotlin_Interop_refToObjC(ObjHeader* obj) {
  RuntimeAssert(false, "Unavailable operation");
  return nullptr;
}

extern "C" ALWAYS_INLINE OBJ_GETTER(Kotlin_Interop_refFromObjC, void* obj) {
  RuntimeAssert(false, "Unavailable operation");
  RETURN_OBJ(nullptr);
}

#endif // KONAN_OBJC_INTEROP
