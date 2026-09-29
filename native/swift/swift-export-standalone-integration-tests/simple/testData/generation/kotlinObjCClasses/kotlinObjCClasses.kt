// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlinx.cinterop.ExperimentalForeignApi
// WITH_PLATFORM_LIBS
// APPLE_ONLY_VALIDATION

// MODULE: fooKitInterop

// FILE: fooKitInterop.def
language = Objective-C
modules = FooKit
package = foo

// FILE: Foo.h
#import <Foundation/Foundation.h>

@protocol Zar
- (int)barValue;
@end

// FILE: module.modulemap
module FooKit {
    header "Foo.h"
    export *
}

// MODULE: main(fooKitInterop)
// SWIFT_EXPORT_CONFIG: unsupportedDeclarationsReporterKind=inline
// FILE: main.kt
// Kotlin subclasses of Objective-C classes (and thus Kotlin implementations of Objective-C protocols,
// which Kotlin/Native requires to have an Objective-C superclass) are not exported.
import foo.ZarProtocol
import kotlinx.cinterop.CPointer
import platform.Foundation.NSCopyingProtocol
import platform.Foundation.NSZone
import platform.darwin.NSObject

class PlainNSObjectSubclass : NSObject() {
    fun foo(): Int = 42
}

class CopyableImpl : NSObject(), NSCopyingProtocol {
    override fun copyWithZone(zone: CPointer<NSZone>?): Any = this
}

class ZarImpl : NSObject(), ZarProtocol {
    override fun barValue(): Int = 42
}

class RegularKotlinClass

fun producePlain(): PlainNSObjectSubclass = PlainNSObjectSubclass()
fun consumePlain(x: PlainNSObjectSubclass): Unit = Unit
fun produceZarImpl(): ZarImpl = ZarImpl()

// References through the Objective-C protocol types themselves are still exported.
fun produceCopyable(): NSCopyingProtocol = CopyableImpl()
fun consumeCopyable(x: NSCopyingProtocol): Unit = Unit
fun produceZar(): ZarProtocol = ZarImpl()
