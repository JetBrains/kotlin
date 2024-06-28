// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false

// MODULE: objcinterop
// FILE: objcinterop.def
language = Objective-C
headers = lib.h

// FILE: lib.h
#import <Foundation/Foundation.h>

@protocol MyProtocol
@end

// MODULE: main(objcinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package mypackage

import kotlin.test.*
import objcinterop.MyProtocolProtocol
import platform.darwin.NSObject

val kclass = MyProtocolProtocol::class

fun box(): String {
    // isInstance
    assertFailsWith<IllegalStateException> {
        kclass.isInstance(object : NSObject(), MyProtocolProtocol {})
    }
    assertFailsWith<IllegalStateException> {
        kclass.isInstance(null)
    }
    assertFailsWith<IllegalStateException> {
        kclass.isInstance(NSObject())
    }
    // names
    assertFailsWith<IllegalStateException> {
        kclass.simpleName
    }
    assertFailsWith<IllegalStateException> {
        kclass.qualifiedName
    }
    assertEquals("unreflected class (KClass for Objective-C protocols is not supported yet)", kclass.toString())
    return "OK"
}