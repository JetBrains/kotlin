/*
 * Copyright 2010-2019 JetBrains s.r.o.
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

#import "Memory.h"
#import "Types.h"

#if KONAN_OBJC_INTEROP

#import <objc/runtime.h>

#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSSet.h>

#import "CallsChecker.hpp"
#import "Exceptions.h"
#import "ObjCExport.h"
#import "ObjCExportCollections.h"

extern "C" {

// Imports from ObjCExportUtils.kt:

OBJ_GETTER0(Kotlin_NSEnumeratorAsKIterator_create);

void Kotlin_NSEnumeratorAsKIterator_done(KRef thiz);
void Kotlin_NSEnumeratorAsKIterator_setNext(KRef thiz, KRef value);

void Kotlin_ObjCExport_ThrowCollectionTooLarge();
void Kotlin_ObjCExport_ThrowCollectionConcurrentModification();

}

static inline KInt objCSizeToKotlinOrThrow(NSUInteger size) {
  if (size > std::numeric_limits<KInt>::max()) {
    Kotlin_ObjCExport_ThrowCollectionTooLarge();
  }

  return size;
}

// Exported to ObjCExportUtils.kt:

// Note: a lot of functions below call Objective-C virtual methods of arbitrary classes
// in Runnable thread state. Some these functions are marked with NO_EXTERNAL_CALLS_CHECK.
// On the one hand, these calls aren't guaranteed to be fast, and also can call back to Kotlin.
// Anything of this is harmful in Runnable state.
// On the other hand, anything of this is unlikely to actually happen, and switching thread state
// to Native might kill the performance, because these functions are just tiny wrappers for tiny functions.
// So let's just ignore this for now, and deal with it later.
// TODO: come up with a proper solution.

NO_EXTERNAL_CALLS_CHECK
extern "C" KInt Kotlin_NSArrayAsKList_getSize(KRef obj) {
  NSArray* array = (NSArray*) GetAssociatedObject(obj);
  return objCSizeToKotlinOrThrow([array count]);
}

NO_EXTERNAL_CALLS_CHECK
extern "C" OBJ_GETTER(Kotlin_NSArrayAsKList_get, KRef obj, KInt index) {
  NSArray* array = (NSArray*) GetAssociatedObject(obj);
  id element = [array objectAtIndex:index];
  RETURN_RESULT_OF(refFromObjCOrNSNull, element);
}

NO_EXTERNAL_CALLS_CHECK
extern "C" void Kotlin_NSMutableArrayAsKMutableList_add(KRef thiz, KInt index, KRef element) {
  NSMutableArray* mutableArray = (NSMutableArray*) GetAssociatedObject(thiz);
  [mutableArray insertObject:refToObjCOrNSNull(element) atIndex:index];
}

NO_EXTERNAL_CALLS_CHECK
extern "C" OBJ_GETTER(Kotlin_NSMutableArrayAsKMutableList_removeAt, KRef thiz, KInt index) {
  NSMutableArray* mutableArray = (NSMutableArray*) GetAssociatedObject(thiz);

  KRef res = refFromObjCOrNSNull([mutableArray objectAtIndex:index], OBJ_RESULT);
  [mutableArray removeObjectAtIndex:index];

  return res;
}

NO_EXTERNAL_CALLS_CHECK
extern "C" OBJ_GETTER(Kotlin_NSMutableArrayAsKMutableList_set, KRef thiz, KInt index, KRef element) {
  NSMutableArray* mutableArray = (NSMutableArray*) GetAssociatedObject(thiz);

  KRef res = refFromObjCOrNSNull([mutableArray objectAtIndex:index], OBJ_RESULT);
  [mutableArray replaceObjectAtIndex:index withObject:refToObjCOrNSNull(element)];

  return res;
}

NO_EXTERNAL_CALLS_CHECK
extern "C" void Kotlin_NSEnumeratorAsKIterator_computeNext(KRef thiz) {
  NSEnumerator* enumerator = (NSEnumerator*) GetAssociatedObject(thiz);
  id next = [enumerator nextObject];
  if (next == nullptr) {
    Kotlin_NSEnumeratorAsKIterator_done(thiz);
  } else {
    ObjHolder holder;
    Kotlin_NSEnumeratorAsKIterator_setNext(thiz, refFromObjCOrNSNull(next, holder.slot()));
  }
}

NO_EXTERNAL_CALLS_CHECK
extern "C" KInt Kotlin_NSSetAsKSet_getSize(KRef thiz) {
  NSSet* set = (NSSet*) GetAssociatedObject(thiz);
  return objCSizeToKotlinOrThrow(set.count);
}

extern "C" KBoolean Kotlin_NSSetAsKSet_contains(KRef thiz, KRef element) {
  NSSet* set = (NSSet*) GetAssociatedObject(thiz);
  id objCElement = refToObjCOrNSNull(element);
  kotlin::ThreadStateGuard guard(kotlin::ThreadState::kNative);
  return [set containsObject:objCElement];
}

extern "C" OBJ_GETTER(Kotlin_NSSetAsKSet_getElement, KRef thiz, KRef element) {
  NSSet* set = (NSSet*) GetAssociatedObject(thiz);
  id objCElement = refToObjCOrNSNull(element);
  id res;
  {
    kotlin::ThreadStateGuard guard(kotlin::ThreadState::kNative);
    res = [set member:objCElement];
  }
  RETURN_RESULT_OF(refFromObjCOrNSNull, res);
}

static inline OBJ_GETTER(CreateKIteratorFromNSEnumerator, NSEnumerator* enumerator) {
  RETURN_RESULT_OF(invokeAndAssociate, Kotlin_NSEnumeratorAsKIterator_create, objc_retain(enumerator));
}

NO_EXTERNAL_CALLS_CHECK
extern "C" OBJ_GETTER(Kotlin_NSSetAsKSet_iterator, KRef thiz) {
  NSSet* set = (NSSet*) GetAssociatedObject(thiz);
  RETURN_RESULT_OF(CreateKIteratorFromNSEnumerator, [set objectEnumerator]);
}

NO_EXTERNAL_CALLS_CHECK
extern "C" KInt Kotlin_NSDictionaryAsKMap_getSize(KRef thiz) {
  NSDictionary* dict = (NSDictionary*) GetAssociatedObject(thiz);
  return objCSizeToKotlinOrThrow(dict.count);
}

extern "C" KBoolean Kotlin_NSDictionaryAsKMap_containsKey(KRef thiz, KRef key) {
  NSDictionary* dict = (NSDictionary*) GetAssociatedObject(thiz);
  id objCKey = refToObjCOrNSNull(key);
  kotlin::ThreadStateGuard guard(kotlin::ThreadState::kNative);
  return [dict objectForKey:objCKey] != nullptr;
}

extern "C" KBoolean Kotlin_NSDictionaryAsKMap_containsValue(KRef thiz, KRef value) {
  NSDictionary* dict = (NSDictionary*) GetAssociatedObject(thiz);
  id objCValue = refToObjCOrNSNull(value);
  kotlin::ThreadStateGuard guard(kotlin::ThreadState::kNative);
  for (id key in dict) {
    if ([[dict objectForKey:key] isEqual:objCValue]) {
      return true;
    }
  }

  return false;
}

extern "C" OBJ_GETTER(Kotlin_NSDictionaryAsKMap_get, KRef thiz, KRef key) {
  NSDictionary* dict = (NSDictionary*) GetAssociatedObject(thiz);
  id objCKey = refToObjCOrNSNull(key);
  id value;
  {
    kotlin::ThreadStateGuard guard(kotlin::ThreadState::kNative);
    value = [dict objectForKey:objCKey];
  }
  RETURN_RESULT_OF(refFromObjCOrNSNull, value);
}

extern "C" OBJ_GETTER(Kotlin_NSDictionaryAsKMap_getOrThrowConcurrentModification, KRef thiz, KRef key) {
  NSDictionary* dict = (NSDictionary*) GetAssociatedObject(thiz);
  id objCKey = refToObjCOrNSNull(key);
  id value;
  {
    kotlin::ThreadStateGuard guard(kotlin::ThreadState::kNative);
    value = [dict objectForKey:objCKey];
  }
  if (value == nullptr) {
    Kotlin_ObjCExport_ThrowCollectionConcurrentModification();
  }

  RETURN_RESULT_OF(refFromObjCOrNSNull, value);
}

extern "C" KBoolean Kotlin_NSDictionaryAsKMap_containsEntry(KRef thiz, KRef key, KRef value) {
  NSDictionary* dict = (NSDictionary*) GetAssociatedObject(thiz);
  id objCValue = refToObjCOrNSNull(value);
  id objCKey = refToObjCOrNSNull(key);
  kotlin::ThreadStateGuard guard(kotlin::ThreadState::kNative);
  return [objCValue isEqual:[dict objectForKey:objCKey]];
}

NO_EXTERNAL_CALLS_CHECK
extern "C" OBJ_GETTER(Kotlin_NSDictionaryAsKMap_keyIterator, KRef thiz) {
  NSDictionary* dict = (NSDictionary*) GetAssociatedObject(thiz);
  RETURN_RESULT_OF(CreateKIteratorFromNSEnumerator, [dict keyEnumerator]);
}

NO_EXTERNAL_CALLS_CHECK
extern "C" OBJ_GETTER(Kotlin_NSDictionaryAsKMap_valueIterator, KRef thiz) {
  NSDictionary* dict = (NSDictionary*) GetAssociatedObject(thiz);
  RETURN_RESULT_OF(CreateKIteratorFromNSEnumerator, [dict objectEnumerator]);
}

#else  // KONAN_OBJC_INTEROP

extern "C" KInt Kotlin_NSArrayAsKList_getSize(KRef obj) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  return -1;
}

extern "C" OBJ_GETTER(Kotlin_NSArrayAsKList_get, KRef obj, KInt index) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  RETURN_OBJ(nullptr);
}

extern "C" void Kotlin_NSMutableArrayAsKMutableList_add(KRef thiz, KInt index, KRef element) {
  RuntimeAssert(false, "Objective-C interop is disabled");
}

extern "C" OBJ_GETTER(Kotlin_NSMutableArrayAsKMutableList_removeAt, KRef thiz, KInt index) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  RETURN_OBJ(nullptr);
}

extern "C" OBJ_GETTER(Kotlin_NSMutableArrayAsKMutableList_set, KRef thiz, KInt index, KRef element) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  RETURN_OBJ(nullptr);
}

extern "C" void Kotlin_NSEnumeratorAsKIterator_computeNext(KRef thiz) {
  RuntimeAssert(false, "Objective-C interop is disabled");
}

extern "C" KInt Kotlin_NSSetAsKSet_getSize(KRef thiz) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  return -1;
}

extern "C" KBoolean Kotlin_NSSetAsKSet_contains(KRef thiz, KRef element) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  return false;
}

extern "C" OBJ_GETTER(Kotlin_NSSetAsKSet_getElement, KRef thiz, KRef element) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  RETURN_OBJ(nullptr);
}

extern "C" OBJ_GETTER(Kotlin_NSSetAsKSet_iterator, KRef thiz) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  RETURN_OBJ(nullptr);
}

extern "C" KInt Kotlin_NSDictionaryAsKMap_getSize(KRef thiz) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  return -1;
}

extern "C" KBoolean Kotlin_NSDictionaryAsKMap_containsKey(KRef thiz, KRef key) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  return false;
}

extern "C" KBoolean Kotlin_NSDictionaryAsKMap_containsValue(KRef thiz, KRef value) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  return false;
}

extern "C" OBJ_GETTER(Kotlin_NSDictionaryAsKMap_get, KRef thiz, KRef key) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  RETURN_OBJ(nullptr);
}

extern "C" OBJ_GETTER(Kotlin_NSDictionaryAsKMap_getOrThrowConcurrentModification, KRef thiz, KRef key) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  RETURN_OBJ(nullptr);
}

extern "C" KBoolean Kotlin_NSDictionaryAsKMap_containsEntry(KRef thiz, KRef key, KRef value) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  return false;
}

extern "C" OBJ_GETTER(Kotlin_NSDictionaryAsKMap_keyIterator, KRef thiz) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  RETURN_OBJ(nullptr);
}

extern "C" OBJ_GETTER(Kotlin_NSDictionaryAsKMap_valueIterator, KRef thiz) {
  RuntimeAssert(false, "Objective-C interop is disabled");
  RETURN_OBJ(nullptr);
}

#endif // KONAN_OBJC_INTEROP