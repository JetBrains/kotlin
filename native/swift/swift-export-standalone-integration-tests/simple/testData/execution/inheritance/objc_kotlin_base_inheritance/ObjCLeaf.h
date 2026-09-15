/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

#import <Foundation/Foundation.h>
#import "KotlinBase.h"

/// A class declared purely in Objective-C, inheriting `KotlinBase` directly.
@interface ObjCLeaf : KotlinBase
@property (nonatomic, copy) NSString *label;
@end

/// Inherits `ObjCLeaf`, to cover a second Objective-C level below `KotlinBase`.
@interface ObjCDeeperLeaf : ObjCLeaf
@end

/// Constructed with `+new` rather than `alloc`/`init`, which `KotlinBase` no longer declares unavailable.
ObjCLeaf *makeObjCLeafWithNew(void);
