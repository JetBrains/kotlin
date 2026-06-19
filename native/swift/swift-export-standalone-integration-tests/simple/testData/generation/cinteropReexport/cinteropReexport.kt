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

@protocol Bar
- (int)barValue;
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
import foo.BarProtocol

fun consumesFoo(x: Foo): Int = 0

fun producesFoo(): Foo? = null

// The cinterop names the Objective-C protocol `Bar` as the Kotlin interface `BarProtocol`; the
// generated Swift must reference it under its original Objective-C name `FooKit.Bar`.
fun consumesBar(x: BarProtocol): Int = 0
