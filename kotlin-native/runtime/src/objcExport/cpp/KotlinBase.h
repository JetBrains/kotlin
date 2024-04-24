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

// Initialize this class with kotlin.native.ref.ExternalRCRef
// Does not retain `ref` itself.
// If `ref` already points to another `KotlinBase` instance,
// this returns that instance.
- (instancetype)initWithExternalRCRef:(uintptr_t)ref NS_REFINED_FOR_SWIFT;

// Return kotlin.native.ref.ExternalRCRef stored in this class
- (uintptr_t)externalRCRef NS_REFINED_FOR_SWIFT;

@end
