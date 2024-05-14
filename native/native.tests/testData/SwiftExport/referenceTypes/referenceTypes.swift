import ReferenceTypes

func ctorFoo() throws {
    let one = Foo(x: 1)
    let two = Foo(x: 2)
    try assertFalse(one === two)
}

class ReferenceTypesTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "ctorFoo", method: withAutorelease(ctorFoo)),
        ]
    }
}