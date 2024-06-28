// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false

// MODULE: cinterop
// FILE: lib.def
language = Objective-C
headers = lib.h
headerFilter = lib.h

// FILE: lib.h
#import <Foundation/NSObject.h>
#import <Foundation/NSDate.h>
#import <Foundation/NSUUID.h>

@interface ObjCClass : NSObject
- (NSString*)fooWithArg:(int)arg arg2:(NSString*)arg2;
- (NSString*)fooWithArg:(int)ohNoOtherName name2:(NSString*)name2;
- (NSString*)fooWithArg:(int)arg name3:(NSString*)name3;
@end

// FILE: lib.m
#import "lib.h"

@implementation ObjCClass {
}

- (NSString*)fooWithArg:(int)arg arg2:(NSString*)arg2 {
    return @"A";
}

- (NSString*)fooWithArg:(int)ohNoOtherName name2:(NSString*)name2 {
    return @"B";
}

- (NSString*)fooWithArg:(int)arg name3:(NSString*)name3 {
    return @"C";
}

@end

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

// required for K1
@file:Suppress("CONFLICTING_OVERLOADS", "UNRESOLVED_REFERENCE", "API_NOT_AVAILABLE")

import lib.ObjCClass
import kotlinx.cinterop.ObjCSignatureOverride

class OverrideAll : ObjCClass() {
    @ObjCSignatureOverride
    override fun fooWithArg(arg: Int, arg2: String?) = "D"
    @ObjCSignatureOverride
    override fun fooWithArg(ohNoOtherName: Int, name2: String?) = "E"
    @ObjCSignatureOverride
    override fun fooWithArg(arg: Int, name3: String?) = "F"
}

class OverrideNone : ObjCClass() {
}

class OverrideOne : ObjCClass() {
    override fun fooWithArg(arg: Int, arg2: String?) = "G"
}

class OverrideWithDifferentFirstArgName : ObjCClass() {
    @ObjCSignatureOverride
    override fun fooWithArg(a: Int, arg2: String?) = "H"
    @ObjCSignatureOverride
    override fun fooWithArg(b: Int, name2: String?) = "I"
    @ObjCSignatureOverride
    override fun fooWithArg(c: Int, name3: String?) = "J"
}

fun test(x: ObjCClass, expected: String) {
    val res = x.fooWithArg(arg = 0, arg2 = "") +
            x.fooWithArg(ohNoOtherName = 0, name2="") +
            x.fooWithArg(arg = 0, name3 = "")
    if (res != expected) throw IllegalStateException("Fail ${x::class}: ${res} instead of $expected")
}

fun box(): String {
    test(ObjCClass(), "ABC")
    test(OverrideAll(), "DEF")
    test(OverrideNone(), "ABC")
    test(OverrideOne(), "GBC")
    test(OverrideWithDifferentFirstArgName(), "HIJ")

//  Also test non-virtual calls
    val x1 = OverrideAll()
    val res1 = x1.fooWithArg(arg = 0, arg2 = "") +
            x1.fooWithArg(ohNoOtherName = 0, name2="") +
            x1.fooWithArg(arg = 0, name3 = "")
    if (res1 != "DEF") throw IllegalStateException("Fail OverrideAll non-virtual: ${res1} instead of DEF")

    val x2 = OverrideNone()
    val res2 = x2.fooWithArg(arg = 0, arg2 = "") +
            x2.fooWithArg(ohNoOtherName = 0, name2="") +
            x2.fooWithArg(arg = 0, name3 = "")
    if (res2 != "ABC") throw IllegalStateException("Fail OverrideNone non-virtual: ${res2} instead of ABC")

    return "OK"
}