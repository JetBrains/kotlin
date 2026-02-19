/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
#include "cinterop.h"

int allocatedBase = 0;
int deallocatedBase = 0;
int allocatedDerived = 0;
int deallocatedDerived = 0;

@implementation Base

- (id)alloc {
    allocatedBase += self == Base.self;
    return [super alloc];
}

- (void)dealloc {
    deallocatedBase += self == Base.self;
    [super dealloc];
}

@end

@implementation Derived

- (id)alloc {
    allocatedDerived += self == Derived.self;
    return [super alloc];
}

- (void)dealloc {
    deallocatedDerived += self == Derived.self;
    [super dealloc];
}

@end

int test(uintptr_t externalRCRefDerived) {
    id baseValue = [[[Derived alloc] initWithExternalRCRefUnsafe:(void *)externalRCRefDerived
                                                         options:KotlinBaseConstructionOptionsAsBestFittingWrapper] autorelease];

    if (allocatedBase + deallocatedBase != 0 || allocatedDerived != 1 || deallocatedDerived != 0) {
        return false;
    }

    id derivedValue = [[[Base alloc] initWithExternalRCRefUnsafe:(void *)externalRCRefDerived
                                                         options:KotlinBaseConstructionOptionsAsBestFittingWrapper] autorelease];

    if (allocatedBase != 1 || deallocatedBase != 1 || allocatedDerived != 1 || deallocatedDerived != 0) {
        return false;
    }

    [baseValue release];

    return baseValue == derivedValue;
}
