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

func testProducingBlockWithRefType() throws {
    let block = produce_func_with_ref_id()
    let f = Foo(i: 0)

    try assertEquals(actual: f.i, expected: 0)
    var received = block(f)

    try assertEquals(actual: f, expected: received)
    try assertEquals(actual: f.i, expected: 2)
    try assertEquals(actual: received.i, expected: 2)

    received = produce_func_with_ref_id()(f)

    try assertEquals(actual: f, expected: received)
    try assertEquals(actual: f.i, expected: 4)
    try assertEquals(actual: received.i, expected: 4)

    let zip = produce_func_with_ref_zip()
    let b = zip(f, Foo(i: 1))
    try assertEquals(actual: b.left.i, expected: 4)
    try assertEquals(actual: b.right.i, expected: 1)
}

func testConsumingBlockWithRefType() throws {
    let foo = Foo(i: 0)

    save(foo: foo)

    var foo_to_receive: Foo? = nil
    let f = consume_closure_with_ref_id { f in
        foo_to_receive = f
        save(foo: Foo(i: 5))
        return read_foo()
    }

    try assertEquals(actual: foo, expected: foo_to_receive)
    try assertEquals(actual: f.i, expected: 5)
}

func testSavedSwiftBlockWithRefOnKotlinSide() throws {
    var f: Foo = Foo(i: 0)
    refId_closure_property = { f in
        return Foo(i: f.i + 2)
    }
    try assertEquals(actual: f.i, expected: 0)

    var nf = call_saved_refId_closure(f: f)
    try assertEquals(actual: f.i, expected: 0)
    try assertFalse(f === nf)
    try assertEquals(actual: nf.i, expected: 2)

    nf = refId_closure_property(nf)
    try assertEquals(actual: nf.i, expected: 4)
}

class Functional_typeTests : TestProvider {
    var tests: [TestCase] = []

    init() {
        providers.append(self)
        tests = [
            TestCase(name: "testCallingClosureReceivedFromKotlin", method: withAutorelease(testCallingClosureReceivedFromKotlin)),
            TestCase(name: "testCallingClosureSentToKotlin", method: withAutorelease(testCallingClosureSentToKotlin)),
            TestCase(name: "testSavedSwiftBlockOnKotlinSide", method: withAutorelease(testSavedSwiftBlockOnKotlinSide)),
            TestCase(name: "testProducingBlockWithRefType", method: withAutorelease(testProducingBlockWithRefType)),
            TestCase(name: "testConsumingBlockWithRefType", method: withAutorelease(testConsumingBlockWithRefType)),
            TestCase(name: "testSavedSwiftBlockWithRefOnKotlinSide", method: withAutorelease(testSavedSwiftBlockWithRefOnKotlinSide)),
        ]
    }
}
