import Main
import Testing

@Test
func swiftOverridesSuspendMemberExtensionOverloads() async throws {
    class SwiftMemberExtensions: AsyncMemberExtensions {
        override func measure(_ receiver: Int32) async throws -> String { "swift-int(\(receiver))" }
        override func measure(_ receiver: String) async throws -> String { "swift-string(\(receiver))" }
    }

    let o = SwiftMemberExtensions()
    #expect(try await measureInt(o: o, x: 1) == "swift-int(1)")
    #expect(try await measureString(o: o, s: "s") == "swift-string(s)")

    let k = AsyncMemberExtensions()
    #expect(try await measureInt(o: k, x: 1) == "kotlin-int(1)")
    #expect(try await measureString(o: k, s: "s") == "kotlin-string(s)")
}

@Test
func swiftOverridesSuspendFunctionWithContextParameters() async throws {
    class SwiftMemberExtensions: AsyncMemberExtensions {
        override func combine(_ context: (a: CtxA, b: CtxB), _ receiver: String, count: Int32) async throws -> String {
            "swift>" + (try await super.combine(context, receiver, count: count))
        }
        override func tag(_ context: CtxA) async throws -> String { "swift-tag-a(\(context.tag))" }
        override func tag(_ context: CtxB) async throws -> String { "swift-tag-b(\(context.tag))" }
    }

    let o = SwiftMemberExtensions()
    #expect(try await combine(o: o, s: "s", count: 2) == "swift>kotlin-combine(a, b, s, 2)")
    #expect(try await tagA(o: o) == "swift-tag-a(a)")
    #expect(try await tagB(o: o) == "swift-tag-b(b)")

    let k = AsyncMemberExtensions()
    #expect(try await combine(o: k, s: "s", count: 2) == "kotlin-combine(a, b, s, 2)")
}
