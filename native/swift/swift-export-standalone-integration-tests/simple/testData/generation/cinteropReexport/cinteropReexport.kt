// KIND: STANDALONE
// WITH_PLATFORM_LIBS
// APPLE_ONLY_VALIDATION

// MODULE: fooKitInterop

// FILE: fooKitInterop.def
language = Objective-C
modules = FooKit BarKit
package = foo

// FILE: Foo.h
#import <Foundation/Foundation.h>

@interface Foo : NSObject
- (int)someValue;
@end

@protocol Zar
- (int)barValue;
@end

// FILE: Bar.h
#import <Foundation/Foundation.h>

@interface Bar : NSObject
- (int)someValue;
@end

// FILE: module.modulemap
module FooKit {
    header "Foo.h"
    export *
}

module BarKit {
    header "Bar.h"
    export *
}

// MODULE: CinteropReexport(fooKitInterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package main

import foo.Foo
import foo.Bar
import foo.ZarProtocol

fun consumesFoo(x: Foo): Int = 0

fun consumesBar(x: Bar): Int = 0

fun producesFoo(): Foo? = null

// The cinterop names the Objective-C protocol `Zar` as the Kotlin interface `ZarProtocol`; the
// generated Swift must reference it under its original Objective-C name `Zar`.
fun consumesBar(x: ZarProtocol): Int = 0
