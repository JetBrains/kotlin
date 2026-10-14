import Inheritance
import Testing

@Test
func contextualMemberExtensionOverloadsDispatchToSwift() throws {
    class SwiftContextualMeasurer: ContextAnchor, ContextualMeasurer {
        func scaled(_ context: CtxA, _ receiver: Int32) -> String { "\(context.tag):int:\(receiver)" }
        func scaled(_ context: CtxA, _ receiver: String) -> String { "\(context.tag):string:\(receiver)" }
    }
    let value = SwiftContextualMeasurer()
    #expect(scaledInt(value: value, x: 1) == "a:int:1")
    #expect(scaledString(value: value, s: "s") == "a:string:s")
}

@Test
func contextOnlyOverloadsDispatchToSwift() throws {
    class SwiftContextOnly: ContextAnchor, ContextOnlyOverloads {
        func tag(_ context: CtxA) -> String { "swift-\(context.tag)" }
        func tag(_ context: CtxB) -> String { "swift-\(context.tag)" }
    }
    let value = SwiftContextOnly()
    #expect(tagA(value: value) == "swift-a")
    #expect(tagB(value: value) == "swift-b")
}
