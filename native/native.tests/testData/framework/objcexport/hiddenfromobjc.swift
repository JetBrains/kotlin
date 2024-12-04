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
            actual: HiddenfromobjcKt.useUnavailable(a: HiddenfromobjcKt.createUnavailableInterface()),
            expected: "I'm actually unavailable, call me later."
    )

    let wrapper = WrapperOverUnavailable(arg: HiddenfromobjcKt.createUnavailableInterface() as AnyObject)
    try assertEquals(
                actual: HiddenfromobjcKt.useUnavailable(a: wrapper.arg),
                expected: "I'm actually unavailable, call me later."
        )
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
    }
}