/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
#include <inttypes.h>
#import <Foundation/Foundation.h>

struct ObjHeader;

@interface KotlinBase : NSObject <NSCopying>
+ (instancetype)createRetainedWrapper:(struct ObjHeader *)obj;
- (instancetype)initWithExternalRCRef:(uintptr_t)ref NS_REFINED_FOR_SWIFT;
- (uintptr_t)externalRCRef NS_REFINED_FOR_SWIFT;
@end

@interface Base : KotlinBase
- (instancetype)initWithExternalRCRef:(uintptr_t)ref;
@end

@interface Derived : Base
- (instancetype)initWithExternalRCRef:(uintptr_t)ref;
@end

bool test(uintptr_t externalRCRefDerived);
