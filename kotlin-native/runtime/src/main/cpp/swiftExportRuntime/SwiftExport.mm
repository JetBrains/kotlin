/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "SwiftExport.hpp"

#if KONAN_OBJC_INTEROP

#include "KAssert.h"
#include "Memory.h"
#include "WritableTypeInfo.hpp"
#include "std_support/Atomic.hpp"
#include "Types.h"
#include "ObjCExport.h"
#include "KotlinBase.h"

using namespace kotlin;

extern "C" RUNTIME_WEAK Class Kotlin_SwiftExport_wrapIntoExistential(Class) {
    RuntimeFail("Must only be used with Swift Export; overriden in KotlinRuntimeSupport.swift");
}

namespace {

Class createExistentialWrapperClass(const TypeInfo* typeInfo) noexcept {
    // We create a normal objc class generated on the fly, using objcexport mechanisms
    // It suits us as a viable existential marker, since it conforms to all the necessary protocols
    Class marker = Kotlin_ObjCExport_GetOrCreateObjCClass(typeInfo);
    // function provided by KotlinRuntimeSupport.swift, returns _KotlinExistential<marker>
    Class existential = Kotlin_SwiftExport_wrapIntoExistential(marker);

    RuntimeAssert(existential != nil, "Failed to create existential wrapper for %p", typeInfo);
    return existential;
}

typedef NS_OPTIONS(NSUInteger, WrapperClassOptions) {
WrapperClassOptionBoundBridges = 1 << 0,
    WrapperClassOptionExistentials = 1 << 1,
    WrapperClassOptionsAll = 0b11
};

Class computeWrapperClass(const TypeInfo *&typeInfo, WrapperClassOptions options) noexcept;

Class getOrCreateWrapperClass(const TypeInfo *typeInfo, WrapperClassOptions options) noexcept {
    auto &objCExport = kotlin::objCExport(typeInfo);
    std_support::atomic_ref<Class> clazz(objCExport.swiftClass);

    if (Class matchingClass = clazz.load(std::memory_order_acquire)) {
        // There's already a stored Class, but we need to determine if it fits `options`.
        if (auto *typeAdapter = objCExport.typeAdapter; typeAdapter != nullptr && typeAdapter->objCName != nullptr) {
            // The cached class is bound public wrapper...
            if (options & WrapperClassOptionBoundBridges) {
                // ...which is what we want
                return matchingClass;
            } else {
                // ...but we need an existential, so let's create one
                return createExistentialWrapperClass(typeInfo);
            }
        } else {
            // The cached class is an existential...
            if (options & WrapperClassOptionExistentials) {
                // ...which is what we want
                return matchingClass;
            } else {
                // ...but we need a bound public, so let's try to get one from parent
                auto* superTypeInfo = typeInfo->superType_;
                RuntimeAssert(superTypeInfo != nullptr, "Type %p has no super type", typeInfo);

                return getOrCreateWrapperClass(superTypeInfo, options);
            }
        }
    } else {
        // There's no cached class, let's compute one
        const TypeInfo *fittingTypeInfo = typeInfo;
        Class newlyCreated = computeWrapperClass(fittingTypeInfo, options);

        // If the returned wrapper class was created specifically for this typeInfo...
        if (fittingTypeInfo == typeInfo) {
            // ...We cache `wrapper` directly in `typeInfo`.
            // But don't rewrite it if it's not `nil`, and check that it's the same `Class`.
            Class expected = nil;
            if (!clazz.compare_exchange_strong(expected, newlyCreated, std::memory_order_acq_rel)) {
                RuntimeAssert(expected == newlyCreated, "Trying to store class %p for Kotlin type %p, but it already has %p", newlyCreated, typeInfo, expected);
                return expected;
            }
        }

        return newlyCreated;
    }
}

Class computeWrapperClass(const TypeInfo *&typeInfo, WrapperClassOptions options) noexcept {
    RuntimeAssert(options & WrapperClassOptionsAll, "options argument has to contain at least a single valid option");

    if (typeInfo == theAnyTypeInfo) {
        return nullptr;
    }

    auto& objCExport = kotlin::objCExport(typeInfo);
    Class matchingClass = nullptr;
    const TypeInfo *matchingClassTypeInfo = typeInfo;

    // Attempt to get bound class from the name stored in `typeAdapter`
    if (auto *typeAdapter = objCExport.typeAdapter) {
        if (auto *className = typeAdapter->objCName; className != nullptr && options & WrapperClassOptionBoundBridges) {
            matchingClass = objc_getClass(className);
            matchingClassTypeInfo = typeInfo;
            RuntimeAssert(matchingClass != nil, "Could not find class named %s stored for Kotlin type %p", className, typeInfo);
        }
    }

   // If allowed, attempt to resolve the closest name-bound parent class
    if (matchingClass == nil && (options & (WrapperClassOptionBoundBridges | WrapperClassOptionExistentials)) == WrapperClassOptionBoundBridges) {
        auto* superTypeInfo = typeInfo->superType_;
        RuntimeAssert(superTypeInfo != nullptr, "Type %p has no super type", typeInfo);

        matchingClassTypeInfo = superTypeInfo;
        matchingClass = getOrCreateWrapperClass(matchingClassTypeInfo, WrapperClassOptionBoundBridges);
    }

    // If allowed, we default to creating an existential
    if (matchingClass == nil && options & WrapperClassOptionExistentials) {
        matchingClassTypeInfo = typeInfo;
        matchingClass = createExistentialWrapperClass(typeInfo);
    }

    typeInfo = matchingClassTypeInfo;
    return matchingClass;
}

Class anyWrapperClass() {
    return Kotlin_ObjCExport_GetOrCreateObjCClass(theAnyTypeInfo); // This is KotlinBase, but renamed by our name clash resolution machinery.
}

} // namespace

/**
 * Returns the most appropriate wrapper class reflecting the class hierarchy of the wrapped type.
 *
 * 1. For public exported classes: Returns their generated bound bridge class
 * 2. For private (unexported) classes: Returns the bridge class of their closest public ancestor
 * 3. For private classes without public ancestors (except kotlin.Any): Returns the universal existential wrapper
 * 4. For all other cases: Returns KotlinBase
 */
Class swiftExportRuntime::classWrapperFor(const TypeInfo* typeInfo) noexcept {
    RuntimeAssert(compiler::swiftExport(), "Only available in Swift Export");
    AssertThreadState(ThreadState::kNative); // May take some time.
    Class result = getOrCreateWrapperClass(typeInfo, WrapperClassOptionBoundBridges)
        ?: getOrCreateWrapperClass(typeInfo, WrapperClassOptionExistentials)
        ?: anyWrapperClass();
    return result;
}

/**
 * Returns a wrapper class that represents the interfaces supported by the wrapped class.
 * The selection follows these rules:
 *
 * 1. For public exported classes: Returns their generated bound bridge class
 * 2. For private (unexported) classes: Returns the universal existential wrapper
 * 3. For all other cases: Returns KotlinBase
 */
Class swiftExportRuntime::protocolWrapperFor(const TypeInfo* typeInfo) noexcept {
    RuntimeAssert(compiler::swiftExport(), "Only available in Swift Export");
    AssertThreadState(ThreadState::kNative); // May take some time.
    Class result = getOrCreateWrapperClass(typeInfo, WrapperClassOptionBoundBridges | WrapperClassOptionExistentials) ?: anyWrapperClass();
    return result;
}
#endif
