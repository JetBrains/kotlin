// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: kt55938.def
language = Objective-C
headers = kt55938.h

// FILE: kt55938.h
#import <Foundation/Foundation.h>

@interface ObjCClass : NSObject
+ (int)foo;
@end

// FILE: kt55938.m
#include "kt55938.h"

@implementation ObjCClass {
}
+ (int)foo {
    return 42;
}
@end

// MODULE: lib(cinterop)
// FILE: lib.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package kt55938lib

import kt55938.*
inline fun foo() = ObjCClass.foo()

// MODULE: main(lib, cinterop)
// FILE: main.kt
import kt55938lib.*

fun box(): String {
    val foo = foo()
    if (foo != 42) return "FAIL $foo"
    return "OK"
}
