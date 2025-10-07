// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false

// MODULE: objcinterop
// FILE: objcinterop.def
language = Objective-C
headers = lib.h

// FILE: lib.h
#import <Foundation/Foundation.h>

@interface MyClass: NSObject
@end

MyClass* _Nonnull newMyClass();

MyClass* _Nonnull newPrivateMyClass();

// FILE: lib.m
#import "lib.h"

@implementation MyClass
@end

MyClass* newMyClass() {
    return [[MyClass alloc] init];
}

@interface PrivateMyClass : MyClass
@end

@implementation PrivateMyClass
@end

MyClass* newPrivateMyClass() {
    return [[PrivateMyClass alloc] init];
}

// MODULE: main(objcinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package mypackage

import kotlin.reflect.KClass
import kotlin.test.*
import kotlin.text.*
import objcinterop.*
import platform.darwin.NSObject

val kclass = MyClass::class

class KotlinMyClass : MyClass()

inline fun <reified T: Any> getKClass(): KClass<*> = T::class

inline fun <T> assertEquals2Way(lhs: T, rhs: T) {
    assertEquals(lhs, rhs)
    assertEquals(rhs, lhs)
    assertEquals(lhs.hashCode(), rhs.hashCode())
}

inline fun <T> assertNotEquals2Way(lhs: T, rhs: T) {
    assertNotEquals(lhs, rhs)
    assertNotEquals(rhs, lhs)
}

fun box(): String {
    val instance1 = MyClass()
    val instance2 = newMyClass()
    val instance3 = newPrivateMyClass()
    val instance4 = KotlinMyClass()

    val ksubclass = instance3::class
    // Should be KotlinMyClass::class when it's fully supported
    val kotlinKClass = instance4::class

    // Getting KClass
    assertEquals2Way(kclass, getKClass<MyClass>())
    assertEquals2Way(kclass, instance1::class)
    assertEquals2Way(kclass, instance2::class)
    assertNotEquals2Way(kclass, ksubclass)
    assertNotEquals2Way<KClass<*>>(kclass, kotlinKClass)
    assertNotEquals2Way<KClass<*>>(kotlinKClass, ksubclass)

    assertFailsWith<IllegalStateException> {
        KotlinMyClass::class.equals(instance4::class)
    }
    assertNotEquals(KotlinMyClass::class, instance4::class)

    assertFailsWith<IllegalStateException> {
        KotlinMyClass::class.hashCode()
    }

    // isInstance
    assertTrue(kclass.isInstance(instance1))
    assertTrue(kclass.isInstance(instance2))
    assertTrue(kclass.isInstance(instance3))
    assertTrue(kclass.isInstance(instance4))
    assertFalse(kclass.isInstance(null))
    assertFalse(kclass.isInstance(NSObject()))

    assertFalse(ksubclass.isInstance(instance1))
    assertFalse(ksubclass.isInstance(instance2))
    assertTrue(ksubclass.isInstance(instance3))
    assertFalse(ksubclass.isInstance(instance4))
    assertFalse(ksubclass.isInstance(null))
    assertFalse(ksubclass.isInstance(NSObject()))

    assertFalse(kotlinKClass.isInstance(instance1))
    assertFalse(kotlinKClass.isInstance(instance2))
    assertFalse(kotlinKClass.isInstance(instance3))
    assertTrue(kotlinKClass.isInstance(instance4))
    assertFalse(kotlinKClass.isInstance(null))
    assertFalse(kotlinKClass.isInstance(NSObject()))

    // names
    assertEquals("MyClass", kclass.simpleName)
    assertNull(kclass.qualifiedName)
    assertEquals("class MyClass", kclass.toString())

    assertEquals("PrivateMyClass", ksubclass.simpleName)
    assertNull(ksubclass.qualifiedName)
    assertEquals("class PrivateMyClass", ksubclass.toString())

    assertEquals("KotlinMyClass0", kotlinKClass.simpleName)
    assertNull(kotlinKClass.qualifiedName)
    assertTrue(kotlinKClass.toString().matches("class .*\\.mypackage\\.KotlinMyClass0".toRegex()))

    return "OK"
}