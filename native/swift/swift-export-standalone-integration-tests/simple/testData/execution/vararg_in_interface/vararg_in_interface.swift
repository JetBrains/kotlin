import VarargInInterface
import Testing

// Implements a Kotlin interface whose method has a vararg parameter; subclasses an exported
// open Kotlin class to satisfy the KotlinBase requirement of the exported protocol.
private final class SwiftDriver: KotlinObject, Driver {
    func addListener(queryKeys: String...) -> String {
        queryKeys.joined(separator: ",")
    }
}

private final class SwiftBase: BaseDriver {
    override func addListener(queryKeys: String...) -> String {
        queryKeys.joined(separator: "+")
    }
}

// Implements an interface with a primitive (Int -> IntArray) vararg method.
private final class SwiftCounter: KotlinObject, Counter {
    func count(values: Int32...) -> Int32 {
        values.reduce(0, +)
    }
}

@Test
func testVarargReverseBridgeInInterface() throws {
    // Kotlin calls addListener on the Swift conformer through the reverse bridge.
    try #require(useDriver(driver: SwiftDriver()) == "a,b,c")
}

@Test
func testVarargReverseBridgeInOpenClass() throws {
    try #require(useBase(base: SwiftBase()) == "x+y")
    try #require(useBase(base: BaseDriver()) == "base:x,y")
}

@Test
func testPrimitiveVarargReverseBridge() throws {
    try #require(useCounter(counter: SwiftCounter()) == 6)
}
