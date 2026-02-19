// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=false
// MODULE: cinterop
// FILE: kt57640.def
language = Objective-C
headers = kt57640.h

// FILE: kt57640.h
#import <Foundation/NSObject.h>

@interface Base2 : NSObject
@property (readwrite) Base2* delegate;
@end

@protocol Foo2
@property (readwrite) id<Foo2> delegate;
@end

@protocol Bar2
@property (readwrite) id<Bar2> delegate;
@end

@interface Derived2 : Base2<Bar2, Foo2>
// This interface does not have re-declaration of property `delegate`.
// Return type of getter `delegate()` and param type of setter `setDelegate()` are:
//   the type of property defined in the first mentioned protocol (id<Bar>), which is incompatible with property type.
@end

@interface DerivedWithPropertyOverride2 : Base2<Bar2, Foo2>
// This interface does not have re-declaration of property `delegate`.
// Return type of getter `delegate()` and param type of setter `setDelegate()` are `DerivedWithPropertyOverride*`
@property (readwrite) DerivedWithPropertyOverride2* delegate;
@end

// FILE: kt57640.m
#import "kt57640.h"

@implementation Base2
@end

@implementation Derived2
@end

@implementation DerivedWithPropertyOverride2
@end

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import kt57640.*
import kotlin.test.*

/**
 * class KotlinInterfaceDerived would cause errors, this deserves a diagnostic test
 * error: class 'KotlinInterfaceDerived' must override 'delegate' because it inherits multiple implementations for it.
 * error: 'delegate' clashes with 'delegate': return types are incompatible.
 * error: 'delegate' clashes with 'delegate': property types do not match.
 */
//class KotlinInterfaceDerived: Base(), FooProtocol, BarProtocol

fun box(): String {
    testAssignmentBaseToDerived()

    testAssignmentBaseToDerivedWithPropertyOverride()

    return "OK"
}

private fun testAssignmentBaseToDerived() {
    val derived = Derived2()
    val base = Base2()
    derived.delegate = base

    val delegate16_Bar: Bar2Protocol? = derived.delegate()
    val delegate16_Any: Any? = delegate16_Bar
    assertTrue(delegate16_Bar is Base2)
    assertTrue(delegate16_Any is Base2)
    assertFalse(delegate16_Bar is Foo2Protocol)
    assertFalse(delegate16_Any is Foo2Protocol)
// Behavior of next "is" check depends on existence of static type analysis: static analysis calculates "is" result as "true",
// while at run-time, the type is Base, which isn't BarProtocol, and the "is" result is "false"
//    assertFalse(delegate16_Bar is BarProtocol)
    assertFalse(delegate16_Any is Bar2Protocol)  // actual type is Base, which isn't BarProtocol
    assertFalse(delegate16_Bar is Derived2)
    assertFalse(delegate16_Any is Derived2)
    assertEquals(base, delegate16_Bar)
    assertEquals(base, delegate16_Any)

    val delegate14_Foo: Foo2Protocol? = (derived as Foo2Protocol).delegate
    val delegate14_Any: Any? = delegate14_Foo
    assertTrue(delegate14_Foo is Base2)
    assertTrue(delegate14_Any is Base2)
// Behavior of next "is" check depends on existence of static type analysis: static analysis calculates "is" result as "true",
// while at run-time, the type is Base, which isn't FooProtocol, and the "is" result is "false"
//    assertFalse(delegate14_Foo is FooProtocol)
    assertFalse(delegate14_Any is Foo2Protocol) // actual type is Base, which isn't FooProtocol
    assertFalse(delegate14_Foo is Bar2Protocol)
    assertFalse(delegate14_Any is Bar2Protocol)
    assertFalse(delegate14_Foo is Derived2)
    assertEquals(base, delegate14_Foo as Base2)
    assertEquals(base, delegate14_Any as Base2)

    val delegate15_Bar: Bar2Protocol? = (derived as Bar2Protocol).delegate
    val delegate15_Any: Any? = delegate15_Bar
    assertTrue(delegate15_Bar is Base2)
    assertTrue(delegate15_Any is Base2)
    assertFalse(delegate15_Bar is Foo2Protocol)
    assertFalse(delegate15_Any is Foo2Protocol)
// Behavior of next "is" check depends on existence of static type analysis: static analysis calculates "is" result as "true",
// while at run-time, the type is Base, which isn't BarProtocol, and the "is" result is "false"
//    assertFalse(delegate15_Bar is BarProtocol)
    assertFalse(delegate15_Any is Bar2Protocol) // actual type is Base, which isn't BarProtocol
    assertFalse(delegate15_Bar is Derived2)
    assertFalse(delegate15_Any is Derived2)
    assertEquals(base, delegate15_Bar as Base2)
    assertEquals(base, delegate15_Any as Base2)
}

private fun testAssignmentBaseToDerivedWithPropertyOverride() {
    val derived = DerivedWithPropertyOverride2()
    val base = Base2()
    derived.delegate = base

    val delegate19_DerivedWithPropertyOverride: DerivedWithPropertyOverride2? = derived.delegate()
    assertTrue(delegate19_DerivedWithPropertyOverride is Base2)
    assertTrue(delegate19_DerivedWithPropertyOverride is Foo2Protocol)
    assertTrue(delegate19_DerivedWithPropertyOverride is Bar2Protocol)
    assertTrue(delegate19_DerivedWithPropertyOverride is DerivedWithPropertyOverride2)
    assertEquals(base, delegate19_DerivedWithPropertyOverride)

    val delegate14_Foo: Foo2Protocol? = (derived as Foo2Protocol).delegate
    assertTrue(delegate14_Foo is Base2)
// Behavior of next "is" check depends on existence of static type analysis: static analysis calculates "is" result as "true",
// while at run-time, the type is Base, which isn't FooProtocol, and the "is" result is "false"
//    assertFalse(delegate14_Foo is FooProtocol)
    assertFalse(delegate14_Foo is Bar2Protocol)
    assertFalse(delegate14_Foo is DerivedWithPropertyOverride2)
    assertEquals(base, delegate14_Foo)
    val delegate18_Foo: Foo2Protocol? = (derived as Foo2Protocol).delegate() // static type is FooProtocol?, not DerivedWithPropertyOverride?, this deserves a diagnostic test
    assertTrue(delegate18_Foo is Base2)
// Behavior of next "is" check depends on existence of static type analysis: static analysis calculates "is" result as "true",
// while at run-time, the type is Base, which isn't FooProtocol, and the "is" result is "false"
//    assertFalse(delegate18_Foo is FooProtocol)
    assertFalse(delegate18_Foo is Bar2Protocol)
    assertFalse(delegate18_Foo is DerivedWithPropertyOverride2)
    assertEquals(base, delegate18_Foo)

    val delegate15_Bar: Bar2Protocol? = (derived as Bar2Protocol).delegate
    assertTrue(delegate15_Bar is Base2)
    assertFalse(delegate15_Bar is Foo2Protocol)
// Behavior of next "is" check depends on existence of static type analysis: static analysis calculates "is" result as "true",
// while at run-time, the type is Base, which isn't BarProtocol, and the "is" result is "false"
//    assertFalse(delegate15_Bar is BarProtocol)
    assertFalse(delegate15_Bar is DerivedWithPropertyOverride2)
    assertEquals(base, delegate15_Bar)
    val delegate19_Bar: Bar2Protocol? = (derived as Bar2Protocol).delegate() // static type is BarProtocol?, not DerivedWithPropertyOverride?, this deserves a diagnostic test
    assertTrue(delegate19_Bar is Base2)
    assertFalse(delegate19_Bar is Foo2Protocol)
// Behavior of next "is" check depends on existence of static type analysis: static analysis calculates "is" result as "true",
// while at run-time, the type is Base, which isn't BarProtocol, and the "is" result is "false"
//    assertFalse(delegate19_Bar is BarProtocol)
    assertFalse(delegate19_Bar is DerivedWithPropertyOverride2)
    assertEquals(base, delegate19_Bar)
}
