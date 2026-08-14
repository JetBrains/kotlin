import Main
import Testing

// Overloads differing in arity.
@Test
func swiftOverridesEachSuspendOverloadSeparately() async throws {
    class SwiftOverloads: AsyncOverloads {
        override func pick(arg1: String) async throws -> String { "swift-pick(\(arg1))" }
        override func pick(arg1: String, arg2: Int32) async throws -> String { "swift-pick(\(arg1), \(arg2))" }
    }

    let o = SwiftOverloads()
    #expect(try await callPick1(o: o, arg1: "a") == "swift-pick(a)")
    #expect(try await callPick2(o: o, arg1: "b", arg2: 1) == "swift-pick(b, 1)")

    // The plain Kotlin class keeps both Kotlin bodies.
    let k = AsyncOverloads()
    #expect(try await callPick1(o: k, arg1: "a") == "kotlin-pick(a)")
    #expect(try await callPick2(o: k, arg1: "b", arg2: 1) == "kotlin-pick(b, 1)")
}

// Overloads of the same arity, told apart by parameter type only
@Test
func swiftOverridesSuspendOverloadsDistinguishedByParameterType() async throws {
    class SwiftOverloads: AsyncOverloads {
        override func same(arg: String) async throws -> String { "swift-same-string(\(arg))" }
        override func same(arg: Int32) async throws -> String { "swift-same-int(\(arg))" }
    }

    let o = SwiftOverloads()
    #expect(try await callSameString(o: o, arg: "s") == "swift-same-string(s)")
    #expect(try await callSameInt(o: o, arg: 7) == "swift-same-int(7)")
}
