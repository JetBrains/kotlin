// KIND: STANDALONE
// WITH_PLATFORM_LIBS
// APPLE_ONLY_VALIDATION

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
- (int)someValue;
@end

// FILE: module.modulemap
module FooKit {
    header "Foo.h"
    export *
}

// MODULE: CinteropReexport(fooKitInterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package main

import foo.Foo

fun consumesFoo(x: Foo): Int = 0

fun producesFoo(): Foo? = null
