// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=true
// MODULE: cinterop
// FILE: kt57640.def
language = Objective-C
headers = kt57640.h

// FILE: kt57640.h
#import <Foundation/NSObject.h>

@interface Base3 : NSObject
@property (readwrite) Base3* delegate;
@end

@protocol Foo3
@property (readwrite) id<Foo3> delegate;
@end

@protocol Bar3
@property (readwrite) id<Bar3> delegate;
@end

@interface Derived3 : Base3<Bar3, Foo3>
// This interface does not have re-declaration of property `delegate`.
// Return type of getter `delegate()` and param type of setter `setDelegate()` are:
//   the type of property defined in the first mentioned protocol (id<Bar>), which is incompatible with property type.
@end

@interface DerivedWithPropertyOverride3 : Base3<Bar3, Foo3>
// This interface does not have re-declaration of property `delegate`.
// Return type of getter `delegate()` and param type of setter `setDelegate()` are `DerivedWithPropertyOverride*`
@property (readwrite) DerivedWithPropertyOverride3* delegate;
@end

// FILE: kt57640.m
#import "kt57640.h"

@implementation Base3
@end

@implementation Derived3
@end

@implementation DerivedWithPropertyOverride3
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
    val derived = Derived3()
    val base = Base3()
    derived.delegate = base

    val derivedFoo = derived as Foo3Protocol
    val derivedBar = derived as Bar3Protocol
    assertFailsWith<ClassCastException> { val delegate14_Foo: Foo3Protocol? = derivedFoo.delegate }
    assertFailsWith<ClassCastException> { val delegate15_Bar: Bar3Protocol? = derivedBar.delegate }
    assertFailsWith<ClassCastException> { val delegate16_Bar: Bar3Protocol? = derived.delegate() }
}

private fun testAssignmentBaseToDerivedWithPropertyOverride() {
    val derived = DerivedWithPropertyOverride3()
    val base = Base3()
    derived.delegate = base

    val derived_Foo = derived as Foo3Protocol
    val derivedBar = derived as Bar3Protocol
    assertFailsWith<ClassCastException> { val delegate19_DerivedWithPropertyOverride: DerivedWithPropertyOverride3? = derived.delegate() }
    assertFailsWith<ClassCastException> { val delegate14_Foo: Foo3Protocol? = derived_Foo.delegate }
    assertFailsWith<ClassCastException> { val delegate18_Foo: Foo3Protocol? = derived_Foo.delegate() } // static type is FooProtocol?, not DerivedWithPropertyOverride?, this deserves a diagnostic test
    assertFailsWith<ClassCastException> { val delegate15_Bar: Bar3Protocol? = derivedBar.delegate }
    assertFailsWith<ClassCastException> { val delegate19_Bar: Bar3Protocol? = derivedBar.delegate() } // static type is BarProtocol?, not DerivedWithPropertyOverride?, this deserves a diagnostic test
}
