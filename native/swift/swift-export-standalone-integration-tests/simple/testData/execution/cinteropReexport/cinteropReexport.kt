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

@protocol Bar
- (int)barValue;
@end

@interface Foo : NSObject <Bar>
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

- (int)barValue {
    return self.payload;
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
import foo.BarProtocol

fun payloadTriple(x: Foo): Int = x.payload * 3

// Exercises a re-exported Objective-C protocol: `BarProtocol` (Kotlin) must surface to Swift under
// its original Objective-C name `FooKit.Bar`, otherwise the generated Swift fails to compile.
fun barValuePlusOne(x: BarProtocol): Int = x.barValue() + 1
