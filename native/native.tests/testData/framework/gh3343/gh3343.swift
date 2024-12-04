import Foundation
import Gh3343

func testGh3343() throws {
    let list = Gh3343Kt.run()
    try assertEquals(actual: list[0] as? String, expected: "42")
    try assertEquals(actual: list[1] as? Int, expected: 2)
    try assertEquals(actual: list[2] as? Int, expected: 117)
    try assertEquals(actual: list[3] as? String, expected: "zzz")
}

// -------- Execution of the test --------

class Gh3343Tests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "Gh3343", method: withAutorelease(testGh3343)),
        ]
    }
}