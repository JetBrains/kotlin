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

#if KONAN_OBJC_INTEROP

#import <Foundation/NSObject.h>
#import <Foundation/NSValue.h>
#import <Foundation/NSString.h>
#import <Foundation/NSMethodSignature.h>
#import <Foundation/NSError.h>
#import <Foundation/NSException.h>
#import <Foundation/NSDictionary.h>
#import <objc/runtime.h>
#import <objc/objc-exception.h>
#import <dispatch/dispatch.h>

#import "ObjCExport.h"
#import "MemoryPrivate.hpp"
#import "Runtime.h"
#import "Utils.h"
#import "Exceptions.h"

// Note: defined by a compiler-generated bitcode.
extern "C" const uint8_t objCExportEnabled;

struct ObjCToKotlinMethodAdapter {
  const char* selector;
  const char* encoding;
  IMP imp;
};

struct KotlinToObjCMethodAdapter {
  const char* selector;
  MethodNameHash nameSignature;
  int vtableIndex;
  const void* kotlinImpl;
};

struct ObjCTypeAdapter {
  const TypeInfo* kotlinTypeInfo;

  const void * const * kotlinVtable;
  int kotlinVtableSize;

  const MethodTableRecord* kotlinMethodTable;
  int kotlinMethodTableSize;

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

struct TypeInfoObjCExportAddition {
  /*convertReferenceToObjC*/ void* convert;
  Class objCClass;
  const ObjCTypeAdapter* typeAdapter;
};

struct WritableTypeInfo {
  TypeInfoObjCExportAddition objCExport;
};


static char associatedTypeInfoKey;

static const TypeInfo* getAssociatedTypeInfo(Class clazz) {
  return (const TypeInfo*)[objc_getAssociatedObject(clazz, &associatedTypeInfoKey) pointerValue];
}

static void setAssociatedTypeInfo(Class clazz, const TypeInfo* typeInfo) {
  objc_setAssociatedObject(clazz, &associatedTypeInfoKey, [NSValue valueWithPointer:typeInfo], OBJC_ASSOCIATION_RETAIN);
}

extern "C" id Kotlin_ObjCExport_GetAssociatedObject(ObjHeader* obj) {
  return GetAssociatedObject(obj);
}

inline static OBJ_GETTER(AllocInstanceWithAssociatedObject, const TypeInfo* typeInfo, id associatedObject) {
  ObjHeader* result = AllocInstance(typeInfo, OBJ_RESULT);
  SetAssociatedObject(result, associatedObject);
  return result;
}

static Class getOrCreateClass(const TypeInfo* typeInfo);
static void initializeClass(Class clazz);

@interface KotlinBase : NSObject <ConvertibleToKotlin, NSCopying>
@end;

@implementation KotlinBase {
  KRef kotlinObj;
}

-(KRef)toKotlin:(KRef*)OBJ_RESULT {
  RETURN_OBJ(kotlinObj);
}

+(void)initialize {
  initializeClass(self);
}

+(instancetype)allocWithZone:(NSZone*)zone {
  Kotlin_initRuntimeIfNeeded();

  KotlinBase* result = [super allocWithZone:zone];

  const TypeInfo* typeInfo = getAssociatedTypeInfo(self);
  if (typeInfo == nullptr) {
    [NSException raise:NSGenericException
          format:@"%s is not allocatable or +[KotlinBase initialize] method wasn't called on it",
          class_getName(object_getClass(self))];
  }

  if (typeInfo->instanceSize_ < 0) {
    [NSException raise:NSGenericException
          format:@"%s must be allocated and initialized with a factory method",
          class_getName(object_getClass(self))];
  }

  AllocInstanceWithAssociatedObject(typeInfo, result, &result->kotlinObj);

  return result;
}

+(instancetype)createWrapper:(ObjHeader*)obj {
  KotlinBase* result = [super allocWithZone:nil];
  // TODO: should we call NSObject.init ?
  UpdateRef(&result->kotlinObj, obj);

  if (!obj->permanent()) {
    SetAssociatedObject(obj, result);
  }
  // TODO: permanent objects should probably be supported as custom types.

  return [result autorelease];
}

-(instancetype)retain {
  ObjHeader* obj = kotlinObj;
  if (obj->permanent()) { // TODO: consider storing `isPermanent` to self field.
    [super retain];
  } else {
    AddRefFromAssociatedObject(obj);
  }
  return self;
}

-(oneway void)release {
  ObjHeader* obj = kotlinObj;
  if (obj->permanent()) {
    [super release];
  } else {
    ReleaseRefFromAssociatedObject(kotlinObj);
  }
}

-(void)releaseAsAssociatedObject {
  RuntimeAssert(!kotlinObj->permanent(), "");
  [super release];
}

- (instancetype)copyWithZone:(NSZone *)zone {
  // TODO: write documentation.
  return [self retain];
}

@end;

extern "C" void Kotlin_ObjCExport_releaseAssociatedObject(void* associatedObject) {
  if (associatedObject != nullptr) {
    [((id)associatedObject) releaseAsAssociatedObject];
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

static Protocol* getProtocolForInterface(const TypeInfo* interfaceInfo) {
  const ObjCTypeAdapter* protocolAdapter = getTypeAdapter(interfaceInfo);
  if (protocolAdapter != nullptr) {
    Protocol* protocol = objc_getProtocol(protocolAdapter->objCName);
    if (protocol != nullptr) {
      return protocol;
    } else {
      // TODO: construct the protocol in compiler instead, because this case can't be handled easily.
    }
  }

  return nullptr;
}

static const TypeInfo* getOrCreateTypeInfo(Class clazz);

static void initializeClass(Class clazz) {
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
    Protocol* protocol = getProtocolForInterface(typeInfo->implementedInterfaces_[i]);
    if (protocol != nullptr) {
      class_addProtocol(clazz, protocol);
      class_addProtocol(object_getClass(clazz), protocol);
    }
  }

}

@interface NSObject (NSObjectToKotlin) <ConvertibleToKotlin>
@end;

extern "C" id objc_retainBlock(id self);
extern "C" id objc_retainAutoreleaseReturnValue(id self);

@implementation NSObject (NSObjectToKotlin)
-(ObjHeader*)toKotlin:(ObjHeader**)OBJ_RESULT {
  const TypeInfo* typeInfo = getOrCreateTypeInfo(object_getClass(self));
  RETURN_RESULT_OF(AllocInstanceWithAssociatedObject, typeInfo, objc_retain(self));
}

-(void)releaseAsAssociatedObject {
  objc_release(self);
}
@end;

@interface NSString (NSStringToKotlin) <ConvertibleToKotlin>
@end;

@implementation NSString (NSStringToKotlin)
-(ObjHeader*)toKotlin:(ObjHeader**)OBJ_RESULT {
  RETURN_RESULT_OF(Kotlin_Interop_CreateKStringFromNSString, self);
}
@end;

extern "C" {

OBJ_GETTER(Kotlin_boxBoolean, KBoolean value);
OBJ_GETTER(Kotlin_boxChar, KChar value);
OBJ_GETTER(Kotlin_boxByte, KByte value);
OBJ_GETTER(Kotlin_boxShort, KShort value);
OBJ_GETTER(Kotlin_boxInt, KInt value);
OBJ_GETTER(Kotlin_boxLong, KLong value);
OBJ_GETTER(Kotlin_boxFloat, KFloat value);
OBJ_GETTER(Kotlin_boxDouble, KDouble value);

}

@interface NSNumber (NSNumberToKotlin) <ConvertibleToKotlin>
@end;

static Class __NSCFBooleanClass = nullptr;

@implementation NSNumber (NSNumberToKotlin)
-(ObjHeader*)toKotlin:(ObjHeader**)OBJ_RESULT {
  const char* type = self.objCType;

  // TODO: the code below makes some assumption on char, short, int and long sizes.

  switch (type[0]) {
    case 'S': RETURN_RESULT_OF(Kotlin_boxChar, self.unsignedShortValue);
    case 'c': {
      Class booleanClass = __NSCFBooleanClass;
      if (booleanClass == nullptr) {
        // Note: __NSCFBoolean is not visible to linker, so this case can't be handled with a category.
        booleanClass = __NSCFBooleanClass = objc_getClass("__NSCFBoolean");
        if (booleanClass == nullptr) {
          [NSException raise:NSGenericException format:@"__NSCFBoolean class not found"];
        }
      }

      if (object_getClass(self) == booleanClass) {
        RETURN_RESULT_OF(Kotlin_boxBoolean, self.boolValue);
      } else {
        RETURN_RESULT_OF(Kotlin_boxByte, self.charValue);
      }
    }
    case 's': RETURN_RESULT_OF(Kotlin_boxShort, self.shortValue);
    case 'i': RETURN_RESULT_OF(Kotlin_boxInt, self.intValue);
    case 'q': RETURN_RESULT_OF(Kotlin_boxLong, self.longLongValue);
    case 'f': RETURN_RESULT_OF(Kotlin_boxFloat, self.floatValue);
    case 'd': RETURN_RESULT_OF(Kotlin_boxDouble, self.doubleValue);

    default:  return [super toKotlin:OBJ_RESULT];
  }
}
@end;

@interface NSBlock <NSObject>
@end;

@interface NSBlock (NSBlockToKotlin) <ConvertibleToKotlin>
@end;

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

// Note: replaced by compiler in appropriate compilation modes.
__attribute__((weak)) const TypeInfo * const * Kotlin_ObjCExport_functionAdaptersToBlock = nullptr;

static const TypeInfo* getFunctionTypeInfoForBlock(id block) {
  const char* encoding = getBlockEncoding(block);

  // TODO: optimize:
  NSMethodSignature *signature = [NSMethodSignature signatureWithObjCTypes:encoding];
  int parameterCount = signature.numberOfArguments - 1; // 1 for the block itself.

  if (parameterCount > 22) {
    [NSException raise:NSGenericException format:@"Blocks with %d (>22) parameters aren't supported", parameterCount];
  }

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

  // TODO: support Unit-as-void.

  return Kotlin_ObjCExport_functionAdaptersToBlock[parameterCount];
}

@implementation NSBlock (NSBlockToKotlin)
-(ObjHeader*)toKotlin:(ObjHeader**)OBJ_RESULT {

  const TypeInfo* typeInfo = getFunctionTypeInfoForBlock(self);
  RETURN_RESULT_OF(AllocInstanceWithAssociatedObject, typeInfo, objc_retainBlock(self));
  // TODO: call (Any) constructor?
}

-(void)releaseAsAssociatedObject {
  objc_release(self);
}

@end;

static id Kotlin_ObjCExport_refToObjC_slowpath(ObjHeader* obj);

extern "C" id Kotlin_ObjCExport_refToObjC(ObjHeader* obj) {
  if (obj == nullptr) return nullptr;

  if (obj->has_meta_object()) {
    id associatedObject = GetAssociatedObject(obj);
    if (associatedObject != nullptr) {
      return objc_retainAutoreleaseReturnValue(associatedObject);
    }
  }

  convertReferenceToObjC converter = (convertReferenceToObjC)obj->type_info()->writableInfo_->objCExport.convert;
  if (converter != nullptr) {
    return converter(obj);
  }

  return Kotlin_ObjCExport_refToObjC_slowpath(obj);
}

extern "C" ALWAYS_INLINE id Kotlin_Interop_refToObjC(ObjHeader* obj) {
  if (obj == nullptr) {
    return nullptr;
  } else if (!objCExportEnabled || obj->type_info() == theObjCPointerHolderTypeInfo) {
    return *reinterpret_cast<id*>(obj + 1); // First field.
  } else {
    return Kotlin_ObjCExport_refToObjC(obj);
  }
}

extern "C" OBJ_GETTER(Kotlin_ObjCExport_refFromObjC, id obj) {
  if (obj == nullptr) RETURN_OBJ(nullptr);
  id convertible = (id<ConvertibleToKotlin>)obj;
  return [convertible toKotlin:OBJ_RESULT];
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

static const TypeInfo* createTypeInfo(
  const TypeInfo* superType,
  const KStdVector<const TypeInfo*>& superInterfaces,
  const KStdVector<const void*>& vtable,
  const KStdVector<MethodTableRecord>& methodTable
) {
  TypeInfo* result = (TypeInfo*)konanAllocMemory(sizeof(TypeInfo) + vtable.size() * sizeof(void*));
  result->typeInfo_ = result;

  MakeGlobalHash(nullptr, 0, &result->name_);
  result->instanceSize_ = superType->instanceSize_;
  result->superType_ = superType;
  result->objOffsets_ = superType->objOffsets_;
  result->objOffsetsCount_ = superType->objOffsetsCount_;

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
  for (int i = 0; i < implementedInterfaces.size(); ++i) {
    implementedInterfaces_[i] = implementedInterfaces[i];
  }

  result->implementedInterfaces_ = implementedInterfaces_;
  result->implementedInterfacesCount_ = implementedInterfaces.size();

  MethodTableRecord* openMethods_ = konanAllocArray<MethodTableRecord>(methodTable.size());
  for (int i = 0; i < methodTable.size(); ++i) openMethods_[i] = methodTable[i];

  result->openMethods_ = openMethods_;
  result->openMethodsCount_ = methodTable.size();

  result->fields_ = nullptr;
  result->fieldsCount_ = 0;

  result->packageName_ = nullptr;
  result->relativeName_ = nullptr; // TODO: add some info.
  result->writableInfo_ = (WritableTypeInfo*)konanAllocMemory(sizeof(WritableTypeInfo));

  for (int i = 0; i < vtable.size(); ++i) result->vtable()[i] = vtable[i];

  return result;
}

static void addDefinedSelectors(Class clazz, KStdUnorderedSet<SEL>& result) {
  unsigned int objcMethodCount;
  Method* objcMethods = class_copyMethodList(clazz, &objcMethodCount);

  for (int i = 0; i < objcMethodCount; ++i) {
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

static const TypeInfo* createTypeInfo(Class clazz, const TypeInfo* superType) {
  Class superClass = class_getSuperclass(clazz);

  KStdUnorderedSet<SEL> definedSelectors;
  addDefinedSelectors(clazz, definedSelectors);

  const ObjCTypeAdapter* superTypeAdapter = getTypeAdapter(superType);

  const void * const * superVtable = nullptr;
  int superVtableSize = getTypeAdapter(getMostSpecificKotlinClass(superType))->kotlinVtableSize;

  const MethodTableRecord* superMethodTable = nullptr;
  int superMethodTableSize = 0;

  if (superTypeAdapter != nullptr) {
    // Then super class is Kotlin class.

    // And if it is abstract, then vtable and method table are not available from TypeInfo,
    // but present in type adapter instead:
    superVtable = superTypeAdapter->kotlinVtable;
    superMethodTable = superTypeAdapter->kotlinMethodTable;
    superMethodTableSize = superTypeAdapter->kotlinMethodTableSize;
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

  KStdVector<const TypeInfo*> addedInterfaces = getProtocolsAsInterfaces(clazz);

  KStdVector<const TypeInfo*> supers(
        superType->implementedInterfaces_,
        superType->implementedInterfaces_ + superType->implementedInterfacesCount_
  );

  for (const TypeInfo* t = superType; t != nullptr; t = t->superType_) {
    supers.push_back(t);
  }

  for (const TypeInfo* t : supers) {
    const ObjCTypeAdapter* typeAdapter = getTypeAdapter(t);
    if (typeAdapter == nullptr) continue;

    for (int i = 0; i < typeAdapter->reverseAdapterNum; ++i) {
      const KotlinToObjCMethodAdapter* adapter = &typeAdapter->reverseAdapters[i];
      if (definedSelectors.find(sel_registerName(adapter->selector)) == definedSelectors.end()) continue;

      if (adapter->kotlinImpl == nullptr) {
        [NSException raise:NSGenericException
              format:@"[%s %s] can't be implemented",
              class_getName(clazz), adapter->selector];
        // TODO: describe the reasons
      }

      insertOrReplace(methodTable, adapter->nameSignature, const_cast<void*>(adapter->kotlinImpl));
      if (adapter->vtableIndex != -1) vtable[adapter->vtableIndex] = adapter->kotlinImpl;
    }
  }

  for (const TypeInfo* typeInfo : addedInterfaces) {
    const ObjCTypeAdapter* typeAdapter = getTypeAdapter(typeInfo);
    if (typeAdapter == nullptr) continue;

    for (int i = 0; i < typeAdapter->reverseAdapterNum; ++i) {
      const KotlinToObjCMethodAdapter* adapter = &typeAdapter->reverseAdapters[i];
      if (adapter->kotlinImpl == nullptr) {
        [NSException raise:NSGenericException
              format:@"[%s %s] can't be implemented",
              class_getName(clazz), adapter->selector];
      }

      insertOrReplace(methodTable, adapter->nameSignature, const_cast<void*>(adapter->kotlinImpl));
      RuntimeAssert(adapter->vtableIndex == -1, "");
    }
  }

  // TODO: consider forbidding the class being abstract.

  const TypeInfo* result = createTypeInfo(superType, addedInterfaces, vtable, methodTable);

  // TODO: it will probably never be requested, since such a class can't be instantiated in Kotlin.
  result->writableInfo_->objCExport.objCClass = clazz;
  return result;
}

static SimpleMutex typeInfoCreationMutex;

static const TypeInfo* getOrCreateTypeInfo(Class clazz) {
  const TypeInfo* result = getAssociatedTypeInfo(clazz);
  if (result != nullptr) {
    return result;
  }

  Class superClass = class_getSuperclass(clazz);

  const TypeInfo* superType = superClass == nullptr ?
    theAnyTypeInfo :
    getOrCreateTypeInfo(superClass);

  LockGuard<SimpleMutex> lockGuard(typeInfoCreationMutex);

  result = getAssociatedTypeInfo(clazz); // double-checking.
  if (result == nullptr) {
    result = createTypeInfo(clazz, superType);
    setAssociatedTypeInfo(clazz, result);
  }

  return result;
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

  char classNameBuffer[64];
  snprintf(classNameBuffer, sizeof(classNameBuffer), "kobjcc%d", anonymousClassNextId++);
  const char* className = classNameBuffer;

  Class result = objc_allocateClassPair(superClass, className, 0);
  RuntimeAssert(result != nullptr, "");

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

__attribute__((constructor))
static void checkLoadedOnce() {
  Class marker = objc_allocateClassPair([NSObject class], "KotlinFrameworkLoadedOnceMarker", 0);
  if (marker == nullptr) {
    [NSException raise:NSGenericException
          format:@"Only one Kotlin framework can be loaded currently"];
  } else {
    objc_registerClassPair(marker);
  }
}

#else

extern "C" ALWAYS_INLINE id Kotlin_Interop_refToObjC(ObjHeader* obj) {
  RuntimeAssert(false, "Unavailable operation");
  return nullptr;
}

#endif // KONAN_OBJC_INTEROP
