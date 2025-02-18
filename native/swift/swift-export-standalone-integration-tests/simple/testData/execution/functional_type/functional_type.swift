import FunctionalType

func testCallingClosureReceivedFromKotlin() throws {
    let block = produceClosureIncrementingI()
    try assertEquals(actual: read(), expected: 0)
    block()
    try assertEquals(actual: read(), expected: 1)
    block()
    try assertEquals(actual: read(), expected: 2)
}

func testCallingClosureSentToKotlin() throws {
    var i: Int = 0
    foo_consume_simple {
      i += 1
    }
    try assertEquals(actual: i, expected: 0)
    call_consumed_simple_block()
    try assertEquals(actual: i, expected: 1)
    call_consumed_simple_block()
    try assertEquals(actual: i, expected: 2)
}

func testSavedSwiftBlockOnKotlinSide() throws {
    var i: Int = 0
    closure_property = {
      i += 2
    }
    try assertEquals(actual: i, expected: 0)
    call_saved_closure()
    try assertEquals(actual: i, expected: 2)
    closure_property()
    try assertEquals(actual: i, expected: 4)
}

func testFunctionalTypeTypealias() throws {
    var i: Int = 0
    callback_property = {
      i += 2
    }
    try assertEquals(actual: i, expected: 0)
    call_saved_callback()
    try assertEquals(actual: i, expected: 2)
    callback_property()
    try assertEquals(actual: i, expected: 4)
}

class Functional_typeTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "testCallingClosureReceivedFromKotlin", method: withAutorelease(testCallingClosureReceivedFromKotlin)),
            TestCase(name: "testCallingClosureSentToKotlin", method: withAutorelease(testCallingClosureSentToKotlin)),
            TestCase(name: "testSavedSwiftBlockOnKotlinSide", method: withAutorelease(testSavedSwiftBlockOnKotlinSide)),
            TestCase(name: "testFunctionalTypeTypealias", method: withAutorelease(testFunctionalTypeTypealias)),
        ]
    }
}
