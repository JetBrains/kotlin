import KotlinRuntime
import SealedValueBridging
import Testing

private func describe(_ value: any Shape) -> String {
    switch value.sealedType() {
        case let .circle(type): "circle: \(type.value)"
        case let .oval(type): "oval: \(type.value)"
    }
}

@Test
func testDispatchOnAReturnedValue() throws {
    try #require(describe(asReturn()) == "circle: Circle")
}

@Test
func testDispatchOnAnUnwrappedNullableReturn() throws {
    try #require(asNullableReturn(empty: true) == nil)
    try #require(describe(try #require(asNullableReturn(empty: false))) == "oval: Oval")
}

@Test
func testDispatchOnAListElement() throws {
    try #require(asListReturn().map(describe) == ["circle: Circle", "oval: Oval"])
}

@Test
func testDispatchOnAMapValue() throws {
    let map = asMapValueReturn()
    try #require(describe(try #require(map["circle"])) == "circle: Circle")
    try #require(describe(try #require(map["oval"])) == "oval: Oval")
}

@Test
func testDispatchOnACallbackArgument() throws {
    // The wrapper is built at the callback boundary rather than on return.
    try #require(asCallbackArgument { describe($0) } == "oval: Oval")
}

@Test
func testDispatchOnAStoredProperty() throws {
    let holder = Holder()
    try #require(describe(holder.state) == "circle: Circle")
    holder.state = Oval()
    try #require(describe(holder.state) == "oval: Oval")
}
