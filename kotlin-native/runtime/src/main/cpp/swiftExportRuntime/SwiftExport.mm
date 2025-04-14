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
    Class marker = Kotlin_ObjCExport_GetOrCreateClass(typeInfo);
    // function provided by KotlinRuntimeSupport.swift, returns _KotlinExistential<marker>
    Class existential = Kotlin_SwiftExport_wrapIntoExistential(marker);

    RuntimeAssert(existential != nil, "Failed to create existential wrapper for %p", typeInfo);
    return existential;
}

typedef NS_OPTIONS(NSUInteger, WrapperClassOptions) {
    WrapperClassOptionKotlinAny = 1 << 0,
    WrapperClassOptionBoundBridges = 1 << 1,
    WrapperClassOptionExistentials = 1 << 2,
    WrapperClassOptionsAll = 0b111
};

Class computeWrapperClass(const TypeInfo *&typeInfo, WrapperClassOptions options) noexcept;

Class getOrCreateWrapperClass(const TypeInfo *typeInfo, WrapperClassOptions options) noexcept {
    if (typeInfo == theAnyTypeInfo) {
        // If this is the `Kolin.Any` – we return `KotlinBase`, if it is allowed, or nullptr.
        return options & WrapperClassOptionKotlinAny ? KotlinBase.self : nullptr;
    }

    auto &objCExport = kotlin::objCExport(typeInfo);
    std_support::atomic_ref<Class> clazz(objCExport.swiftClass);

    if (Class bestFitting = clazz.load(std::memory_order_relaxed)) {
        // There's already a stored Class, but we need to determine if it fits `options`.
        if (auto *typeAdapter = objCExport.typeAdapter; typeAdapter != nullptr && typeAdapter->objCName != nullptr) {
            // The cached class is bound public wrapper...
            if (options & WrapperClassOptionBoundBridges) {
                // ...which is what we want

                // We first read it as relaxed, but successful read requires acquire barrier.
                std::atomic_thread_fence(std::memory_order_acquire);
                return bestFitting;
            } else {
                // ...but we need an existential, so let's create one
                return createExistentialWrapperClass(typeInfo);
            }
        } else {
            // The cached class is an existential...
            if (options & WrapperClassOptionExistentials) {
                // ...which is what we want

                // We first read it as relaxed, but successful read requires acquire barrier.
                std::atomic_thread_fence(std::memory_order_acquire);
                return bestFitting;
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
    if (!(options & WrapperClassOptionsAll)) {
        RuntimeAssert(false, "options argument has to contain at least a single valid option");
        return nullptr;
    }

    auto& objCExport = kotlin::objCExport(typeInfo);
    Class bestFitting = nullptr;
    const TypeInfo *bestFittingTypeInfo = typeInfo;

    // Attempt to get bound class from the name stored in `typeAdapter`
    if (auto *typeAdapter = objCExport.typeAdapter) {
        if (auto *className = typeAdapter->objCName; className != nullptr && options & WrapperClassOptionBoundBridges) {
            bestFitting = objc_getClass(className);
            bestFittingTypeInfo = typeInfo;
            RuntimeAssert(bestFitting != nil, "Could not find class named %s stored for Kotlin type %p", className, typeInfo);
        }
    }
    // If this is `Kotlin.Any` – return `KotlinBase` or `nullptr`, depending on `options`
    if (typeInfo == theAnyTypeInfo) {
        return options & WrapperClassOptionKotlinAny ? KotlinBase.self : nullptr;
    }
   // If allowed, attempt to resolve the closest name-bound parent class
    if (bestFitting == nil && (options & (WrapperClassOptionBoundBridges | WrapperClassOptionExistentials)) == WrapperClassOptionBoundBridges) {
        auto* superTypeInfo = typeInfo->superType_;
        RuntimeAssert(superTypeInfo != nullptr, "Type %p has no super type", typeInfo);

        bestFittingTypeInfo = superTypeInfo;
        bestFitting = getOrCreateWrapperClass(bestFittingTypeInfo, WrapperClassOptionBoundBridges);
    }

    // If allowed, we default to creating an existential
    if (bestFitting == nil && options & WrapperClassOptionExistentials) {
        bestFittingTypeInfo = typeInfo;
        bestFitting = createExistentialWrapperClass(typeInfo);
    }

    if (bestFitting == nil && options & WrapperClassOptionKotlinAny) {
        return KotlinBase.self;
    }

    return bestFitting;
}

} // namespace

Class swiftExportRuntime::bestFittingClassFor(const TypeInfo* typeInfo) noexcept {
    RuntimeAssert(compiler::swiftExport(), "Only available in Swift Export");
    AssertThreadState(ThreadState::kNative); // May take some time.
    Class result = getOrCreateWrapperClass(typeInfo, WrapperClassOptionKotlinAny | WrapperClassOptionBoundBridges);
    return result;
}

Class swiftExportRuntime::existentialWrapperClassFor(const TypeInfo* typeInfo) noexcept {
    RuntimeAssert(compiler::swiftExport(), "Only available in Swift Export");
    AssertThreadState(ThreadState::kNative); // May take some time.
    Class result = getOrCreateWrapperClass(typeInfo, WrapperClassOptionBoundBridges | WrapperClassOptionExistentials);
    return result;
}
#endif
