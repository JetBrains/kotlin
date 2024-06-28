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

class OverrideMethodsOfAnyTests : SimpleTestProvider {
    override init() {
        super.init()

        test("TestSwift", testSwift)
        test("TestObjC", testObjC)
    }
}
