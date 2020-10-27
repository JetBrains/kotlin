/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import First
import Second

func testClashingNames() throws {
    try assertEquals(actual: "first", expected: First.TestKt.name)
    try assertEquals(actual: "second", expected: Second.TestKt.name)

    let c1 = First.C()
    let c2 = Second.C()
    try assertTrue(type(of: c1) == First.C.self)
    try assertTrue(type(of: c2) == Second.C.self)
    try assertTrue(First.C.self != Second.C.self)
    try assertTrue(objc_getClass(class_getName(First.C.self)) as AnyObject === First.C.self)
    try assertTrue(objc_getClass(class_getName(Second.C.self)) as AnyObject === Second.C.self)
}

extension I1Impl : I2 {}

func testInteraction() throws {
    try assertEquals(actual: SecondKt.getFortyTwoFrom(i2: I1Impl()), expected: 42)
}

func testIsolation1() throws {
    try assertFalse(SecondKt.isUnit(obj: FirstKt.getUnit()))

    // Ensure frameworks don't share the same runtime (state):
    try assertFalse(First.RuntimeState().consumeChange())
    try assertFalse(Second.RuntimeState().consumeChange())
    Second.RuntimeState().produceChange()
    try assertFalse(First.RuntimeState().consumeChange())
    try assertTrue(Second.RuntimeState().consumeChange())
}

func testIsolation2() throws {
    try assertEquals(actual: FirstKt.getI1().getFortyTwo(), expected: 42)
    try assertEquals(actual: SecondKt.getI2().getFortyTwo(), expected: 42)
}

func testIsolation3() throws {
#if false // Disabled for now to avoid depending on platform libs.
    FirstKt.getAnonymousObject()
    SecondKt.getAnonymousObject()
    FirstKt.getNamedObject()
    SecondKt.getNamedObject()
#endif
}

// https://youtrack.jetbrains.com/issue/KT-34261
// When First and Second are static frameworks with caches, this test fails due to bad cache isolation:
// Caches included into both frameworks have 'ktypew' globals (with same name, hidden visibility and common linkage)
// for writable part of this "unexposed stdlib class" TypeInfo.
// ld ignores hidden visibility and merges common globals, so two independent frameworks happen to share
// the same global instead of two different globals. Things go wrong at runtime then: this writable TypeInfo part
// is used to store Obj-C class for this Kotlin class. So after the first object is obtained in Swift, both TypeInfos
// have its class, and the second object is wrong then.
func testIsolation4() throws {
    let obj1: Any = First.SharedKt.getUnexposedStdlibClassInstance()
    try assertTrue(obj1 is First.KotlinBase)
    try assertFalse(obj1 is Second.KotlinBase)

    let obj2: Any = Second.SharedKt.getUnexposedStdlibClassInstance()
    try assertFalse(obj2 is First.KotlinBase)
    try assertTrue(obj2 is Second.KotlinBase)
}

class MultipleTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        tests = [
            TestCase(name: "TestClashingNames", method: withAutorelease(testClashingNames)),
            TestCase(name: "TestInteraction", method: withAutorelease(testInteraction)),
            TestCase(name: "TestIsolation1", method: withAutorelease(testIsolation1)),
            TestCase(name: "TestIsolation2", method: withAutorelease(testIsolation2)),
            TestCase(name: "TestIsolation3", method: withAutorelease(testIsolation3)),
            TestCase(name: "TestIsolation4", method: withAutorelease(testIsolation4)),
        ]
        providers.append(self)
    }

}