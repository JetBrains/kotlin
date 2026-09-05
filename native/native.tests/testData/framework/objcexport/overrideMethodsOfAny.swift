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

class OverrideMethodsOfAnyTests : SimpleTestProvider {
    override init() {
        super.init()

        test("TestSwift", testSwift)
        test("TestObjC", testObjC)
        test("TestDescribedByKotlin", testDescribedByKotlin)
    }
}
