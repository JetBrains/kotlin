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
};

@interface KotlinBase : NSObject <NSCopying>

+ (instancetype)createRetainedWrapper:(struct ObjHeader *)obj;

// Given kotlin.native.internal.ref.ExternalRCRef `ref`:
// * if it's already bound to another `KotlinBase` instance, replaces `self` with that instance
// * otherwise:
//   * find the best-fitting Obj-C class corresponding to `ref`'s Kotlin class
//   * construct its instance and replace `self`
// The code panics if the determined best-fitting class is not a subclass of `self`'s type.
// This situation happens if there's some unexported Swift class inheriting from an exported
// open class: this is not currently supported.
+ (instancetype)createWithExternalRCRef:(void *)ref NS_SWIFT_NAME(__create(externalRCRef:));

+ (KotlinBase *)createExistentialWrappingExternalRCRef:(void *)ref NS_SWIFT_NAME(__createExistential(externalRCRef:));

+ (instancetype)new NS_UNAVAILABLE;
- (instancetype)init NS_UNAVAILABLE;

- (instancetype)initWithExternalRCRefUnsafe:(void *)ref
                                    options:(KotlinBaseConstructionOptions)options NS_DESIGNATED_INITIALIZER NS_REFINED_FOR_SWIFT;

+ (KotlinBase *)createExistentialWrappingExternalRCRef:(void *)ref NS_SWIFT_NAME(__createExistential(externalRCRef:));

+ (instancetype)new NS_UNAVAILABLE;
- (instancetype)init NS_UNAVAILABLE;

- (instancetype)initWithExternalRCRefUnsafe:(void *)ref cache:(BOOL)shouldCache substitute:(BOOL)shouldSubstitute NS_DESIGNATED_INITIALIZER NS_REFINED_FOR_SWIFT;

// Return kotlin.native.internal.ref.ExternalRCRef stored in this class
- (void *)externalRCRef NS_REFINED_FOR_SWIFT NS_RETURNS_INNER_POINTER;

@end
