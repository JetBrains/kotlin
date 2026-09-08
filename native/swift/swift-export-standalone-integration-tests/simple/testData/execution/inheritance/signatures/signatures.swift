import Inheritance
import Testing

@Test
func swiftOverridesEachKotlinOverloadSeparately() throws {
    // KT-87875: reverse bridges used to be bound to the method with a matching NAME, so all `pick`
    // overloads competed for one vtable slot (and the final `pick()` had none). Each override must
    // now be reached through the Kotlin driver that calls that exact overload.
    class SwiftOverloads: Overloads {
        override func pick(arg1: String) -> String {
            return "swift-pick(\(arg1))"
        }
        override func pick(arg1: String, arg2: Int32) -> String {
            return "swift-pick(\(arg1), \(arg2))"
        }
        override func same(arg: String) -> String {
            return "swift-same-string(\(arg))"
        }
        override func same(arg: Int32) -> String {
            return "swift-same-int(\(arg))"
        }
    }
    let o = SwiftOverloads()

    #expect(callPick1(o: o, arg1: "a") == "swift-pick(a)")
    #expect(callPick2(o: o, arg1: "b", arg2: 1) == "swift-pick(b, 1)")
    // Overloads of the same arity, told apart by their parameter types only.
    #expect(callSameString(o: o, arg: "c") == "swift-same-string(c)")
    #expect(callSameInt(o: o, arg: 2) == "swift-same-int(2)")
    // The final overload keeps the Kotlin implementation.
    #expect(o.pick() == "kotlin-final")

    // A plain Kotlin instance is unaffected.
    let k = Overloads()
    #expect(callPick1(o: k, arg1: "a") == "kotlin-pick(a)")
    #expect(callPick2(o: k, arg1: "b", arg2: 1) == "kotlin-pick(b, 1)")
    #expect(callSameString(o: k, arg: "c") == "kotlin-same-string(c)")
    #expect(callSameInt(o: k, arg: 2) == "kotlin-same-int(2)")
}

@Test
func swiftOverridesEachKotlinInterfaceOverloadSeparately() throws {
    // Same as above for overloads declared in an interface, whose reverse bridges are installed into
    // the interface table rather than the vtable.
    class SwiftSpeaker: OverloadedSpeakerBase {
        override func say() -> String {
            return "swift-say"
        }
        override func say(times: Int32) -> String {
            return "swift-say(\(times))"
        }
    }
    let s = SwiftSpeaker()

    #expect(callSay(s: s) == "swift-say")
    #expect(callSayTimes(s: s, times: 3) == "swift-say(3)")

    let k = OverloadedSpeakerBase()
    #expect(callSay(s: k) == "kotlin-say")
    #expect(callSayTimes(s: k, times: 3) == "kotlin-say(3)")
}

@Test
func kotlinDefaultsReachSwiftOverrideWithRichSignature() throws {
    class SwiftDefaultSignature: DefaultSignatureBase {
        override func format(
            prefix: String,
            payload: DataPayload,
            mode: InheritanceMode,
            note: String?,
            `repeat`: Int32
        ) -> String {
            let modeText = mode == .kotlinMode ? "K" : "S"
            return "\(prefix)|\(payload.text)|\(payload.number)|\(modeText)|\(note ?? "nil")|\(`repeat`)"
        }
    }

    let value = SwiftDefaultSignature()
    #expect(callFormatWithKotlinDefaults(value: value, prefix: "defaults") == "defaults|default-payload|7|K|nil|2")

    let explicit = callFormatExplicitly(
        value: value,
        prefix: "explicit",
        payload: DataPayload(text: "payload", number: 9),
        mode: .swiftMode,
        note: "note",
        repeat: 4
    )
    #expect(explicit == "explicit|payload|9|S|note|4")
}

@Test
func overloadedReverseBridgesSelectTheSwiftImplementation() throws {
    class SwiftOverloadedSignature: OverloadedSignatureBase {
        override func choose(value: String) -> String { "swift-one:\(value)" }
        override func choose(value: String, count: Int32) -> String { "swift-two:\(value):\(count)" }
        override func choose(value: String, count: Int32, mode: InheritanceMode) -> String {
            "swift-three:\(value):\(count):\(mode == .swiftMode ? "S" : "K")"
        }
    }

    let value = SwiftOverloadedSignature()
    #expect(callChooseOne(value: value, text: "a") == "swift-one:a")
    #expect(callChooseTwo(value: value, text: "b", count: 2) == "swift-two:b:2")
    #expect(callChooseThree(value: value, text: "c", count: 3, mode: .swiftMode) == "swift-three:c:3:S")
}
