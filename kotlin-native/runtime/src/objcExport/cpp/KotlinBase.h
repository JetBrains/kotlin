/*
 * Copyright 2010-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

#include <inttypes.h>
#import <Foundation/Foundation.h>

struct ObjHeader;

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
- (instancetype)initWithExternalRCRef:(uintptr_t)ref NS_REFINED_FOR_SWIFT;

// Return kotlin.native.internal.ref.ExternalRCRef stored in this class
- (uintptr_t)externalRCRef NS_REFINED_FOR_SWIFT;

@end
