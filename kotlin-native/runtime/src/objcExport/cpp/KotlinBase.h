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
+ (instancetype)_createClassWrapperForExternalRCRef:(void *)ref NS_SWIFT_NAME(__createClassWrapper(externalRCRef:));

/**
 * Creates and/or returns a `KotlinBase` instance that acts as an existential wrapper for the provided external Kotlin reference (`ref`).
 *
 * Unlike `+_createClassWrapperForExternalRCRef:`, this method does not necessarily return a subclass of the receiver class.
 * Instead, it may construct or retrieve an appropriate wrapper object for the provided reference that may not adhere to the receiver's type hierarchy.
 *
 * @param ref A pointer to an external Kotlin reference (`kotlin.native.internal.ref.ExternalRCRef`) that this method wraps in a new or existing existential `KotlinBase` instance.
 * @return A `KotlinBase` instance that serves as a general wrapper for the provided reference.
 *
 * @see `+_createClassWrapperForExternalRCRef:`
 */
+ (KotlinBase *)_createProtocolWrapperForExternalRCRef:(void *)ref NS_SWIFT_NAME(__createProtocolWrapper(externalRCRef:));

+ (instancetype)new NS_UNAVAILABLE;
- (instancetype)init NS_UNAVAILABLE;

/**
 * Designated initializer allowing the creation of a `KotlinBase` instance directly bound to a kotlin object pointed by a
 * provided external reference ( kotlin.native.internal.ref.ExternalRCRef) `ref`.
 *
 * Depending on the provided `options` determining semantics, this initializer may:
 * 1. Cache the current instance as an associated wrapper object for the provided kotlin object external reference (`ref`).
 * 2. Substitute the current instance with a cached wrapper object already associated with the provided reference (`ref`),
 *    if such a wrapper exists and fits the context specified by `options`.
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
