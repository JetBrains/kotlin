import FunctionalType
import ref_types
import primitive_types
import collection_types
import data

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

func testBlockWithRefType() throws {
    var lastB: Bar? = nil
    var receivedB: Bar? = nil
    saveRefBlock { b in
        lastB = b
        return Bar(i: b.i+1)
    }

    receivedB = callRefBlock(with: Bar(i: 0))
    try assertEquals(actual: receivedB!.i, expected: 1)
    try assertEquals(actual: lastB!.i, expected: 0)
}

func testBlockWithPrimType() throws {
    var last: Int8? = nil
    var received: Int8? = nil
    savePrimBlock { byte in
        last = byte
        return byte+1
    }

    received = callPrimBlock(with: 0)
    try assertEquals(actual: received, expected: 1)
    try assertEquals(actual: last, expected: 0)
}

func testBlockWithListType() throws {
    var last: [Int32] = []
    var received: [Int32] = []
    saveListBlock { it in
        last = it
        return it.reversed()
    }

    received = callListBlock(with: [1, 2, 3])
    try assertEquals(actual: received, expected: [3, 2, 1])
    try assertEquals(actual: last, expected: [1, 2, 3])
}

class Functional_typeTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "testCallingClosureReceivedFromKotlin", method: withAutorelease(testCallingClosureReceivedFromKotlin)),
            TestCase(name: "testCallingClosureSentToKotlin", method: withAutorelease(testCallingClosureSentToKotlin)),
            TestCase(name: "testSavedSwiftBlockOnKotlinSide", method: withAutorelease(testSavedSwiftBlockOnKotlinSide)),
            TestCase(name: "testBlockWithRefType", method: withAutorelease(testBlockWithRefType)),
            TestCase(name: "testBlockWithPrimType", method: withAutorelease(testBlockWithPrimType)),
            TestCase(name: "testBlockWithListType", method: withAutorelease(testBlockWithListType)),
        ]
    }
}
