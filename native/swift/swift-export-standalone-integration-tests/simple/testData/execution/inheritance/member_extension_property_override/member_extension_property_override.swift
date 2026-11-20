import Inheritance
import Testing

class SwiftLengthView: PropertyAnchor, LengthView {
    var slots: [String: String] = [:]

    func getLen(_ receiver: String) -> Int32 { Int32(receiver.count) * 10 }
    func getSlot(_ receiver: String) -> String { slots[receiver] ?? "swift-none" }
    func setSlot(_ receiver: String, value: String) { slots[receiver] = value }
    func getScoped(_ context: PropCtx, _ receiver: String) -> String { "\(context.tag):\(receiver)" }
}

@Test
func memberExtensionPropertyDispatchesToSwift() throws {
    #expect(readLen(value: SwiftLengthView(), s: "abc") == 30)
}

@Test
func memberExtensionVarDispatchesToSwift() throws {
    let value = SwiftLengthView()
    #expect(readSlot(value: value, s: "k") == "swift-none")
    writeSlot(value: value, s: "k", newValue: "v")
    #expect(value.slots["k"] == "v")
    #expect(readSlot(value: value, s: "k") == "v")
}

@Test
func defaultMemberExtensionPropertyUsesKotlinImplementation() throws {
    #expect(readDoubled(value: SwiftLengthView(), s: "ab") == "abab")
    #expect(SwiftLengthView().getDoubled("ab") == "abab")
}

@Test
func contextualMemberExtensionPropertyDispatchesToSwift() throws {
    #expect(readScoped(value: SwiftLengthView(), s: "s") == "ctx:s")
}

@Test
func memberExtensionPropertyOverloadsDispatchToSwift() throws {
    class SwiftOverloadedView: PropertyAnchor, OverloadedView {
        func getDescribe(_ receiver: Int32) -> String { "int:\(receiver)" }
        func getDescribe(_ receiver: String) -> String { "string:\(receiver)" }
    }
    let value = SwiftOverloadedView()
    #expect(describeInt(value: value, x: 1) == "int:1")
    #expect(describeString(value: value, s: "s") == "string:s")
}

@Test
func contextualPropertyDispatchesToSwift() throws {
    class SwiftContextualLabel: PropertyAnchor, ContextualLabel {
        func getLabel(_ context: PropCtx) -> String { "swift-\(context.tag)" }
    }
    #expect(readLabel(value: SwiftContextualLabel()) == "swift-ctx")
}

@Test
func openMemberExtensionPropertyOverridesDispatchToSwift() throws {
    class SwiftOpenLengthView: OpenLengthView {
        override func getLen(_ receiver: String) -> Int32 { super.getLen(receiver) * 100 }
        override func getSlot(_ receiver: String) -> String { "swift>" + super.getSlot(receiver) }
        override func setSlot(_ receiver: String, newValue: String) { super.setSlot(receiver, newValue: newValue + "!") }
    }
    let value = SwiftOpenLengthView()
    #expect(readOpenLen(value: value, s: "abc") == 300)
    #expect(readOpenSlot(value: value, s: "k") == "swift>kotlin-none")
    writeOpenSlot(value: value, s: "k", newValue: "v")
    #expect(readOpenSlot(value: value, s: "k") == "swift>v!")

    let plain = OpenLengthView()
    #expect(readOpenLen(value: plain, s: "abc") == 3)
    writeOpenSlot(value: plain, s: "k", newValue: "v")
    #expect(readOpenSlot(value: plain, s: "k") == "v")
}
