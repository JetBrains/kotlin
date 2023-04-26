import Foundation
import Kt57791

class FooImpl : Foo {
    func bar() -> String? { "zzz" }
}

func testKt57791() throws {
    try assertTrue(KnlibraryKt.foobar(foo: FooImpl()))
}

class TestTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "Kt57791", method: withAutorelease(testKt57791)),
        ]
    }
}