import Inheritance
import KotlinRuntimeSupport
import Testing

@Test
func singleMemberExtensionDispatchesToSwift() throws {
    class SwiftSingle: MemberExtensionAnchor, SingleMemberExtension {
        func bump(_ receiver: Int32) -> Int32 { receiver + 1 }
    }
    #expect(callSingle(value: SwiftSingle(), x: 41) == 42)
}

@Test
func memberExtensionOverloadsByReceiverDispatchToSwift() throws {
    class SwiftMeasurer: MemberExtensionAnchor, Measurer {
        func measure(_ receiver: Int32) -> Int32 { receiver * 2 }
        func measure(_ receiver: String) -> Int32 { Int32(receiver.count) }
    }
    let value = SwiftMeasurer()
    #expect(measureInt(value: value, x: 21) == 42)
    #expect(measureString(value: value, s: "four") == 4)
}

@Test
func genericSaverMemberExtensionDispatchesToSwift() throws {
    class SwiftSaver: MemberExtensionAnchor, Saver {
        func save(_ receiver: any SaverScope, value: (any KotlinRuntimeSupport._KotlinBridgeable)?) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            Payload(text: "saved:\((value as! Payload).text)")
        }
        func restore(value: any KotlinRuntimeSupport._KotlinBridgeable) -> (any KotlinRuntimeSupport._KotlinBridgeable)? { value }
    }
    let saved = saveWith(saver: SwiftSaver(), value: Payload(text: "x")) as! Payload
    #expect(saved.text == "saved:x")
}

@Test
func memberExtensionOverloadsByParameterDispatchToSwift() throws {
    class SwiftCombiner: MemberExtensionAnchor, Combiner {
        func combine(_ receiver: String, other: Int32) -> String { "\(receiver)#\(other)" }
        func combine(_ receiver: String, other: String) -> String { "\(receiver)+\(other)" }
    }
    let value = SwiftCombiner()
    #expect(combineInt(value: value, s: "a", x: 1) == "a#1")
    #expect(combineString(value: value, s: "a", x: "b") == "a+b")
}

@Test
func openClassMemberExtensionOverloadsDispatchToSwift() throws {
    class SwiftOpenMeasurer: OpenMeasurer {
        override func describe(_ receiver: Int32) -> String { "swift-int" }
        override func describe(_ receiver: String) -> String { "swift-string" }
    }
    let value = SwiftOpenMeasurer()
    #expect(describeInt(value: value, x: 1) == "swift-int")
    #expect(describeString(value: value, s: "s") == "swift-string")
    #expect(describeInt(value: OpenMeasurer(), x: 1) == "kotlin-int")
}
