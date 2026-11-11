/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#import "Types.h"
#import "Memory.h"

#if KONAN_OBJC_INTEROP

#include <atomic>
#include <mutex>
#include <unordered_map>
#include <variant>

#import <Foundation/Foundation.h>
#import <objc/runtime.h>
#import <objc/objc-exception.h>
#import <dispatch/dispatch.h>

#import "CallsChecker.hpp"
#import "KotlinBase.h"
#include "ObjCBackRef.hpp"
#import "ObjCExport.h"
#import "ObjCExportInit.h"
#import "ObjCExportPrivate.h"
#import "Runtime.h"
#import "concurrent/Mutex.hpp"
#import "Exceptions.h"
#include "ExternalRCRef.hpp"
#include "WritableTypeInfo.hpp"
#include "TypeInfoObjCExportAddition.hpp"
#include "std_support/Atomic.hpp"
#include "swiftExportRuntime/SwiftExport.hpp"
#include "StackTrace.hpp"

extern "C" const TypeInfo* Kotlin_SwiftExport_getOrCreateTypeInfoForSwiftSubclass(Class, Class, const TypeInfo*);
extern "C" void* Kotlin_SwiftExport_allocInstanceForSwiftSubclass(Class);

@interface NSObject (NSObjectPrivateMethods)
// Implemented for NSObject in libobjc/NSObject.mm
-(BOOL)_tryRetain;
@end

static void injectToRuntime();

extern "C" KInt Kotlin_hashCode(KRef str);
extern "C" KBoolean Kotlin_equals(KRef lhs, KRef rhs);
extern "C" OBJ_GETTER(Kotlin_toString, KRef obj);

namespace {

using PermanentRef = KRef;
using RegularRef = kotlin::mm::ObjCBackRef;

// The reference of a secondary wrapper (Swift Export only).
//
// A secondary wrapper is not the cached wrapper (the associated object) of its Kotlin object: the object has another
// one, or this wrapper was not meant to be cached. Nothing but Swift refers to it. So it is counted as a plain
// Objective-C object, and it holds one reference to the Kotlin object for as long as it lives.
class SecondaryRef : private kotlin::Pinned {
public:
    // Adopt `raw` together with the one reference that the wrapper got it with.
    explicit SecondaryRef(kotlin::mm::ExternalRCRefImpl* raw) noexcept : raw_(raw) {}

    ~SecondaryRef() {
        // Both can be called with any thread state.
        raw_->releaseRef();
        raw_->dispose();
    }

    kotlin::mm::ExternalRCRefImpl* get() const noexcept { return raw_; }

    // Safe to use: the wrapper holds a reference for its whole life.
    KRef ref() const noexcept { return raw_->ref(); }

private:
    kotlin::mm::ExternalRCRefImpl* raw_;
};

struct {
    const KotlinToObjCMethodAdapter *toString;
    const KotlinToObjCMethodAdapter *hashCode;
    const KotlinToObjCMethodAdapter *equals;
} anyReverseAdapters = {};

static void initializeAnyMemberReverseAdapters(void) noexcept;
static VTableElement nonRecursiveAnyMemberImpl(KRef obj, const KotlinToObjCMethodAdapter *adapter) noexcept;

// The cached wrapper of the Kotlin object behind `ref`, if there is one. Not retained.
// The caller holds `ref`, so the Kotlin object is alive, and so is its cached wrapper.
// `object` gets the Kotlin object, to be used as a key only. It stays nil for a permanent object: it has no cached wrapper.
static id cachedWrapperOf(kotlin::mm::RawExternalRCRef* ref, KRef* object) noexcept {
    if (kotlin::mm::externalRCRefAsPermanentObject(ref)) return nil;
    // TODO: Make it okay to get/replace associated objects w/o runnable state.
    kotlin::CalledFromNativeGuard guard;
    KRef obj = kotlin::mm::dereferenceExternalRCRef(ref);
    *object = obj;
    return Kotlin_ObjCExport_GetAssociatedObject(obj);
}

// The secondary wrappers that are alive, by Kotlin object and wrapper class (Swift Export only).
//
// A Kotlin object has one cached wrapper, of the class that the runtime finds by the Kotlin class. An instantiation
// of a generic Swift class can't be found this way: the Kotlin object doesn't know its type arguments. So Swift code
// asks for a wrapper of a class by name, and the wrapper is a secondary one. It is found here, so that one Kotlin
// object and one Swift class give one Swift object, and `===` holds for it.
//
// The table doesn't own the wrappers. A wrapper leaves the table in its `dealloc`, and holds its Kotlin object
// until then, so a key never names a dead object.
class SecondaryWrappers : private kotlin::Pinned {
public:
    // Whether there is nothing to find. A program without generic classes has few secondary wrappers, or none.
    bool empty() const noexcept { return count_.load(std::memory_order_relaxed) == 0; }

    // The wrapper of `cls` for `object` if it is alive, retained.
    id findRetained(KRef object, Class cls) noexcept {
        if (object == nullptr || empty()) return nil;
        std::lock_guard guard(lock_);
        auto it = wrappers_.find(Key{object, cls});
        if (it == wrappers_.end()) return nil;
        // A wrapper that is being deallocated waits for the lock in `remove`. It can't be retained.
        return [it->second _tryRetain] ? it->second : nil;
    }

    // Adds `candidate`, unless the wrapper of `cls` for `object` is alive: then that one is returned, retained.
    id findRetainedOrAdd(KRef object, Class cls, id candidate) noexcept {
        std::lock_guard guard(lock_);
        auto [it, inserted] = wrappers_.try_emplace(Key{object, cls}, candidate);
        if (inserted) {
            count_.fetch_add(1, std::memory_order_relaxed);
        } else if ([it->second _tryRetain]) {
            return it->second;
        } else {
            byWrapper_.erase(it->second);
            it->second = candidate;
        }
        byWrapper_.insert_or_assign(candidate, Key{object, cls});
        return nil;
    }

    void remove(id wrapper) noexcept {
        if (empty()) return;
        std::lock_guard guard(lock_);
        auto key = byWrapper_.find(wrapper);
        if (key == byWrapper_.end()) return;
        wrappers_.erase(key->second);
        byWrapper_.erase(key);
        count_.fetch_sub(1, std::memory_order_relaxed);
    }

private:
    struct Key {
        KRef object;
        Class cls;
        bool operator==(const Key& other) const noexcept { return object == other.object && cls == other.cls; }
    };
    struct KeyHash {
        size_t operator()(const Key& key) const noexcept {
            return std::hash<void*>()(key.object) * 31 + std::hash<void*>()((void*)key.cls);
        }
    };

    kotlin::SpinLock lock_;
    std::atomic<size_t> count_ = 0;
    std::unordered_map<Key, id, KeyHash> wrappers_;
    std::unordered_map<id, Key> byWrapper_;
};

static SecondaryWrappers& secondaryWrappers() noexcept {
    static auto* instance = new SecondaryWrappers();
    return *instance;
}

// Gives back a retained reference that no wrapper took.
static void dropExternalRCRef(kotlin::mm::RawExternalRCRef* ref) noexcept {
    kotlin::mm::releaseExternalRCRef(ref);
    kotlin::mm::disposeExternalRCRef(ref);
}

}

// Note: `KotlinBase`'s `toKotlin` and `_tryRetain` methods will terminate if
// called with non-frozen object on a wrong worker. `retain` will also terminate
// in these conditions if backref's refCount is zero.

@implementation KotlinBase {
  // `SecondaryRef` is set once in `constructWithExternalRCRefUnsafe:options:`, before the wrapper is visible to anyone.
  std::variant<RegularRef, PermanentRef, SecondaryRef> refHolder;
}

-(KRef)toKotlin:(KRef*)OBJ_RESULT {
    auto obj = std::visit([](auto&& arg) noexcept -> KRef {
        using T = std::decay_t<decltype(arg)>;
        if constexpr (std::is_same_v<T, PermanentRef>) {
            return arg;
        } else if constexpr (std::is_same_v<T, RegularRef>) {
            return *arg;
        } else if constexpr (std::is_same_v<T, SecondaryRef>) {
            return arg.ref();
        }
    }, refHolder);
    RETURN_OBJ(obj);
}

+(void)load {
  injectToRuntime();
}

+(void)initialize {
  if (kotlin::mm::IsCurrentThreadRegistered()) {
    kotlin::AssertThreadState(kotlin::ThreadState::kNative);
  }
  if (self == [KotlinBase class]) {
    injectToRuntime(); // In case `initialize` is called before `load` (see e.g. https://youtrack.jetbrains.com/issue/KT-50982).
    Kotlin_ObjCExport_initialize();
    initializeAnyMemberReverseAdapters();
  }
  if (kotlin::compiler::swiftExport()) {
      // Swift Export generates types that don't need to be additionally initialized.
      return;
  }
  Kotlin_ObjCExport_initializeClass(self);
}

+(instancetype)allocWithZone:(NSZone*)zone {
  if (kotlin::compiler::swiftExport()) {
      // Swift Export types create Kotlin object themselves and do not dynamically associate
      // Swift type info with Kotlin type info.
      return [super allocWithZone:zone];
  }

  Kotlin_initRuntimeIfNeeded();
  kotlin::ThreadStateGuard guard(kotlin::ThreadState::kRunnable);

  KotlinBase* result = [super allocWithZone:zone];

  const TypeInfo* typeInfo = Kotlin_ObjCExport_getAssociatedTypeInfo(self);
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
  ObjHolder holder;
  auto obj = AllocInstanceWithAssociatedObject(typeInfo, result, holder.slot());
  RuntimeAssert(obj != nullptr, "Allocated null");
  RuntimeAssert(!obj->permanent(), "Allocated permanent object");
  result->refHolder.emplace<RegularRef>(obj);
  return result;
}

+(instancetype)createRetainedWrapper:(ObjHeader*)obj {
  kotlin::AssertThreadState(kotlin::ThreadState::kRunnable);

  if (kotlin::compiler::swiftExport()) {
    void *ref = kotlin::mm::createRetainedExternalRCRef(obj);
    kotlin::ThreadStateGuard guard(kotlin::ThreadState::kNative);
    return [self _createClassWrapperForExternalRCRef:ref];
  }

  KotlinBase* candidate = [super allocWithZone:nil];
  bool permanent = obj->permanent();

  if (!permanent) { // TODO: permanent objects should probably be supported as custom types.
    auto& candidateRegularRef = candidate->refHolder.emplace<RegularRef>(obj);
    if (id old = AtomicCompareAndSwapAssociatedObject(obj, nullptr, candidate)) {
      kotlin::ThreadStateGuard guard(kotlin::ThreadState::kNative);
      candidateRegularRef.release();
      [candidate releaseAsAssociatedObject];
      return objc_retain(old);
    }
  } else {
    candidate->refHolder.emplace<PermanentRef>(obj);
  }

  return candidate;
}

-(instancetype)retain {
    if (auto* ref = std::get_if<RegularRef>(&refHolder)) {
        ref->retain();
    } else {
        [super retain];
    }
    return self;
}

-(BOOL)_tryRetain {
    if (auto* ref = std::get_if<RegularRef>(&refHolder)) {
        return ref->tryRetain();
    } else {
        return [super _tryRetain];
    }
}

-(oneway void)release {
    if (auto* ref = std::get_if<RegularRef>(&refHolder)) {
        ref->release();
    } else {
        [super release];
    }
}

-(void)releaseAsAssociatedObject {
    RuntimeAssert(std::holds_alternative<RegularRef>(refHolder), "Can only be called for regular objects");
    // No need for any special handling. Weak reference handling machinery
    // has already cleaned up the reference to Kotlin object.
    [super release];
}

-(void)dealloc {
    // A secondary wrapper leaves the table that finds it.
    if (std::holds_alternative<SecondaryRef>(refHolder)) {
        secondaryWrappers().remove(self);
    }
    [super dealloc];
}

- (instancetype)copyWithZone:(NSZone *)zone {
  // TODO: write documentation.
  return [self retain];
}

+ (id)_createClassWrapperForExternalRCRef:(void *)ref {
    RuntimeAssert(kotlin::compiler::swiftExport(), "Must be used in Swift Export only");
    kotlin::AssertThreadState(kotlin::ThreadState::kNative);

    auto externalRCRef = static_cast<kotlin::mm::RawExternalRCRef *>(ref);
    Class bestFittingClass = kotlin::swiftExportRuntime::classWrapperFor(kotlin::mm::typeOfExternalRCRef(externalRCRef));

    RuntimeAssert(
            [bestFittingClass isSubclassOfClass:self], "Best-fitting class is %s which is not a subclass of self (%s)",
            class_getName(bestFittingClass), class_getName(self));

    // Call unsafe initializer with the best-fitting class.
    return [[bestFittingClass alloc] initWithExternalRCRefUnsafe:ref options:KotlinBaseConstructionOptionsAsBestFittingWrapper];
}

// `self` is retained for the caller in every branch, and `ref` is consumed.
+ (id)_createTypedWrapperForExternalRCRef:(void *)ref {
    RuntimeAssert(kotlin::compiler::swiftExport(), "Must be used in Swift Export only");
    kotlin::AssertThreadState(kotlin::ThreadState::kNative);

    auto externalRCRef = static_cast<kotlin::mm::RawExternalRCRef *>(ref);

    // The cached wrapper fits when it is of a subclass of the receiver: a Swift subclass, or an exported Kotlin subclass
    // without type parameters.
    KRef object = nullptr;
    id cached = cachedWrapperOf(externalRCRef, &object);
    if (cached != nil && [cached isKindOfClass:self]) {
        id result = [cached retain];
        dropExternalRCRef(externalRCRef);
        return result;
    }

    Class bestFittingClass = kotlin::swiftExportRuntime::classWrapperFor(kotlin::mm::typeOfExternalRCRef(externalRCRef));
    if ([bestFittingClass isSubclassOfClass:self]) {
        // The runtime knows a class for the Kotlin object, and it fits: the same as `_createClassWrapperForExternalRCRef:`.
        return [[bestFittingClass alloc] initWithExternalRCRefUnsafe:ref options:KotlinBaseConstructionOptionsAsBestFittingWrapper];
    }

    // The receiver is a class that the runtime can't find by the Kotlin class: an instantiation of a generic Swift class.
    // The wrapper is a secondary one, and there is one per Kotlin object and class.
    if (id existing = secondaryWrappers().findRetained(object, self)) {
        dropExternalRCRef(externalRCRef);
        return existing;
    }
    return [[self alloc] initWithExternalRCRefUnsafe:ref options:KotlinBaseConstructionOptionsAsExistentialWrapper];
}

+ (id)_createProtocolWrapperForExternalRCRef:(void *)ref conformsTo:(NS_NOESCAPE BOOL (^)(Class))conformsTo {
    RuntimeAssert(kotlin::compiler::swiftExport(), "Must be used in Swift Export only");
    kotlin::AssertThreadState(kotlin::ThreadState::kNative);

    auto externalRCRef = reinterpret_cast<kotlin::mm::RawExternalRCRef*>(ref);

    const TypeInfo *typeInfo = kotlin::mm::typeOfExternalRCRef(externalRCRef);
    Class bestFittingClass = kotlin::swiftExportRuntime::classWrapperFor(typeInfo);

    if (conformsTo != nil && conformsTo(bestFittingClass)) {
        return [[bestFittingClass alloc] initWithExternalRCRefUnsafe:ref options:KotlinBaseConstructionOptionsAsBestFittingWrapper];
    } else {
        Class wrapperClass = kotlin::swiftExportRuntime::protocolWrapperFor(typeInfo);

        KotlinBaseConstructionOptions options = wrapperClass == bestFittingClass ?
            KotlinBaseConstructionOptionsAsBestFittingWrapper : KotlinBaseConstructionOptionsAsExistentialWrapper;

        return [[wrapperClass alloc] initWithExternalRCRefUnsafe:ref options:options];
    }
}

- (instancetype)init {
    if (!kotlin::compiler::swiftExport()) {
        return [super init];
    }

    void* ref = Kotlin_SwiftExport_allocInstanceForSwiftSubclass([self class]);
    return [self constructWithExternalRCRefUnsafe:ref options:KotlinBaseConstructionOptionsAsBoundBridge];
}

/*
 * KotlinBase maintains a 1:1 association between wrapper instances and Kotlin objects for better performance and object identity preservation.
 * However, in rare cases multiple wrapper types may be required for a single Kotlin object, with only the designated "best-fitting" wrapper being cached.
 *
 * Swift Export runtime offers two methods for wrapper class resolution:
 * 1. classWrapperFor(): preserves class hierarchy relationship guarantees
 * 2. protocolWrapperFor(): preserves protocol conformance guarantees
 *
 * We postulate that class hierarchy preservation takes precedence; thus classWrapperFor() output is designated
 * as best-fitting wrapper. Implementation handles instance caching/substitution based on
 * wrapper designation from call sites.
 *
 * @see `_createProtocolWrapperForExternalRCRef`
 * @see `_createClassWrapperForExternalRCRef`
 */
- (instancetype)initWithExternalRCRefUnsafe:(void *)ref options:(KotlinBaseConstructionOptions)options {
    return [self constructWithExternalRCRefUnsafe:ref options:options];
}

- (instancetype)constructWithExternalRCRefUnsafe:(void *)ref options:(KotlinBaseConstructionOptions)options __attribute__((objc_direct)) {
    RuntimeAssert(kotlin::compiler::swiftExport(), "Must be used in Swift Export only");
    kotlin::AssertThreadState(kotlin::ThreadState::kNative);

    auto externalRCRef = static_cast<kotlin::mm::RawExternalRCRef *>(ref);

    BOOL shouldCache = options == KotlinBaseConstructionOptionsAsBestFittingWrapper || options == KotlinBaseConstructionOptionsAsBoundBridge;
    BOOL shouldSubstitute = options == KotlinBaseConstructionOptionsAsBestFittingWrapper;
    BOOL shouldTrapOnSubstitution = options == KotlinBaseConstructionOptionsAsBoundBridge;

    if (auto obj = kotlin::mm::externalRCRefAsPermanentObject(externalRCRef)) {
        refHolder.emplace<PermanentRef>(obj);
        // Cannot attach associated objects to permanent objects.
        return self;
    }

    auto& regularRef = refHolder.emplace<RegularRef>(kotlin::mm::ExternalRCRefImpl::fromRaw(externalRCRef));

    // The Kotlin object, as a key of `secondaryWrappers()` only.
    KRef wrappedObject = nullptr;
    id newSelf = ({
        // TODO: Make it okay to get/replace associated objects w/o runnable state.
        kotlin::CalledFromNativeGuard guard;
        // `ref` holds a strong reference to obj, no need to place obj onto a stack.
        KRef obj = regularRef.ref();
        wrappedObject = obj;

        shouldCache ? AtomicCompareAndSwapAssociatedObject(obj, nullptr, self) : Kotlin_ObjCExport_GetAssociatedObject(obj);
    });

    RuntimeCheck(shouldSubstitute || !shouldTrapOnSubstitution || newSelf == nullptr, "Newly created Kotlin object for bound bridge type should never have an associated object. Please submit a bug report.");

    // For Swift subclasses of exported Kotlin classes: create a TypeInfo with patched vtable
    // so that Kotlin virtual dispatch reaches Swift overrides via reverse bridges.
    if (shouldTrapOnSubstitution) {
        kotlin::CalledFromNativeGuard guard;
        KRef obj = regularRef.ref();
        const TypeInfo* currentTypeInfo = obj->type_info();
        const ObjCTypeAdapter* adapter = kotlin::objCExport(currentTypeInfo).typeAdapter;
        if (adapter != nullptr && adapter->objCName != nullptr) {
            auto patchedTypeInfo = [adapter, currentTypeInfo, self]() noexcept -> const TypeInfo* {
                // Class lookups below may instantiate Swift metadata and trigger +initialize, so they have to happen in the native state.
                kotlin::ThreadStateGuard guard(kotlin::ThreadState::kNative);
                Class boundClass = objc_getClass(adapter->objCName);
                if (boundClass != nil && [self class] != boundClass && [[self class] isSubclassOfClass:boundClass]) {
                    return Kotlin_SwiftExport_getOrCreateTypeInfoForSwiftSubclass([self class], boundClass, currentTypeInfo);
                }
                return nullptr;
            }();
            if (patchedTypeInfo != nullptr && patchedTypeInfo != currentTypeInfo) {
                    kotlin::ThreadStateGuard guard(kotlin::ThreadState::kRunnable);
                    auto* typeInfoSlot = clearPointerBits(obj->typeInfoOrMeta_, OBJECT_TAG_MASK);
                    kotlin::std_support::atomic_ref{typeInfoSlot->typeInfo_}
                        .store(const_cast<TypeInfo*>(patchedTypeInfo), std::memory_order_release);
            }
        }
    }

    if (shouldCache && newSelf == nullptr) {
        // No previous associated object was set: this wrapper is the cached one now.
        return self;
    }

    if (![[newSelf class] isSubclassOfClass:[self class]] || !shouldSubstitute) {
        // This wrapper was not cached, and the cached one, if any, doesn't fit for substitution.
        // It is a secondary wrapper: it keeps the reference that it was given, and Swift counts it as a plain object.
        // Otherwise nothing would ever free it: the Kotlin object frees its cached wrapper only.
        auto* raw = std::get<RegularRef>(refHolder).detach();
        refHolder.emplace<SecondaryRef>(raw);
        // One wrapper per Kotlin object and class, so that one static type gives one Swift object.
        if (id existing = secondaryWrappers().findRetainedOrAdd(wrappedObject, [self class], self)) {
            // A wrapper of this class is alive already. This one goes away with the reference that it keeps.
            [self release];
            return existing;
        }
        return self;
    }

    KotlinBase* retiredSelf = self; // old `self`
    self = [newSelf retain]; // new `self`, retained.

    // Fully release old `self`:
    [retiredSelf release]; // decrement ExternalRCRef refcount,
    [retiredSelf releaseAsAssociatedObject]; // and decrement NSObject refcount.

    // Return new `self`.
    return self;
}

- (void *)externalRCRef {
    auto ref = std::visit([](auto&& arg) noexcept -> kotlin::mm::RawExternalRCRef* {
        using T = std::decay_t<decltype(arg)>;
        if constexpr (std::is_same_v<T, PermanentRef>) {
            return kotlin::mm::permanentObjectAsExternalRCRef(arg);
        } else if constexpr (std::is_same_v<T, RegularRef>) {
            return arg.get()->toRaw();
        } else if constexpr (std::is_same_v<T, SecondaryRef>) {
            return arg.get()->toRaw();
        }
    }, refHolder);
    return static_cast<void *>(ref);
}

- (NSString *)description {
    kotlin::CalledFromNativeGuard guard;
    ObjHolder h1;
    ObjHolder h2;
    KRef obj = [self toKotlin:h1.slot()];
    if (VTableElement impl = nonRecursiveAnyMemberImpl(obj, anyReverseAdapters.toString)) {
        // Matches OBJ_GETTER(Kotlin_toString, KRef): the receiver first, the result slot last.
        using ToString = ObjHeader* (*)(KRef, ObjHeader**);
        return Kotlin_Interop_CreateNSStringFromKString(reinterpret_cast<ToString>(const_cast<void *>(impl))(obj, h2.slot()));
    }
    return Kotlin_Interop_CreateNSStringFromKString(Kotlin_toString(obj, h2.slot()));
}

- (NSUInteger)hash {
    kotlin::CalledFromNativeGuard guard;
    ObjHolder holder;
    KRef obj = [self toKotlin:holder.slot()];
    if (VTableElement impl = nonRecursiveAnyMemberImpl(obj, anyReverseAdapters.hashCode)) {
        using HashCode = KInt (*)(KRef);
        return (NSUInteger)reinterpret_cast<HashCode>(const_cast<void *>(impl))(obj);
    }
    return (NSUInteger)Kotlin_hashCode(obj);
}

- (BOOL)isEqual:(id)other {
    if (self == other) {
        return YES;
    }

    if (other == nil) {
        return NO;
    }

    // All `NSObject`'s, `__SwiftObject`'s and `NSProxy`-ies wrapping them should respond well to `toKotlin:`.
    // However, other system- or user- defined root classes may not.
    // But, at the very least, we expect them to conform to NSObject protocol. There's no test for that.
    if (![other respondsToSelector:Kotlin_ObjCExport_toKotlinSelector]) {
        return NO;
    }

    kotlin::CalledFromNativeGuard guard;
    ObjHolder lhsHolder;
    ObjHolder rhsHolder;
    KRef lhs = [self toKotlin:lhsHolder.slot()];
    KRef rhs = [other toKotlin:rhsHolder.slot()];
    if (VTableElement impl = nonRecursiveAnyMemberImpl(lhs, anyReverseAdapters.equals)) {
        using Equals = KBoolean (*)(KRef, KRef);
        return reinterpret_cast<Equals>(const_cast<void *>(impl))(lhs, rhs);
    }
    return Kotlin_equals(lhs, rhs);
}

@end

@interface NSObject (NSObjectToKotlin)
@end

@implementation NSObject (NSObjectToKotlin)
-(ObjHeader*)toKotlin:(ObjHeader**)OBJ_RESULT {
  RETURN_RESULT_OF(Kotlin_ObjCExport_convertUnmappedObjCObject, self);
}

-(void)releaseAsAssociatedObject {
  objc_release(self);
}
@end

@interface NSString (NSStringToKotlin)
@end

@implementation NSString (NSStringToKotlin)
-(ObjHeader*)toKotlin:(ObjHeader**)OBJ_RESULT {
  RETURN_RESULT_OF(Kotlin_Interop_CreateKStringFromNSString, self);
}
@end

extern "C" {

OBJ_GETTER(Kotlin_boxByte, KByte value);
OBJ_GETTER(Kotlin_boxShort, KShort value);
OBJ_GETTER(Kotlin_boxInt, KInt value);
OBJ_GETTER(Kotlin_boxLong, KLong value);
OBJ_GETTER(Kotlin_boxUByte, KUByte value);
OBJ_GETTER(Kotlin_boxUShort, KUShort value);
OBJ_GETTER(Kotlin_boxUInt, KUInt value);
OBJ_GETTER(Kotlin_boxULong, KULong value);
OBJ_GETTER(Kotlin_boxFloat, KFloat value);
OBJ_GETTER(Kotlin_boxDouble, KDouble value);

}

@interface NSNumber (NSNumberToKotlin)
@end

@implementation NSNumber (NSNumberToKotlin)
-(ObjHeader*)toKotlin:(ObjHeader**)OBJ_RESULT {
  const char* type = self.objCType;

  {
    // Extracting known primitive numbers and boxing them should be fast enough.
    kotlin::CallsCheckerIgnoreGuard guard;

    // TODO: the code below makes some assumption on char, short, int and long sizes.
    switch (type[0]) {
      case 'c': RETURN_RESULT_OF(Kotlin_boxByte, self.charValue);
      case 's': RETURN_RESULT_OF(Kotlin_boxShort, self.shortValue);
      case 'i': RETURN_RESULT_OF(Kotlin_boxInt, self.intValue);
      case 'l': if constexpr (sizeof(long) == 8) {
                  RETURN_RESULT_OF(Kotlin_boxLong, self.longLongValue);
                } else {
                  RETURN_RESULT_OF(Kotlin_boxInt, self.intValue);
                }
      case 'q': RETURN_RESULT_OF(Kotlin_boxLong, self.longLongValue);
      case 'C': RETURN_RESULT_OF(Kotlin_boxUByte, self.unsignedCharValue);
      case 'S': RETURN_RESULT_OF(Kotlin_boxUShort, self.unsignedShortValue);
      case 'I': RETURN_RESULT_OF(Kotlin_boxUInt, self.unsignedIntValue);
      case 'L': if constexpr (sizeof(long) == 8) {
                  RETURN_RESULT_OF(Kotlin_boxULong, self.unsignedLongLongValue);
                } else {
                  RETURN_RESULT_OF(Kotlin_boxUInt, self.unsignedIntValue);
                }
      case 'Q': RETURN_RESULT_OF(Kotlin_boxULong, self.unsignedLongLongValue);
      case 'f': RETURN_RESULT_OF(Kotlin_boxFloat, self.floatValue);
      case 'd': RETURN_RESULT_OF(Kotlin_boxDouble, self.doubleValue);
    }
  }

  RETURN_RESULT_OF(Kotlin_ObjCExport_convertUnmappedObjCObject, self);
}
@end

@interface NSDecimalNumber (NSDecimalNumberToKotlin)
@end

@implementation NSDecimalNumber (NSDecimalNumberToKotlin)
// Overrides [NSNumber toKotlin:] implementation.
-(ObjHeader*)toKotlin:(ObjHeader**)OBJ_RESULT {
  RETURN_RESULT_OF(Kotlin_ObjCExport_convertUnmappedObjCObject, self);
}
@end

static void injectToRuntimeImpl() {
  // If the code below fails, then it is most likely caused by KT-42254.
  constexpr const char* errorMessage = "runtime injected twice; https://youtrack.jetbrains.com/issue/KT-42254 might be related";

  RuntimeCheck(Kotlin_ObjCExport_toKotlinSelector == nullptr, errorMessage);
  Kotlin_ObjCExport_toKotlinSelector = @selector(toKotlin:);

  RuntimeCheck(Kotlin_ObjCExport_releaseAsAssociatedObjectSelector == nullptr, errorMessage);
  Kotlin_ObjCExport_releaseAsAssociatedObjectSelector = @selector(releaseAsAssociatedObject);
}

static void injectToRuntime() {
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    injectToRuntimeImpl();
  });
}

namespace {

static void initializeAnyMemberReverseAdapters() noexcept {
  auto findAnyMemberReverseAdapter = [](SEL selector) noexcept -> const KotlinToObjCMethodAdapter *{
    const ObjCTypeAdapter *anyAdapter = kotlin::objCExport(theAnyTypeInfo).typeAdapter;
    if (anyAdapter == nullptr)
      return nullptr;
    for (int i = 0; i < anyAdapter->reverseAdapterNum; ++i) {
      const KotlinToObjCMethodAdapter *adapter = &anyAdapter->reverseAdapters[i];
      if (adapter->vtableIndex < 0 || adapter->kotlinImpl == nullptr)
        continue;
      if (sel_registerName(adapter->selector) == selector)
        return adapter;
    }
    return nullptr;
  };

  anyReverseAdapters = {
    .toString = findAnyMemberReverseAdapter(@selector(description)),
    .hashCode = findAnyMemberReverseAdapter(@selector(hash)),
    .equals = findAnyMemberReverseAdapter(@selector(isEqual:))
  };
}

VTableElement nonRecursiveAnyMemberImpl(KRef obj, const KotlinToObjCMethodAdapter *adapter) noexcept {
    if (!adapter)
        return nullptr;
    const TypeInfo* typeInfo = obj->type_info();
    const int vtableIndex = adapter->vtableIndex;
    // If the type is `TF_OBJC_DYNAMIC`, it's vtable entry for `Any` methods will point to
    // the reverse-adapter to the corresponding `NSObject` method. When an ObjC class (bound to a Kotlin class) does not
    // override these methods, they end up calling `KotlinBase`. `KotlinBase` now has to tie the recursive knot:
    // it must find the first non-`TF_OBJC_DYNAMIC` superclass and use its implementation; it acts as a sort
    // of a `super` call.
    while ((typeInfo->flags_ & TF_OBJC_DYNAMIC) != 0) {
        RuntimeAssert(typeInfo->superType_ != nullptr, "Type %p is TF_OBJC_DYNAMIC and doesn't have a super type", typeInfo);
        typeInfo = typeInfo->superType_;
    }
    return typeInfo->vtable()[vtableIndex];
}

} // namespace

#endif // KONAN_OBJC_INTEROP
