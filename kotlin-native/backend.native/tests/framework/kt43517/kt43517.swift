import Foundation
import Kt43517

func testKt43517() throws {
    try assertEquals(
            actual: Kt43517Kt.compareEnums(e1: Kt43517Kt.produceEnum(), e2: Kt43517Kt.produceEnum()),
            expected: true
    )
    try assertEquals(
        actual: Kt43517Kt.getFirstField(s: Kt43517Kt.getGlobalS()),
        expected: 3
    )
}

class Kt43517Tests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "Kt43517", method: withAutorelease(testKt43517)),
        ]
    }
}