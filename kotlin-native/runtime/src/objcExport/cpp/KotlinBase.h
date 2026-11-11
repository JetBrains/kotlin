/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <inttypes.h>
#import <Foundation/Foundation.h>

struct ObjHeader;

typedef NS_ENUM(NSUInteger, KotlinBaseConstructionOptions) {
    KotlinBaseConstructionOptionsAsBestFittingWrapper = 3,
    KotlinBaseConstructionOptionsAsBoundBridge = 7,
    KotlinBaseConstructionOptionsAsExistentialWrapper = 8,
};

NS_ASSUME_NONNULL_BEGIN

@interface KotlinBase : NSObject <NSCopying>

+ (instancetype)createRetainedWrapper:(struct ObjHeader *)obj;

/**
 * Creates and/or returns an instance of the class that is bound to the provided external Kotlin reference (`ref`).
 *
 * This method attempts to ensure that the created instance is a subclass of the receiver class.
 *
 * ### Note
 * - The resolved Objective-C class for the Kotlin class associated with `ref` must be a valid subclass of the current class.
 *   Failure to meet this requirement results in an error.
 * - If an existing wrapper instance already fits the reference (`ref`), it may be used directly instead of creating a new one.
 *
 * @param ref A pointer to an external Kotlin reference (`kotlin.native.internal.ref.ExternalRCRef`)
 * @return A new or reused subclass instance of the wrapper class on which this method is called.
 *
 * @see `+_createProtocolWrapperForExternalRCRef:`
 */
+ (instancetype)_createClassWrapperForExternalRCRef:(void *)ref NS_RETURNS_RETAINED NS_SWIFT_NAME(__createClassWrapper(externalRCRef:));

/**
 * Creates and/or returns a `KotlinBase` instance that acts as an existential wrapper for the provided external Kotlin reference (`ref`).
 *
 * Unlike `+_createClassWrapperForExternalRCRef:`, this method does not necessarily return a subclass of the receiver class.
 * Instead, it may construct or retrieve an appropriate wrapper object for the provided reference that may not adhere to the receiver's type hierarchy.
 *
 * @param ref A pointer to an external Kotlin reference (`kotlin.native.internal.ref.ExternalRCRef`) that this method wraps in a new or existing existential `KotlinBase` instance.
 * @param conformsTo A non-escaping predicate that reports whether a candidate wrapper class conforms to the requested Swift protocol.
 * When the best-fitting class satisfies it, that concrete wrapper is preferred over the existential one. Passing `nil` always selects the existential wrapper.
 * @return A `KotlinBase` instance that serves as a general wrapper for the provided reference.
 *
 * @see `+_createClassWrapperForExternalRCRef:`
 */
+ (KotlinBase *)_createProtocolWrapperForExternalRCRef:(void *)ref
                                            conformsTo:(NS_NOESCAPE BOOL (^ _Nullable)(Class candidate))conformsTo NS_RETURNS_RETAINED NS_SWIFT_NAME(__createProtocolWrapper(externalRCRef:conformsTo:));

/**
 * Creates and/or returns a wrapper for the provided external Kotlin reference (`ref`) that is an instance of the receiver class
 * or of its subclass.
 *
 * Unlike `+_createClassWrapperForExternalRCRef:`, the class that is bound to the Kotlin class of `ref` doesn't have to be
 * a subclass of the receiver. The receiver is a class that the runtime can't find by a Kotlin class: every instantiation
 * of a generic Swift class is a class of its own, and a Kotlin object doesn't know its type arguments.
 *
 * - If the cached wrapper of the Kotlin object is of the receiver class or of its subclass, it is returned.
 * - If the class that is bound to the Kotlin class is a subclass of the receiver, the result is the same as that of
 *   `+_createClassWrapperForExternalRCRef:`.
 * - Otherwise the result is a secondary wrapper: an instance of the receiver class that is not cached in the Kotlin object.
 *   There is one such wrapper per Kotlin object and class while Swift holds it, so `===` holds for one static type.
 *   Two wrappers of one Kotlin object are equal (`isEqual:`).
 *
 * A permanent Kotlin object has no cached wrapper, and it gets a new wrapper on every call.
 *
 * @param ref A pointer to an external Kotlin reference (`kotlin.native.internal.ref.ExternalRCRef`)
 * @return A new or reused instance of the receiver class or of its subclass.
 */
+ (instancetype)_createTypedWrapperForExternalRCRef:(void *)ref NS_RETURNS_RETAINED NS_SWIFT_NAME(__createTypedWrapper(externalRCRef:));

/**
 * Designated initializer creating a fresh Kotlin.Any object.
 */
- (instancetype)init NS_DESIGNATED_INITIALIZER;

/**
 * Designated initializer allowing the creation of a `KotlinBase` instance directly bound to a kotlin object pointed by a
 * provided external reference ( kotlin.native.internal.ref.ExternalRCRef) `ref`.
 *
 * Depending on the provided `options` determining semantics, this initializer may:
 * 1. Cache the current instance as an associated wrapper object for the provided kotlin object external reference (`ref`).
 * 2. Substitute the current instance with a cached wrapper object already associated with the provided reference (`ref`),
 *    if such a wrapper exists and fits the context specified by `options`.
 * 3. Make the current instance a secondary wrapper, if it is not cached: with `KotlinBaseConstructionOptionsAsExistentialWrapper`,
 *    or when another wrapper is cached and doesn't fit. A secondary wrapper is freed when its last reference goes.
 *    If a secondary wrapper of the same class is alive for the Kotlin object, it is returned instead of the current instance.
 *
 * ### Parameters:
 * @param ref A pointer representing a `kotlin.native.internal.ref.ExternalRCRef`, an external Kotlin reference.
 * @param options An instance of `KotlinBaseConstructionOptions` that specifies how the instance should be created.
 *
 */
- (instancetype)initWithExternalRCRefUnsafe:(void *)ref
                                    options:(KotlinBaseConstructionOptions)options NS_DESIGNATED_INITIALIZER NS_REFINED_FOR_SWIFT;

// Return kotlin.native.internal.ref.ExternalRCRef stored in this class
- (void *)externalRCRef NS_REFINED_FOR_SWIFT NS_RETURNS_INNER_POINTER;

@end

NS_ASSUME_NONNULL_END
