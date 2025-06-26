import FunctionalType
import Testing
import ref_types
import optional_types
import primitive_types
import collection_types
import data
import receivers

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
    for _ in 0...1_000_000 {
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
}

@Test
func testBlockWithOptRefType() throws {
    var lastB: Bar? = nil
    var receivedB: Bar? = nil
    saveOptRefBlock { b in
        lastB = b
        return b.flatMap { it in Bar(i: it.i+1) }
    }
    receivedB = callOptRefBlock(with: Bar(i: 0))
    try #require(receivedB!.i == 1)
    try #require(lastB!.i == 0)

    receivedB = callOptRefBlock(with: nil)
    try #require(receivedB == nil)
    try #require(lastB == nil)
}

@Test
func testBlockWithOptPrimType() throws {
    var last: Int32? = nil
    var received: Int32? = nil
    saveOptPrimBlock { i in
        last = i
        return i?.advanced(by: 1)
    }
    received = callOptPrimBlock(with: 0)
    try #require(received == 1)
    try #require(last == 0)

    received = callOptPrimBlock(with: nil)
    try #require(received == nil)
    try #require(last == nil)
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

@Test
func testFunctionWithIntReceiver() throws {
    var received: Int32? = nil
    fooReceiverInt { it in
        received = it
    }
    try #require(received == 5)
}

@Test
func testFunctionWithStringReceiver() throws {
    var received: String? = nil
    fooReceiverString { it in
        received = it
    }
    try #require(received == "hello")
}

@Test
func testFunctionWithBarReceiver() throws {
    var received: Bar? = nil
    fooReceiverBar { it in
        received = it
    }
    try #require(received!.i == 5)
}

@Test
func testFunctionWithListReceiver() throws {
    var received: [Int32]? = nil
    fooReceiverList { it in
        received = it
    }
    try #require(received!.count == 3)
    try #require(received == [1, 2, 3])
}
