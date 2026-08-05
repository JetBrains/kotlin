import Inheritance
import Testing

@Test
func kotlinPassesSwiftBackedObjectIntoSwiftOverride() throws {
    final class SwiftCargo: Cargo {
        override func label() -> String { "swift-cargo" }
    }
    class SwiftHandler: Handler {
        var received: Cargo? = nil
        override func handle(cargo: Cargo) -> String {
            received = cargo
            return "swift-handled"
        }
    }

    let handler = SwiftHandler()
    let cargo = SwiftCargo()

    #expect(callHandle(handler: handler, cargo: cargo) == "swift-handled")
    // The argument has to arrive as the same Swift instance, not as a fresh wrapper over its Kotlin half.
    #expect(handler.received === cargo)
    #expect(handler.received is SwiftCargo)

    // A purely Kotlin argument reaches the same override with no identity to carry across.
    #expect(callHandleWithKotlinCargo(handler: handler) == "swift-handled")
    #expect((handler.received is SwiftCargo) == false)
}

@Test
func swiftOverrideCallsBackIntoSwiftBackedParameter() throws {
    final class SwiftCargo: Cargo {
        override func label() -> String { "swift-cargo" }
    }
    class SwiftHandler: Handler {
        override func handle(cargo: Cargo) -> String { "swift:\(cargo.label())" }
    }

    // A virtual call on the argument, made from inside the reverse bridge, must resolve to the argument's own
    // Swift override and not to Cargo's Kotlin body.
    #expect(callHandle(handler: SwiftHandler(), cargo: SwiftCargo()) == "swift:swift-cargo")
    #expect(callHandle(handler: SwiftHandler(), cargo: Cargo()) == "swift:kotlin-cargo")
    // Control: Kotlin's own body sees the very same argument the very same way.
    #expect(callHandle(handler: Handler(), cargo: SwiftCargo()) == "kotlin-handled:swift-cargo")
}

@Test
func swiftBackedParameterSurvivesKotlinStorageBeforeReachingOverride() throws {
    final class SwiftCargo: Cargo {
        override func label() -> String { "boxed-swift-cargo" }
    }
    class SwiftHandler: Handler {
        var received: Cargo? = nil
        override func handle(cargo: Cargo) -> String {
            received = cargo
            return "boxed:\(cargo.label())"
        }
    }

    let handler = SwiftHandler()
    let cargo = SwiftCargo()
    let box = CargoBox(cargo: cargo)

    #expect(callHandleFromBox(handler: handler, box: box) == "boxed:boxed-swift-cargo")
    #expect(handler.received === cargo)
    #expect(box.cargo === cargo)
}
