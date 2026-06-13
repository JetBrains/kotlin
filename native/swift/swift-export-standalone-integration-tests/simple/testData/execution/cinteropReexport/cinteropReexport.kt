// KIND: STANDALONE
// WITH_PLATFORM_LIBS

// MODULE: fooKitInterop
// SWIFT_EXPORT_CONFIG: reexportAsObjCModule=FooKit

// FILE: fooKitInterop.def
language = Objective-C
headers = Foo.h
headerFilter = Foo.h
package = foo

// FILE: Foo.h
#import <Foundation/Foundation.h>

@interface Foo : NSObject
@property (nonatomic, assign) int payload;
- (instancetype)initWithPayload:(int)payload;
- (int)doubled;
@end

// FILE: Foo.m
#import "Foo.h"

@implementation Foo

- (instancetype)initWithPayload:(int)payload {
    if (self = [super init]) {
        _payload = payload;
    }
    return self;
}

- (int)doubled {
    return self.payload * 2;
}

@end

// FILE: module.modulemap
module FooKit {
    header "Foo.h"
    export *
}

// MODULE: Main(fooKitInterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import foo.Foo

fun payloadTriple(x: Foo): Int = x.payload * 3
