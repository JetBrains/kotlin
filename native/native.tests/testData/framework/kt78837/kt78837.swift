import Foundation
import Kt78837

class DisplayableBarImpl : DisplayableBar {
    func displayString() -> String { "bar" }
}

func testKt78837() throws {
    try assertTrue(Kt78837Kt.foo(bar: DisplayableBarImpl()))
}

class Kt78837Tests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "Kt78837", method: withAutorelease(testKt78837)),
        ]
    }
}