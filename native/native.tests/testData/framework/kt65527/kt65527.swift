import Foundation
import Kt65527

func testKt65527() throws {
    let c = CKt65527()
    try assertTrue(c.prop.boolValue)
    c.prop = false
    try assertFalse(c.prop.boolValue)
}

class Kt65527Tests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "Kt65527", method: withAutorelease(testKt65527)),
        ]
    }
}