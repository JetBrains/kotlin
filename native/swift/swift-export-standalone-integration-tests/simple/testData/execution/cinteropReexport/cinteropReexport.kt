// KIND: STANDALONE
// WITH_PLATFORM_LIBS

// MODULE: fooKitInterop

// FILE: fooKitInterop.def
language = Objective-C
modules = FooKit
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
@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlin.experimental.ExperimentalNativeApi::class,
    kotlin.native.runtime.NativeRuntimeApi::class,
)

import foo.Foo
import foo.BarProtocol
import platform.darwin.NSObject
import kotlin.native.ref.WeakReference

fun payloadTriple(x: Foo): Int = x.payload * 3

// Exercises a re-exported Objective-C protocol: `BarProtocol` (Kotlin) must surface to Swift under
// its original Objective-C name `FooKit.Bar`, otherwise the generated Swift fails to compile.
fun barValuePlusOne(x: BarProtocol): Int = x.barValue() + 1

// ObjC/Swift objects returned as Kotlin `Any` must surface in Swift unchanged instead of being
// wrapped into a KotlinExistential (KT-90924).
fun returnBarAsAny(x: BarProtocol): Any = x

fun returnFooAsAny(x: Foo): Any = x

fun anyIdentity(x: Any): Any = x

// Diagnostic helper for KT-90924: what Kotlin actually receives for various Swift values.
fun describeAny(x: Any): String = x::class.simpleName ?: x::class.toString()

// Leak probe for KT-90924: a value passed through an `Any` parameter must not be pinned by the
// bridge once Swift drops its references.
private var trackedThroughAny: WeakReference<Any>? = null

fun trackThroughAny(x: Any): Int {
    trackedThroughAny = WeakReference(x)
    return 0
}

fun isTrackedThroughAnyAlive(): Boolean {
    repeat(3) { kotlin.native.runtime.GC.collect() }
    return trackedThroughAny?.get() != null
}

// A Kotlin implementation of a re-exported ObjC protocol: unlike a foreign ObjC/Swift object, it
// must keep surfacing through the Kotlin wrapper machinery (as a KotlinExistential), not as the
// raw registered ObjC object (KT-90924).
private class KotlinBar : NSObject(), BarProtocol {
    override fun barValue(): Int = 42
}

fun returnKotlinBarAsAny(): Any = KotlinBar()
