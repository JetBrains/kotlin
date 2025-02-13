import FunctionalType
import Testing
import ref_types
import primitive_types
import collection_types
import data

@Test
func testCallingClosureReceivedFromKotlin() throws {
    let block = produceClosureIncrementingI()
    try #require(read() == 0)
    block()
    try #require(read() == 1)
    block()
    try #require(read() == 2)
}

@Test
func testCallingClosureSentToKotlin() throws {
    var i: Int = 0
    foo_consume_simple {
      i += 1
    }
    try #require(i == 0)
    call_consumed_simple_block()
    try #require(i == 1)
    call_consumed_simple_block()
    try #require(i == 2)
}

@Test
func testSavedSwiftBlockOnKotlinSide() throws {
    var i: Int = 0
    closure_property = {
      i += 2
    }
    try #require(i == 0)
    call_saved_closure()
    try #require(i == 2)
    closure_property()
    try #require(i == 4)
}

@Test
func testFunctionalTypeTypealias() throws {
    var i: Int = 0
    callback_property = {
      i += 2
    }
    try #require(i == 0)
    call_saved_callback()
    try #require(i == 2)
    callback_property()
    try #require(i == 4)
}

@Test
func testBlockWithRefType() throws {
    var lastB: Bar? = nil
    var receivedB: Bar? = nil
    saveRefBlock { b in
        lastB = b
        return Bar(i: b.i+1)
    }
    receivedB = callRefBlock(with: Bar(i: 0))
    try #require(receivedB!.i == 1)
    try #require(lastB!.i == 0)
}

@Test
func testBlockWithPrimType() throws {
    var last: Int8? = nil
    var received: Int8? = nil
    savePrimBlock { byte in
        last = byte
        return byte+1
    }

    received = callPrimBlock(with: 0)
    try #require(received == 1)
    try #require(last == 0)
}

@Test
func testBlockWithListType() throws {
    var last: [Int32] = []
    var received: [Int32] = []
    saveListBlock { it in
        last = it
        return it.reversed()
    }

    received = callListBlock(with: [1, 2, 3])
    try #require(received == [3, 2, 1])
    try #require(last == [1, 2, 3])
}
