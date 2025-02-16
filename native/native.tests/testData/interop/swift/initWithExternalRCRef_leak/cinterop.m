/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
#include "cinterop.h"

Base* preallocated = nil;
bool deallocated = false;

@implementation Base
- (instancetype)initWithExternalRCRef:(uintptr_t)ref {
    return [super initWithExternalRCRef: ref];
}
- (void)dealloc {
    if (preallocated == self) {
        deallocated = true;
    }
}
@end

@implementation Derived
- (instancetype)initWithExternalRCRef:(uintptr_t)ref {
    return [super initWithExternalRCRef: ref];
}
@end

bool test(uintptr_t externalRCRefDerived) {
    preallocated = [Base alloc];
    Base* b = [preallocated initWithExternalRCRef: externalRCRefDerived];
    return deallocated;
}
