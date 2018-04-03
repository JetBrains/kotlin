/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

#import <Foundation/NSObject.h>
#import <Foundation/NSArray.h>
#import <Foundation/NSDictionary.h>
#import <Foundation/NSSet.h>
#import <Foundation/NSNull.h>

#import "Exceptions.h"
#import "Runtime.h"
#import "ObjCExport.h"

extern "C" {

// Imports from ObjCExportUtils.kt:

OBJ_GETTER0(Kotlin_NSArrayAsKList_create);
OBJ_GETTER0(Kotlin_NSMutableArrayAsKMutableList_create);
OBJ_GETTER0(Kotlin_NSEnumeratorAsKIterator_create);
OBJ_GETTER0(Kotlin_NSSetAsKSet_create);
OBJ_GETTER0(Kotlin_NSDictionaryAsKMap_create);

KBoolean Kotlin_Iterator_hasNext(KRef iterator);
OBJ_GETTER(Kotlin_Iterator_next, KRef iterator);
KInt Kotlin_Collection_getSize(KRef collection);
KBoolean Kotlin_Set_contains(KRef set, KRef element);
OBJ_GETTER(Kotlin_Set_getElement, KRef set, KRef element);
OBJ_GETTER(Kotlin_Set_iterator, KRef set);
void Kotlin_MutableCollection_removeObject(KRef collection, KRef element);
void Kotlin_MutableCollection_addObject(KRef list, KRef obj);
OBJ_GETTER(Kotlin_MutableSet_createWithCapacity, KInt capacity);

KInt Kotlin_Map_getSize(KRef map);
KBoolean Kotlin_Map_containsKey(KRef map, KRef key);
OBJ_GETTER(Kotlin_Map_get, KRef map, KRef key);
OBJ_GETTER(Kotlin_Map_keyIterator, KRef map);
OBJ_GETTER(Kotlin_List_get, KRef list, KInt index);

OBJ_GETTER(Kotlin_MutableMap_createWithCapacity, KInt capacity);
void Kotlin_MutableMap_set(KRef map, KRef key, KRef value);
void Kotlin_MutableMap_remove(KRef map, KRef key);
void Kotlin_MutableList_addObjectAtIndex(KRef list, KInt index, KRef obj);
void Kotlin_MutableList_removeObjectAtIndex(KRef list, KInt index);
void Kotlin_MutableList_removeLastObject(KRef list);
void Kotlin_MutableList_setObject(KRef list, KInt index, KRef obj);

void Kotlin_NSEnumeratorAsKIterator_done(KRef thiz);
void Kotlin_NSEnumeratorAsKIterator_setNext(KRef thiz, KRef value);

void Kotlin_ObjCExport_ThrowCollectionTooLarge();
void Kotlin_ObjCExport_ThrowCollectionConcurrentModification();

} // extern "C"

// Objective-C collections can't store `nil`, and the common convention is to use `NSNull.null` instead.
// Follow the convention when converting Kotlin `null`:

static inline id refToObjCOrNSNull(KRef obj) {
  if (obj == nullptr) {
    return NSNull.null;
  } else {
    return Kotlin_ObjCExport_refToObjC(obj);
  }
}

static inline OBJ_GETTER(refFromObjCOrNSNull, id obj) {
  if (obj == NSNull.null) {
    RETURN_OBJ(nullptr);
  } else {
    RETURN_RESULT_OF(Kotlin_ObjCExport_refFromObjC, obj);
  }
}

static inline KInt objCCapacityToKotlin(NSUInteger capacity) {
  KInt max = std::numeric_limits<KInt>::max();
  return capacity > max ? max : capacity;
}

static inline KInt objCSizeToKotlinOrThrow(NSUInteger size) {
  if (size > std::numeric_limits<KInt>::max()) {
    Kotlin_ObjCExport_ThrowCollectionTooLarge();
  }

  return size;
}

static inline KInt objCIndexToKotlinOrThrow(NSUInteger index) {
  if (index > std::numeric_limits<KInt>::max()) {
    ThrowArrayIndexOutOfBoundsException();
  }

  return index;
}

static inline OBJ_GETTER(invokeAndAssociate, KRef (*func)(KRef* result), id obj) {
  Kotlin_initRuntimeIfNeeded();

  KRef kotlinObj = func(OBJ_RESULT);

  SetAssociatedObject(kotlinObj, obj);

  return kotlinObj;
}

@interface NSArray (NSArrayToKotlin) <ConvertibleToKotlin>
@end;

@implementation NSArray (NSArrayToKotlin)
-(KRef)toKotlin:(KRef*)OBJ_RESULT {
  RETURN_RESULT_OF(invokeAndAssociate, Kotlin_NSArrayAsKList_create, objc_retain(self));
}

-(void)releaseAsAssociatedObject {
  objc_release(self);
}
@end;

@interface NSMutableArray (NSMutableArrayToKotlin) <ConvertibleToKotlin>
@end;

@implementation NSMutableArray (NSArrayToKotlin)
-(KRef)toKotlin:(KRef*)OBJ_RESULT {
  RETURN_RESULT_OF(invokeAndAssociate, Kotlin_NSMutableArrayAsKMutableList_create, objc_retain(self));
}

-(void)releaseAsAssociatedObject {
  objc_release(self);
}
@end;


@interface NSSet (NSSetToKotlin) <ConvertibleToKotlin>
@end;

@implementation NSSet (NSSetToKotlin)
-(KRef)toKotlin:(KRef*)OBJ_RESULT {
  RETURN_RESULT_OF(invokeAndAssociate, Kotlin_NSSetAsKSet_create, objc_retain(self));
}

-(void)releaseAsAssociatedObject {
  objc_release(self);
}

@end;

@interface NSDictionary (NSDictionaryToKotlin) <ConvertibleToKotlin>
@end;

@implementation NSDictionary (NSDictionaryToKotlin)
-(KRef)toKotlin:(KRef*)OBJ_RESULT {
  RETURN_RESULT_OF(invokeAndAssociate, Kotlin_NSDictionaryAsKMap_create, objc_retain(self));
}

-(void)releaseAsAssociatedObject {
  objc_release(self);
}

@end;

@interface KIteratorAsNSEnumerator : NSEnumerator
@end;

@implementation KIteratorAsNSEnumerator {
  KRef iterator;
}

-(void)dealloc {
  UpdateRef(&iterator, nullptr);
  [super dealloc];
}

+(id)createWithKIterator:(KRef)iterator {
  KIteratorAsNSEnumerator* result = [[[KIteratorAsNSEnumerator alloc] init] autorelease];
  UpdateRef(&result->iterator, iterator);
  return result;
}

- (id)nextObject {
  if (Kotlin_Iterator_hasNext(iterator)) {
    ObjHolder holder;
    return refToObjCOrNSNull(Kotlin_Iterator_next(iterator, holder.slot()));
  } else {
    return nullptr;
  }
}
@end;

@interface KListAsNSArray : NSArray <ConvertibleToKotlin>
@end;

@implementation KListAsNSArray {
  KRef list;
}

-(void)dealloc {
  UpdateRef(&list, nullptr);
  [super dealloc];
}

+(id)createWithKList:(KRef)list {
  KListAsNSArray* result = [[[KListAsNSArray alloc] init] autorelease];
  UpdateRef(&result->list, list);
  return result;
}

-(KRef)toKotlin:(KRef*)OBJ_RESULT {
  RETURN_OBJ(list);
}

-(id)objectAtIndex:(NSUInteger)index {
  ObjHolder kotlinValueHolder;
  KRef kotlinValue = Kotlin_List_get(list, index, kotlinValueHolder.slot());
  return refToObjCOrNSNull(kotlinValue);
}

-(NSUInteger)count {
  return Kotlin_Collection_getSize(list);
}

@end;

@interface KMutableListAsNSMutableArray : NSMutableArray <ConvertibleToKotlin>
@end;

@implementation KMutableListAsNSMutableArray {
  KRef list;
}

-(void)dealloc {
  UpdateRef(&list, nullptr);
  [super dealloc];
}

+(id)createWithKList:(KRef)list {
  KMutableListAsNSMutableArray* result = [[[KMutableListAsNSMutableArray alloc] init] autorelease];
  UpdateRef(&result->list, list);
  return result;
}

-(KRef)toKotlin:(KRef*)OBJ_RESULT {
  RETURN_OBJ(list);
}

-(id)objectAtIndex:(NSUInteger)index {
  ObjHolder kotlinValueHolder;
  KRef kotlinValue = Kotlin_List_get(list, index, kotlinValueHolder.slot());
  return refToObjCOrNSNull(kotlinValue);
}

-(NSUInteger)count {
  return Kotlin_Collection_getSize(list);
}

- (void)insertObject:(id)anObject atIndex:(NSUInteger)index {
  ObjHolder holder;
  KRef kotlinObject = refFromObjCOrNSNull(anObject, holder.slot());
  Kotlin_MutableList_addObjectAtIndex(list, objCIndexToKotlinOrThrow(index), kotlinObject);
}

- (void)removeObjectAtIndex:(NSUInteger)index {
  Kotlin_MutableList_removeObjectAtIndex(list, objCIndexToKotlinOrThrow(index));
}

- (void)addObject:(id)anObject {
  ObjHolder holder;
  Kotlin_MutableCollection_addObject(list, refFromObjCOrNSNull(anObject, holder.slot()));
}

- (void)removeLastObject {
  Kotlin_MutableList_removeLastObject(list);
}

- (void)replaceObjectAtIndex:(NSUInteger)index withObject:(id)anObject {
  ObjHolder holder;
  KRef kotlinObject = refFromObjCOrNSNull(anObject, holder.slot());
  Kotlin_MutableList_setObject(list, objCIndexToKotlinOrThrow(index), kotlinObject);
}

@end;

@interface KSetAsNSSet : NSSet <ConvertibleToKotlin>
@end;

static inline id KSet_getElement(KRef set, id object) {
  if (object == NSNull.null) {
    return Kotlin_Set_contains(set, nullptr) ? object : nullptr;
  } else {
    ObjHolder requestHolder, resultHolder;
    KRef request = Kotlin_ObjCExport_refFromObjC(object, requestHolder.slot());
    KRef result = Kotlin_Set_getElement(set, request, resultHolder.slot());

    // Note: if result is nullptr, then it can't be a null element of the set, because request != nullptr;
    // so map nullptr to nullptr:
    return Kotlin_ObjCExport_refToObjC(result);
  }
}

@implementation KSetAsNSSet {
  KRef set;
}

-(void)dealloc {
  UpdateRef(&set, nullptr);
  [super dealloc];
}

+(id)createWithKSet:(KRef)set {
  KSetAsNSSet* result = [[[KSetAsNSSet alloc] init] autorelease];
  UpdateRef(&result->set, set);
  return result;
}

-(KRef)toKotlin:(KRef*)OBJ_RESULT {
  RETURN_OBJ(set);
}

-(NSUInteger) count {
  return Kotlin_Collection_getSize(set);
}

- (id)member:(id)object {
  return KSet_getElement(set, object);
}

// Not mandatory, just an optimization:
- (BOOL)containsObject:(id)anObject {
  ObjHolder holder;
  return Kotlin_Set_contains(set, refFromObjCOrNSNull(anObject, holder.slot()));
}

- (NSEnumerator*)objectEnumerator {
  ObjHolder holder;
  return [KIteratorAsNSEnumerator createWithKIterator:Kotlin_Set_iterator(set, holder.slot())];
}
@end;

@interface KotlinMutableSet : NSMutableSet <ConvertibleToKotlin>
@end;

@implementation KotlinMutableSet {
  KRef set;
}

-(instancetype)init {
  if (self = [super init]) {
    Kotlin_initRuntimeIfNeeded();
    Kotlin_MutableSet_createWithCapacity(8, &self->set);
  }

  return self;
}

- (instancetype)initWithCapacity:(NSUInteger)numItems {
  if (self = [super init]) {
    Kotlin_initRuntimeIfNeeded();
    Kotlin_MutableSet_createWithCapacity(objCCapacityToKotlin(numItems), &self->set);
  }

  return self;
}

- (instancetype)initWithObjects:(const id _Nonnull [_Nullable])objects count:(NSUInteger)cnt {
  if (self = [self initWithCapacity:cnt]) {
    for (NSUInteger i = 0; i < cnt; ++i) {
      [self addObject:objects[i]];
    }
  }

  return self;
}

// TODO: what about
// - (nullable instancetype)initWithCoder:(NSCoder *)aDecoder
// ?

-(void)dealloc {
  UpdateRef(&set, nullptr);
  [super dealloc];
}

+(id)createWithKSet:(KRef)set {
  KotlinMutableSet* result = [[[super allocWithZone:nullptr] init] autorelease];
  UpdateRef(&result->set, set);
  return result;
}

-(KRef)toKotlin:(KRef*)OBJ_RESULT {
  RETURN_OBJ(set);
}

-(NSUInteger) count {
  return Kotlin_Collection_getSize(set);
}

- (id)member:(id)object {
  return KSet_getElement(set, object);
}

// Not mandatory, just an optimization:
- (BOOL)containsObject:(id)anObject {
  ObjHolder holder;
  return Kotlin_Set_contains(set, refFromObjCOrNSNull(anObject, holder.slot()));
}

- (NSEnumerator*)objectEnumerator {
  ObjHolder holder;
  return [KIteratorAsNSEnumerator createWithKIterator:Kotlin_Set_iterator(set, holder.slot())];
}

- (void)addObject:(id)object {
  ObjHolder holder;
  Kotlin_MutableCollection_addObject(set, refFromObjCOrNSNull(object, holder.slot()));
}

- (void)removeObject:(id)object {
  ObjHolder holder;
  Kotlin_MutableCollection_removeObject(set, refFromObjCOrNSNull(object, holder.slot()));
}
@end;

@interface KMapAsNSDictionary : NSDictionary <ConvertibleToKotlin>
@end;

static inline id KMap_get(KRef map, id aKey) {
  ObjHolder keyHolder, valueHolder;

  KRef kotlinKey = refFromObjCOrNSNull(aKey, keyHolder.slot());
  KRef kotlinValue = Kotlin_Map_get(map, kotlinKey, valueHolder.slot());

  if (kotlinValue == nullptr) {
    // Either null or not found.
    return Kotlin_Map_containsKey(map, kotlinKey) ? NSNull.null : nullptr;
  } else {
    return refToObjCOrNSNull(kotlinValue);
  }
}

@implementation KMapAsNSDictionary {
  KRef map;
}

-(void)dealloc {
  UpdateRef(&map, nullptr);
  [super dealloc];
}

+(id)createWithKMap:(KRef)map {
  KMapAsNSDictionary* result = [[[KMapAsNSDictionary alloc] init] autorelease];
  UpdateRef(&result->map, map);
  return result;
}

-(KRef)toKotlin:(KRef*)OBJ_RESULT {
  RETURN_OBJ(map);
}

// According to documentation, initWithObjects:forKeys:count: is required to be overridden when subclassing.
// But that doesn't make any sense, since this class can't be arbitrary initialized.

-(NSUInteger) count {
  return Kotlin_Map_getSize(map);
}

- (id)objectForKey:(id)aKey {
  return KMap_get(map, aKey);
}

- (NSEnumerator *)keyEnumerator {
  ObjHolder holder;
  return [KIteratorAsNSEnumerator createWithKIterator:Kotlin_Map_keyIterator(map, holder.slot())];
}

@end;

@interface KotlinMutableDictionary : NSMutableDictionary <ConvertibleToKotlin>
@end;

@implementation KotlinMutableDictionary {
  KRef map;
}

-(void)dealloc {
  UpdateRef(&map, nullptr);
  [super dealloc];
}

-(instancetype)init {
  if (self = [super init]) {
    Kotlin_initRuntimeIfNeeded();
    Kotlin_MutableMap_createWithCapacity(8, &self->map);
  }
  return self;
}

// 'initWithObjects:forKeys:count:' seems to be implemented in base class.

// TODO: what about
//  - (nullable instancetype)initWithCoder:(NSCoder *)aDecoder
// ?

- (instancetype)initWithCapacity:(NSUInteger)numItems {
  if (self = [super init]) {
    Kotlin_initRuntimeIfNeeded();
    Kotlin_MutableMap_createWithCapacity(objCCapacityToKotlin(numItems), &self->map);
  }
  return self;
}

+(id)createWithKMap:(KRef)map {
  KotlinMutableDictionary* result = [[[KotlinMutableDictionary alloc] init] autorelease];
  UpdateRef(&result->map, map);
  return result;
}

-(KRef)toKotlin:(KRef*)OBJ_RESULT {
  RETURN_OBJ(map);
}

-(NSUInteger) count {
  return Kotlin_Map_getSize(map);
}

- (id)objectForKey:(id)aKey {
  return KMap_get(map, aKey);
}

- (NSEnumerator *)keyEnumerator {
  ObjHolder holder;
  return [KIteratorAsNSEnumerator createWithKIterator:Kotlin_Map_keyIterator(map, holder.slot())];
}

- (void)setObject:(id)anObject forKey:(id<NSCopying>)aKey {
  ObjHolder keyHolder, valueHolder;

  id keyCopy = [aKey copyWithZone:nullptr]; // Correspond to the expected NSMutableDictionary behaviour.
  KRef kotlinKey = refFromObjCOrNSNull(keyCopy, keyHolder.slot());
  objc_release(keyCopy);

  KRef kotlinValue = refFromObjCOrNSNull(anObject, valueHolder.slot());

  Kotlin_MutableMap_set(map, kotlinKey, kotlinValue);
}

- (void)removeObjectForKey:(id)aKey {
  ObjHolder holder;
  KRef kotlinKey = refFromObjCOrNSNull(aKey, holder.slot());

  Kotlin_MutableMap_remove(map, kotlinKey);
}

@end;

@interface NSEnumerator (NSEnumeratorAsAssociatedObject)
@end;

@implementation NSEnumerator (NSEnumeratorAsAssociatedObject)
-(void)releaseAsAssociatedObject {
  objc_release(self);
}
@end;

// Referenced from the generated code:

extern "C" id Kotlin_Interop_CreateNSArrayFromKList(KRef obj) {
  return [KListAsNSArray createWithKList:obj];
}

extern "C" id Kotlin_Interop_CreateNSMutableArrayFromKList(KRef obj) {
  return [KMutableListAsNSMutableArray createWithKList:obj];
}

extern "C" id Kotlin_Interop_CreateNSSetFromKSet(KRef obj) {
  return [KSetAsNSSet createWithKSet:obj];
}

extern "C" id Kotlin_Interop_CreateKotlinMutableSetFromKSet(KRef obj) {
  return [KotlinMutableSet createWithKSet:obj];
}

extern "C" id Kotlin_Interop_CreateNSDictionaryFromKMap(KRef obj) {
  return [KMapAsNSDictionary createWithKMap:obj];
}

extern "C" id Kotlin_Interop_CreateKotlinMutableDictonaryFromKMap(KRef obj) {
  return [KotlinMutableDictionary createWithKMap:obj];
}

// Imported to ObjCExportUtils.kt:

extern "C" KInt Kotlin_NSArrayAsKList_getSize(KRef obj) {
  NSArray* array = (NSArray*) GetAssociatedObject(obj);
  return objCSizeToKotlinOrThrow([array count]);
}

extern "C" OBJ_GETTER(Kotlin_NSArrayAsKList_get, KRef obj, KInt index) {
  NSArray* array = (NSArray*) GetAssociatedObject(obj);
  id element = [array objectAtIndex:index];
  RETURN_RESULT_OF(refFromObjCOrNSNull, element);
}

extern "C" void Kotlin_NSMutableArrayAsKMutableList_add(KRef thiz, KInt index, KRef element) {
  NSMutableArray* mutableArray = (NSMutableArray*) GetAssociatedObject(thiz);
  [mutableArray insertObject:refToObjCOrNSNull(element) atIndex:index];
}

extern "C" OBJ_GETTER(Kotlin_NSMutableArrayAsKMutableList_removeAt, KRef thiz, KInt index) {
  NSMutableArray* mutableArray = (NSMutableArray*) GetAssociatedObject(thiz);

  KRef res = refFromObjCOrNSNull([mutableArray objectAtIndex:index], OBJ_RESULT);
  [mutableArray removeObjectAtIndex:index];

  return res;
}

extern "C" OBJ_GETTER(Kotlin_NSMutableArrayAsKMutableList_set, KRef thiz, KInt index, KRef element) {
  NSMutableArray* mutableArray = (NSMutableArray*) GetAssociatedObject(thiz);

  KRef res = refFromObjCOrNSNull([mutableArray objectAtIndex:index], OBJ_RESULT);
  [mutableArray replaceObjectAtIndex:index withObject:refToObjCOrNSNull(element)];

  return res;
}

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

extern "C" KInt Kotlin_NSSetAsKSet_getSize(KRef thiz) {
  NSSet* set = (NSSet*) GetAssociatedObject(thiz);
  return objCSizeToKotlinOrThrow(set.count);
}

extern "C" KBoolean Kotlin_NSSetAsKSet_contains(KRef thiz, KRef element) {
  NSSet* set = (NSSet*) GetAssociatedObject(thiz);
  return [set containsObject:refToObjCOrNSNull(element)];
}

extern "C" OBJ_GETTER(Kotlin_NSSetAsKSet_getElement, KRef thiz, KRef element) {
  NSSet* set = (NSSet*) GetAssociatedObject(thiz);
  id res = [set member:refToObjCOrNSNull(element)];
  RETURN_RESULT_OF(refFromObjCOrNSNull, res);
}

static inline OBJ_GETTER(CreateKIteratorFromNSEnumerator, NSEnumerator* enumerator) {
  RETURN_RESULT_OF(invokeAndAssociate, Kotlin_NSEnumeratorAsKIterator_create, objc_retain(enumerator));
}

extern "C" OBJ_GETTER(Kotlin_NSSetAsKSet_iterator, KRef thiz) {
  NSSet* set = (NSSet*) GetAssociatedObject(thiz);
  RETURN_RESULT_OF(CreateKIteratorFromNSEnumerator, [set objectEnumerator]);
}

extern "C" KInt Kotlin_NSDictionaryAsKMap_getSize(KRef thiz) {
  NSDictionary* dict = (NSDictionary*) GetAssociatedObject(thiz);
  return objCSizeToKotlinOrThrow(dict.count);
}

extern "C" KBoolean Kotlin_NSDictionaryAsKMap_containsKey(KRef thiz, KRef key) {
  NSDictionary* dict = (NSDictionary*) GetAssociatedObject(thiz);
  return [dict objectForKey:refToObjCOrNSNull(key)] != nullptr;
}

extern "C" KBoolean Kotlin_NSDictionaryAsKMap_containsValue(KRef thiz, KRef value) {
  NSDictionary* dict = (NSDictionary*) GetAssociatedObject(thiz);
  id objCValue = refToObjCOrNSNull(value);
  for (id key in dict) {
    if ([[dict objectForKey:key] isEqual:objCValue]) {
      return true;
    }
  }

  return false;
}

extern "C" OBJ_GETTER(Kotlin_NSDictionaryAsKMap_get, KRef thiz, KRef key) {
  NSDictionary* dict = (NSDictionary*) GetAssociatedObject(thiz);
  id value = [dict objectForKey:refToObjCOrNSNull(key)];
  RETURN_RESULT_OF(refFromObjCOrNSNull, value);
}

extern "C" OBJ_GETTER(Kotlin_NSDictionaryAsKMap_getOrThrowConcurrentModification, KRef thiz, KRef key) {
  NSDictionary* dict = (NSDictionary*) GetAssociatedObject(thiz);
  id value = [dict objectForKey:refToObjCOrNSNull(key)];
  if (value == nullptr) {
    Kotlin_ObjCExport_ThrowCollectionConcurrentModification();
  }

  RETURN_RESULT_OF(refFromObjCOrNSNull, value);
}

extern "C" KBoolean Kotlin_NSDictionaryAsKMap_containsEntry(KRef thiz, KRef key, KRef value) {
  NSDictionary* dict = (NSDictionary*) GetAssociatedObject(thiz);
  return [refToObjCOrNSNull(value) isEqual:[dict objectForKey:refToObjCOrNSNull(key)]];
}

extern "C" OBJ_GETTER(Kotlin_NSDictionaryAsKMap_keyIterator, KRef thiz) {
  NSDictionary* dict = (NSDictionary*) GetAssociatedObject(thiz);
  RETURN_RESULT_OF(CreateKIteratorFromNSEnumerator, [dict keyEnumerator]);
}

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