/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Kt

private class SwiftOverridingMethodsOfAny : Hashable, Equatable, CustomStringConvertible {
    var hashValue: Int { return 42 }

    static func == (lhs: SwiftOverridingMethodsOfAny, rhs: SwiftOverridingMethodsOfAny) -> Bool {
        return true
    }

    var description: String { return "toString" }
}

private func testSwift() throws {
    try OverrideMethodsOfAnyKt.test(obj: SwiftOverridingMethodsOfAny(), other: SwiftOverridingMethodsOfAny(), swift: true)
}

private class ObjCOverridingMethodsOfAny : NSObject {
    override var hash: Int { return 42 }

    override func isEqual(_ other: Any?) -> Bool {
        return other is ObjCOverridingMethodsOfAny
    }

    override var description: String { return "toString" }
}

private func testObjC() throws {
    try OverrideMethodsOfAnyKt.test(obj: ObjCOverridingMethodsOfAny(), other: ObjCOverridingMethodsOfAny(), swift: false)
}

private class OverridingDescribedByKotlin : DescribedByKotlin {
    override var description: String { return "swift-described" }

    override var hash: Int { return 9 }
}

private class InheritingDescribedByKotlin : DescribedByKotlin {
}

private class CallingSuperDescribedByKotlin : DescribedByKotlin {
    override var description: String { return "swift+" + super.description }

    override var hash: Int { return super.hash }

    override func isEqual(_ other: Any?) -> Bool { return super.isEqual(other) }
}

private func testDescribedByKotlin() throws {
    // Obj-C-level overrides win over the Kotlin implementations they shadow.
    try OverrideMethodsOfAnyKt.testDescribedByKotlin(
        obj: OverridingDescribedByKotlin(), expectedToString: "swift-described", expectedHashCode: 9)

    // Without an override, Kotlin's own implementations must still be reached.
    try OverrideMethodsOfAnyKt.testDescribedByKotlin(
        obj: InheritingDescribedByKotlin(), expectedToString: "kotlin-described", expectedHashCode: 7)

    // `super.description` has to land in Kotlin instead of bouncing back to -description.
    try OverrideMethodsOfAnyKt.testDescribedByKotlin(
        obj: CallingSuperDescribedByKotlin(), expectedToString: "swift+kotlin-described", expectedHashCode: 7)
}

private class CallingSuperMethodsOfAny : InheritingMethodsOfAny {
    override var description: String { return "swift+" + super.description }

    override var hash: Int { return super.hash }

    override func isEqual(_ other: Any?) -> Bool { return super.isEqual(other) }
}

private func testCallingSuperMethodsOfAny() throws {
    let obj = CallingSuperMethodsOfAny()
    let other = CallingSuperMethodsOfAny()

    // `super.description` has to land in kotlin.Any.toString(), which renders
    // "$className@$hashCodeStr" -- the prefix contributes no "@" of its own.
    let rendered = obj.description
    try assertTrue(rendered.hasPrefix("swift+"))
    try assertTrue(rendered.contains(Character("@")), "Unexpected rendering: \(rendered)")

    // Kotlin's toString() routes through -description, so it sees the Obj-C override too.
    try assertEquals(actual: OverrideMethodsOfAnyKt.callToString(obj: obj), expected: rendered)

    // kotlin.Any.toString() calls hashCode() virtually, so the rendering above also went through
    // `super.hash`.
    try assertEquals(actual: Int(OverrideMethodsOfAnyKt.callHashCode(obj: obj)), expected: obj.hash)

    // `super.isEqual:` reaches kotlin.Any's reference equality.
    try assertTrue(obj.isEqual(obj))
    try assertFalse(obj.isEqual(other))
    try assertTrue(OverrideMethodsOfAnyKt.callEquals(obj: obj, other: obj))
    try assertFalse(OverrideMethodsOfAnyKt.callEquals(obj: obj, other: other))
}

class OverrideMethodsOfAnyTests : SimpleTestProvider {
    override init() {
        super.init()

        test("TestSwift", testSwift)
        test("TestObjC", testObjC)
        test("TestDescribedByKotlin", testDescribedByKotlin)
        test("TestCallingSuperMethodsOfAny", testCallingSuperMethodsOfAny)
    }
}
