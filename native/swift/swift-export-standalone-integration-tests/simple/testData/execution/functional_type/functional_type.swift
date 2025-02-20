import FunctionalType
import Testing

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
