import Foundation
import SwiftConformanceToInterface

class SwiftCallback : NSObject, Callback {
    func produce() -> String { return "OK" }
}

func testSwiftConformanceToInterface() throws {
    // A Swift class conforming to a Kotlin interface: the call Kotlin makes back into it
    // is dispatched through the itable of a type info built at run time.
    try assertEquals(actual: Caller.shared.useCallback(c: SwiftCallback()), expected: "OK")
}

// -------- Execution of the test --------

class SwiftConformanceToInterfaceTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "SwiftConformanceToInterface", method: withAutorelease(testSwiftConformanceToInterface)),
        ]
    }
}
