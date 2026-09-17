/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

#import "ObjCLeaf.h"

@implementation ObjCLeaf

- (instancetype)init {
    self = [super init];
    if (self != nil) {
        _label = @"objc-leaf";
    }
    return self;
}

@end

@implementation ObjCDeeperLeaf

- (instancetype)init {
    self = [super init];
    if (self != nil) {
        self.label = @"objc-deeper-leaf";
    }
    return self;
}

@end

ObjCLeaf *makeObjCLeafWithNew(void) {
    return [ObjCLeaf new];
}
