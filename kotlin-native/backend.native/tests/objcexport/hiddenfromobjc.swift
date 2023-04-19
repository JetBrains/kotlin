/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Kt

private func testUnavailableEnum() throws {
    try assertEquals(actual: HiddenfromobjcKt.useOfUnavailableEnum(param: HiddenfromobjcKt.createUnavailableEnum()), expected: "A")
    try assertEquals(actual: HiddenfromobjcKt.useOfNullableUnavailableEnum(param: nil), expected: "null")
    try assertEquals(actual: HiddenfromobjcKt.useOfNullableUnavailableEnum(param: HiddenfromobjcKt.createUnavailableEnum()), expected: "A")
}

private func testUnavailableObject() throws {
    try assertEquals(actual: HiddenfromobjcKt.useOfUnavailableObject(param: HiddenfromobjcKt.getUnavailableObject()), expected: "objectField")
    try assertEquals(actual: HiddenfromobjcKt.useOfNullableUnavailableObject(param: HiddenfromobjcKt.getUnavailableObject()), expected: "objectField")
    try assertEquals(actual: HiddenfromobjcKt.useOfNullableUnavailableObject(param: nil), expected: "null")
}

private func testUnavailableInterface() throws {
    try assertEquals(actual: HiddenfromobjcKt.useOfNullableUnavailableInterface(
            param: HiddenfromobjcKt.createUnavailableInterface()),
            expected: "I'm actually unavailable, call me later."
    )
    try assertEquals(actual: HiddenfromobjcKt.useOfNullableUnavailableInterface(param: nil), expected: "null")
}

private func testSealedClass() throws {
    try assertEquals(actual: HiddenfromobjcKt.useSealedClass(param: HiddenfromobjcKt.createSealedClass()), expected: "A")
    try assertEquals(actual: HiddenfromobjcKt.useSealedClass(param: SealedClass.B()), expected: "B")
}

private func testUnavailableGenerics() throws {
    try assertEquals(
            actual: HiddenfromobjcKt.useUnavailable(a: HiddenfromobjcKt.createChildOfChildOfUnavailableInterface()),
            expected: "f"
    )

    let wrapper = WrapperOverUnavailable(arg: HiddenfromobjcKt.createChildOfChildOfUnavailableInterface() as AnyObject)
    try assertEquals(
                actual: HiddenfromobjcKt.useUnavailable(a: wrapper.arg),
                expected: "f"
        )
}

private func testPartiallyUnavailableHierarchy() throws {
    try assertEquals(
            actual: HiddenfromobjcKt.useOfChildOfChildOfUnavailableInterface(param: HiddenfromobjcKt.createChildOfChildOfUnavailableInterface()),
            expected: "h"
    )
    try assertEquals(actual: HiddenfromobjcKt.useOfNullableUnavailableInterface(
                param: HiddenfromobjcKt.createChildOfChildOfUnavailableInterface()),
                expected: "f"
    )
}

// Ensure that interfaces of partially hidden hierarchy of interfaces
// are exposed correctly to swift.
protocol PhhiF : PhhiA, PhhiD, PhhiE {
}

class PhhiSwiftClass : PhhiF {
    func a() -> String {
        "swift::a"
    }

    func d() -> String {
        "swift::d"
    }

    func e() -> String {
        "swift::e"
    }
}

private func testPartiallyHiddenHierarchyInterfaces() throws {
    let instance = PhhiClass()
    try assertEquals(actual: HiddenfromobjcKt.callA(param: instance), expected: "class::a")
    try assertEquals(actual: HiddenfromobjcKt.callD(param: instance), expected: "class::d")
    try assertEquals(actual: HiddenfromobjcKt.callE(param: instance), expected: "class::e")

    let swiftInstance = PhhiSwiftClass()
    try assertEquals(actual: HiddenfromobjcKt.callA(param: swiftInstance), expected: "swift::a")
    try assertEquals(actual: HiddenfromobjcKt.callD(param: swiftInstance), expected: "swift::d")
    try assertEquals(actual: HiddenfromobjcKt.callE(param: swiftInstance), expected: "swift::e")
}

class HiddenfromobjcTests : SimpleTestProvider {
    override init() {
        super.init()

        // Here we check that even if type is erased from Objective-C declarations, we still able to use them properly with proper objects.
        test("testUnavailableEnum", testUnavailableEnum)
        test("testUnavailableObject", testUnavailableObject)
        test("testUnavailableInterface", testUnavailableInterface)
        test("testSealedClass", testSealedClass)
        test("testUnavailableGenerics", testUnavailableGenerics)
        test("testPartiallyUnavailableHierarchy", testPartiallyUnavailableHierarchy)
    }
}